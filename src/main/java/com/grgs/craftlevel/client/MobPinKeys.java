package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MobPinKeys {

    public static final String CATEGORY = "key.categories.craftlevel";
    public static KeyMapping PIN_MOB;

    private MobPinKeys() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        PIN_MOB = new KeyMapping(
                "key.craftlevel.pin_mob",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_E,
                CATEGORY
        );
        event.register(PIN_MOB);
    }
}
