package com.grgs.craftlevel.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixins opcionais só com o mod alvo instalado.
 * <p>
 * Usa {@link LoadingModList} (disponível cedo no boot) — {@code Class.forName} no
 * classloader do CraftLevel falha para mods sem dependência declarada (ex.: Epic Fight).
 */
public final class CraftLevelMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("EpicFightPortraitMixin")
                || mixinClassName.endsWith("EpicFightPortraitRendererMixin")) {
            return modPresent("epicfight");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    private static boolean modPresent(String modId) {
        try {
            LoadingModList list = LoadingModList.get();
            return list != null && list.getModFileById(modId) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
