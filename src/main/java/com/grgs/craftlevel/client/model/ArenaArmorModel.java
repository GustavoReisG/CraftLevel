package com.grgs.craftlevel.client.model;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.item.ArenaGeoArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Geo compartilhado da Arena Armor (helmet, chestplate, leggings, boots). */
public class ArenaArmorModel extends GeoModel<ArenaGeoArmorItem> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(CraftLevel.MODID, "geo/armor/arena_armor.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CraftLevel.MODID, "textures/item/armor/arena_armor_top.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(CraftLevel.MODID, "animations/armor/arena_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(ArenaGeoArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ArenaGeoArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ArenaGeoArmorItem animatable) {
        return ANIMATION;
    }
}
