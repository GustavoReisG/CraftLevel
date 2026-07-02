package com.grgs.craftlevel.scaling;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.util.DebugLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry em memória dos escaladores declarados em config.json → scaling.
 * <p>
 * Carregado no startup e em /craftlevel reload. Consultado por stats refresh e combate.
 */
public final class ScalingRegistry {

    private static final Map<String, ScalingDefinition> SCALERS = new HashMap<>();
    private static ScalingDefinition lpScaling = new ScalingDefinition(100, 1.06, ScalingMode.POWER, ApplyMode.OVERWRITE);
    private static final Set<String> WARNED_MISSING = Collections.newSetFromMap(new HashMap<>());

    private ScalingRegistry() {}

    public static void load(Map<String, ScalingDefinition> scalers, ScalingDefinition lp) {
        SCALERS.clear();
        SCALERS.putAll(scalers);
        lpScaling = lp;
        WARNED_MISSING.clear();
        CraftLevel.LOGGER.info("[CraftLevel] Loaded {} scaler(s)", SCALERS.size());
        DebugLog.info("Escaladores carregados: {}", SCALERS.keySet());
    }

    /** Escalador usado para calcular LP necessário por level (lp_scaling). */
    public static ScalingDefinition getLpScaling() {
        return lpScaling;
    }

    public static ScalingDefinition get(String name) {
        return SCALERS.get(name);
    }

    /** Retorna escalador ou registra aviso (sem crash) se não existir. */
    public static ScalingDefinition require(String name, String context) {
        ScalingDefinition def = SCALERS.get(name);
        if (def == null) {
            warnMissing(name, context);
        }
        return def;
    }

    private static void warnMissing(String name, String context) {
        String key = name + "|" + context;
        if (WARNED_MISSING.add(key)) {
            CraftLevel.LOGGER.warn("[CraftLevel] Scaler '{}' not found ({})", name, context);
        }
    }
}
