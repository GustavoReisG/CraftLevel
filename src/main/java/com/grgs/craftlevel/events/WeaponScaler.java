package com.grgs.craftlevel.events;

import com.grgs.craftlevel.capability.IPlayerLevel;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.UUID;

public class WeaponScaler {

    private static final UUID WEAPON_MODIFIER_UUID = UUID.fromString("e1f2a3b4-c5d6-7890-abef-123456789abc");
    private static final String WEAPON_MODIFIER_NAME = "craftlevel_weapon_damage";

    public static void apply(Player player) {
        player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(cap -> {
            int level = cap.getLevel();

            Attribute attr = Attributes.ATTACK_DAMAGE;
            var instance = player.getAttribute(attr);
            if (instance == null) return;

            // Remove o modifier antigo do CraftLevel pra esse atributo
            instance.removeModifier(WEAPON_MODIFIER_UUID);

            ItemStack weapon = player.getMainHandItem();

            // 1. Dano base que a arma define (via attribute modifiers do item)
            double weaponBaseDamage = 0;
            var weaponMods = weapon.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attr);
            for (AttributeModifier mod : weaponMods) {
                if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                    weaponBaseDamage += mod.getAmount();
                }
            }

            // 2. Sharpness — calcula o dano vanilla dele
            int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, weapon);
            double sharpnessDamage = sharpnessLevel > 0 ? (sharpnessLevel * 0.5 + 0.5) : 0;

            // 3. Soma os dois — "dano cru total"
            double totalRawDamage = weaponBaseDamage + sharpnessDamage;

            // 4. Multiplicador do nível pro stat "common" (generic.attack_damage)
            ConfigParser.TierValues commonTiers = ConfigParser.playerStats.getOrDefault(
                    "minecraft:generic.attack_damage",
                    new ConfigParser.TierValues(1.0, 1.0, 1.0, 1.0, "addition")
            );
            double multiplier = ConfigParser.getScaledMultiplier(commonTiers, level);

            // 5. Bônus final = a diferença entre o total escalado e o total original
            double bonus = (totalRawDamage * (multiplier - 1));

            if (bonus > 0) {
                AttributeModifier modifier = new AttributeModifier(
                        WEAPON_MODIFIER_UUID,
                        WEAPON_MODIFIER_NAME,
                        bonus,
                        AttributeModifier.Operation.ADDITION
                );
                instance.addPermanentModifier(modifier);
            }

            // 6. Calcula e guarda os bônus condicionais (Smite, Bane)
            calculateConditionalBonus(cap, weapon, multiplier);
        });
    }

    private static void calculateConditionalBonus(IPlayerLevel cap, ItemStack weapon, double multiplier) {
        int smiteLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, weapon);
        double smiteDamage = smiteLevel > 0 ? (smiteLevel * 2.5) : 0;
        cap.setUndeadBonus((smiteDamage * (multiplier-1)));

        int baneLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, weapon);
        double baneDamage = baneLevel > 0 ? (baneLevel * 2.5) : 0;
        cap.setArthropodBonus((baneDamage * (multiplier-1)));

    }
}