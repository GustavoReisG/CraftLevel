package com.grgs.craftlevel.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;

import java.util.List;

/**
 * Força render translucent no item (alpha parcial no vidro).
 * Mantém o model normal para o {@code ItemRenderer} centrar com {@code translate(-0.5)}.
 */
public final class TranslucentItemModel extends BakedModelWrapper<BakedModel> {

    private static final List<RenderType> TRANSLUCENT =
            List.of(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));

    public TranslucentItemModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return TRANSLUCENT;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }
}
