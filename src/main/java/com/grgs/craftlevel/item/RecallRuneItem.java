package com.grgs.craftlevel.item;

import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import com.grgs.craftlevel.skill.RecallSkill;
import com.grgs.craftlevel.skill.SkillCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/** Runa de Recall: sneak + face TOP define o ponto; segurar teleporta. */
public class RecallRuneItem extends Item {

    private static final String TAG_DIM = "RecallDim";
    private static final String TAG_X = "RecallX";
    private static final String TAG_Y = "RecallY";
    private static final String TAG_Z = "RecallZ";

    public RecallRuneItem(Properties properties) {
        super(properties);
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null
                && tag.contains(TAG_DIM, Tag.TAG_STRING)
                && tag.contains(TAG_X, Tag.TAG_INT);
    }

    public static Optional<ResourceKey<Level>> boundDimension(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return Optional.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_DIM));
        if (id == null) {
            return Optional.empty();
        }
        return Optional.of(ResourceKey.create(Registries.DIMENSION, id));
    }

    public static Optional<BlockPos> boundPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_X, Tag.TAG_INT)) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z)));
    }

    public static void bind(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_DIM, dimension.location().toString());
        tag.putInt(TAG_X, pos.getX());
        tag.putInt(TAG_Y, pos.getY());
        tag.putInt(TAG_Z, pos.getZ());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isBound(stack) || super.isFoil(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return RecallSkill.CHARGE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (context.getClickedFace() != Direction.UP) {
            if (!context.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("item.craftlevel.recall_rune.need_top"),
                        true
                );
            }
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos standOn = context.getClickedPos().above();
        if (!context.getLevel().isClientSide()) {
            bind(stack, context.getLevel().dimension(), standOn);
            context.getLevel().playSound(
                    null,
                    standOn,
                    SoundEvents.LODESTONE_COMPASS_LOCK,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.1F
            );
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable(
                                "item.craftlevel.recall_rune.bound",
                                standOn.getX(),
                                standOn.getY(),
                                standOn.getZ()
                        ),
                        true
                );
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!isBound(stack)) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("item.craftlevel.recall_rune.unbound"),
                        true
                );
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            int remaining = SyncPlayerGloryPacket.ClientGloryState.remainingCooldownTicks(
                    SkillCatalog.RECALL,
                    level.getGameTime()
            );
            if (remaining > 0) {
                return InteractionResultHolder.fail(stack);
            }
        } else if (player instanceof ServerPlayer serverPlayer && !RecallSkill.isReady(serverPlayer)) {
            long now = serverPlayer.serverLevel().getGameTime();
            int remaining = serverPlayer.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                    .map(glory -> (int) Math.ceil(
                            (glory.getCooldownReadyAt(SkillCatalog.RECALL) - now) / 20.0
                    ))
                    .orElse(0);
            serverPlayer.displayClientMessage(
                    Component.translatable("gui.craftlevel.skill.cooldown", Math.max(1, remaining)),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.PORTAL_TRIGGER,
                    SoundSource.PLAYERS,
                    0.45F,
                    0.85F
            );
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (level.isClientSide() || remainingUseTicks % 16 != 0) {
            return;
        }
        level.playSound(
                null,
                entity.blockPosition(),
                SoundEvents.PORTAL_AMBIENT,
                SoundSource.PLAYERS,
                0.25F,
                0.7F + (1.0F - remainingUseTicks / (float) RecallSkill.CHARGE_TICKS) * 0.6F
        );
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return stack;
        }
        if (!RecallSkill.isReady(player) || !isBound(stack)) {
            return stack;
        }
        Optional<ResourceKey<Level>> dim = boundDimension(stack);
        Optional<BlockPos> pos = boundPos(stack);
        if (dim.isEmpty() || pos.isEmpty()) {
            return stack;
        }
        RecallSkill.teleport(player, dim.get(), pos.get());
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("item.craftlevel.recall_rune.tooltip.bind")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.craftlevel.recall_rune.tooltip.hold")
                .withStyle(ChatFormatting.GRAY));
        Optional<BlockPos> pos = boundPos(stack);
        if (pos.isPresent()) {
            BlockPos bound = pos.get();
            tooltip.add(Component.translatable(
                    "item.craftlevel.recall_rune.tooltip.bound",
                    bound.getX(),
                    bound.getY(),
                    bound.getZ()
            ).withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.craftlevel.recall_rune.tooltip.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
