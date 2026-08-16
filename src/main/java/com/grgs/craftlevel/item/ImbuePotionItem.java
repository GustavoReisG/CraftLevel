package com.grgs.craftlevel.item;

import com.grgs.craftlevel.skill.ImbueMasterSkill;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Poção de embuir: um item, variantes via NBT de poção (como {@code minecraft:potion}).
 * <ul>
 *   <li>{@code tintindex 0} — líquido ({@link PotionUtils#getColor})</li>
 *   <li>{@code tintindex 1} — rolha/tampa ({@code CorkColor} NBT ou marrom padrão)</li>
 * </ul>
 * Uso: mão com a poção + arma/tool na outra mão → animação {@link UseAnim#BRUSH},
 * partículas na cor da poção; ao terminar aplica imbue (base 250, até 500 com Imbue Master; máx. 1000).
 * Só o mesmo ID de poção pode ser reaplicado; água limpa o imbue.
 * Craft: frasco + poção + botão de carvalho (+ tinta opcional na tampa).
 * Vidro translúcido: {@code TranslucentItemModel} no bake (sem BEWLR).
 */
public class ImbuePotionItem extends Item {

    /** NBT da cor da tampa (RGB); ausente = marrom clássico. */
    public static final String CORK_COLOR_TAG = "CorkColor";
    /** Marrom padrão da rolha. */
    public static final int DEFAULT_CORK_COLOR = 5320730;

    /** Duração do brush (~1,6 s). */
    private static final int USE_DURATION = 32;

    public ImbuePotionItem(Properties properties) {
        super(properties);
    }

    /** Cor da tampa (tintindex 1). */
    public static int getCorkColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(CORK_COLOR_TAG, net.minecraft.nbt.Tag.TAG_INT)) {
            return tag.getInt(CORK_COLOR_TAG);
        }
        return DEFAULT_CORK_COLOR;
    }

    @Override
    public ItemStack getDefaultInstance() {
        // Variante “natural” (água): rolha marrom via tintindex 1.
        return PotionUtils.setPotion(super.getDefaultInstance(), Potions.WATER);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(
                PotionUtils.getPotion(stack).getName(getDescriptionId() + ".effect.")
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        PotionUtils.addPotionTooltip(stack, tooltip, 1.0F);
        tooltip.add(Component.translatable("item.craftlevel.imbue_potion.tooltip.use")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        if (ItemImbue.isCleaningPotion(stack)) {
            tooltip.add(Component.translatable("item.craftlevel.imbue_potion.tooltip.clean")
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        } else {
            int amount = ItemImbue.PER_POTION;
            if (level != null && level.isClientSide()) {
                amount = ImbueMasterSkill.amountPerPotion(
                        ImbueMasterSkill.clientSkillLevel()
                );
            }
            tooltip.add(Component.translatable(
                    "item.craftlevel.imbue_potion.tooltip.amount",
                    amount,
                    ItemImbue.MAX
            ).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("item.craftlevel.imbue_potion.tooltip.same_potion")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack potion = player.getItemInHand(hand);
        ItemStack target = otherHandItem(player, hand);

        if (!ItemImbue.canApplyFromPotion(target, potion)) {
            return InteractionResultHolder.fail(potion);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(potion);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) {
            return;
        }
        ItemStack target = otherHandItem(player, entity.getUsedItemHand());
        if (!ItemImbue.isWeaponOrTool(target)) {
            entity.stopUsingItem();
            return;
        }

        if (level.isClientSide() && remainingUseDuration % 2 == 0) {
            spawnImbueParticles(level, entity, stack);
        }

        if (!level.isClientSide() && remainingUseDuration % 5 == 0) {
            level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SoundEvents.BRUSH_GENERIC,
                    SoundSource.PLAYERS,
                    0.35F,
                    0.9F + level.random.nextFloat() * 0.2F
            );
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return stack;
        }

        InteractionHand hand = player.getUsedItemHand();
        ItemStack target = otherHandItem(player, hand);
        boolean cleaning = ItemImbue.isCleaningPotion(stack);

        boolean ok = cleaning
                ? ItemImbue.clear(target)
                : ItemImbue.applyFromPotion(target, stack, ImbueMasterSkill.amountForPlayer(player));
        if (!ok) {
            return stack;
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                cleaning ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_EMPTY,
                SoundSource.PLAYERS,
                1.0F,
                cleaning ? 1.2F : 1.0F
        );

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(
                    LivingEntity entityLiving,
                    InteractionHand hand,
                    ItemStack itemStack
            ) {
                if (!itemStack.isEmpty()
                        && entityLiving.getUsedItemHand() == hand
                        && entityLiving.getUseItemRemainingTicks() > 0) {
                    return HumanoidModel.ArmPose.BRUSH;
                }
                return HumanoidModel.ArmPose.ITEM;
            }
        });
    }

    private static ItemStack otherHandItem(Player player, InteractionHand hand) {
        return player.getItemInHand(
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND
        );
    }

    private static void spawnImbueParticles(Level level, LivingEntity entity, ItemStack stack) {
        int color = PotionUtils.getColor(stack);
        double r = (double) (color >> 16 & 255) / 255.0D;
        double g = (double) (color >> 8 & 255) / 255.0D;
        double b = (double) (color & 255) / 255.0D;

        Vec3 eye = entity.getEyePosition();
        Vec3 look = entity.getViewVector(1.0F);
        HumanoidArm arm = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? entity.getMainArm()
                : entity.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        // Posição aproximada da mão (estilo brush).
        Vec3 sideVec = new Vec3(-look.z, 0.0D, look.x).normalize().scale(side * 0.35D);
        Vec3 pos = eye.add(look.scale(0.55D)).add(sideVec).add(0.0D, -0.25D, 0.0D);

        for (int i = 0; i < 3; i++) {
            double ox = (level.random.nextDouble() - 0.5D) * 0.25D;
            double oy = (level.random.nextDouble() - 0.5D) * 0.25D;
            double oz = (level.random.nextDouble() - 0.5D) * 0.25D;
            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT,
                    pos.x + ox,
                    pos.y + oy,
                    pos.z + oz,
                    r,
                    g,
                    b
            );
        }

        if (level.random.nextInt(3) == 0) {
            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.SPLASH,
                    pos.x,
                    pos.y,
                    pos.z,
                    (level.random.nextDouble() - 0.5D) * 0.1D,
                    0.05D,
                    (level.random.nextDouble() - 0.5D) * 0.1D
            );
        }
    }

    /**
     * Creative: água (“natural”) primeiro, depois as demais poções.
     * Tampa marrom por padrão (sem {@link #CORK_COLOR_TAG}).
     */
    public static void fillCreative(net.minecraft.world.item.CreativeModeTab.Output output, Item item) {
        output.accept(PotionUtils.setPotion(new ItemStack(item), Potions.WATER));
        for (Potion potion : BuiltInRegistries.POTION) {
            if (potion == Potions.EMPTY || potion == Potions.WATER) {
                continue;
            }
            output.accept(PotionUtils.setPotion(new ItemStack(item), potion));
        }
    }
}
