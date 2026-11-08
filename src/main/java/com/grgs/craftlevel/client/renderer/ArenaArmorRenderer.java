package com.grgs.craftlevel.client.renderer;

import com.grgs.craftlevel.client.model.ArenaArmorModel;
import com.grgs.craftlevel.item.ArenaGeoArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Renderer GeckoLib da armadura de arena. */
public class ArenaArmorRenderer extends GeoArmorRenderer<ArenaGeoArmorItem> {

    public ArenaArmorRenderer() {
        super(new ArenaArmorModel());
    }
}
