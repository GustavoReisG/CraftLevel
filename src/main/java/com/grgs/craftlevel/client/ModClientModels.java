package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ajusta models baked no client: poção de embuir usa {@link TranslucentItemModel}
 * para vidro com alpha sem BEWLR (pivot/centro do ItemRenderer intacto).
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientModels {

    private static final ModelResourceLocation IMBUE_POTION =
            new ModelResourceLocation(new ResourceLocation(CraftLevel.MODID, "imbue_potion"), "inventory");
    private static final ModelResourceLocation IMBUE_FLASK =
            new ModelResourceLocation(new ResourceLocation(CraftLevel.MODID, "imbue_flask"), "inventory");

    private ModClientModels() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        wrapTranslucent(event, IMBUE_POTION);
        wrapTranslucent(event, IMBUE_FLASK);
    }

    private static void wrapTranslucent(ModelEvent.ModifyBakingResult event, ModelResourceLocation id) {
        BakedModel original = event.getModels().get(id);
        if (original != null) {
            event.getModels().put(id, new TranslucentItemModel(original));
        }
    }
}
