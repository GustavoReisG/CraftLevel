package com.grgs.craftlevel.item;

import com.grgs.craftlevel.client.PlayerEmeraldBankClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Token de Storage que representa um valor fixo no emerald bank. */
public class EmeraldStorageItem extends Item {

    private final int bankValue;

    public EmeraldStorageItem(int bankValue, Properties properties) {
        super(properties);
        this.bankValue = bankValue;
    }

    public int getBankValue() {
        return bankValue;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        long balance = PlayerEmeraldBankClient.getCachedBalance();
        if (balance >= 0L) {
            tooltip.add(Component.translatable("item.craftlevel.emerald_storage.bank_balance", balance)
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.craftlevel.emerald_storage.bank_balance_unknown")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
