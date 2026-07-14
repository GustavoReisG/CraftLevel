package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.VillagerConfigFiles;
import com.grgs.craftlevel.config.VillagerParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.LanguageHook;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Gera traduções de profissões craftlevel:* para o formato usado pelo Forge. */
public final class ProfessionLanguages {

    private static final Map<String, String> ENTRIES = new LinkedHashMap<>();
    private static boolean serverHookRegistered;

    private ProfessionLanguages() {}

    public static void refresh() {
        ENTRIES.clear();
        VillagerParser.parseProfessions(VillagerConfigFiles.loadVillagerJson()).forEach((id, def) -> {
            if (!id.startsWith(CraftLevel.MODID + ":")) {
                return;
            }
            ResourceLocation professionId = ResourceLocation.tryParse(id);
            if (professionId == null) {
                return;
            }
            ENTRIES.put(translationKey(professionId), displayName(professionId.getPath()));
        });

        registerServerHookOnce();
        injectClient();

        CraftLevel.LOGGER.info("[CraftLevel] Registered {} profession language(s)", ENTRIES.size());
    }

    public static String translationKey(ResourceLocation professionId) {
        if ("minecraft".equals(professionId.getNamespace())) {
            return "entity.minecraft.villager." + professionId.getPath();
        }
        return "entity.minecraft.villager." + professionId.getNamespace() + "." + professionId.getPath();
    }

    public static String translationKey(String fullId) {
        ResourceLocation professionId = ResourceLocation.tryParse(fullId);
        if (professionId == null) {
            return "entity.minecraft.villager." + ProfessionIds.pathFromInput(fullId);
        }
        return translationKey(professionId);
    }

    public static String displayName(String professionPath) {
        if (professionPath == null || professionPath.isBlank()) {
            return "Profession";
        }
        String[] words = professionPath.replace('-', '_').split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                label.append(word.substring(1).toLowerCase());
            }
        }
        return label.length() > 0 ? label.toString() : "Profession";
    }

    public static Map<String, String> entries() {
        return Collections.unmodifiableMap(ENTRIES);
    }

    private static void registerServerHookOnce() {
        if (serverHookRegistered) {
            return;
        }
        LanguageHook.captureLanguageMap(ENTRIES);
        serverHookRegistered = true;
    }

    private static void injectClient() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        com.grgs.craftlevel.client.ProfessionLanguageClient.inject();
    }
}
