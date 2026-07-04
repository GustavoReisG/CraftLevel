package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.config.ConfigManager;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Estado editável de client.json na UI.
 * Defaults em {@link ConfigManager#DEFAULT_CLIENT}.
 */
public final class ClientConfigEditorState {

    static final String KEY_HUD = "hud";
    static final String KEY_LP_BAR = "lp_bar";
    static final String KEY_LEVEL_CIRCLE = "level_circle";
    static final String KEY_HEALTH_BAR = "health_bar";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_ANCHOR = "anchor";
    static final String KEY_X = "x";
    static final String KEY_Y = "y";
    static final String KEY_SCALE = "scale";
    static final String KEY_FONT_SCALE = "font_scale";

    public record HudElementValues(String anchor, Integer x, Integer y, Float scale, Float fontScale) {}

    public record HealthBarValues(Boolean enabled, String anchor, Integer x, Integer y, Float scale, Float fontScale) {}

    private static ClientConfigEditorState defaultTemplate;

    private HudElementValues lpBar;
    private HudElementValues levelCircle;
    private HealthBarValues healthBar;

    public ClientConfigEditorState() {
        copyFrom(defaultsTemplate());
    }

    public static ClientConfigEditorState createDefault() {
        return copyOf(defaultsTemplate());
    }

    public static ClientConfigEditorState loadFrom(Path file) {
        ClientConfigEditorState state = createDefault();
        if (!Files.exists(file)) {
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            state.importFrom(json);
        } catch (Exception ignored) {
            // JSON inválido: mantém defaults
        }
        return state;
    }

    void importFrom(JsonObject json) {
        if (!json.has(KEY_HUD) || !json.get(KEY_HUD).isJsonObject()) {
            return;
        }
        JsonObject hud = json.getAsJsonObject(KEY_HUD);
        lpBar = mergeElement(readElement(hud, KEY_LP_BAR, false), lpBar, false);
        levelCircle = mergeElement(readElement(hud, KEY_LEVEL_CIRCLE, true), levelCircle, true);
        healthBar = mergeHealthBar(readHealthBar(hud), healthBar);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonObject hud = new JsonObject();
        hud.add(KEY_LP_BAR, writeElement(resolvedLpBar(), false));
        hud.add(KEY_LEVEL_CIRCLE, writeElement(resolvedLevelCircle(), true));
        hud.add(KEY_HEALTH_BAR, writeHealthBar(resolvedHealthBar()));
        root.add(KEY_HUD, hud);
        return root;
    }

    public HudElementValues lpBar() {
        return lpBar;
    }

    public HudElementValues levelCircle() {
        return levelCircle;
    }

    public void setLpBar(HudElementValues values) {
        lpBar = values;
    }

    public void setLevelCircle(HudElementValues values) {
        levelCircle = values;
    }

    public HealthBarValues healthBar() {
        return healthBar;
    }

    public void setHealthBar(HealthBarValues values) {
        healthBar = values;
    }

    public HudElementValues resolvedLpBar() {
        return mergeElement(lpBar, defaultsTemplate().lpBar, false);
    }

    public HudElementValues resolvedLevelCircle() {
        return mergeElement(levelCircle, defaultsTemplate().levelCircle, true);
    }

    public HealthBarValues resolvedHealthBar() {
        return mergeHealthBar(healthBar, defaultsTemplate().healthBar);
    }

    private static ClientConfigEditorState defaultsTemplate() {
        if (defaultTemplate == null) {
            defaultTemplate = parseDefaultTemplate();
        }
        return defaultTemplate;
    }

    private static ClientConfigEditorState parseDefaultTemplate() {
        ClientConfigEditorState state = new ClientConfigEditorState(false);
        try {
            JsonObject json = JsonParser.parseString(ConfigManager.DEFAULT_CLIENT).getAsJsonObject();
            state.importFrom(json);
        } catch (Exception ignored) {
            state.lpBar = new HudElementValues("bottom_left", 0, 0, 0.08f, null);
            state.levelCircle = new HudElementValues("top_left", 0, 0, 0.1f, 2.0f);
            state.healthBar = new HealthBarValues(true, "bottom_center", -50, -48, 0.08f, 0.65f);
        }
        return state;
    }

    private static ClientConfigEditorState copyOf(ClientConfigEditorState source) {
        ClientConfigEditorState state = new ClientConfigEditorState(false);
        state.copyFrom(source);
        return state;
    }

    private ClientConfigEditorState(boolean initializeFromDefaults) {
        if (initializeFromDefaults) {
            copyFrom(defaultsTemplate());
        }
    }

    private void copyFrom(ClientConfigEditorState source) {
        lpBar = source.lpBar;
        levelCircle = source.levelCircle;
        healthBar = source.healthBar;
    }

    private static HudElementValues mergeElement(HudElementValues loaded, HudElementValues defaults, boolean withFontScale) {
        return new HudElementValues(
                loaded.anchor() != null ? loaded.anchor() : defaults.anchor(),
                loaded.x() != null ? loaded.x() : defaults.x(),
                loaded.y() != null ? loaded.y() : defaults.y(),
                loaded.scale() != null ? loaded.scale() : defaults.scale(),
                withFontScale
                        ? (loaded.fontScale() != null ? loaded.fontScale() : defaults.fontScale())
                        : null
        );
    }

    private static HealthBarValues mergeHealthBar(HealthBarValues loaded, HealthBarValues defaults) {
        return new HealthBarValues(
                loaded.enabled() != null ? loaded.enabled() : defaults.enabled(),
                loaded.anchor() != null ? loaded.anchor() : defaults.anchor(),
                loaded.x() != null ? loaded.x() : defaults.x(),
                loaded.y() != null ? loaded.y() : defaults.y(),
                loaded.scale() != null ? loaded.scale() : defaults.scale(),
                loaded.fontScale() != null ? loaded.fontScale() : defaults.fontScale()
        );
    }

    private static HudElementValues readElement(JsonObject hud, String elementKey, boolean withFontScale) {
        if (!hud.has(elementKey) || !hud.get(elementKey).isJsonObject()) {
            return new HudElementValues(null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(elementKey);
        return new HudElementValues(
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE),
                withFontScale ? readOptionalFloat(element, KEY_FONT_SCALE) : null
        );
    }

    private static HealthBarValues readHealthBar(JsonObject hud) {
        if (!hud.has(KEY_HEALTH_BAR) || !hud.get(KEY_HEALTH_BAR).isJsonObject()) {
            return new HealthBarValues(null, null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(KEY_HEALTH_BAR);
        return new HealthBarValues(
                readOptionalBoolean(element, KEY_ENABLED),
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE),
                readOptionalFloat(element, KEY_FONT_SCALE)
        );
    }

    private static JsonObject writeElement(HudElementValues values, boolean withFontScale) {
        JsonObject element = new JsonObject();
        element.addProperty(KEY_ANCHOR, values.anchor());
        element.addProperty(KEY_X, values.x());
        element.addProperty(KEY_Y, values.y());
        element.addProperty(KEY_SCALE, values.scale());
        if (withFontScale && values.fontScale() != null) {
            element.addProperty(KEY_FONT_SCALE, values.fontScale());
        }
        return element;
    }

    private static JsonObject writeHealthBar(HealthBarValues values) {
        JsonObject element = new JsonObject();
        element.addProperty(KEY_ENABLED, values.enabled());
        element.addProperty(KEY_ANCHOR, values.anchor());
        element.addProperty(KEY_X, values.x());
        element.addProperty(KEY_Y, values.y());
        element.addProperty(KEY_SCALE, values.scale());
        element.addProperty(KEY_FONT_SCALE, values.fontScale());
        return element;
    }

    private static Boolean readOptionalBoolean(JsonObject json, String key) {
        try {
            return json.get(key).getAsBoolean();
        } catch (Exception e) {
            return null;
        }
    }

    private static String readOptionalString(JsonObject json, String key) {
        try {
            return json.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer readOptionalInt(JsonObject json, String key) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private static Float readOptionalFloat(JsonObject json, String key) {
        try {
            return json.get(key).getAsFloat();
        } catch (Exception e) {
            return null;
        }
    }
}
