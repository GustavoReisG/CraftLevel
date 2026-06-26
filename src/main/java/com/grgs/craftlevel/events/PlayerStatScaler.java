package com.grgs.craftlevel.events;

import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Aplica scaling de atributos do jogador com base no level (vida, etc.). */
public class PlayerStatScaler {

    private static final UUID MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String MODIFIER_NAME = "craftlevel_player_bonus";

    /** Percorre player_stats.json > attribute e aplica um modifier por atributo configurado. */
    public static void apply(Player player, int level) {
        for (String attributeId : ConfigParser.getPlayerAttributeIds()) {
            try {
                Attribute attr = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(attributeId));
                if (attr == null) continue;

                var instance = player.getAttribute(attr);
                if (instance == null) continue;

                instance.removeModifier(MODIFIER_UUID);

                double scaling = ConfigParser.getPlayerAttributeScaling(attributeId);
                double multiplier = ConfigParser.getScaledMultiplier(scaling, level);
                double base = instance.getBaseValue();
                double bonus = (base * multiplier) - base;

                if (bonus != 0) {
                    instance.addPermanentModifier(new AttributeModifier(
                            MODIFIER_UUID,
                            MODIFIER_NAME,
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    ));
                }

                if (attr == Attributes.MAX_HEALTH) {
                    player.setHealth((float) instance.getValue());
                }

            } catch (Exception ignored) {
            }
        }
    }
}
