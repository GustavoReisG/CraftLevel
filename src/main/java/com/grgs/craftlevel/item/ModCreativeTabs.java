package com.grgs.craftlevel.item;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Aba criativa do Craft Level. */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CraftLevel.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.craftlevel"))
                    .icon(() -> new ItemStack(ModItems.EMERALD_WALLET.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ANCESTRAL_GEM.get());
                        output.accept(ModItems.IMBUE_FLASK.get());
                        ImbuePotionItem.fillCreative(output, ModItems.IMBUE_POTION.get());
                        output.accept(ModItems.EMERALD_WALLET.get());
                        output.accept(ModBlocks.COMPRESSED_EMERALD_BLOCK.get());
                        output.accept(ModBlocks.PLAYER_GRAVESTONE.get());
                        output.accept(ModBlocks.ALTAR_OF_DEATH.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {}
}
