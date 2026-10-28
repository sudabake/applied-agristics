package io.github.sudabake.appliedagristics.biology;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Slow food restoration without saturation, healing or amplifier multiplication. */
public final class NourishmentEffect extends MobEffect {
    public NourishmentEffect() { super(MobEffectCategory.BENEFICIAL,0x9bc565); }
    @Override public boolean shouldApplyEffectTickThisTick(int ticks,int amplifier) { return ticks%100==0; }
    @Override public boolean applyEffectTick(LivingEntity entity,int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) player.getFoodData().eat(1,0);
        return true;
    }
}
