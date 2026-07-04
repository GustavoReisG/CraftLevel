"""MODEL + lp_bar referência: copia textura real da pedra e estrutura das gemas."""
from __future__ import annotations

from pathlib import Path

from PIL import Image

MODEL = Path(r"C:\Users\Gusta\AppData\Local\Temp\ai-chat-attachment-16674431991924000795.png")
REF = (
    Path(__file__).resolve().parents[1]
    / "src/main/resources/assets/craftlevel/textures/gui/lp_bar.png"
)
OUT = (
    Path(__file__).resolve().parents[1]
    / "src/main/resources/assets/craftlevel/textures/gui/bar_frame.png"
)


def is_white(r: int, g: int, b: int, a: int) -> bool:
    return a > 0 and r == 255 and g == 255 and b == 255


def is_gem_gray(r: int, g: int, b: int, a: int) -> bool:
    return a > 0 and r == 136 and g == 136 and b == 136


def is_lime_gem(r: int, g: int, b: int, a: int) -> bool:
    return a > 0 and g > r + 25 and g > 60


def is_stone(r: int, g: int, b: int, a: int) -> bool:
    return a > 0 and not is_lime_gem(r, g, b, a)


def nearest_match(
    img: Image.Image,
    x: int,
    y: int,
    predicate,
    radius: int = 12,
) -> tuple[int, int, int] | None:
    w, h = img.size
    for dist in range(1, radius + 1):
        for ny in range(max(0, y - dist), min(h, y + dist + 1)):
            for nx in range(max(0, x - dist), min(w, x + dist + 1)):
                if abs(nx - x) + abs(ny - y) > dist:
                    continue
                pixel = img.getpixel((nx, ny))
                if predicate(*pixel):
                    return pixel[:3]
    return None


def lime_to_emerald(r: int, g: int, b: int) -> tuple[int, int, int]:
    """
    Mantém a hierarquia de brilho da gema lp_bar, deslocando verde-limão → esmeralda.
    Tiers discretos = pixel art.
    """
    if g >= 210:
        return (98, 246, 168)
    if g >= 185:
        return (78, 218, 142)
    if g >= 160:
        return (58, 192, 118)
    if g >= 130:
        return (40, 158, 96)
    if g >= 100:
        return (26, 124, 76)
    if g >= 70:
        return (16, 92, 56)
    return (10, 58, 36)


def process(model: Image.Image, ref: Image.Image) -> Image.Image:
    mw, mh = model.size
    ref_scaled = ref.resize((mw, mh), Image.NEAREST)
    out = model.copy()
    opx = out.load()
    rpx = ref_scaled.load()

    for y in range(mh):
        for x in range(mw):
            mr, mg, mb, ma = opx[x, y]
            if ma == 0:
                continue

            if is_white(mr, mg, mb, ma):
                rr, rg, rb, ra = rpx[x, y]
                if is_stone(rr, rg, rb, ra):
                    stone = (rr, rg, rb)
                else:
                    found = nearest_match(ref_scaled, x, y, is_stone)
                    stone = found if found else (58, 57, 51)
                opx[x, y] = (*stone, 255)

            elif is_gem_gray(mr, mg, mb, ma):
                rr, rg, rb, ra = rpx[x, y]
                if is_lime_gem(rr, rg, rb, ra):
                    gem = (rr, rg, rb)
                else:
                    found = nearest_match(ref_scaled, x, y, is_lime_gem)
                    gem = found if found else (82, 162, 20)
                opx[x, y] = (*lime_to_emerald(*gem), 255)

    return out


def main() -> None:
    model = Image.open(MODEL).convert("RGBA")
    ref = Image.open(REF).convert("RGBA")
    result = process(model, ref)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    result.save(OUT)
    print("Saved", OUT, result.size)


if __name__ == "__main__":
    main()
