package io.github.sudabake.appliedagristics.biology;

import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Biological identity belongs to a stack, never to its translated display name. */
@EventBusSubscriber(modid = Agristics.ID)
public final class BioData {
    private BioData() {}
    public static ResourceLocation identity(ItemStack stack) { return stack.get(Agristics.BIO_ID.get()); }
    public static boolean seed(ItemStack stack) { return stack.is(Agristics.BIO_SEED.get()) && identified(stack); }
    public static ResourceLocation cropId(ResourceLocation entity) { return ResourceLocation.fromNamespaceAndPath(Agristics.ID, "bio/" + entity.getNamespace() + "/" + entity.getPath()); }
    public static ResourceLocation entityId(ResourceLocation crop) {
        if (!crop.getNamespace().equals(Agristics.ID) || !crop.getPath().startsWith("bio/")) return null;
        var parts = crop.getPath().substring(4).split("/", 2); return parts.length == 2 ? ResourceLocation.tryParse(parts[0] + ":" + parts[1]) : null;
    }
    public static boolean identified(ItemStack stack) {
        var id = identity(stack);
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }
    public static ItemStack withIdentity(net.minecraft.world.level.ItemLike item, ResourceLocation id) {
        var stack = new ItemStack(item); stack.set(Agristics.BIO_ID.get(), id); return stack;
    }
    public static InteractionResult sample(Player player, InteractionHand hand, Entity target) {
        var held = player.getItemInHand(hand);
        if (!held.is(Agristics.SAMPLER.get())) return InteractionResult.PASS;
        if (target instanceof net.neoforged.neoforge.entity.PartEntity<?> part) target = part.getParent();
        if (!(target instanceof Mob) || !target.isAlive() || identity(held) != null || player.isSpectator()) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            var sample = withIdentity(Agristics.SAMPLER.get(), BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
            // Even in creative, split off exactly one blank swab; never label the whole stack.
            held.shrink(1);
            if (held.isEmpty()) player.setItemInHand(hand, sample);
            else if (!player.getInventory().add(sample)) player.drop(sample, false);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(Agristics.SAMPLER.get())) return;
        var result=sample(event.getEntity(),event.getHand(),event.getTarget());
        if(result.consumesAction()) { event.setCancellationResult(result);event.setCanceled(true); }
    }
    @SubscribeEvent public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getItemStack().is(Agristics.SAMPLER.get())) return;
        var result=sample(event.getEntity(),event.getHand(),event.getTarget());
        if(result.consumesAction()) { event.setCancellationResult(result);event.setCanceled(true); }
    }
}
