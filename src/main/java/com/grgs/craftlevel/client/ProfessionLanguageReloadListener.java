package com.grgs.craftlevel.client;

import com.grgs.craftlevel.villager.ProfessionLanguages;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Reaplica traduções de profissão após reload de resources (ex.: F3+T). */
@OnlyIn(Dist.CLIENT)
final class ProfessionLanguageReloadListener implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier barrier,
            ResourceManager resourceManager,
            ProfilerFiller preparationsProfiler,
            ProfilerFiller reloadProfiler,
            Executor backgroundExecutor,
            Executor gameExecutor
    ) {
        return barrier.wait(null).thenRunAsync(() ->
                Minecraft.getInstance().execute(ProfessionLanguages::refresh), gameExecutor);
    }
}
