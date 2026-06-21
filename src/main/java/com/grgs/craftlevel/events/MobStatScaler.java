package com.grgs.craftlevel.events;

import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.UUID;

public class MobStatScaler {

    private static final UUID MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final String MODIFIER_NAME = "craftlevel_mob_bonus";

    public static void apply(Mob mob, int level) {
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();

        for (String attributeId : getAttributeIds(mobId)) {
            try {
                Attribute attr = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(attributeId));
                if (attr == null) continue;

                var instance = mob.getAttribute(attr);
                if (instance == null) continue;

                // Remove modifier antigo
                instance.removeModifier(MODIFIER_UUID);

                ConfigParser.TierValues tiers = ConfigParser.getStatForMob(mobId, attributeId);
                double multiplier = ConfigParser.getScaledMultiplier(tiers, level);
                double base = instance.getBaseValue();

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
                        bonus = (base * multiplier) - base; // base * 2.5 - base = bônus fixo
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
                    mob.setHealth((float) instance.getValue());
                }

            } catch (Exception e) {
                // pula atributo com erro silenciosamente
            }
        }
    }

    private static Iterable<String> getAttributeIds(String mobId) {
        if (ConfigParser.mobOverrides.containsKey(mobId)) {
            return ConfigParser.mobOverrides.get(mobId).keySet();
        }
        return ConfigParser.mobStats.keySet();
    }
}