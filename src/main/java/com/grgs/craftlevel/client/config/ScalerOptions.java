package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Nomes de escaladores definidos em config.json → scaling (sessão de edição + arquivo carregado). */
final class ScalerOptions {

    private ScalerOptions() {}

    static List<String> optionsIncluding(Minecraft minecraft, String currentValue) {
        Set<String> names = new LinkedHashSet<>();

        ConfigPersistence.session().scalers().keySet().forEach(names::add);

        if (ConfigManager.config.has("scaling") && ConfigManager.config.get("scaling").isJsonObject()) {
            ConfigManager.config.getAsJsonObject("scaling").keySet().forEach(names::add);
        }

        if (currentValue != null && !currentValue.isEmpty()) {
            names.add(currentValue);
        }

        if (names.isEmpty()) {
            names.add("");
        }

        return new ArrayList<>(names);
    }

    static String resolveInitial(String currentValue, List<String> options) {
        if (currentValue != null && !currentValue.isEmpty() && options.contains(currentValue)) {
            return currentValue;
        }
        return options.get(0);
    }
}
