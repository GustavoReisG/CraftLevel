package com.grgs.craftlevel.arena;

import java.util.ArrayList;
import java.util.List;

/**
 * Pontos fixos da dimensão. O primeiro é {@code 0, 62, 0}.
 * Os seguintes afastam 500 blocos no X e no Z: {@code 500, 62, 0}, {@code -500, 62, 0}, {@code 0, 62, 500} e assim por diante.
 */
public final class ArenaSlots {

    public static final int SPACING = 500;
    private static final List<Slot> SLOTS = build();

    private ArenaSlots() {}

    public record Slot(int x, int y, int z) {}

    public static int count() {
        return SLOTS.size();
    }

    public static Slot get(int index) {
        return SLOTS.get(index);
    }

    private static List<Slot> build() {
        List<Slot> slots = new ArrayList<>();
        for (int ring = 0; ring <= 16; ring++) {
            if (ring == 0) {
                slots.add(new Slot(0, ArenaSchematic.BASE_Y, 0));
                continue;
            }
            int distance = ring * SPACING;
            slots.add(new Slot(distance, ArenaSchematic.BASE_Y, 0));
            slots.add(new Slot(-distance, ArenaSchematic.BASE_Y, 0));
            slots.add(new Slot(0, ArenaSchematic.BASE_Y, distance));
            slots.add(new Slot(0, ArenaSchematic.BASE_Y, -distance));
            for (int x = -ring; x <= ring; x++) {
                for (int z = -ring; z <= ring; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != ring || x == 0 || z == 0) {
                        continue;
                    }
                    slots.add(new Slot(x * SPACING, ArenaSchematic.BASE_Y, z * SPACING));
                }
            }
        }
        return List.copyOf(slots);
    }
}
