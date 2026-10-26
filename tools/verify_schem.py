import gzip
import io
import math
import struct
import sys
from collections import Counter
from pathlib import Path


def read_schem(path: Path):
    raw = gzip.decompress(path.read_bytes())
    buf = io.BytesIO(raw)

    def rb():
        return buf.read(1)[0]

    def rs():
        return struct.unpack(">h", buf.read(2))[0]

    def ri():
        return struct.unpack(">i", buf.read(4))[0]

    def rstr():
        n = rs()
        return buf.read(n).decode("utf-8")

    def payload(tag):
        if tag == 1:
            return rb()
        if tag == 2:
            return rs()
        if tag == 3:
            return ri()
        if tag == 4:
            return struct.unpack(">q", buf.read(8))[0]
        if tag == 7:
            n = ri()
            return buf.read(n)
        if tag == 8:
            return rstr()
        if tag == 9:
            et = rb()
            n = ri()
            return [payload(et) for _ in range(n)]
        if tag == 10:
            data = {}
            while True:
                t = rb()
                if t == 0:
                    break
                data[rstr()] = payload(t)
            return data
        if tag == 11:
            n = ri()
            return [ri() for _ in range(n)]
        raise ValueError(tag)

    t = rb()
    name = rstr()
    return name, payload(t)


def main():
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "src/main/resources/data/craftlevel/arena/deepdark_arena.schem")
    name, root = read_schem(path)
    w, h, l = root["Width"], root["Height"], root["Length"]
    inv = {v: k for k, v in root["Palette"].items()}
    bd = root["BlockData"]
    blocks = []
    i = 0
    while len(blocks) < w * h * l:
        value = 0
        shift = 0
        while True:
            b = bd[i]
            i += 1
            value |= (b & 0x7F) << shift
            if (b & 0x80) == 0:
                break
            shift += 7
        blocks.append(value)

    def get(x, y, z):
        return inv[blocks[x + z * w + y * w * l]]

    cx = cz = w // 2
    print("ok", name, w, h, l, "palette", root["PaletteMax"])
    print("center", get(cx, 1, cz), get(cx, 2, cz))
    bad = 0
    for x in range(w):
        for z in range(l):
            if math.hypot(x - cx + 0.5, z - cz + 0.5) <= 3.01:
                if get(x, 1, z) not in ("minecraft:polished_deepslate", "minecraft:chiseled_deepslate"):
                    print("center floor", x, z, get(x, 1, z))
                    bad += 1
                if get(x, 2, z) != "minecraft:air":
                    print("center air", x, z, get(x, 2, z))
                    bad += 1
    print("center bad", bad)
    for ox, oz in ((10, 10), (10, -10), (-10, 10), (-10, -10)):
        cells = []
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                cells.append(get(cx + ox + dx, 1, cz + oz + dz))
                if get(cx + ox + dx, 2, cz + oz + dz) != "minecraft:air":
                    print("spawn blocked", ox, oz, get(cx + ox + dx, 2, cz + oz + dz))
        print("spawn", ox, oz, Counter(cells))
    print("soul lanterns", sum(1 for b in blocks if "soul_lantern" in inv[b]))


if __name__ == "__main__":
    main()
