package com.grgs.craftlevel.events;

import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PlayerStatScaler {

    // UUID fixo por mod — garante que sempre removemos o modifier certo
    private static final UUID MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String MODIFIER_NAME = "craftlevel_player_bonus";

    public static void apply(Player player, int level) {
        for (String attributeId : ConfigParser.playerStats.keySet()) {
            try {
                Attribute attr = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(attributeId));
                if (attr == null) continue;

                var instance = player.getAttribute(attr);
                if (instance == null) continue;

                // Remove modifier antigo do CraftLevel se existir
                instance.removeModifier(MODIFIER_UUID);

                ConfigParser.TierValues tiers = ConfigParser.playerStats.getOrDefault(
                        attributeId,
                        new ConfigParser.TierValues(1.0, 1.0, 1.0, 1.0, "addition")
                );

                double multiplier = ConfigParser.getScaledMultiplier(tiers, level);
                double base = ConfigParser.playerBaseStats.getOrDefault(
                        attributeId,
                        instance.getBaseValue()
                );

                double bonus;
                AttributeModifier.Operation operation;

                switch (tiers.operation().toLowerCase()) {
                    case "multiply_total" -> {
                        operation = AttributeModifier.Operation.MULTIPLY_TOTAL;
                        bonus = multiplier - 1; // 2.5 - 1 = 1.5 (150% de bônus)
                    }
                    case "multiply_base" -> {
                        operation = AttributeModifier.Operation.MULTIPLY_BASE;
                        bonus = multiplier - 1;
                    }
                    default -> {
                        operation = AttributeModifier.Operation.ADDITION;
                        bonus = (base * multiplier) - base;
                    }
                }

                if (bonus != 0) {
                    AttributeModifier modifier = new AttributeModifier(
                            MODIFIER_UUID,
                            MODIFIER_NAME,
                            bonus,
                            operation
                    );
                    instance.addPermanentModifier(modifier);
                }

                if (attr == Attributes.MAX_HEALTH) {
                    player.setHealth((float) instance.getValue());
                }

            } catch (Exception ignored) {
            }
        }
    }
}