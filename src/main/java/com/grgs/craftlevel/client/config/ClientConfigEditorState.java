package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.config.ConfigDefaults;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Estado editável de client.json na UI.
 * Defaults em {@code assets/craftlevel/defaults/client.json}.
 */
public final class ClientConfigEditorState {

    static final String KEY_HUD = "hud";
    static final String KEY_LP_BAR = "lp_bar";
    static final String KEY_LEVEL_CIRCLE = "level_circle";
    static final String KEY_HEALTH_BAR = "health_bar";
    static final String KEY_HUNGER_BAR = "hunger_bar";
    static final String KEY_EMERALDS = "emeralds";
    static final String KEY_SOULS = "souls";
    static final String KEY_GLORY_KILLS = "glory_kills";
    static final String KEY_MOB_FOCUS = "mob_focus";
    static final String KEY_SKILLS = "skills";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_ANCHOR = "anchor";
    static final String KEY_X = "x";
    static final String KEY_Y = "y";
    static final String KEY_SCALE = "scale";
    static final String KEY_FONT_SCALE = "font_scale";
    static final String KEY_FONT_Y_OFFSET = "font_y_offset";
    static final String KEY_ARMOR_MARGIN = "armor_margin";
    static final String KEY_MOUNT_HEALTH_MARGIN = "mount_health_margin";
    static final String KEY_BREATH_MARGIN = "breath_margin";
    static final String KEY_ICON_SIDE = "icon_side";
    static final String KEY_DIRECTION = "direction";
    static final String KEY_FILL_SENSE = "fill_sense";
    static final String KEY_TEXT_SENSE = "text_sense";
    static final String KEY_MOB_YAW = "mob_yaw";
    static final String KEY_HEAD_YAW = "head_yaw";
    static final String KEY_CAMERA_PITCH = "camera_pitch";

    public static final String[] ICON_SIDES = {"left", "right"};
    public static final String[] MOB_FOCUS_DIRECTIONS = {"data_first", "portrait_first"};
    public static final String[] SKILLS_DIRECTIONS = {"row", "column"};
    public static final String[] SKILLS_FILL_SENSES = {"ascending", "center", "descending"};
    public static final String[] SKILLS_TEXT_SENSES = {"after_icon", "before_icon"};

    public record HudElementValues(
            String anchor, Integer x, Integer y, Float scale, Float fontScale, Float fontYOffset
    ) {}

    public record HealthBarValues(
            Boolean enabled, String anchor, Integer x, Integer y, Float scale, Float fontScale,
            Integer armorMargin
    ) {}

    public record HungerBarValues(
            Boolean enabled, String anchor, Integer x, Integer y, Float scale, Float fontScale,
            Integer breathMargin, Integer mountHealthMargin
    ) {}

    public record EmeraldHudValues(
            Boolean enabled, String anchor, Integer x, Integer y, Float scale, Float fontScale, String iconSide
    ) {}

    public record SoulsHudValues(
            Boolean enabled, String anchor, Integer x, Integer y, Float scale, Float fontScale, String iconSide
    ) {}

    public record GloryKillsHudValues(
            Boolean enabled, String anchor, Integer x, Integer y, Float scale, Float fontScale, String iconSide
    ) {}

    public record MobFocusValues(
            String anchor, Integer x, Integer y, Float scale, Float fontScale, String direction,
            Float mobYaw, Float headYaw, Float cameraPitch
    ) {}

    public record SkillsHudValues(
            Boolean enabled,
            String direction,
            String fillSense,
            String textSense,
            String anchor,
            Integer x,
            Integer y,
            Float scale
    ) {}

    private static ClientConfigEditorState defaultTemplate;

    private HudElementValues lpBar;
    private HudElementValues levelCircle;
    private HealthBarValues healthBar;
    private HungerBarValues hungerBar;
    private EmeraldHudValues emeralds;
    private SoulsHudValues souls;
    private GloryKillsHudValues gloryKills;
    private MobFocusValues mobFocus;
    private SkillsHudValues skills;

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
        lpBar = mergeElement(readElement(hud, KEY_LP_BAR, true), lpBar, true);
        levelCircle = mergeElement(readElement(hud, KEY_LEVEL_CIRCLE, true), levelCircle, true);
        healthBar = mergeHealthBar(readHealthBar(hud), healthBar);
        hungerBar = mergeHungerBar(readHungerBar(hud), hungerBar);
        emeralds = mergeEmeralds(readEmeralds(hud), emeralds);
        souls = mergeSouls(readSouls(hud), souls);
        gloryKills = mergeGloryKills(readGloryKills(hud), gloryKills);
        mobFocus = mergeMobFocus(readMobFocus(hud), mobFocus);
        skills = mergeSkills(readSkills(hud), skills);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonObject hud = new JsonObject();
        hud.add(KEY_LP_BAR, writeElement(resolvedLpBar(), true));
        hud.add(KEY_LEVEL_CIRCLE, writeElement(resolvedLevelCircle(), true));
        hud.add(KEY_HEALTH_BAR, writeHealthBar(resolvedHealthBar()));
        hud.add(KEY_HUNGER_BAR, writeHungerBar(resolvedHungerBar()));
        hud.add(KEY_EMERALDS, writeEmeralds(resolvedEmeralds()));
        hud.add(KEY_SOULS, writeSouls(resolvedSouls()));
        hud.add(KEY_GLORY_KILLS, writeGloryKills(resolvedGloryKills()));
        hud.add(KEY_MOB_FOCUS, writeMobFocus(resolvedMobFocus()));
        hud.add(KEY_SKILLS, writeSkills(resolvedSkills()));
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

    public HungerBarValues hungerBar() {
        return hungerBar;
    }

    public void setHungerBar(HungerBarValues values) {
        hungerBar = values;
    }

    public EmeraldHudValues emeralds() {
        return emeralds;
    }

    public void setEmeralds(EmeraldHudValues values) {
        emeralds = values;
    }

    public SoulsHudValues souls() {
        return souls;
    }

    public void setSouls(SoulsHudValues values) {
        souls = values;
    }

    public GloryKillsHudValues gloryKills() {
        return gloryKills;
    }

    public void setGloryKills(GloryKillsHudValues values) {
        gloryKills = values;
    }

    public MobFocusValues mobFocus() {
        return mobFocus;
    }

    public void setMobFocus(MobFocusValues values) {
        mobFocus = values;
    }

    public SkillsHudValues skills() {
        return skills;
    }

    public void setSkills(SkillsHudValues values) {
        skills = values;
    }

    public HudElementValues resolvedLpBar() {
        return mergeElement(lpBar, defaultsTemplate().lpBar, true);
    }

    public HudElementValues resolvedLevelCircle() {
        return mergeElement(levelCircle, defaultsTemplate().levelCircle, true);
    }

    public HealthBarValues resolvedHealthBar() {
        return mergeHealthBar(healthBar, defaultsTemplate().healthBar);
    }

    public HungerBarValues resolvedHungerBar() {
        return mergeHungerBar(hungerBar, defaultsTemplate().hungerBar);
    }

    public EmeraldHudValues resolvedEmeralds() {
        return mergeEmeralds(emeralds, defaultsTemplate().emeralds);
    }

    public SoulsHudValues resolvedSouls() {
        return mergeSouls(souls, defaultsTemplate().souls);
    }

    public GloryKillsHudValues resolvedGloryKills() {
        return mergeGloryKills(gloryKills, defaultsTemplate().gloryKills);
    }

    public MobFocusValues resolvedMobFocus() {
        return mergeMobFocus(mobFocus, defaultsTemplate().mobFocus);
    }

    public SkillsHudValues resolvedSkills() {
        return mergeSkills(skills, defaultsTemplate().skills);
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
            JsonObject json = ConfigDefaults.readJson("client.json");
            if (json == null) {
                throw new IllegalStateException("missing client.json default");
            }
            state.importFrom(json);
        } catch (Exception ignored) {
            state.lpBar = new HudElementValues("bottom_left", 2, -2, 0.11f, 0.6f, null);
            state.levelCircle = new HudElementValues("top_left", 2, 2, 0.15f, 1.0f, -2.0f);
            state.healthBar = new HealthBarValues(true, "bottom_center", -62, -31, 0.1f, 0.6f, 10);
            state.hungerBar = new HungerBarValues(true, "bottom_center", 60, -31, 0.1f, 0.6f, 10, 0);
            state.emeralds = new EmeraldHudValues(true, "bottom_left", 2, -20, 0.7f, 1.0f, "left");
            state.souls = new SoulsHudValues(true, "top_left", 2, 50, 0.7f, 1.0f, "left");
            state.gloryKills = new GloryKillsHudValues(true, "top_left", 2, 70, 0.7f, 1.0f, "left");
            state.mobFocus = new MobFocusValues("top_left", 50, 8, 0.7f, 0.6f, "portrait_first", 0.6f, 0.0f, -0.2f);
            state.skills = new SkillsHudValues(true, "row", "ascending", "after_icon", "top_right", -8, 40, 0.85f);
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
        hungerBar = source.hungerBar;
        emeralds = source.emeralds;
        souls = source.souls;
        gloryKills = source.gloryKills;
        mobFocus = source.mobFocus;
        skills = source.skills;
    }

    private static HudElementValues mergeElement(HudElementValues loaded, HudElementValues defaults, boolean withFontScale) {
        return new HudElementValues(
                loaded.anchor() != null ? loaded.anchor() : defaults.anchor(),
                loaded.x() != null ? loaded.x() : defaults.x(),
                loaded.y() != null ? loaded.y() : defaults.y(),
                loaded.scale() != null ? loaded.scale() : defaults.scale(),
                withFontScale
                        ? (loaded.fontScale() != null ? loaded.fontScale() : defaults.fontScale())
                        : null,
                withFontScale
                        ? (loaded.fontYOffset() != null ? loaded.fontYOffset() : defaults.fontYOffset())
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
                loaded.fontScale() != null ? loaded.fontScale() : defaults.fontScale(),
                loaded.armorMargin() != null ? loaded.armorMargin() : defaults.armorMargin()
        );
    }

    private static HungerBarValues mergeHungerBar(HungerBarValues loaded, HungerBarValues defaults) {
        return new HungerBarValues(
                loaded.enabled() != null ? loaded.enabled() : defaults.enabled(),
                loaded.anchor() != null ? loaded.anchor() : defaults.anchor(),
                loaded.x() != null ? loaded.x() : defaults.x(),
                loaded.y() != null ? loaded.y() : defaults.y(),
                loaded.scale() != null ? loaded.scale() : defaults.scale(),
                loaded.fontScale() != null ? loaded.fontScale() : defaults.fontScale(),
                loaded.breathMargin() != null ? loaded.breathMargin() : defaults.breathMargin(),
                loaded.mountHealthMargin() != null ? loaded.mountHealthMargin() : defaults.mountHealthMargin()
        );
    }

    private static EmeraldHudValues mergeEmeralds(EmeraldHudValues loaded, EmeraldHudValues defaults) {
        return new EmeraldHudValues(
                loaded.enabled() != null ? loaded.enabled() : defaults.enabled(),
                loaded.anchor() != null ? loaded.anchor() : defaults.anchor(),
                loaded.x() != null ? loaded.x() : defaults.x(),
                loaded.y() != null ? loaded.y() : defaults.y(),
                loaded.scale() != null ? loaded.scale() : defaults.scale(),
                loaded.fontScale() != null ? loaded.fontScale() : defaults.fontScale(),
                loaded.iconSide() != null ? loaded.iconSide() : defaults.iconSide()
        );
    }

    private static SoulsHudValues mergeSouls(SoulsHudValues loaded, SoulsHudValues defaults) {
        return new SoulsHudValues(
                loaded.enabled() != null ? loaded.enabled() : defaults.enabled(),
                loaded.anchor() != null ? loaded.anchor() : defaults.anchor(),
                loaded.x() != null ? loaded.x() : defaults.x(),
                loaded.y() != null ? loaded.y() : defaults.y(),
                loaded.scale() != null ? loaded.scale() : defaults.scale(),
                loaded.fontScale() != null ? loaded.fontScale() : defaults.fontScale(),
                loaded.iconSide() != null ? loaded.iconSide() : defaults.iconSide()
        );
    }

    private static GloryKillsHudValues mergeGloryKills(GloryKillsHudValues loaded, GloryKillsHudValues defaults) {
        return new GloryKillsHudValues(
                loaded.enabled() != null ? loaded.enabled() : defaults.enabled(),
                loaded.anchor() != null ? loaded.anchor() : defaults.anchor(),
                loaded.x() != null ? loaded.x() : defaults.x(),
                loaded.y() != null ? loaded.y() : defaults.y(),
                loaded.scale() != null ? loaded.scale() : defaults.scale(),
                loaded.fontScale() != null ? loaded.fontScale() : defaults.fontScale(),
                loaded.iconSide() != null ? loaded.iconSide() : defaults.iconSide()
        );
    }

    private static MobFocusValues mergeMobFocus(MobFocusValues loaded, MobFocusValues defaults) {
        return new MobFocusValues(
                loaded.anchor() != null ? loaded.anchor() : defaults.anchor(),
                loaded.x() != null ? loaded.x() : defaults.x(),
                loaded.y() != null ? loaded.y() : defaults.y(),
                loaded.scale() != null ? loaded.scale() : defaults.scale(),
                loaded.fontScale() != null ? loaded.fontScale() : defaults.fontScale(),
                loaded.direction() != null ? loaded.direction() : defaults.direction(),
                loaded.mobYaw() != null ? loaded.mobYaw() : defaults.mobYaw(),
                loaded.headYaw() != null ? loaded.headYaw() : defaults.headYaw(),
                loaded.cameraPitch() != null ? loaded.cameraPitch() : defaults.cameraPitch()
        );
    }

    private static SkillsHudValues mergeSkills(SkillsHudValues loaded, SkillsHudValues defaults) {
        SkillsHudValues fallback = defaults != null
                ? defaults
                : new SkillsHudValues(true, "row", "ascending", "after_icon", "top_right", -8, 40, 0.85f);
        if (loaded == null) {
            return fallback;
        }
        return new SkillsHudValues(
                loaded.enabled() != null ? loaded.enabled() : fallback.enabled(),
                loaded.direction() != null ? loaded.direction() : fallback.direction(),
                loaded.fillSense() != null ? loaded.fillSense() : fallback.fillSense(),
                loaded.textSense() != null ? loaded.textSense() : fallback.textSense(),
                loaded.anchor() != null ? loaded.anchor() : fallback.anchor(),
                loaded.x() != null ? loaded.x() : fallback.x(),
                loaded.y() != null ? loaded.y() : fallback.y(),
                loaded.scale() != null ? loaded.scale() : fallback.scale()
        );
    }

    private static HudElementValues readElement(JsonObject hud, String elementKey, boolean withFontScale) {
        if (!hud.has(elementKey) || !hud.get(elementKey).isJsonObject()) {
            return new HudElementValues(null, null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(elementKey);
        return new HudElementValues(
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE),
                withFontScale ? readOptionalFloat(element, KEY_FONT_SCALE) : null,
                withFontScale ? readOptionalFloat(element, KEY_FONT_Y_OFFSET) : null
        );
    }

    private static HealthBarValues readHealthBar(JsonObject hud) {
        if (!hud.has(KEY_HEALTH_BAR) || !hud.get(KEY_HEALTH_BAR).isJsonObject()) {
            return new HealthBarValues(null, null, null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(KEY_HEALTH_BAR);
        return new HealthBarValues(
                readOptionalBoolean(element, KEY_ENABLED),
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE),
                readOptionalFloat(element, KEY_FONT_SCALE),
                readOptionalInt(element, KEY_ARMOR_MARGIN)
        );
    }

    private static HungerBarValues readHungerBar(JsonObject hud) {
        if (!hud.has(KEY_HUNGER_BAR) || !hud.get(KEY_HUNGER_BAR).isJsonObject()) {
            return new HungerBarValues(null, null, null, null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(KEY_HUNGER_BAR);
        Integer mountHealthMargin = readOptionalInt(element, KEY_MOUNT_HEALTH_MARGIN);
        if (mountHealthMargin == null && hud.has(KEY_HEALTH_BAR) && hud.get(KEY_HEALTH_BAR).isJsonObject()) {
            mountHealthMargin = readOptionalInt(hud.getAsJsonObject(KEY_HEALTH_BAR), KEY_MOUNT_HEALTH_MARGIN);
        }
        return new HungerBarValues(
                readOptionalBoolean(element, KEY_ENABLED),
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE),
                readOptionalFloat(element, KEY_FONT_SCALE),
                readOptionalInt(element, KEY_BREATH_MARGIN),
                mountHealthMargin
        );
    }

    private static EmeraldHudValues readEmeralds(JsonObject hud) {
        if (!hud.has(KEY_EMERALDS) || !hud.get(KEY_EMERALDS).isJsonObject()) {
            return new EmeraldHudValues(null, null, null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(KEY_EMERALDS);
        return new EmeraldHudValues(
                readOptionalBoolean(element, KEY_ENABLED),
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE),
                readOptionalFloat(element, KEY_FONT_SCALE),
                readOptionalString(element, KEY_ICON_SIDE)
        );
    }

    private static SoulsHudValues readSouls(JsonObject hud) {
        if (!hud.has(KEY_SOULS) || !hud.get(KEY_SOULS).isJsonObject()) {
            return new SoulsHudValues(null, null, null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(KEY_SOULS);
        return new SoulsHudValues(
                readOptionalBoolean(element, KEY_ENABLED),
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE),
                readOptionalFloat(element, KEY_FONT_SCALE),
                readOptionalString(element, KEY_ICON_SIDE)
        );
    }

    private static GloryKillsHudValues readGloryKills(JsonObject hud) {
        if (!hud.has(KEY_GLORY_KILLS) || !hud.get(KEY_GLORY_KILLS).isJsonObject()) {
            return new GloryKillsHudValues(null, null, null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(KEY_GLORY_KILLS);
        return new GloryKillsHudValues(
                readOptionalBoolean(element, KEY_ENABLED),
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE),
                readOptionalFloat(element, KEY_FONT_SCALE),
                readOptionalString(element, KEY_ICON_SIDE)
        );
    }

    private static MobFocusValues readMobFocus(JsonObject hud) {
        if (!hud.has(KEY_MOB_FOCUS) || !hud.get(KEY_MOB_FOCUS).isJsonObject()) {
            return new MobFocusValues(null, null, null, null, null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(KEY_MOB_FOCUS);
        return new MobFocusValues(
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE),
                readOptionalFloat(element, KEY_FONT_SCALE),
                readOptionalString(element, KEY_DIRECTION),
                readOptionalFloat(element, KEY_MOB_YAW),
                readOptionalFloat(element, KEY_HEAD_YAW),
                readOptionalFloat(element, KEY_CAMERA_PITCH)
        );
    }

    private static SkillsHudValues readSkills(JsonObject hud) {
        if (!hud.has(KEY_SKILLS) || !hud.get(KEY_SKILLS).isJsonObject()) {
            return new SkillsHudValues(null, null, null, null, null, null, null, null);
        }
        JsonObject element = hud.getAsJsonObject(KEY_SKILLS);
        return new SkillsHudValues(
                readOptionalBoolean(element, KEY_ENABLED),
                readOptionalString(element, KEY_DIRECTION),
                readOptionalString(element, KEY_FILL_SENSE),
                readOptionalString(element, KEY_TEXT_SENSE),
                readOptionalString(element, KEY_ANCHOR),
                readOptionalInt(element, KEY_X),
                readOptionalInt(element, KEY_Y),
                readOptionalFloat(element, KEY_SCALE)
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
        if (withFontScale && values.fontYOffset() != null) {
            element.addProperty(KEY_FONT_Y_OFFSET, values.fontYOffset());
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
        element.addProperty(KEY_ARMOR_MARGIN, values.armorMargin());
        return element;
    }

    private static JsonObject writeHungerBar(HungerBarValues values) {
        JsonObject element = new JsonObject();
        element.addProperty(KEY_ENABLED, values.enabled());
        element.addProperty(KEY_ANCHOR, values.anchor());
        element.addProperty(KEY_X, values.x());
        element.addProperty(KEY_Y, values.y());
        element.addProperty(KEY_SCALE, values.scale());
        element.addProperty(KEY_FONT_SCALE, values.fontScale());
        element.addProperty(KEY_BREATH_MARGIN, values.breathMargin());
        element.addProperty(KEY_MOUNT_HEALTH_MARGIN, values.mountHealthMargin());
        return element;
    }

    private static JsonObject writeEmeralds(EmeraldHudValues values) {
        JsonObject element = new JsonObject();
        element.addProperty(KEY_ENABLED, values.enabled());
        element.addProperty(KEY_ANCHOR, values.anchor());
        element.addProperty(KEY_X, values.x());
        element.addProperty(KEY_Y, values.y());
        element.addProperty(KEY_SCALE, values.scale());
        element.addProperty(KEY_FONT_SCALE, values.fontScale());
        element.addProperty(KEY_ICON_SIDE, values.iconSide());
        return element;
    }

    private static JsonObject writeSouls(SoulsHudValues values) {
        JsonObject element = new JsonObject();
        element.addProperty(KEY_ENABLED, values.enabled());
        element.addProperty(KEY_ANCHOR, values.anchor());
        element.addProperty(KEY_X, values.x());
        element.addProperty(KEY_Y, values.y());
        element.addProperty(KEY_SCALE, values.scale());
        element.addProperty(KEY_FONT_SCALE, values.fontScale());
        element.addProperty(KEY_ICON_SIDE, values.iconSide());
        return element;
    }

    private static JsonObject writeGloryKills(GloryKillsHudValues values) {
        JsonObject element = new JsonObject();
        element.addProperty(KEY_ENABLED, values.enabled());
        element.addProperty(KEY_ANCHOR, values.anchor());
        element.addProperty(KEY_X, values.x());
        element.addProperty(KEY_Y, values.y());
        element.addProperty(KEY_SCALE, values.scale());
        element.addProperty(KEY_FONT_SCALE, values.fontScale());
        element.addProperty(KEY_ICON_SIDE, values.iconSide());
        return element;
    }

    private static JsonObject writeMobFocus(MobFocusValues values) {
        JsonObject element = new JsonObject();
        element.addProperty(KEY_ANCHOR, values.anchor());
        element.addProperty(KEY_X, values.x());
        element.addProperty(KEY_Y, values.y());
        element.addProperty(KEY_SCALE, values.scale());
        element.addProperty(KEY_FONT_SCALE, values.fontScale());
        element.addProperty(KEY_DIRECTION, values.direction());
        element.addProperty(KEY_MOB_YAW, values.mobYaw());
        element.addProperty(KEY_HEAD_YAW, values.headYaw());
        element.addProperty(KEY_CAMERA_PITCH, values.cameraPitch());
        return element;
    }

    private static JsonObject writeSkills(SkillsHudValues values) {
        JsonObject element = new JsonObject();
        element.addProperty(KEY_ENABLED, values.enabled());
        element.addProperty(KEY_DIRECTION, values.direction());
        element.addProperty(KEY_FILL_SENSE, values.fillSense());
        element.addProperty(KEY_TEXT_SENSE, values.textSense());
        element.addProperty(KEY_ANCHOR, values.anchor());
        element.addProperty(KEY_X, values.x());
        element.addProperty(KEY_Y, values.y());
        element.addProperty(KEY_SCALE, values.scale());
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
