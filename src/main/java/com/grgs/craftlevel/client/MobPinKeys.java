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
    public static KeyMapping OPEN_SKILLS;
    public static KeyMapping SPELL_HOTBAR;

    private MobPinKeys() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        PIN_MOB = new KeyMapping(
                "key.craftlevel.pin_mob",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_E,
                CATEGORY
        );
        OPEN_SKILLS = new KeyMapping(
                "key.craftlevel.open_skills",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                CATEGORY
        );
        SPELL_HOTBAR = new KeyMapping(
                "key.craftlevel.spell_hotbar",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Q,
                CATEGORY
        );
        event.register(PIN_MOB);
        event.register(OPEN_SKILLS);
        event.register(SPELL_HOTBAR);
    }
}
