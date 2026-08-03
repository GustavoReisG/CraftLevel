package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Toca o som de level up no máximo um por vez.
 * Volume controlado pelo slider {@link SoundSource#PLAYERS} nas opções do Minecraft.
 * Se já estiver tocando, pedidos novos são ignorados (não acumulam).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class LevelUpSoundQueue {

    /** Duração do ogg (~5.36s) + folga; fallback se isActive falhar. */
    private static final long SOUND_DURATION_MS = 5500L;

    private static SoundInstance current;
    private static long currentEndsAtMs;

    private LevelUpSoundQueue() {}

    /** Toca o som só se nenhum outro level-up estiver em execução. */
    public static void tryPlay() {
        clearIfFinished();
        if (current != null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null) {
            return;
        }

        current = new SimpleSoundInstance(
                ModSounds.LEVEL_UP.get().getLocation(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true
        );
        currentEndsAtMs = System.currentTimeMillis() + SOUND_DURATION_MS;
        mc.getSoundManager().play(current);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            clearIfFinished();
        }
    }

    private static void clearIfFinished() {
        if (current == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean finished = mc.getSoundManager() == null
                || !mc.getSoundManager().isActive(current)
                || System.currentTimeMillis() >= currentEndsAtMs;
        if (finished) {
            current = null;
        }
    }
}
