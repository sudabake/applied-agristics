package io.github.sudabake.appliedagristics.testing;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.biology.BioData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Optional installed-EAEP UI regression. Uses its real widgets and network packets. */
public final class EaepClientChecks {
    private static final BlockPos PROVIDER = new BlockPos(36,100,10);
    private static int ticks;
    private static boolean selected;
    private static boolean resumed;
    private static volatile boolean dispatched;
    private static volatile boolean passed;

    private static Object field(Object owner,String name) throws ReflectiveOperationException {
        for(var type=owner.getClass();type!=null;type=type.getSuperclass()) {
            try { var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner); }
            catch(NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    public static boolean tick(Minecraft mc) throws ReflectiveOperationException {
        if(!Boolean.getBoolean("appliedagristics.eaepSmoke")) return true;
        ticks++;
        if(ticks==1) mc.getSingleplayerServer().execute(()-> {
            var level=mc.getSingleplayerServer().overworld();
            for(var pos:List.of(PROVIDER,PROVIDER.east(),PROVIDER.south(),PROVIDER.east().south())) level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(PROVIDER,AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
            level.setBlockAndUpdate(PROVIDER.east(),Agristics.ME_BIO.get().defaultBlockState());
            level.setBlockAndUpdate(PROVIDER.south(),AEBlocks.DRIVE.block().defaultBlockState());
            var drive=(DriveBlockEntity)level.getBlockEntity(PROVIDER.south());
            drive.getInternalInventory().setItemDirect(0,AEItems.ITEM_CELL_1K.stack());
            var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            appeng.api.parts.PartHelper.setPart(level,PROVIDER.east().south(),null,player,
                appeng.core.definitions.AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT));
        });
        if(ticks==60) {
            var button=(Button)field(mc.screen,"eap$uploadBtn");
            button.onPress();
        }
        if(ticks>60 && !selected && mc.screen.getClass().getName().endsWith("ProviderSelectScreen")) {
            @SuppressWarnings("unchecked") var buttons=(List<Button>)field(mc.screen,"entryButtons");
            var button=buttons.stream().filter(b->b.visible && b.active).findFirst();
            if(button.isPresent()) {
                Screenshot.grab(mc.gameDirectory,"agr-eaep-provider-selection.png",mc.getMainRenderTarget(),message->{});
                button.get().onPress();selected=true;
            }
        }
        if(selected && ticks%20==0 && !dispatched) mc.getSingleplayerServer().execute(()-> {
            var level=mc.getSingleplayerServer().overworld();
            var provider=(PatternProviderBlockEntity)level.getBlockEntity(PROVIDER);
            var inventory=provider.getLogic().getPatternInv();
            for(int slot=0;slot<inventory.size();slot++) {
                var pattern=PatternDetailsHelper.decodePattern(inventory.getStackInSlot(slot),level);
                if(pattern==null) continue;
                var expected=AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(),ResourceLocation.parse("minecraft:cow")));
                if(!pattern.getPrimaryOutput().what().equals(expected)) throw new IllegalStateException("EAEP changed biological output identity");
                var counters=new KeyCounter[pattern.getInputs().length];
                for(int i=0;i<counters.length;i++) {
                    counters[i]=new KeyCounter();
                    var input=pattern.getInputs()[i];var stack=input.getPossibleInputs()[0];
                    counters[i].add(stack.what(),stack.amount()*input.getMultiplier());
                }
                dispatched=provider.getLogic().pushPattern(pattern,counters);
                if(!dispatched) throw new IllegalStateException("Uploaded pattern did not dispatch to the cultivator");
                com.mojang.logging.LogUtils.getLogger().info("EAEP_REAL_UPLOAD_PASSED widget=true providerPattern=true dispatch=true species=minecraft:cow");
                break;
            }
        });
        if(dispatched && ticks%20==0) mc.getSingleplayerServer().execute(()-> {
            var drive=(DriveBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(PROVIDER.south());
            var key=AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(),ResourceLocation.parse("minecraft:cow")));
            var storage=drive.getMainNode().getGrid().getStorageService().getInventory();
            passed=storage.extract(key,1,appeng.api.config.Actionable.SIMULATE,appeng.api.networking.security.IActionSource.ofMachine(drive))==1;
            if(ticks%100==0) {
                var machine=(io.github.sudabake.appliedagristics.block.MEBioCultivator)mc.getSingleplayerServer().overworld().getBlockEntity(PROVIDER.east());
                com.mojang.logging.LogUtils.getLogger().info("EAEP_PROCESSING_STATUS status={} state={} output={} sameGrid={} visible={}",machine.status,machine.snapshot(0,""),machine.items.getStackInSlot(3),machine.getMainNode().getGrid()==drive.getMainNode().getGrid(),storage.getAvailableStacks());
            }
        });
        if(dispatched && !resumed) {mc.player.closeContainer();resumed=true;}
        if(passed) {
            com.mojang.logging.LogUtils.getLogger().info("EAEP_REAL_RETURN_PASSED cowSeeds=1 networkReturn=true");
            return true;
        }
        if(ticks>420) throw new IllegalStateException("EAEP actual upload/dispatch timed out, selected="+selected+", dispatched="+dispatched);
        return false;
    }
}
