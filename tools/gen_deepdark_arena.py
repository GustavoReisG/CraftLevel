"""Gera deepdark_arena.schem quadrada (Sponge v2), referência visual amphitheater-cave."""
import gzip
import math
import random
import struct
from pathlib import Path

W, H, L = 55, 34, 55
CX, CZ = W // 2, L // 2
rng = random.Random(77)

palette: dict[str, int] = {}
blocks = [0] * (W * H * L)

# Arena playable square: floor from inner wall inset.
# Walls start at HALF=16 from center => floor is 33x33 (covers ±10 spawns).
FLOOR_HALF = 16
WALL_INNER = 17
WALL_OUTER = 23
SPAWN_OFFSETS = ((10, 10), (10, -10), (-10, 10), (-10, -10))
SMOOTH_CENTER_R = 3


def pid(name: str) -> int:
    if name not in palette:
        palette[name] = len(palette)
    return palette[name]


def idx(x: int, y: int, z: int) -> int:
    return x + z * W + y * W * L


def setb(x: int, y: int, z: int, name: str) -> None:
    if 0 <= x < W and 0 <= y < H and 0 <= z < L:
        blocks[idx(x, y, z)] = pid(name)


def getb(x: int, y: int, z: int) -> int:
    return blocks[idx(x, y, z)]


def in_bounds(x: int, z: int) -> bool:
    return 0 <= x < W and 0 <= z < L


def chebyshev(x: int, z: int) -> int:
    return max(abs(x - CX), abs(z - CZ))


def euclid(x: int, z: int) -> float:
    return math.hypot(x - CX + 0.5, z - CZ + 0.5)


def is_smooth_zone(x: int, z: int) -> bool:
    if euclid(x, z) <= SMOOTH_CENTER_R + 0.01:
        return True
    for ox, oz in SPAWN_OFFSETS:
        if abs(x - (CX + ox)) <= 1 and abs(z - (CZ + oz)) <= 1:
            return True
    return False


AIR = "minecraft:air"
pid(AIR)

DEEPSLATE = "minecraft:deepslate"
COBBLED = "minecraft:cobbled_deepslate"
POLISHED = "minecraft:polished_deepslate"
BRICKS = "minecraft:deepslate_bricks"
CRACKED = "minecraft:cracked_deepslate_bricks"
TILES = "minecraft:deepslate_tiles"
CRACKED_TILES = "minecraft:cracked_deepslate_tiles"
CHISELED = "minecraft:chiseled_deepslate"
REINFORCED = "minecraft:reinforced_deepslate"
SCULK = "minecraft:sculk"
SCULK_VEIN = (
    "minecraft:sculk_vein[down=true,east=false,north=false,south=false,"
    "up=false,waterlogged=false,west=false]"
)
SCULK_CATALYST = "minecraft:sculk_catalyst[bloom=false]"
SOUL_LANTERN = "minecraft:soul_lantern[hanging=false,waterlogged=false]"
SOUL_LANTERN_H = "minecraft:soul_lantern[hanging=true,waterlogged=false]"
SOUL_TORCH_U = "minecraft:soul_torch"
GLOW = (
    "minecraft:glow_lichen[down=true,east=false,north=false,south=false,"
    "up=false,waterlogged=false,west=false]"
)
BARRIER = "minecraft:barrier"
DRIP_TIP = "minecraft:pointed_dripstone[thickness=tip,vertical_direction=down,waterlogged=false]"
DRIP_FRUSTUM = (
    "minecraft:pointed_dripstone[thickness=frustum,vertical_direction=down,waterlogged=false]"
)
DRIP_MIDDLE = (
    "minecraft:pointed_dripstone[thickness=middle,vertical_direction=down,waterlogged=false]"
)
DRIP_BASE = "minecraft:pointed_dripstone[thickness=base,vertical_direction=down,waterlogged=false]"

SLAB_BOTTOM = "minecraft:deepslate_tile_slab[type=bottom,waterlogged=false]"
SLAB_TOP = "minecraft:deepslate_tile_slab[type=top,waterlogged=false]"
WALL = (
    "minecraft:deepslate_brick_wall[east=none,north=none,south=none,up=true,"
    "waterlogged=false,west=none]"
)

STAIRS = {
    "n": "minecraft:deepslate_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]",
    "s": "minecraft:deepslate_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]",
    "e": "minecraft:deepslate_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
    "w": "minecraft:deepslate_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]",
}
STAIRS_TOP = {
    "n": "minecraft:deepslate_brick_stairs[facing=north,half=top,shape=straight,waterlogged=false]",
    "s": "minecraft:deepslate_brick_stairs[facing=south,half=top,shape=straight,waterlogged=false]",
    "e": "minecraft:deepslate_brick_stairs[facing=east,half=top,shape=straight,waterlogged=false]",
    "w": "minecraft:deepslate_brick_stairs[facing=west,half=top,shape=straight,waterlogged=false]",
}


def floor_tile(x: int, z: int) -> str:
    """Piso liso no centro/spawns; grid de tiles no resto; sculk só perto da borda."""
    if is_smooth_zone(x, z):
        return POLISHED
    d_edge = FLOOR_HALF - chebyshev(x, z)
    # near wall: weathered edge
    if d_edge <= 1:
        r = (x * 19 + z * 7) % 8
        if r == 0:
            return SCULK
        if r in (1, 2):
            return CRACKED_TILES
        if r == 3:
            return COBBLED
        return TILES
    if d_edge <= 3 and (x * 13 + z * 11) % 17 == 0:
        return CRACKED_TILES
    # subtle grid
    if (x + z) % 6 == 0:
        return POLISHED
    if (x * 3 + z) % 11 == 0:
        return CRACKED
    return TILES


# --- foundation ---
for z in range(L):
    for x in range(W):
        setb(x, 0, z, COBBLED if ((x + z) & 3) == 0 else DEEPSLATE)

# --- floor ---
for z in range(CZ - FLOOR_HALF, CZ + FLOOR_HALF + 1):
    for x in range(CX - FLOOR_HALF, CX + FLOOR_HALF + 1):
        setb(x, 1, z, floor_tile(x, z))

# clear air above floor
for z in range(CZ - FLOOR_HALF, CZ + FLOOR_HALF + 1):
    for x in range(CX - FLOOR_HALF, CX + FLOOR_HALF + 1):
        for y in range(2, 22):
            setb(x, y, z, AIR)

# sculk veins only near edges, never smooth zones
for z in range(CZ - FLOOR_HALF, CZ + FLOOR_HALF + 1):
    for x in range(CX - FLOOR_HALF, CX + FLOOR_HALF + 1):
        if is_smooth_zone(x, z):
            continue
        d_edge = FLOOR_HALF - chebyshev(x, z)
        if 1 <= d_edge <= 3 and rng.random() < 0.12:
            setb(x, 2, z, SCULK_VEIN)
        elif d_edge <= 2 and rng.random() < 0.05:
            setb(x, 2, z, GLOW)


def fill_rect(x0, y0, z0, x1, y1, z1, name):
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                setb(x, y, z, name)


def pillar(x, z, y0, y1, core=BRICKS, cap=CHISELED):
    for y in range(y0, y1 + 1):
        setb(x, y, z, cap if y == y1 else core)
        # 2x2 bulk for main pillars
        setb(x + 1, y, z, core if y < y1 else cap)
        setb(x, y, z + 1, core if y < y1 else cap)
        setb(x + 1, y, z + 1, core if y < y1 else cap)


# --- square walls with galleries ---
# Outer solid shell WALL_INNER..WALL_OUTER, height ~14, with carved arches.

def on_wall_ring(x: int, z: int) -> bool:
    c = chebyshev(x, z)
    return WALL_INNER <= c <= WALL_OUTER


# Fill wall volume
for z in range(L):
    for x in range(W):
        if not on_wall_ring(x, z):
            continue
        c = chebyshev(x, z)
        for y in range(1, 16):
            # material mix by depth
            if c == WALL_INNER:
                mat = REINFORCED if y in (2, 8, 14) else BRICKS
            elif c == WALL_OUTER:
                mat = DEEPSLATE if (x + y + z) % 5 else COBBLED
            else:
                r = (x + y * 3 + z) % 7
                if r == 0:
                    mat = CRACKED
                elif r == 1:
                    mat = TILES
                elif r == 2:
                    mat = POLISHED
                else:
                    mat = BRICKS
            setb(x, y, z, mat)

# Carve gallery arches on each face (two levels)
# Bay pattern along each wall: every 4 blocks a recess with lantern.

def carve_arch(x, z, y_base, facing):
    """Carve 3-wide x 3-tall recess opening toward arena."""
    openings = []
    if facing == "n":  # wall on +Z side, open toward -Z
        for dx in (-1, 0, 1):
            for dy in (0, 1, 2):
                openings.append((x + dx, y_base + dy, z))
            # deepen one block inward (toward center = -z)
            openings.append((x + dx, y_base, z - 1))
            openings.append((x + dx, y_base + 1, z - 1))
    elif facing == "s":
        for dx in (-1, 0, 1):
            for dy in (0, 1, 2):
                openings.append((x + dx, y_base + dy, z))
            openings.append((x + dx, y_base, z + 1))
            openings.append((x + dx, y_base + 1, z + 1))
    elif facing == "e":
        for dz in (-1, 0, 1):
            for dy in (0, 1, 2):
                openings.append((x, y_base + dy, z + dz))
            openings.append((x + 1, y_base, z + dz))
            openings.append((x + 1, y_base + 1, z + dz))
    else:  # w
        for dz in (-1, 0, 1):
            for dy in (0, 1, 2):
                openings.append((x, y_base + dy, z + dz))
            openings.append((x - 1, y_base, z + dz))
            openings.append((x - 1, y_base + 1, z + dz))
    for ox, oy, oz in openings:
        setb(ox, oy, oz, AIR)


def place_lantern_in_bay(x, z, y, facing):
    setb(x, y, z, SOUL_LANTERN)
    # small shelf
    if facing == "n":
        setb(x, y - 1, z, SLAB_BOTTOM)
    elif facing == "s":
        setb(x, y - 1, z, SLAB_BOTTOM)
    else:
        setb(x, y - 1, z, SLAB_BOTTOM)


# North wall (z = CZ+WALL_INNER), south, east, west
for level, y_base in enumerate((3, 9)):
    # along north face
    z = CZ + WALL_INNER
    for i, x in enumerate(range(CX - FLOOR_HALF + 2, CX + FLOOR_HALF - 1, 4)):
        if abs(x - CX) < 2:
            continue
        carve_arch(x, z, y_base, "n")
        place_lantern_in_bay(x, z, y_base + 1, "n")
        # arch frame top
        setb(x, y_base + 3, z, STAIRS_TOP["s"])
        setb(x - 1, y_base + 3, z, CHISELED)
        setb(x + 1, y_base + 3, z, CHISELED)

    z = CZ - WALL_INNER
    for x in range(CX - FLOOR_HALF + 2, CX + FLOOR_HALF - 1, 4):
        if abs(x - CX) < 2:
            continue
        carve_arch(x, z, y_base, "s")
        place_lantern_in_bay(x, z, y_base + 1, "s")
        setb(x, y_base + 3, z, STAIRS_TOP["n"])
        setb(x - 1, y_base + 3, z, CHISELED)
        setb(x + 1, y_base + 3, z, CHISELED)

    x = CX + WALL_INNER
    for z in range(CZ - FLOOR_HALF + 2, CZ + FLOOR_HALF - 1, 4):
        if abs(z - CZ) < 2:
            continue
        carve_arch(x, z, y_base, "w")
        place_lantern_in_bay(x, z, y_base + 1, "w")
        setb(x, y_base + 3, z, STAIRS_TOP["e"])
        setb(x, y_base + 3, z - 1, CHISELED)
        setb(x, y_base + 3, z + 1, CHISELED)

    x = CX - WALL_INNER
    for z in range(CZ - FLOOR_HALF + 2, CZ + FLOOR_HALF - 1, 4):
        if abs(z - CZ) < 2:
            continue
        carve_arch(x, z, y_base, "e")
        place_lantern_in_bay(x, z, y_base + 1, "e")
        setb(x, y_base + 3, z, STAIRS_TOP["w"])
        setb(x, y_base + 3, z - 1, CHISELED)
        setb(x, y_base + 3, z + 1, CHISELED)

# Corner towers / reinforced pillars
for ox, oz in ((-1, -1), (-1, 1), (1, -1), (1, 1)):
    px = CX + ox * (WALL_INNER - 1)
    pz = CZ + oz * (WALL_INNER - 1)
    pillar(px, pz, 2, 15, core=REINFORCED if abs(ox) else BRICKS, cap=CHISELED)
    setb(px, 16, pz, SOUL_LANTERN)
    setb(px + 1, 16, pz + 1, SOUL_LANTERN)

# Mid-wall buttress pillars on each side
for side in range(4):
    for t in (-10, -5, 0, 5, 10):
        if side == 0:  # north
            px, pz = CX + t, CZ + WALL_INNER - 1
        elif side == 1:
            px, pz = CX + t, CZ - WALL_INNER
        elif side == 2:
            px, pz = CX + WALL_INNER - 1, CZ + t
        else:
            px, pz = CX - WALL_INNER, CZ + t
        for y in range(2, 16):
            setb(px, y, pz, CHISELED if y in (2, 8, 15) else BRICKS)
        setb(px, 16, pz, SOUL_LANTERN)

# Gallery walkways (flat ledges) at y=7 and y=13 on WALL_INNER+1
for ledge_y in (7, 13):
    for z in range(L):
        for x in range(W):
            c = chebyshev(x, z)
            if c == WALL_INNER + 1:
                setb(x, ledge_y, z, POLISHED)
                if (x + z) % 5 == 0:
                    setb(x, ledge_y + 1, z, WALL)

# Inner parapet low wall with cracks and sculk
for z in range(CZ - FLOOR_HALF - 1, CZ + FLOOR_HALF + 2):
    for x in range(CX - FLOOR_HALF - 1, CX + FLOOR_HALF + 2):
        c = chebyshev(x, z)
        if c != WALL_INNER:
            continue
        # top of first tier already solid; add decorative lip at y=2 facing inward
        # replace some inner face with cracked / sculk
        if (x + z) % 9 == 0:
            setb(x, 2, z, SCULK)
        elif (x + z) % 5 == 0:
            setb(x, 2, z, CRACKED)

# Sculk catalysts at corners of floor (outside smooth/spawn)
for ox, oz in ((FLOOR_HALF - 1, FLOOR_HALF - 1), (FLOOR_HALF - 1, -(FLOOR_HALF - 1)),
               (-(FLOOR_HALF - 1), FLOOR_HALF - 1), (-(FLOOR_HALF - 1), -(FLOOR_HALF - 1))):
    setb(CX + ox, 1, CZ + oz, SCULK)
    setb(CX + ox, 2, CZ + oz, SCULK_CATALYST)

# Outer cavern rock beyond walls
for z in range(L):
    for x in range(W):
        c = chebyshev(x, z)
        if WALL_OUTER < c <= 27:
            for y in range(1, 20):
                noise = math.sin(x * 0.55) + math.cos(z * 0.5) + math.sin(y * 0.35)
                if c >= WALL_OUTER + 1 - 0.4 * noise or y > 12:
                    mat = DEEPSLATE
                    if (x + y + z) % 10 == 0:
                        mat = SCULK
                    elif (x * 2 + z) % 6 == 0:
                        mat = COBBLED
                    setb(x, y, z, mat)

# Cave ceiling over whole build
for z in range(L):
    for x in range(W):
        c = chebyshev(x, z)
        if c > 27:
            continue
        n = 0.7 * math.sin(x * 0.4) + 0.7 * math.cos(z * 0.45) + 0.35 * math.sin((x + z) * 0.2)
        # higher open space over arena floor
        if c <= FLOOR_HALF:
            ceil_y = int(24 + n)
        else:
            ceil_y = int(20 + n + (c - FLOOR_HALF) * 0.25)
        ceil_y = max(18, min(30, ceil_y))
        for y in range(ceil_y, H):
            mat = DEEPSLATE
            if (x + y * 3 + z) % 8 == 0:
                mat = SCULK
            elif (x + z) % 5 == 0:
                mat = COBBLED
            setb(x, y, z, mat)
        # dripstone over floor
        if c <= FLOOR_HALF + 2 and rng.random() < 0.1:
            length = rng.randint(1, 5)
            for i in range(length):
                yy = ceil_y - 1 - i
                if yy < 14:
                    break
                if i == length - 1:
                    setb(x, yy, z, DRIP_TIP)
                elif i == 0:
                    setb(x, yy, z, DRIP_BASE)
                elif i == 1 and length > 3:
                    setb(x, yy, z, DRIP_MIDDLE)
                else:
                    setb(x, yy, z, DRIP_FRUSTUM)
        if c <= FLOOR_HALF and (x + z * 5) % 29 == 0:
            setb(x, ceil_y - 1, z, SOUL_LANTERN_H)

# Ensure smooth center: polish disk r=3, clear air above
for z in range(L):
    for x in range(W):
        if euclid(x, z) <= SMOOTH_CENTER_R + 0.01:
            setb(x, 1, z, POLISHED)
            for y in range(2, 18):
                setb(x, y, z, AIR)
setb(CX, 1, CZ, CHISELED)

# Ensure spawn pads smooth and clear
for ox, oz in SPAWN_OFFSETS:
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            x, z = CX + ox + dx, CZ + oz + dz
            setb(x, 1, z, POLISHED)
            for y in range(2, 10):
                setb(x, y, z, AIR)

# Barrier cage above wall top to contain players (invisible)
for z in range(L):
    for x in range(W):
        if chebyshev(x, z) == WALL_OUTER:
            for y in range(16, 24):
                if getb(x, y, z) == pid(AIR):
                    setb(x, y, z, BARRIER)

# Re-clear fight volume after wall carves that may have spilled
for z in range(CZ - FLOOR_HALF + 1, CZ + FLOOR_HALF):
    for x in range(CX - FLOOR_HALF + 1, CX + FLOOR_HALF):
        if chebyshev(x, z) < WALL_INNER:
            for y in range(3, 18):
                # keep floor decorations at y=2
                if y >= 3:
                    bname = None
                    # don't wipe hanging dripstone tips that hang low - only clear mid
                    if y < 14:
                        setb(x, y, z, AIR)

# Restore y=2 edge veins after clear? clear started at 3, ok.
# Restore soul lanterns that might have been cleared - they are in walls.

# Final pass: smooth zones again (air + polished floor)
for z in range(L):
    for x in range(W):
        if is_smooth_zone(x, z):
            setb(x, 1, z, POLISHED if not (x == CX and z == CZ) else CHISELED)
            for y in range(2, 12):
                setb(x, y, z, AIR)

solid = sum(1 for b in blocks if b != pid(AIR))
print("palette", len(palette), "solid", solid)
assert solid > 5000


def write_string(buf: bytearray, s: str) -> None:
    data = s.encode("utf-8")
    buf.extend(struct.pack(">h", len(data)))
    buf.extend(data)


def write_payload(buf: bytearray, tag_type: int, value) -> None:
    if tag_type == 1:
        buf.append(value & 0xFF)
    elif tag_type == 2:
        buf.extend(struct.pack(">h", value))
    elif tag_type == 3:
        buf.extend(struct.pack(">i", value))
    elif tag_type == 4:
        buf.extend(struct.pack(">q", value))
    elif tag_type == 5:
        buf.extend(struct.pack(">f", value))
    elif tag_type == 6:
        buf.extend(struct.pack(">d", value))
    elif tag_type == 7:
        buf.extend(struct.pack(">i", len(value)))
        buf.extend(value)
    elif tag_type == 8:
        write_string(buf, value)
    elif tag_type == 9:
        et, items = value
        buf.append(et)
        buf.extend(struct.pack(">i", len(items)))
        for item in items:
            write_payload(buf, et, item)
    elif tag_type == 10:
        for k, (t, v) in value.items():
            write_tag(buf, t, k, v)
        buf.append(0)
    elif tag_type == 11:
        buf.extend(struct.pack(">i", len(value)))
        for i in value:
            buf.extend(struct.pack(">i", i))
    else:
        raise ValueError(tag_type)


def write_tag(buf: bytearray, tag_type: int, name: str, value) -> None:
    buf.append(tag_type)
    write_string(buf, name)
    write_payload(buf, tag_type, value)


bd = bytearray()
for b in blocks:
    v = b
    while True:
        byte = v & 0x7F
        v >>= 7
        if v:
            bd.append(byte | 0x80)
        else:
            bd.append(byte)
            break

root = {
    "Version": (3, 2),
    "DataVersion": (3, 3465),
    "Width": (2, W),
    "Height": (2, H),
    "Length": (2, L),
    "PaletteMax": (3, len(palette)),
    "Palette": (10, {k: (3, v) for k, v in palette.items()}),
    "BlockData": (7, bytes(bd)),
    "BlockEntities": (9, (10, [])),
    "Offset": (11, [0, 0, 0]),
    "Metadata": (
        10,
        {
            "WEOffsetX": (3, 0),
            "WEOffsetY": (3, 0),
            "WEOffsetZ": (3, 0),
            "Name": (8, "deepdark_arena"),
        },
    ),
}

out = bytearray()
write_tag(out, 10, "Schematic", root)
path = Path("src/main/resources/data/craftlevel/arena/deepdark_arena.schem")
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(gzip.compress(bytes(out), compresslevel=9))
print("wrote", path, "bytes", path.stat().st_size)

# copy to Downloads
dest = Path(r"C:\Users\Gusta\Downloads\Arena\deepdark_arena.schem")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_bytes(path.read_bytes())
print("copied", dest)
