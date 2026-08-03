package com.grgs.craftlevel.item;

import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.events.CraftLevelEvents;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.PlayLevelUpSoundPacket;
import com.grgs.craftlevel.scaling.StatRefreshService;
import com.grgs.craftlevel.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Frasco que sobe +1 level em qualquer mob (não player),
 * desde que o level do mob fique estritamente abaixo do level do player.
 */
public class LevelFlaskItem extends Item {

    /** Nome sempre laranja (estilo lendário). */
    private static final Rarity LEGENDARY = Rarity.create("craftlevel_legendary", ChatFormatting.GOLD);

    public LevelFlaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return LEGENDARY;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("item.craftlevel.level_flask.tooltip.use")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.craftlevel.level_flask.tooltip.cap")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.craftlevel.level_flask.tooltip.players")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (target instanceof Player || !(target instanceof Mob mob)) {
            return InteractionResult.FAIL;
        }

        Level level = player.level();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        CraftLevelEvents.refreshMobLevel(mob);

        int playerLevel = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                .map(cap -> Math.max(1, cap.getLevel()))
                .orElse(1);
        int mobLevel = mob.getCapability(MobLevelProvider.MOB_LEVEL)
                .map(cap -> Math.max(0, cap.getLevel()))
                .orElse(0);

        if (mobLevel <= 0 || mobLevel >= playerLevel) {
            level.playSound(
                    null,
                    mob.getX(),
                    mob.getY(),
                    mob.getZ(),
                    ModSounds.LEVEL_FLASK_FAIL.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
            return InteractionResult.FAIL;
        }

        int newLevel = mobLevel + 1;
        mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(cap -> {
            cap.setLevel(newLevel);
            StatRefreshService.refreshMob(mob, newLevel);
        });
        PacketHandler.syncMobLevel(mob);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(serverPlayer, new PlayLevelUpSoundPacket());
        }
        return InteractionResult.SUCCESS;
    }
}
