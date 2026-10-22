package io.github.sudabake.appliedagristics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Particle behavior adapted from Mystical Agriculture's InfusionAltarTileEntity.
 * Copyright (c) 2018 BlakeBr0. MIT; see third_party/mysticalagriculture/LICENSE.txt. */
public final class InfusionEffects {
    public static void converge(ServerLevel level,BlockPos center,BlockPos pedestal,ItemStack stack) {
        if (stack.isEmpty()) return;
        double x=pedestal.getX()+level.random.nextDouble()*.2+.4;
        double y=pedestal.getY()+level.random.nextDouble()*.2+1.2;
        double z=pedestal.getZ()+level.random.nextDouble()*.2+.4;
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,stack),x,y,z,0,center.getX()-pedestal.getX(),.25,center.getZ()-pedestal.getZ(),.18);
    }
    public static void finishPedestal(ServerLevel level,BlockPos pos) { level.sendParticles(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+1.2,pos.getZ()+.5,20,0,0,0,.1); }
    public static void finishCenter(ServerLevel level,BlockPos pos) { level.sendParticles(ParticleTypes.HAPPY_VILLAGER,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,10,0,0,0,.1); }
    private InfusionEffects() {}
}
