package com.grgs.craftlevel.client;

/** Estado client-side da marca Soul Render (entityId do mob; −1 = nenhuma). */
public final class ClientSoulRender {

    private static int markedEntityId = -1;

    private ClientSoulRender() {}

    public static void setMarkedEntityId(int entityId) {
        markedEntityId = entityId;
    }

    public static void clear() {
        markedEntityId = -1;
    }

    public static boolean isMarked(int entityId) {
        return markedEntityId >= 0 && markedEntityId == entityId;
    }

    public static int markedEntityId() {
        return markedEntityId;
    }
}
