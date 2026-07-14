package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Caminhos do resource pack CraftLevel_Textures. */
@OnlyIn(Dist.CLIENT)
public final class ProfessionTextureLoader {

    private static final ResourceLocation WANDERING_TRADER =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/wandering_trader.png");

    private ProfessionTextureLoader() {}

    public static ResourceLocation textureFor(String professionId, String textureName) {
        ResourceLocation professionKey = ResourceLocation.tryParse(professionId);
        if (professionKey == null || !CraftLevel.MODID.equals(professionKey.getNamespace())) {
            return WANDERING_TRADER;
        }
        return ResourceLocation.fromNamespaceAndPath(
                professionKey.getNamespace(),
                "textures/entity/villager/profession/" + professionKey.getPath() + ".png"
        );
    }

    public static Path texturePath(String textureName) {
        return CraftLevelTexturesPack.packTexturePath(textureName);
    }

    public static void ensureFolder() {
        CraftLevelTexturesPack.ensurePackStructure();
        CraftLevelTexturesPack.seedProfessionTextures();
        openFolder(CraftLevelTexturesPack.professionsFolder());
    }

    private static void openFolder(Path folder) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().open(folder.toFile());
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to open folder {}", folder, e);
        }
    }
}
