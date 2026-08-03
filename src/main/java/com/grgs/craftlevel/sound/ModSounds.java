package com.grgs.craftlevel.sound;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sons do Craft Level.
 * <p>
 * Fail do frasco ainda usa placeholder vanilla em {@code sounds.json}
 * até o .ogg final existir em {@code sounds/item/level_flask_fail.ogg}.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CraftLevel.MODID);

    /** Level up do player e sucesso do frasco no mob. */
    public static final RegistryObject<SoundEvent> LEVEL_UP = register("level_up");

    public static final RegistryObject<SoundEvent> LEVEL_FLASK_FAIL = register("level_flask_fail");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(
                name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CraftLevel.MODID, name))
        );
    }
}
