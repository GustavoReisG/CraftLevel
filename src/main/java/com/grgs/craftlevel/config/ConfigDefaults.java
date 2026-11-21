package com.grgs.craftlevel.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.CraftLevel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Defaults canônicos em {@code assets/craftlevel/defaults/*.json}.
 * Usados para semear {@code config/craftlevel/} quando o arquivo ainda não existe.
 */
public final class ConfigDefaults {

    private static final String ROOT = "assets/craftlevel/defaults/";

    private ConfigDefaults() {}

    /** Lê o texto do resource default, ou {@code null} se ausente. */
    public static String readText(String fileName) {
        String path = ROOT + fileName;
        try (InputStream stream = ConfigDefaults.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                CraftLevel.LOGGER.warn("[CraftLevel] Missing default resource: {}", path);
                return null;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to read default resource: {}", path, e);
            return null;
        }
    }

    /** Lê o JSON do resource default, ou {@code null} se ausente/inválido. */
    public static JsonObject readJson(String fileName) {
        String path = ROOT + fileName;
        try (InputStream stream = ConfigDefaults.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                CraftLevel.LOGGER.warn("[CraftLevel] Missing default resource: {}", path);
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to parse default resource: {}", path, e);
            return null;
        }
    }

    /**
     * Copia o resource default para {@code target} se o arquivo ainda não existir.
     * @return true se o arquivo passou a existir (já existia ou foi criado)
     */
    public static boolean copyIfMissing(String fileName, Path target) throws IOException {
        if (Files.exists(target)) {
            return true;
        }
        Files.createDirectories(target.getParent());
        String path = ROOT + fileName;
        try (InputStream stream = ConfigDefaults.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return false;
            }
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
    }

    /** Escreve o default no path se ausente; fallback em texto se o resource faltar. */
    public static void writeIfMissing(String fileName, Path target, String fallbackText) throws IOException {
        if (Files.exists(target)) {
            return;
        }
        Files.createDirectories(target.getParent());
        if (copyIfMissing(fileName, target)) {
            return;
        }
        if (fallbackText != null && !fallbackText.isBlank()) {
            Files.writeString(target, fallbackText);
        }
    }
}
