package com.grgs.craftlevel.events;

import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.UUID;

/** Aplica scaling de atributos nos mobs com base no level deles. */
public class MobStatScaler {

    private static final UUID MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final String MODIFIER_NAME = "craftlevel_mob_bonus";

    /** Usa mob_overrides.json se existir, senão mob_stats.json > attribute. */
    public static void apply(Mob mob, int level) {
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();

        for (String attributeId : ConfigParser.getMobAttributeIds(mobId)) {
            try {
                Attribute attr = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(attributeId));
                if (attr == null) continue;

                var instance = mob.getAttribute(attr);
                if (instance == null) continue;

                instance.removeModifier(MODIFIER_UUID);

                double scaling = ConfigParser.getMobAttributeScaling(mobId, attributeId);
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
                    mob.setHealth((float) instance.getValue());
                }

            } catch (Exception e) {
                // pula atributo com erro silenciosamente
            }
        }
    }
}
