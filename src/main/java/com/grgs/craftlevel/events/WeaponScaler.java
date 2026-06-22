package com.grgs.craftlevel.events;

import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Calcula e aplica scaling de dano de armas e encantamentos com base no level do jogador.
 * Separa bônus (aplicados no atributo) de total (usado no tooltip).
 */
public class WeaponScaler {

    /** UUID fixo para remover/reaplicar o bônus de scaling da arma sem conflitos. */
    private static final UUID WEAPON_BONUS_UUID = UUID.fromString("e1f2a3b4-c5d6-7890-abef-123456789abc");
    private static final String WEAPON_BONUS_NAME = "craftlevel_weapon_bonus_damage";

    /** UUID fixo para o bônus de scaling de encantamentos comuns (ex.: Sharpness). */
    private static final UUID ENCHANTMENT_BONUS_UUID = UUID.fromString("f2a3b4c5-d6e7-8901-bcde-234567890def");
    private static final String ENCHANTMENT_BONUS_NAME = "craftlevel_enchantment_bonus_damage";

    /**
     * Resultado do cálculo de dano da arma.
     * @param weaponBase dano base da arma (atributo do item)
     * @param weaponBonus bônus de scaling só da arma: weaponBase × (mult − 1)
     * @param enchantBonus bônus de scaling só do encantamento: sharpnessBase × (mult − 1)
     * @param totalDamage dano total escalado da arma + encantamentos comuns: (weaponBase + sharpnessBase) × mult
     * @param conditionalDamage dano extra de encantamentos condicionais (Smite, Bane), por tipo de mob
     */
    public record WeaponDamageResult(
            double weaponBase,
            double weaponBonus,
            double enchantBonus,
            double totalDamage,
            Map<String, Double> conditionalDamage
    ) {}

    /**
     * Aplica no jogador apenas os bônus de scaling (sem bases de arma/sharpness).
     * As bases vêm do item equipado e do encantamento vanilla.
     */
    public static void apply(Player player) {
        player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(cap -> {
            Attribute attr = Attributes.ATTACK_DAMAGE;
            var instance = player.getAttribute(attr);
            if (instance == null) return;

            instance.removeModifier(WEAPON_BONUS_UUID);
            instance.removeModifier(ENCHANTMENT_BONUS_UUID);

            ItemStack weapon = player.getMainHandItem();
            WeaponDamageResult result = calculate(cap.getLevel(), weapon);

            // Encantamentos condicionais são aplicados no LivingHurtEvent, não como atributo
            cap.clearConditionalEnchantDamage();
            result.conditionalDamage().forEach(cap::addConditionalEnchantDamage);

            if (result.weaponBonus() > 0) {
                instance.addPermanentModifier(new AttributeModifier(
                        WEAPON_BONUS_UUID,
                        WEAPON_BONUS_NAME,
                        result.weaponBonus(),
                        AttributeModifier.Operation.ADDITION
                ));
            }

            if (result.enchantBonus() > 0) {
                instance.addPermanentModifier(new AttributeModifier(
                        ENCHANTMENT_BONUS_UUID,
                        ENCHANTMENT_BONUS_NAME,
                        result.enchantBonus(),
                        AttributeModifier.Operation.ADDITION
                ));
            }
        });
    }

    /** Calcula bônus (para apply) e total (para tooltip) a partir do level e da arma. */
    public static WeaponDamageResult calculate(int playerLevel, ItemStack weapon) {
        double weaponBaseDamage = getWeaponBaseDamage(weapon);

        double commonEnchantBase = 0;
        Map<String, Double> conditionalDamage = new HashMap<>();

        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(weapon).entrySet()) {
            ResourceLocation enchantId = BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey());
            if (enchantId == null) continue;

            ConfigParser.EnchantmentConfig config = ConfigParser.getEnchantmentConfig(enchantId.toString());
            if (config == null) continue;

            int enchantLevel = entry.getValue();
            double enchantDamage = config.baseDamage() + (config.levelDamage() * enchantLevel);

            if (config.entityType() == null) {
                // Encantamento comum (Sharpness) — entra no scaling de attack_damage
                commonEnchantBase += enchantDamage;
            } else {
                // Encantamento condicional (Smite, Bane) — dano extra só contra certos mobs
                ConfigParser.TierValues tiers = ConfigParser.playerStats.getOrDefault(
                        config.attributeScaling(),
                        new ConfigParser.TierValues(1.0, 1.0, 1.0, 1.0, "addition")
                );
                double scaledDamage = enchantDamage * ConfigParser.getScaledMultiplier(tiers, playerLevel);
                conditionalDamage.merge(config.entityType(), scaledDamage, Double::sum);
            }
        }

        ConfigParser.TierValues attackTiers = ConfigParser.playerStats.getOrDefault(
                "minecraft:generic.attack_damage",
                new ConfigParser.TierValues(1.0, 1.0, 1.0, 1.0, "addition")
        );
        double attackMultiplier = ConfigParser.getScaledMultiplier(attackTiers, playerLevel);

        double weaponBonus = weaponBaseDamage * (attackMultiplier - 1);
        double enchantBonus = commonEnchantBase * (attackMultiplier - 1);
        double totalDamage = (weaponBaseDamage + commonEnchantBase) * attackMultiplier;

        return new WeaponDamageResult(weaponBaseDamage, weaponBonus, enchantBonus, totalDamage, conditionalDamage);
    }

    /** Dano total exibido no tooltip: base do jogador escalada + dano escalado da arma/encantamentos. */
    public static double getTotalAttackDamage(Player player, int playerLevel, ItemStack weapon) {
        WeaponDamageResult result = calculate(playerLevel, weapon);

        var attackAttribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        double playerBase = ConfigParser.playerBaseStats.getOrDefault(
                "minecraft:generic.attack_damage",
                attackAttribute != null ? attackAttribute.getBaseValue() : 1.0
        );

        ConfigParser.TierValues attackTiers = ConfigParser.playerStats.getOrDefault(
                "minecraft:generic.attack_damage",
                new ConfigParser.TierValues(1.0, 1.0, 1.0, 1.0, "addition")
        );
        double attackMultiplier = ConfigParser.getScaledMultiplier(attackTiers, playerLevel);

        return (playerBase * attackMultiplier) + result.totalDamage();
    }

    /** Soma os modifiers de ATTACK_DAMAGE do item na mão principal (só operação ADDITION). */
    private static double getWeaponBaseDamage(ItemStack weapon) {
        Attribute attr = Attributes.ATTACK_DAMAGE;
        double weaponBaseDamage = 0;

        for (AttributeModifier mod : weapon.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attr)) {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                weaponBaseDamage += mod.getAmount();
            }
        }

        return weaponBaseDamage;
    }
}
