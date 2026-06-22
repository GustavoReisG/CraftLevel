package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.ConfigParser;
import com.grgs.craftlevel.events.WeaponScaler;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Substitui a linha vanilla de Attack Damage no tooltip pelo dano real calculado
 * com scaling de level, incluindo encantamentos condicionais como linhas extras.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public class WeaponTooltipHandler {

    /**
     * Roda após o vanilla montar o tooltip — permite remover a linha de dano vanilla
     * e inserir o valor total calculado pelo WeaponScaler.
     */
    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        if (!ConfigParser.enableSystem) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack stack = event.getItemStack();
        int level = getPlayerLevel(player);
        WeaponScaler.WeaponDamageResult result = WeaponScaler.calculate(level, stack);

        if (result.weaponBase() <= 0) return;

        double totalDamage = WeaponScaler.getTotalAttackDamage(player, level, stack);
        if (totalDamage <= 0) return;

        String attackLabel = Component.translatable(Attributes.ATTACK_DAMAGE.getDescriptionId()).getString();
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();

        // Remove linhas vanilla de Attack Damage e guarda onde inserir a nova
        int insertIndex = -1;
        List<Either<FormattedText, TooltipComponent>> filtered = new ArrayList<>();
        for (Either<FormattedText, TooltipComponent> element : elements) {
            if (containsAttackLabel(element, attackLabel)) {
                if (insertIndex < 0) {
                    insertIndex = filtered.size();
                }
                continue;
            }
            filtered.add(element);
        }

        List<Either<FormattedText, TooltipComponent>> replacement = new ArrayList<>();
        replacement.add(Either.left(Component.literal(String.format(Locale.ROOT, "%.1f ", totalDamage))
                .withStyle(ChatFormatting.GREEN)
                .append(Component.translatable(Attributes.ATTACK_DAMAGE.getDescriptionId())
                        .withStyle(ChatFormatting.GREEN))));

        // Linhas extras para Smite, Bane of Arthropods, etc.
        result.conditionalDamage().forEach((entityType, damage) -> {
            if (damage <= 0) return;
            replacement.add(Either.left(Component.literal(String.format(Locale.ROOT, "  %s: +%.1f", entityType, damage))
                    .withStyle(ChatFormatting.DARK_GREEN)));
        });

        if (insertIndex < 0) {
            insertIndex = filtered.size();
        }
        filtered.addAll(insertIndex, replacement);

        elements.clear();
        elements.addAll(filtered);
    }

    /** Prioriza capability; fallback para cache do HUD sincronizado pelo servidor. */
    private static int getPlayerLevel(Player player) {
        var cap = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).resolve();
        if (cap.isPresent() && cap.get().getLevel() > 0) {
            return cap.get().getLevel();
        }
        return CraftLevelHUD.cachedLevel;
    }

    private static boolean containsAttackLabel(Either<FormattedText, TooltipComponent> element, String attackLabel) {
        return element.left()
                .map(text -> formattedTextToString(text).contains(attackLabel))
                .orElse(false);
    }

    private static String formattedTextToString(FormattedText text) {
        if (text instanceof Component component) {
            return component.getString();
        }
        StringBuilder builder = new StringBuilder();
        text.visit(string -> {
            builder.append(string);
            return Optional.empty();
        });
        return builder.toString();
    }
}
