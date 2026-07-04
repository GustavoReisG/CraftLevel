package com.grgs.craftlevel.client;

/** Posicionamento de elementos HUD ancorados na tela. */
final class ClientHudLayout {

    private ClientHudLayout() {}

    static int[] anchorCenter(String anchor, int screenWidth, int screenHeight, int elemWidth, int elemHeight) {
        int ax;
        int ay;
        switch (anchor) {
            case "top_left"      -> { ax = elemWidth / 2;               ay = elemHeight / 2; }
            case "top_center"    -> { ax = screenWidth / 2;             ay = elemHeight / 2; }
            case "top_right"     -> { ax = screenWidth - elemWidth / 2; ay = elemHeight / 2; }
            case "center_left"   -> { ax = elemWidth / 2;               ay = screenHeight / 2; }
            case "center_right"  -> { ax = screenWidth - elemWidth / 2; ay = screenHeight / 2; }
            case "bottom_left"   -> { ax = elemWidth / 2;               ay = screenHeight - elemHeight / 2; }
            case "bottom_center" -> { ax = screenWidth / 2;             ay = screenHeight - elemHeight / 2; }
            case "bottom_right"  -> { ax = screenWidth - elemWidth / 2; ay = screenHeight - elemHeight / 2; }
            default              -> { ax = screenWidth / 2;             ay = screenHeight / 2; }
        }
        return new int[]{ax, ay};
    }
}
