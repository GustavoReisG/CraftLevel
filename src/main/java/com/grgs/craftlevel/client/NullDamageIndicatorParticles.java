package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Suprime partículas vanilla DAMAGE_INDICATOR (ícone de dano ao receber hit). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NullDamageIndicatorParticles {

    private NullDamageIndicatorParticles() {}

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleTypes.DAMAGE_INDICATOR, spriteSet -> createProvider());
    }

    private static ParticleProvider<SimpleParticleType> createProvider() {
        return (options, level, x, y, z, xSpeed, ySpeed, zSpeed) -> null;
    }
}
