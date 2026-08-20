package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.client.config.SkillsHudConfigScreen;
import com.grgs.craftlevel.skill.SkillCatalog;
import com.grgs.craftlevel.skill.SkillDefinition;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Carrossel de skills com efeito ativo (ícone + tempo restante em segundos). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class ActiveSkillsHud {

    public static final int MAX_SLOTS = 9;
    private static final int BASE_ICON = 24;

    private ActiveSkillsHud() {}

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.CROSSHAIR.id().equals(event.getOverlay().id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.renderDebug || minecraft.player == null) {
            return;
        }

        boolean preview = minecraft.screen instanceof SkillsHudConfigScreen;
        if (!preview && !ClientConfig.hudSkillsEnabled) {
            return;
        }

        List<Entry> entries = preview ? previewEntries() : liveEntries(minecraft);
        if (entries.isEmpty() && !preview) {
            return;
        }
        if (preview && entries.isEmpty()) {
            entries = previewEntries();
        }

        render(
                event.getGuiGraphics(),
                minecraft,
                entries,
                preview,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight()
        );
    }

    private static List<Entry> liveEntries(Minecraft minecraft) {
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : ClientActiveSkills.snapshot().entrySet()) {
            int remaining = (int) Math.ceil((e.getValue() - now) / 20.0);
            if (remaining <= 0) {
                continue;
            }
            Optional<SkillDefinition> def = SkillCatalog.byId(e.getKey());
            if (def.isEmpty()) {
                continue;
            }
            list.add(new Entry(def.get(), remaining));
        }
        list.sort(Comparator.comparing(a -> a.def().id()));
        if (list.size() > MAX_SLOTS) {
            return list.subList(0, MAX_SLOTS);
        }
        return list;
    }

    private static List<Entry> previewEntries() {
        List<Entry> list = new ArrayList<>();
        List<SkillDefinition> all = SkillCatalog.all();
        if (all.isEmpty()) {
            return list;
        }
        for (int i = 0; i < MAX_SLOTS; i++) {
            SkillDefinition def = all.get(i % all.size());
            list.add(new Entry(def, 10 + i * 5));
        }
        return list;
    }

    private static void render(
            GuiGraphics graphics,
            Minecraft minecraft,
            List<Entry> entries,
            boolean preview,
            int screenW,
            int screenH
    ) {
        float scale = ClientConfig.hudSkillsScale;
        int iconSize = Math.max(8, Math.round(BASE_ICON * scale));
        int gap = Math.max(2, Math.round(4 * scale));
        int textH = Math.max(6, Math.round(8 * scale));
        boolean row = "row".equals(ClientConfig.hudSkillsDirection);
        boolean textAfter = "after_icon".equals(ClientConfig.hudSkillsTextSense);

        int cellW;
        int cellH;
        if (row) {
            cellW = iconSize;
            cellH = iconSize + gap + textH;
        } else {
            int sampleTextW = minecraft.font.width("999");
            cellW = iconSize + gap + Math.round(sampleTextW * scale);
            cellH = Math.max(iconSize, textH);
        }

        int count = Math.min(MAX_SLOTS, Math.max(entries.size(), preview ? MAX_SLOTS : entries.size()));
        if (preview) {
            count = MAX_SLOTS;
        } else {
            count = entries.size();
        }

        int totalW = row ? count * cellW + Math.max(0, count - 1) * gap : cellW;
        int totalH = row ? cellH : count * cellH + Math.max(0, count - 1) * gap;

        int[] anchor = ClientHudLayout.anchorCenter(
                ClientConfig.hudSkillsAnchor, screenW, screenH, totalW, totalH);
        int originX = anchor[0] - totalW / 2 + ClientConfig.hudSkillsX;
        int originY = anchor[1] - totalH / 2 + ClientConfig.hudSkillsY;

        if (preview) {
            int pad = Math.max(4, Math.round(6 * scale));
            graphics.fill(
                    originX - pad,
                    originY - pad,
                    originX + totalW + pad,
                    originY + totalH + pad,
                    0x88000000
            );
        }

        RenderSystem.enableBlend();
        for (int i = 0; i < count; i++) {
            Entry entry = entries.get(i);
            int slotIndex = slotIndex(i, count, ClientConfig.hudSkillsFillSense);
            int cellX;
            int cellY;
            if (row) {
                cellX = originX + slotIndex * (cellW + gap);
                cellY = originY;
            } else {
                cellX = originX;
                cellY = originY + slotIndex * (cellH + gap);
            }
            drawCell(graphics, minecraft, entry, cellX, cellY, iconSize, gap, scale, row, textAfter);
        }
        RenderSystem.disableBlend();
    }

    /**
     * ascending: 0..n-1 na ordem natural (esq→dir / cima→baixo)
     * descending: invertido
     * center: índice visual centrado (primeiro no meio, depois alterna lados)
     */
    static int slotIndex(int chronologicalIndex, int count, String fillSense) {
        if ("descending".equals(fillSense)) {
            return count - 1 - chronologicalIndex;
        }
        if ("center".equals(fillSense)) {
            // Ordem de preenchimento a partir do centro: 0 no meio, 1 à direita, 2 à esquerda...
            int[] order = new int[count];
            int mid = count / 2;
            int left = mid - 1;
            int right = mid;
            int idx = 0;
            if ((count & 1) == 1) {
                order[idx++] = mid;
                left = mid - 1;
                right = mid + 1;
            }
            while (idx < count) {
                if (right < count) {
                    order[idx++] = right++;
                }
                if (idx < count && left >= 0) {
                    order[idx++] = left--;
                }
            }
            return order[chronologicalIndex];
        }
        return chronologicalIndex;
    }

    private static void drawCell(
            GuiGraphics graphics,
            Minecraft minecraft,
            Entry entry,
            int cellX,
            int cellY,
            int iconSize,
            int gap,
            float scale,
            boolean row,
            boolean textAfter
    ) {
        String time = String.valueOf(entry.remainingSeconds());
        int textW = Math.round(minecraft.font.width(time) * Math.max(0.5f, scale));

        int iconX;
        int iconY;
        int textX;
        int textY;

        if (row) {
            iconX = cellX;
            if (textAfter) {
                iconY = cellY;
                textY = cellY + iconSize + gap;
            } else {
                textY = cellY;
                iconY = cellY + Math.round(8 * scale) + gap;
            }
            textX = cellX + (iconSize - textW) / 2;
        } else {
            iconY = cellY + (Math.max(iconSize, Math.round(8 * scale)) - iconSize) / 2;
            textY = cellY + (Math.max(iconSize, Math.round(8 * scale)) - Math.round(8 * scale)) / 2;
            if (textAfter) {
                iconX = cellX;
                textX = cellX + iconSize + gap;
            } else {
                textX = cellX;
                iconX = cellX + textW + gap;
            }
        }

        graphics.blit(
                entry.def().icon(),
                iconX,
                iconY,
                iconSize,
                iconSize,
                0.0F,
                0.0F,
                32,
                32,
                32,
                32
        );

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(textX, textY, 0);
        pose.scale(Math.max(0.5f, scale), Math.max(0.5f, scale), 1.0f);
        graphics.drawString(minecraft.font, time, 0, 0, 0xFFFFFF, true);
        pose.popPose();
    }

    private record Entry(SkillDefinition def, int remainingSeconds) {}
}
