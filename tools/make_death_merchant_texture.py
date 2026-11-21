"""Gera death_merchant.png a partir do wandering trader (download) ou banker.png."""
from __future__ import annotations

import io
import os
import urllib.request

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT_PROF = os.path.join(
    ROOT,
    "src/main/resources/assets/craftlevel/textures/entity/villager/profession/death_merchant.png",
)
OUT_DEF = os.path.join(ROOT, "src/main/resources/assets/craftlevel/defaults/death_merchant.png")
BANKER = os.path.join(
    ROOT,
    "src/main/resources/assets/craftlevel/textures/entity/villager/profession/banker.png",
)

URLS = [
    "https://minecraft.wiki/images/Wandering_Trader.png",
    "https://static.wikia.nocookie.net/minecraft_gamepedia/images/6/6a/Wandering_Trader.png",
]


def load_source() -> Image.Image:
    for url in URLS:
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=20) as response:
                data = response.read()
            img = Image.open(io.BytesIO(data)).convert("RGBA")
            print(f"downloaded {url} {img.size}")
            return img
        except Exception as exc:  # noqa: BLE001
            print(f"fail {url}: {exc}")
    print("fallback banker")
    return Image.open(BANKER).convert("RGBA")


def crush(c: int) -> int:
    if c < 40:
        return max(0, c - 4)
    if c < 90:
        return int(c * 0.75)
    return min(255, int(c * 0.9 + 8))


def recolor(src: Image.Image) -> Image.Image:
    px = src.load()
    width, height = src.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            mx = max(r, g, b)
            mn = min(r, g, b)
            sat = mx - mn
            if r > 120 and g > 80 and b > 60 and r >= g >= b and sat < 90:
                grey = int((r + g + b) / 3 * 0.35)
                r = g = b = max(20, grey)
            elif b > r + 20 and b > g:
                v = int(b * 0.22)
                r, g, b = v, v + 2, v + 6
            elif g > r + 15 and g > b:
                v = int(g * 0.2)
                r, g, b = v + 8, v, v + 14
            elif r > 140 and g > 100 and b < 120:
                v = int((r + g) * 0.18)
                r, g, b = v + 10, v + 10, v + 12
            elif r > g + 30 and r > b + 20:
                v = int(r * 0.18)
                r, g, b = v + 6, v, v
            else:
                r = int(r * 0.28)
                g = int(g * 0.26)
                b = int(b * 0.32)
            r, g, b = crush(r), crush(g), crush(b)
            r = min(r, 110)
            g = min(g, 105)
            b = min(b, 120)
            px[x, y] = (r, g, b, a)
    return src


def main() -> None:
    os.makedirs(os.path.dirname(OUT_PROF), exist_ok=True)
    os.makedirs(os.path.dirname(OUT_DEF), exist_ok=True)
    img = recolor(load_source())
    img.save(OUT_PROF)
    img.save(OUT_DEF)
    print("saved", OUT_PROF)
    print("saved", OUT_DEF)


if __name__ == "__main__":
    main()
