"""Gera arena_portal.bbmodel + texture para Blockbench (java_block 5.0)."""

from __future__ import annotations

import base64
import json
import random
import struct
import uuid
import zlib
from pathlib import Path


def uid() -> str:
    return str(uuid.uuid4())


def png_rgba(w: int, h: int, pixels: bytearray) -> bytes:
    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    raw = b"".join(b"\x00" + bytes(pixels[y * w * 4 : (y + 1) * w * 4]) for y in range(h))
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def setpx(buf: bytearray, w: int, x: int, y: int, r: int, g: int, b: int, a: int = 255) -> None:
    if 0 <= x < w and 0 <= y < w:
        i = (y * w + x) * 4
        buf[i : i + 4] = bytes([r, g, b, a])


def face(uv: list[float], tex: int = 0, rot: int | None = None) -> dict:
    f: dict = {"uv": uv, "texture": tex}
    if rot is not None:
        f["rotation"] = rot
    return f


def all_faces(uv: list[float]) -> dict:
    return {side: face(list(uv)) for side in ("north", "east", "south", "west", "up", "down")}


def cube(
    name: str,
    fro: list[float],
    to: list[float],
    faces: dict,
    color: int = 0,
    origin: list[float] | None = None,
    rotation: list[float] | None = None,
) -> dict:
    o = origin if origin is not None else [(fro[i] + to[i]) / 2 for i in range(3)]
    el: dict = {
        "name": name,
        "box_uv": False,
        "render_order": "default",
        "rescale": False,
        "locked": False,
        "shade": True,
        "light_emission": 0,
        "export": True,
        "scope": 0,
        "allow_mirror_modeling": True,
        "from": fro,
        "to": to,
        "autouv": 0,
        "color": color,
        "origin": o,
        "faces": faces,
        "type": "cube",
        "uuid": uid(),
    }
    if rotation is not None:
        el["rotation"] = rotation
    return el


def group(name: str, uuid_: str, origin: list[float]) -> dict:
    return {
        "name": name,
        "uuid": uuid_,
        "export": True,
        "locked": False,
        "scope": 0,
        "selected": False,
        "_static": {"properties": {}, "temp_data": {}},
        "origin": origin,
        "rotation": [0, 0, 0],
        "color": 0,
        "children": [],
        "reset": False,
        "shade": True,
        "mirror_uv": False,
        "visibility": True,
        "autouv": 0,
        "isOpen": True,
        "primary_selected": False,
    }


def build_texture() -> tuple[bytes, str]:
    w = 32
    buf = bytearray([0, 0, 0, 0] * w * w)
    rng = random.Random(42)

    for y in range(16):
        for x in range(16):
            v = 90 + rng.randint(0, 40)
            if rng.random() < 0.08:
                setpx(buf, w, x, y, 70, 85, 55)
            else:
                setpx(buf, w, x, y, v, v - 5, v - 12)

    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                i = (y * w + x) * 4
                buf[i] = max(0, buf[i] - 25)
                buf[i + 1] = max(0, buf[i + 1] - 25)
                buf[i + 2] = max(0, buf[i + 2] - 25)

    for y in range(16):
        for x in range(16, 24):
            n = 28 + (x + y) % 5 + rng.randint(0, 8)
            setpx(buf, w, x, y, n, max(0, n - 4), min(255, n + 8))

    for y in range(2, 14):
        setpx(buf, w, 18, y, 55, 45, 70)
        setpx(buf, w, 19, y, 40, 32, 55)

    for y in range(8):
        for x in range(24, 32):
            setpx(buf, w, x, y, 35 + rng.randint(0, 10), 30, 40 + rng.randint(0, 15))
    for y in range(8):
        setpx(buf, w, 27, y, 90, 70, 40)
        setpx(buf, w, 28, y, 70, 55, 30)

    for y in range(8, 16):
        for x in range(24, 32):
            setpx(buf, w, x, y, 20, 18, 28)
    setpx(buf, w, 27, 11, 160, 40, 200)
    setpx(buf, w, 28, 11, 120, 30, 160)
    setpx(buf, w, 27, 12, 120, 30, 160)

    for y in range(16, 32):
        for x in range(16):
            setpx(
                buf,
                w,
                x,
                y,
                220 + rng.randint(-10, 10),
                220 + rng.randint(-10, 5),
                200 + rng.randint(-10, 5),
            )

    for y in range(20, 24):
        for x in range(3, 7):
            setpx(buf, w, x, y, 15, 10, 10)
        for x in range(9, 13):
            setpx(buf, w, x, y, 15, 10, 10)
    for y in range(26, 29):
        for x in range(7, 9):
            setpx(buf, w, x, y, 15, 10, 10)
    for y in range(16, 32):
        setpx(buf, w, 0, y, 180, 175, 160)
        setpx(buf, w, 15, y, 180, 175, 160)

    png = png_rgba(w, w, buf)
    return png, base64.b64encode(png).decode("ascii")


def main() -> None:
    png, tex_b64 = build_texture()

    st = [0, 0, 16, 16]
    nt = [16, 0, 24, 16]
    ht = [24, 0, 32, 8]
    pm = [24, 8, 32, 16]

    stone_faces = all_faces(st)
    metal_faces = all_faces(nt)
    hilt_faces = all_faces(ht)
    pommel_faces = all_faces(pm)
    skull_faces = {
        "north": face([0, 16, 16, 32]),
        "east": face([0, 16, 8, 32]),
        "south": face([0, 16, 16, 32]),
        "west": face([8, 16, 16, 32]),
        "up": face([0, 16, 16, 24]),
        "down": face([0, 24, 16, 32]),
    }
    jaw_faces = {
        "north": face([4, 26, 12, 30]),
        "east": face([4, 26, 8, 30]),
        "south": face([4, 26, 12, 30]),
        "west": face([8, 26, 12, 30]),
        "up": face([4, 26, 12, 28]),
        "down": face([4, 28, 12, 30]),
    }

    elements: list[dict] = []
    elements.append(cube("rock_core", [3, 0, 3.5], [12.5, 7, 12], stone_faces, 1))
    elements.append(cube("rock_base", [2, 0, 2.5], [13.5, 3, 13], stone_faces, 1))
    elements.append(cube("rock_ne", [10, 1, 9], [14.5, 5.5, 13.5], stone_faces, 2))
    elements.append(cube("rock_sw", [1, 0, 2], [5.5, 4.5, 7], stone_faces, 2))
    elements.append(cube("rock_nw", [2, 2, 9], [5, 6.5, 13], stone_faces, 3))
    elements.append(cube("rock_se", [9, 0.5, 2], [13, 4, 6], stone_faces, 3))
    elements.append(cube("rock_top", [4, 6.5, 5], [11.5, 8.5, 11], stone_faces, 1))
    elements.append(cube("rock_ridge", [6, 8, 6.5], [10, 9.2, 9.5], stone_faces, 4))
    elements.append(cube("rock_crack", [5.5, 2, 2.2], [9, 6, 4], stone_faces, 5))

    elements.append(cube("skull", [5.2, 9.0, 5.5], [10.8, 14.6, 11.1], skull_faces, 6))
    elements.append(cube("skull_jaw", [5.8, 8.6, 6.2], [10.2, 9.3, 10.2], jaw_faces, 6))

    sword_origin = [8.0, 6.0, 8.0]
    sword_rot = [18.0, -25.0, 12.0]
    elements.append(
        cube("blade", [7.35, 1.5, 7.35], [8.65, 13.5, 8.65], metal_faces, 7, sword_origin, sword_rot)
    )
    elements.append(
        cube(
            "blade_edge",
            [7.1, 4.0, 7.55],
            [8.9, 11.0, 8.45],
            metal_faces,
            7,
            sword_origin,
            sword_rot,
        )
    )
    elements.append(
        cube("guard", [5.8, 13.2, 7.2], [10.2, 14.0, 8.8], hilt_faces, 8, sword_origin, sword_rot)
    )
    elements.append(
        cube("handle", [7.45, 14.0, 7.45], [8.55, 17.8, 8.55], hilt_faces, 8, sword_origin, sword_rot)
    )
    elements.append(
        cube("pommel", [7.15, 17.6, 7.15], [8.85, 18.6, 8.85], pommel_faces, 8, sword_origin, sword_rot)
    )

    g_rock, g_skull, g_sword = uid(), uid(), uid()
    rock_ids = [e["uuid"] for e in elements if e["name"].startswith("rock_")]
    skull_ids = [e["uuid"] for e in elements if e["name"].startswith("skull")]
    sword_ids = [
        e["uuid"] for e in elements if e["name"] in ("blade", "blade_edge", "guard", "handle", "pommel")
    ]

    model = {
        "meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False},
        "name": "arena_portal",
        "parent": "",
        "java_block_version": "1.9.0",
        "ambientocclusion": True,
        "front_gui_light": False,
        "visible_box": [1, 2, 0],
        "variable_placeholders": "",
        "multi_file_ruleset": "",
        "variable_placeholder_buttons": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 32, "height": 32},
        "elements": elements,
        "groups": [
            group("Rock", g_rock, [8, 4, 8]),
            group("Skull", g_skull, [8, 11, 8]),
            group("Sword", g_sword, sword_origin),
        ],
        "outliner": [
            {"uuid": g_rock, "isOpen": True, "children": rock_ids},
            {"uuid": g_skull, "isOpen": True, "children": skull_ids},
            {"uuid": g_sword, "isOpen": True, "children": sword_ids},
        ],
        "textures": [
            {
                "name": "arena_portal.png",
                "relative_path": "arena_portal.png",
                "folder": "",
                "namespace": "",
                "id": "0",
                "group": "",
                "scope": 0,
                "width": 32,
                "height": 32,
                "uv_width": 32,
                "uv_height": 32,
                "particle": True,
                "use_as_default": False,
                "layers_enabled": False,
                "sync_to_project": "",
                "file_format": "png",
                "render_mode": "default",
                "render_sides": "auto",
                "wrap_mode": "limited",
                "pbr_channel": "color",
                "fps": 7,
                "frame_time": 1,
                "frame_order_type": "loop",
                "frame_order": "",
                "frame_interpolate": False,
                "visible": True,
                "internal": True,
                "saved": True,
                "uuid": uid(),
                "source": "data:image/png;base64," + tex_b64,
            }
        ],
    }

    out_dir = Path(__file__).resolve().parents[1] / "src/main/resources/assets/craftlevel/models/block"
    out_dir.mkdir(parents=True, exist_ok=True)
    bb_path = out_dir / "arena_portal.bbmodel"
    png_path = out_dir / "arena_portal.png"
    bb_path.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8")
    png_path.write_bytes(png)
    print(f"Wrote {bb_path}")
    print(f"Wrote {png_path}")
    print(f"elements={len(elements)} max_y={max(e['to'][1] for e in elements)}")


if __name__ == "__main__":
    main()
