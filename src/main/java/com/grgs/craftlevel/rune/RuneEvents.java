package com.grgs.craftlevel.rune;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.damage.ModDamageTypes;
import com.grgs.craftlevel.item.ItemRunes;
import com.grgs.craftlevel.item.RuneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/** Efeitos das runas: Veloce, Reach, Fortune, Return, Unbreakable, tooltips. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class RuneEvents {

    private static final UUID VELOCE_SPEED_ID = UUID.fromString("a3c8f1e0-7b2d-4c91-9e55-11f0a2b3c4d5");
    private static final UUID VELOCE_ATTACK_ID = UUID.fromString("b4d9f2e1-8c3e-5d02-af66-2201b3c4d5e6");
    private static final UUID REACH_BLOCK_ID = UUID.fromString("c5e0a3f2-9d4f-6e13-b077-3302c4d5e6f7");
    private static final UUID REACH_ENTITY_ID = UUID.fromString("d6f1b4a3-0e5a-7f24-c188-4413d5e6f708");

    private RuneEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        Player player = event.player;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemRunes.clearBrokenIfRepaired(player.getItemBySlot(slot));
        }
        refreshAttributes(player);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        int n = ItemRunes.countActive(player, RuneKind.VELOCE);
        if (n <= 0) {
            return;
        }
        event.setNewSpeed(event.getNewSpeed() * (1.0F + ItemRunes.VELOCE_PER_ITEM * n));
    }

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!ItemRunes.isBroken(stack)) {
            return;
        }
        event.clearModifiers();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        ItemStack weapon = event.getEntity().getMainHandItem();
        if (ItemRunes.isBroken(weapon)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (ItemRunes.isBroken(event.getBow())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurtFromBrokenWeapon(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (event.getSource().getDirectEntity() != player) {
            return;
        }
        if (ItemRunes.isBroken(player.getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    /**
     * Return: +5% chance por peça de devolver o dano ao atacante.
     * O jogador ainda recebe o hit; dano refletido usa damage type próprio (sem loop).
     */
    @SubscribeEvent
    public static void onReturnReflect(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(ModDamageTypes.RETURN_DAMAGE)) {
            return;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker == player || !attacker.isAlive()) {
            return;
        }
        float chance = ItemRunes.returnChance(player);
        if (chance <= 0.0F) {
            return;
        }
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        attacker.hurt(ModDamageTypes.returnDamage(player.level(), player), amount);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<RuneKind> runes = ItemRunes.get(stack);
        if (runes.isEmpty() && !ItemRunes.isBroken(stack)) {
            return;
        }
        List<Component> tip = event.getToolTip();
        if (ItemRunes.isBroken(stack)) {
            tip.add(Component.translatable("item.craftlevel.rune.broken")
                    .withStyle(ChatFormatting.DARK_RED));
        }
        for (RuneKind kind : runes) {
            tip.add(Component.translatable(kind.translationKey())
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static void refreshAttributes(Player player) {
        int veloce = ItemRunes.countActive(player, RuneKind.VELOCE);
        int reach = ItemRunes.countActive(player, RuneKind.REACH);

        setMultiply(player.getAttribute(Attributes.MOVEMENT_SPEED), VELOCE_SPEED_ID, "craftlevel_veloce_speed",
                veloce > 0 ? ItemRunes.VELOCE_PER_ITEM * veloce : 0.0D);
        setMultiply(player.getAttribute(Attributes.ATTACK_SPEED), VELOCE_ATTACK_ID, "craftlevel_veloce_attack",
                veloce > 0 ? ItemRunes.VELOCE_PER_ITEM * veloce : 0.0D);

        setMultiply(player.getAttribute(ForgeMod.BLOCK_REACH.get()), REACH_BLOCK_ID, "craftlevel_reach_block",
                reach > 0 ? ItemRunes.REACH_PER_ITEM * reach : 0.0D);
        setMultiply(player.getAttribute(ForgeMod.ENTITY_REACH.get()), REACH_ENTITY_ID, "craftlevel_reach_entity",
                reach > 0 ? ItemRunes.REACH_PER_ITEM * reach : 0.0D);
    }

    private static void setMultiply(
            AttributeInstance instance,
            UUID id,
            String name,
            double amount
    ) {
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0D) {
            instance.addTransientModifier(new AttributeModifier(
                    id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

}
