"""Recolora lp_bar → hp_bar: verde/esverdeado → vermelho no tom do hp_full."""
from __future__ import annotations

from pathlib import Path

from PIL import Image

FRAME_REF = (
    Path(__file__).resolve().parents[1]
    / "src/main/resources/assets/craftlevel/textures/gui/lp_bar.png"
)
OUT_PATH = (
    Path(__file__).resolve().parents[1]
    / "src/main/resources/assets/craftlevel/textures/gui/hp_bar.png"
)

# Faixas medidas nas texturas originais (gemas verdes do lp_bar → vermelho do hp_full)
GREEN_G = (61, 245)
GREEN_R = (17, 147)
GREEN_B = (2, 61)
TARGET_R = (151, 186)
TARGET_G = (28, 38)
TARGET_B = (17, 24)


def lerp_channel(value: int, src: tuple[int, int], dst: tuple[int, int]) -> int:
    lo, hi = src
    if hi <= lo:
        return dst[0]
    t = max(0.0, min(1.0, (value - lo) / (hi - lo)))
    return int(dst[0] + t * (dst[1] - dst[0]))


def is_greenish(r: int, g: int, b: int, a: int) -> bool:
    if a == 0:
        return False
    return g > r + 2 and g > b + 1


def is_dominant_green(r: int, g: int, b: int, a: int) -> bool:
    return a > 0 and g > max(r, b) + 4


def green_to_hp_red(r: int, g: int, b: int, a: int) -> tuple[int, int, int, int]:
    if a == 0 or not is_greenish(r, g, b, a):
        return r, g, b, a

    if is_dominant_green(r, g, b, a):
        return (
            lerp_channel(g, GREEN_G, TARGET_R),
            lerp_channel(r, GREEN_R, TARGET_G),
            lerp_channel(b, GREEN_B, TARGET_B),
            a,
        )

    excess = g - max(r, b)
    return (
        min(255, r + excess // 2),
        max(0, g - excess // 2),
        max(0, b - excess // 4),
        a,
    )


def recolor_hp_bar(src: Image.Image) -> Image.Image:
    out = src.copy()
    w, h = out.size
    for y in range(h):
        for x in range(w):
            out.putpixel((x, y), green_to_hp_red(*out.getpixel((x, y))))
    return out


def main() -> None:
    src = Image.open(FRAME_REF).convert("RGBA")
    hp_bar = recolor_hp_bar(src)
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    hp_bar.save(OUT_PATH)

    greens = sum(
        1
        for y in range(hp_bar.height)
        for x in range(hp_bar.width)
        if is_greenish(*hp_bar.getpixel((x, y)))
    )
    print("Saved", OUT_PATH)
    print("remaining greenish pixels:", greens)


if __name__ == "__main__":
    main()
