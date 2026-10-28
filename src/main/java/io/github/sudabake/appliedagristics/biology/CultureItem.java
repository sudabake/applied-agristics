package io.github.sudabake.appliedagristics.biology;

import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.*;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** One identified culture per projectile, using vanilla throwing and dispenser behavior. */
public final class CultureItem extends BioItem implements ProjectileItem {
    public CultureItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var held=player.getItemInHand(hand);
        if (!BioData.identified(held)) return InteractionResultHolder.pass(held);
        if (!level.isClientSide) {
            var bottle=new ThrownCulture(Agristics.THROWN_CULTURE.get(),level);
            bottle.setOwner(player); bottle.setPos(player.getX(),player.getEyeY()-.1,player.getZ()); bottle.setItem(held);
            bottle.shootFromRotation(player,player.getXRot(),player.getYRot(),-20,0.7f,1);
            if (!level.addFreshEntity(bottle)) return InteractionResultHolder.fail(held);
            held.consume(1,player);
        }
        level.playSound(player,player.getX(),player.getY(),player.getZ(),SoundEvents.SPLASH_POTION_THROW,SoundSource.PLAYERS,.5f,1);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(held,level.isClientSide);
    }
    @Override public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        var bottle=new ThrownCulture(Agristics.THROWN_CULTURE.get(),level);
        bottle.setPos(pos.x(),pos.y(),pos.z()); bottle.setItem(stack); return bottle;
    }
    @Override public DispenseConfig createDispenseConfig() { return DispenseConfig.builder().power(.7f).uncertainty(1).build(); }
}
