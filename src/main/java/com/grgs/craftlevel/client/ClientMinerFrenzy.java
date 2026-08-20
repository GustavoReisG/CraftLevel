package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import com.grgs.craftlevel.skill.MinerFrenzySkill;
import com.grgs.craftlevel.skill.SkillCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client: BreakSpeed extra do Miner Frenzy (Haste já vem sincronizada do servidor). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class ClientMinerFrenzy {

    private ClientMinerFrenzy() {}

    public static int activeLevel() {
        Long expire = ClientActiveSkills.snapshot().get(SkillCatalog.MINER_FRENZY);
        if (expire == null) {
            return 0;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getGameTime() >= expire) {
            return 0;
        }
        int level = SyncPlayerGloryPacket.ClientGloryState.skillLevel(SkillCatalog.MINER_FRENZY);
        return Math.max(1, level);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || player != mc.player) {
            return;
        }
        int level = activeLevel();
        if (level <= 0) {
            return;
        }
        float extra = MinerFrenzySkill.extraBreakMultiplier(level);
        if (extra <= 1.0001F) {
            return;
        }
        event.setNewSpeed(event.getNewSpeed() * extra);
    }
}
