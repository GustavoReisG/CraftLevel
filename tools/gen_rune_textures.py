"""Gera texturas 16x16 das runas: tablet de pedra detalhado + glifo colorido."""
from PIL import Image
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/craftlevel/textures/item"
OUT.mkdir(parents=True, exist_ok=True)

STONE = {
    "rim": (38, 38, 44, 255),
    "edge": (52, 52, 60, 255),
    "bevel_d": (62, 62, 70, 255),
    "bevel_l": (128, 128, 136, 255),
    "deep": (70, 70, 78, 255),
    "dark": (84, 84, 92, 255),
    "mid": (100, 100, 108, 255),
    "base": (114, 114, 122, 255),
    "light": (132, 132, 140, 255),
    "hi": (150, 150, 156, 255),
    "shine": (172, 172, 178, 255),
    "crack": (44, 44, 50, 255),
    "pore": (90, 90, 98, 255),
    "fleck": (122, 122, 128, 255),
    "moss": (88, 96, 86, 255),
}


def hash2(x, y, salt=0):
    n = (x * 374761393 + y * 668265263 + salt * 982451653) & 0x7FFFFFFF
    n = (n ^ (n >> 13)) * 1274126177
    return (n ^ (n >> 16)) & 0xFF


def set_px(px, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = color


def new_tablet():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    # Cantos transparentes (tablet ligeiramente arredondado)
    skip = {
        (0, 0), (0, 1), (1, 0),
        (15, 0), (14, 0), (15, 1),
        (0, 15), (0, 14), (1, 15),
        (15, 15), (14, 15), (15, 14),
    }

    for y in range(16):
        for x in range(16):
            if (x, y) in skip:
                continue

            # Moldura externa
            if x in (0, 15) or y in (0, 15):
                set_px(px, x, y, STONE["rim"])
                continue
            if x in (1, 14) or y in (1, 14):
                # bisel: topo/esq claro, baixo/dir escuro
                if y == 1 and x not in (1, 14):
                    set_px(px, x, y, STONE["bevel_l"])
                elif x == 1 and y not in (1, 14):
                    set_px(px, x, y, STONE["bevel_l"])
                elif y == 14:
                    set_px(px, x, y, STONE["bevel_d"])
                else:
                    set_px(px, x, y, STONE["bevel_d"])
                continue

            # Face interna com shading diagonal + ruído mineral
            shade = (13 - y) * 3 + (13 - x) * 2
            n = hash2(x, y, 3)
            g = hash2(x, y, 11)
            m = hash2(x, y, 29)

            if shade > 28:
                c = STONE["hi"]
            elif shade > 20:
                c = STONE["light"]
            elif shade > 12:
                c = STONE["base"]
            elif shade > 6:
                c = STONE["mid"]
            else:
                c = STONE["dark"]

            # poros / veios / flecks
            if g % 9 == 0:
                c = STONE["pore"]
            elif g % 14 == 0:
                c = STONE["fleck"]
            elif n % 19 == 0:
                c = STONE["deep"]
            elif m % 23 == 0:
                c = STONE["moss"]

            set_px(px, x, y, c)

    # Anel rebaixado interno (encaixe do glifo)
    for y in range(2, 14):
        for x in range(2, 14):
            if x in (2, 13) or y in (2, 13):
                if y == 2 and x not in (2, 13):
                    set_px(px, x, y, STONE["dark"])
                elif x == 2 and y not in (2, 13):
                    set_px(px, x, y, STONE["dark"])
                elif y == 13:
                    set_px(px, x, y, STONE["mid"])
                else:
                    set_px(px, x, y, STONE["base"])

    # Rachaduras
    for x, y in [
        (3, 4), (4, 5), (5, 5),
        (11, 3), (12, 4), (12, 5),
        (3, 11), (4, 12),
        (10, 11), (11, 11), (12, 12),
        (7, 3), (8, 13),
        (6, 8), (9, 6),
    ]:
        if px[x, y][3] > 0:
            set_px(px, x, y, STONE["crack"])

    # Highlight canto SE
    for x, y in [(3, 3), (4, 3), (3, 4)]:
        set_px(px, x, y, STONE["shine"])
    set_px(px, 5, 3, STONE["hi"])
    set_px(px, 3, 5, STONE["hi"])

    # Sombra canto ID
    for x, y in [(12, 12), (11, 12), (12, 11), (13, 12), (12, 13)]:
        if px[x, y][3] > 0:
            set_px(px, x, y, STONE["deep"])

    return img, px


def draw_pixels(px, coords, color):
    for x, y in coords:
        if 0 <= x < 16 and 0 <= y < 16 and px[x, y][3] > 0:
            px[x, y] = color


def is_stone(c):
    if c[3] != 255:
        return False
    r, g, b = c[0], c[1], c[2]
    return abs(r - g) < 24 and abs(g - b) < 24


def glow_around(px, coords, glow):
    """Halo ortogonal fraco; não fecha vão de 1px entre partes do glifo."""
    core = set(coords)
    for x, y in coords:
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nx, ny = x + dx, y + dy
            if (nx, ny) in core:
                continue
            if not (0 <= nx < 16 and 0 <= ny < 16 and is_stone(px[nx, ny])):
                continue
            # evita preencher o espaço entre dois traços
            if ((nx - 1, ny) in core and (nx + 1, ny) in core) or (
                (nx, ny - 1) in core and (nx, ny + 1) in core
            ):
                continue
            s = px[nx, ny]
            px[nx, ny] = (
                (s[0] * 2 + glow[0]) // 3,
                (s[1] * 2 + glow[1]) // 3,
                (s[2] * 2 + glow[2]) // 3,
                255,
            )


def carve(px, coords, deep, mid, bright, hot=None, deep_pts=None, hot_pts=None):
    glow_around(px, coords, (deep[0] // 2, deep[1] // 2, deep[2] // 2, 255))
    draw_pixels(px, coords, mid)
    if deep_pts:
        draw_pixels(px, deep_pts, deep)
    if hot_pts and hot:
        draw_pixels(px, hot_pts, hot)
    # highlight interno padrão: centro aproximado
    if hot and not hot_pts:
        pass


# Glifos com traço ~2px, centrados em x/y 7–8 (centro do 16x16).

# --- Unbreakable: escudo ---
img, px = new_tablet()
glyph = [
    (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4),
    (5, 5), (6, 5), (9, 5), (10, 5),
    (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6),
    (5, 7), (6, 7), (9, 7), (10, 7),
    (5, 8), (6, 8), (9, 8), (10, 8),
    (6, 9), (7, 9), (8, 9), (9, 9),
    (6, 10), (7, 10), (8, 10), (9, 10),
    (7, 11), (8, 11),
    (7, 12), (8, 12),
]
carve(px, glyph,
      deep=(140, 85, 18, 255),
      mid=(220, 165, 45, 255),
      bright=(245, 210, 110, 255),
      hot=(255, 235, 170, 255),
      deep_pts=[(5, 4), (10, 4), (5, 8), (10, 8), (7, 12), (8, 12)],
      hot_pts=[(7, 6), (8, 6), (7, 9), (8, 9)])
img.save(OUT / "rune_of_unbreakable.png")

# --- Veloce: raio / seta ---
img, px = new_tablet()
glyph = [
    (7, 3), (8, 3),
    (6, 4), (7, 4), (8, 4), (9, 4),
    (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5),
    (4, 6), (5, 6), (7, 6), (8, 6), (10, 6), (11, 6),
    (7, 7), (8, 7),
    (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8),
    (4, 9), (5, 9), (7, 9), (8, 9), (10, 9), (11, 9),
    (7, 10), (8, 10),
    (6, 11), (7, 11), (8, 11), (9, 11),
    (7, 12), (8, 12),
]
carve(px, glyph,
      deep=(20, 110, 145, 255),
      mid=(55, 210, 230, 255),
      bright=(120, 235, 245, 255),
      hot=(190, 250, 255, 255),
      deep_pts=[(7, 3), (8, 3), (4, 6), (11, 6), (4, 9), (11, 9), (7, 12), (8, 12)],
      hot_pts=[(7, 5), (8, 5), (7, 7), (8, 7), (7, 8), (8, 8), (7, 10), (8, 10)])
img.save(OUT / "rune_of_veloce.png")

# --- Reach: mira / cruz ---
img, px = new_tablet()
glyph = [
    (7, 3), (8, 3),
    (7, 4), (8, 4),
    (7, 5), (8, 5),
    (7, 6), (8, 6),
    (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7),
    (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8),
    (7, 9), (8, 9),
    (7, 10), (8, 10),
    (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11),
    (4, 12), (7, 12), (8, 12), (11, 12),
]
carve(px, glyph,
      deep=(50, 120, 30, 255),
      mid=(125, 220, 70, 255),
      bright=(170, 240, 120, 255),
      hot=(210, 255, 170, 255),
      deep_pts=[(7, 3), (8, 3), (4, 7), (11, 7), (4, 12), (11, 12), (7, 12), (8, 12)],
      hot_pts=[(7, 7), (8, 7), (7, 8), (8, 8)])
img.save(OUT / "rune_of_reach.png")

# --- Fortune: esmeralda / losango ---
img, px = new_tablet()
glyph = [
    (7, 3), (8, 3),
    (6, 4), (7, 4), (8, 4), (9, 4),
    (5, 5), (6, 5), (9, 5), (10, 5),
    (4, 6), (5, 6), (10, 6), (11, 6),
    (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7),
    (4, 8), (5, 8), (10, 8), (11, 8),
    (5, 9), (6, 9), (9, 9), (10, 9),
    (6, 10), (7, 10), (8, 10), (9, 10),
    (7, 11), (8, 11),
    (6, 12), (7, 12), (8, 12), (9, 12),
]
carve(px, glyph,
      deep=(12, 110, 55, 255),
      mid=(40, 200, 110, 255),
      bright=(90, 235, 150, 255),
      hot=(170, 255, 200, 255),
      deep_pts=[(7, 3), (8, 3), (4, 6), (11, 6), (4, 8), (11, 8), (7, 11), (8, 11), (6, 12), (9, 12)],
      hot_pts=[(7, 4), (8, 4), (6, 7), (7, 7), (8, 7), (9, 7)])
img.save(OUT / "rune_of_fortune.png")

# --- Return: S / setas circulares ---
img, px = new_tablet()
glyph = [
    # gancho superior (→)
    (8, 3), (9, 3), (10, 3), (11, 3),
    (8, 4), (9, 4), (10, 4), (11, 4),
    (10, 5), (11, 5),
    # faixa média 2px
    (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6),
    (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7),
    # gancho inferior (←)
    (4, 8), (5, 8),
    (4, 9), (5, 9), (6, 9), (7, 9),
    (4, 10), (5, 10), (6, 10), (7, 10),
    (4, 11), (5, 11), (6, 11), (7, 11),
]
carve(px, glyph,
      deep=(150, 40, 50, 255),
      mid=(230, 75, 85, 255),
      bright=(245, 130, 130, 255),
      hot=(255, 190, 185, 255),
      deep_pts=[(11, 3), (11, 4), (4, 10), (4, 11)],
      hot_pts=[(7, 6), (8, 6), (7, 7), (8, 7)])
img.save(OUT / "rune_of_return.png")

# --- Soulbound: espiral / âncora de alma ---
img, px = new_tablet()
glyph = [
    (6, 3), (7, 3), (8, 3), (9, 3),
    (5, 4), (6, 4), (9, 4), (10, 4),
    (4, 5), (5, 5), (10, 5), (11, 5),
    (4, 6), (5, 6), (7, 6), (8, 6), (10, 6), (11, 6),
    (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7),
    (4, 8), (5, 8), (7, 8), (8, 8), (10, 8), (11, 8),
    (5, 9), (6, 9), (9, 9), (10, 9),
    (6, 10), (7, 10), (8, 10), (9, 10),
    (7, 11), (8, 11),
    (6, 12), (7, 12), (8, 12), (9, 12),
]
carve(px, glyph,
      deep=(90, 50, 160, 255),
      mid=(170, 110, 235, 255),
      bright=(200, 160, 250, 255),
      hot=(235, 210, 255, 255),
      deep_pts=[(6, 3), (9, 3), (4, 5), (11, 5), (4, 8), (11, 8), (7, 11), (8, 11), (6, 12), (9, 12)],
      hot_pts=[(7, 6), (8, 6), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8)])
img.save(OUT / "rune_of_soulbound.png")

print("wrote", sorted(p.name for p in OUT.glob("rune_of_*.png")))
