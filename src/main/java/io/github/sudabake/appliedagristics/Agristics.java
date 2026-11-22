package io.github.sudabake.appliedagristics;

import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import io.github.sudabake.appliedagristics.network.Messages;
import io.github.sudabake.appliedagristics.multiblock.PlanterLayout;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;

@Mod(Agristics.ID)
public final class Agristics {
    public static final String ID = "appliedagristics";
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> RECIPE_TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,ID);
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeType<io.github.sudabake.appliedagristics.logic.PlantingRecipe>> PLANTING_TYPE=RECIPE_TYPES.register("planting",()->new net.minecraft.world.item.crafting.RecipeType<>() { @Override public String toString() { return ID+":planting"; } });
    public static final RegistryObject<io.github.sudabake.appliedagristics.logic.PlantingRecipe.Serializer> PLANTING_SERIALIZER=RECIPES.register("planting",io.github.sudabake.appliedagristics.logic.PlantingRecipe.Serializer::new);
    public static final RegistryObject<io.github.sudabake.appliedagristics.logic.EmptyMachineRecipe.Serializer> EMPTY_MACHINE_RECIPE = RECIPES.register("empty_machine_upgrade", io.github.sudabake.appliedagristics.logic.EmptyMachineRecipe.Serializer::new);
    public static final RegistryObject<net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<io.github.sudabake.appliedagristics.biology.BioLootRecipe>> BIO_LOOT_RECIPE = RECIPES.register("bio_loot", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(io.github.sudabake.appliedagristics.biology.BioLootRecipe::new));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final RegistryObject<MachineBlock> EARLY = block("early_planter", MachineBlock.Kind.EARLY);
    public static final RegistryObject<MachineBlock> SEEDS = block("seed_storage", MachineBlock.Kind.SEEDS);
    public static final RegistryObject<MachineBlock> NETWORK = block("network_planter", MachineBlock.Kind.NETWORK);
    public static final RegistryObject<MachineBlock> ME_BIO = block("me_bio_cultivator", MachineBlock.Kind.ME_BIO);
    public static final RegistryObject<MachineBlock> NETWORK_ACCESS = block("me_network_access", MachineBlock.Kind.ACCESS);
    public static final RegistryObject<MachineBlock> ME_INFUSION = block("me_infusion_altar", MachineBlock.Kind.ME_INFUSION);
    public static final RegistryObject<MachineBlock> INPUT_PORT = block("planter_input_port", MachineBlock.Kind.INPUT_PORT);
    public static final RegistryObject<MachineBlock> OUTPUT_PORT = block("planter_output_port", MachineBlock.Kind.OUTPUT_PORT);
    public static final RegistryObject<MachineBlock> ESSENCE_VAULT = block("essence_vault", MachineBlock.Kind.ESSENCE_VAULT);
    public static final RegistryObject<PlanterPartBlock> PLANTER_FRAME = part("planter_frame", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.FRAME);
    public static final RegistryObject<PlanterPartBlock> CULTIVATION_CHAMBER = part("cultivation_chamber", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.CHAMBER);
    public static final RegistryObject<PlanterPartBlock> SPEED_MODULE = part("planter_speed_module", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.SPEED);
    public static final RegistryObject<PlanterPartBlock> PARALLEL_MODULE = part("planter_parallel_module", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.PARALLEL);
    public static final RegistryObject<PlanterPartBlock> CONVERSION_MODULE = part("planter_conversion_module", io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.CONVERSION);
    public static final RegistryObject<io.github.sudabake.appliedagristics.biology.BioCropBlock> BIO_CROP = BLOCKS.register("bio_crop", () -> new io.github.sudabake.appliedagristics.biology.BioCropBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WHEAT)));
    public static final RegistryObject<Item> BIO_SEED = ITEMS.register("bio_seed", () -> new io.github.sudabake.appliedagristics.biology.BioSeedItem(BIO_CROP.get(),new Item.Properties()));
    public static final RegistryObject<BlockEntityType<io.github.sudabake.appliedagristics.biology.BioCropEntity>> BIO_CROP_BE=ENTITIES.register("bio_crop",()->BlockEntityType.Builder.of(io.github.sudabake.appliedagristics.biology.BioCropEntity::new,BIO_CROP.get()).build(null));
    public static final RegistryObject<Item> BIO_ESSENCE = ITEMS.register("bio_essence", () -> new io.github.sudabake.appliedagristics.biology.BioItem(new Item.Properties()));
    public static final RegistryObject<Item> SAMPLER = ITEMS.register("bio_sampler", () -> new io.github.sudabake.appliedagristics.biology.BioItem(new Item.Properties()));
    public static final RegistryObject<Item> EMPTY_BIO_SEED = ITEMS.register("empty_bio_seed", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NUTRIENT = ITEMS.register("culture_nutrient", () -> new Item(new Item.Properties()));
    public static final RegistryObject<BlockEntityType<EarlyPlanter>> EARLY_BE = ENTITIES.register("early_planter", () -> BlockEntityType.Builder.of(EarlyPlanter::new, EARLY.get()).build(null));
    public static final RegistryObject<BlockEntityType<SeedStorage>> SEED_BE = ENTITIES.register("seed_storage", () -> BlockEntityType.Builder.of(SeedStorage::new, SEEDS.get()).build(null));
    public static final RegistryObject<BlockEntityType<NetworkPlanter>> NETWORK_BE = ENTITIES.register("network_planter", () -> BlockEntityType.Builder.of(NetworkPlanter::new, NETWORK.get()).build(null));
    public static final RegistryObject<BlockEntityType<MEBioCultivator>> ME_BIO_BE = ENTITIES.register("me_bio_cultivator", () -> BlockEntityType.Builder.of(MEBioCultivator::new, ME_BIO.get()).build(null));
    public static final RegistryObject<BlockEntityType<NetworkAccess>> ACCESS_BE = ENTITIES.register("me_network_access", () -> BlockEntityType.Builder.of(NetworkAccess::new, NETWORK_ACCESS.get()).build(null));
    public static final RegistryObject<BlockEntityType<MEInfusionAltar>> INFUSION_BE = ENTITIES.register("me_infusion_altar", () -> BlockEntityType.Builder.of(MEInfusionAltar::new, ME_INFUSION.get()).build(null));
    public static final RegistryObject<BlockEntityType<ItemPort>> INPUT_BE = ENTITIES.register("planter_input_port", () -> BlockEntityType.Builder.of((pos,state) -> new ItemPort(pos,state,true), INPUT_PORT.get()).build(null));
    public static final RegistryObject<BlockEntityType<ItemPort>> OUTPUT_BE = ENTITIES.register("planter_output_port", () -> BlockEntityType.Builder.of((pos,state) -> new ItemPort(pos,state,false), OUTPUT_PORT.get()).build(null));
    public static final RegistryObject<BlockEntityType<EssenceVault>> VAULT_BE = ENTITIES.register("essence_vault", () -> BlockEntityType.Builder.of(EssenceVault::new, ESSENCE_VAULT.get()).build(null));
    public static final RegistryObject<MenuType<MachineMenu>> MENU = MENUS.register("machine", () -> IForgeMenuType.create(MachineMenu::new));
    static {
        TABS.register("agriculture", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.appliedagristics"))
            .icon(() -> new ItemStack(EARLY.get())).displayItems((params, out) -> ITEMS.getEntries().stream()
                .forEach(i -> out.accept(i.get()))).build());
    }
    private static RegistryObject<MachineBlock> block(String name, MachineBlock.Kind kind) {
        var block = BLOCKS.register(name, () -> {
            var properties = BlockBehaviour.Properties.of().strength(2.5f);
            if (kind == MachineBlock.Kind.EARLY || kind == MachineBlock.Kind.ME_INFUSION || kind == MachineBlock.Kind.ME_BIO) properties.noOcclusion();
            return MachineBlock.create(kind, properties);
        });
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    private static RegistryObject<PlanterPartBlock> part(String name, io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role role) {
        var block = BLOCKS.register(name, () -> {
            var properties = BlockBehaviour.Properties.of().strength(role==PlanterLayout.Role.FRAME ? 1.5f : 2.5f).sound(role==PlanterLayout.Role.CHAMBER ? new net.minecraft.world.level.block.SoundType(1.0f,1.0f,net.minecraft.sounds.SoundEvents.GLASS_BREAK,net.minecraft.sounds.SoundEvents.STONE_STEP,net.minecraft.sounds.SoundEvents.STONE_PLACE,net.minecraft.sounds.SoundEvents.STONE_HIT,net.minecraft.sounds.SoundEvents.STONE_FALL) : net.minecraft.world.level.block.SoundType.STONE);
            if (role == PlanterLayout.Role.CHAMBER) properties.noOcclusion();
            return new PlanterPartBlock(role, properties);
        });
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    public Agristics() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.SPEED_CARD, ME_BIO.get(), 4);
            appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.SPEED_CARD, ME_INFUSION.get(), 4);
        }));
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        TABS.register(bus); RECIPES.register(bus); RECIPE_TYPES.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Balance.SPEC);
        Messages.register();
    }
}
