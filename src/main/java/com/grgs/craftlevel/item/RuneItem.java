package com.grgs.craftlevel.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Placa de pedra com runa — aplicação na smithing table. */
public class RuneItem extends Item {

    private final RuneKind kind;

    public RuneItem(RuneKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public RuneKind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable(kind.tooltipKey()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.craftlevel.rune.tooltip.smithing")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
