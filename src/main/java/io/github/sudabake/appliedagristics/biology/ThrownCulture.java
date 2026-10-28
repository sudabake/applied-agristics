package io.github.sudabake.appliedagristics.biology;

import io.github.sudabake.appliedagristics.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class ThrownCulture extends ThrowableItemProjectile {
    private boolean settled;
    public ThrownCulture(EntityType<? extends ThrownCulture> type,Level level) { super(type,level); }
    @Override protected Item getDefaultItem() { return Agristics.CULTURE.get(); }
    @Override protected void onHit(HitResult hit) { super.onHit(hit); if (hit.getType()!=HitResult.Type.MISS) resolveImpact(); }
    /** Server-only transaction; also used by regression tests to exercise duplicate collision callbacks. */
    public void resolveImpact() {
        if (!(level() instanceof ServerLevel server) || settled || isRemoved()) return;
        settled=true;
        if (BioData.identified(getItem())) {
            for (var player:server.getEntitiesOfClass(Player.class,getBoundingBox().inflate(3),p->!p.isSpectator() && p.distanceToSqr(this)<=9))
                player.addEffect(new MobEffectInstance(Agristics.NOURISHMENT,Balance.NOURISHMENT_TICKS.get(),0));
            var drop=new net.minecraft.world.entity.item.ItemEntity(server,getX(),getY()+.15,getZ(),BioData.withIdentity(Agristics.EXTRACT.get(),BioData.identity(getItem())));
            drop.setDefaultPickUpDelay(); server.addFreshEntity(drop);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER,getX(),getY(),getZ(),20,.6,.4,.6,.02);
            server.playSound(null,blockPosition(),SoundEvents.SPLASH_POTION_BREAK,SoundSource.NEUTRAL,.7f,1);
        }
        discard();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("settled",settled); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); settled=tag.getBoolean("settled"); }
}
