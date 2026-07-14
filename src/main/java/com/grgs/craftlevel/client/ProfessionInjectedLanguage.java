package com.grgs.craftlevel.client;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/** Language wrapper que adiciona traduções dinâmicas de profissões sem mutar o mapa vanilla. */
@OnlyIn(Dist.CLIENT)
final class ProfessionInjectedLanguage extends Language {

    private final Language delegate;
    private final Map<String, String> dynamic = new HashMap<>();

    ProfessionInjectedLanguage(Language delegate) {
        this.delegate = delegate;
    }

    void setDynamic(Map<String, String> entries) {
        dynamic.clear();
        dynamic.putAll(entries);
    }

    Language delegate() {
        return delegate;
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        String value = dynamic.get(key);
        if (value != null) {
            return value;
        }
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean has(String key) {
        return dynamic.containsKey(key) || delegate.has(key);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return delegate.isDefaultRightToLeft();
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText text) {
        return delegate.getVisualOrder(text);
    }

    @Override
    public Map<String, String> getLanguageData() {
        Map<String, String> merged = new HashMap<>(delegate.getLanguageData());
        merged.putAll(dynamic);
        return Map.copyOf(merged);
    }
}
