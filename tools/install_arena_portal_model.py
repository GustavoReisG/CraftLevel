"""Instala arena_portal.bbmodel + textura e exporta JSON Minecraft + boxes de outline."""

from __future__ import annotations

import json
import math
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC_BB = Path(r"C:\Users\Gusta\Downloads\arena_portal.bbmodel")
SRC_TEX = Path(r"C:\Users\Gusta\Downloads\arena_portal.png")
OUT_BLOCK = ROOT / "src/main/resources/assets/craftlevel/models/block"
OUT_TEX = ROOT / "src/main/resources/assets/craftlevel/textures/block"
OUT_BOXES = ROOT / "tools/arena_portal_model_boxes.txt"


def rotate_point(x: float, y: float, z: float, origin: list[float], axis: str, angle_deg: float):
    ox, oy, oz = origin
    x, y, z = x - ox, y - oy, z - oz
    a = math.radians(angle_deg)
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:  # z
        x, y = x * c - y * s, x * s + y * c
    return x + ox, y + oy, z + oz


def element_aabb(el: dict) -> tuple[float, float, float, float, float, float]:
    fro, to = el["from"], el["to"]
    corners = [
        (fro[0], fro[1], fro[2]),
        (fro[0], fro[1], to[2]),
        (fro[0], to[1], fro[2]),
        (fro[0], to[1], to[2]),
        (to[0], fro[1], fro[2]),
        (to[0], fro[1], to[2]),
        (to[0], to[1], fro[2]),
        (to[0], to[1], to[2]),
    ]
    rot = el.get("rotation")
    origin = el.get("origin", [(fro[i] + to[i]) / 2 for i in range(3)])
    if isinstance(rot, list) and any(abs(v) > 1e-6 for v in rot):
        # Blockbench [x,y,z] degrees — apply non-zero axes (vanilla export uses one)
        for axis, ang in (("x", rot[0]), ("y", rot[1]), ("z", rot[2])):
            if abs(ang) < 1e-6:
                continue
            corners = [rotate_point(*p, origin, axis, ang) for p in corners]
    xs = [p[0] for p in corners]
    ys = [p[1] for p in corners]
    zs = [p[2] for p in corners]
    return min(xs), min(ys), min(zs), max(xs), max(ys), max(zs)


def scale_uv(uv: list[float], res_w: float, res_h: float) -> list[float]:
    """Blockbench usa pixels da resolução; o Minecraft sempre trata 16 = 100% do sprite."""
    return [
        uv[0] * 16.0 / res_w,
        uv[1] * 16.0 / res_h,
        uv[2] * 16.0 / res_w,
        uv[3] * 16.0 / res_h,
    ]


def face_to_mc(face: dict, res_w: float, res_h: float) -> dict | None:
    if not face or face.get("texture") is None:
        return None
    out: dict = {"uv": scale_uv(face["uv"], res_w, res_h), "texture": "#0"}
    if "rotation" in face:
        out["rotation"] = face["rotation"]
    return out


def element_to_mc(el: dict, res_w: float, res_h: float) -> dict:
    faces = {}
    for side, face in el.get("faces", {}).items():
        mc = face_to_mc(face, res_w, res_h)
        if mc:
            faces[side] = mc
    item: dict = {
        "name": el.get("name", "cube"),
        "from": el["from"],
        "to": el["to"],
        "faces": faces,
    }
    rot = el.get("rotation")
    origin = el.get("origin", [8, 8, 8])
    if isinstance(rot, list):
        # Prefer single non-zero axis for vanilla JSON
        axes = [("x", rot[0]), ("y", rot[1]), ("z", rot[2])]
        nonzero = [(a, v) for a, v in axes if abs(v) > 1e-6]
        if nonzero:
            axis, angle = nonzero[0]
            # Snap to allowed angles if close; keep exact Blockbench value otherwise
            item["rotation"] = {"angle": angle, "axis": axis, "origin": origin}
    if el.get("shade") is False:
        item["shade"] = False
    return item


def main() -> None:
    bb = json.loads(SRC_BB.read_text(encoding="utf-8"))
    res = bb.get("resolution") or {}
    res_w = float(res.get("width") or 32)
    res_h = float(res.get("height") or 32)
    elements = [element_to_mc(el, res_w, res_h) for el in bb["elements"]]

    model = {
        "texture_size": [int(res_w), int(res_h)],
        # Espada/skull usam pixels transparentes no atlas — sem cutout fica preto/zoado.
        "render_type": "minecraft:cutout",
        "textures": {
            "0": "craftlevel:block/arena_portal",
            "particle": "craftlevel:block/arena_portal",
        },
        "elements": elements,
        "display": {
            "gui": {
                "rotation": [30, 225, 0],
                "translation": [0, -2.5, 0],
                "scale": [0.45, 0.45, 0.45],
            },
            "ground": {"translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"translation": [0, -2, 0], "scale": [0.4, 0.4, 0.4]},
            "thirdperson_righthand": {
                "rotation": [75, 45, 0],
                "translation": [0, 2.5, 0],
                "scale": [0.3, 0.3, 0.3],
            },
            "firstperson_righthand": {
                "rotation": [0, 135, 0],
                "translation": [0, 1, 0],
                "scale": [0.35, 0.35, 0.35],
            },
        },
    }

    OUT_BLOCK.mkdir(parents=True, exist_ok=True)
    OUT_TEX.mkdir(parents=True, exist_ok=True)

    (OUT_BLOCK / "arena_portal.json").write_text(
        json.dumps(model, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    (OUT_BLOCK / "arena_portal_part.json").write_text(
        json.dumps(
            {"textures": {"particle": "craftlevel:block/arena_portal"}, "elements": []},
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )

    shutil.copy2(SRC_BB, OUT_BLOCK / "arena_portal.bbmodel")
    shutil.copy2(SRC_TEX, OUT_TEX / "arena_portal.png")
    # Keep a copy next to bbmodel for Blockbench relative_path
    shutil.copy2(SRC_TEX, OUT_BLOCK / "arena_portal.png")

    boxes = []
    for el in bb["elements"]:
        mn_x, mn_y, mn_z, mx_x, mx_y, mx_z = element_aabb(el)
        if mx_x - mn_x < 1e-4 or mx_y - mn_y < 1e-4 or mx_z - mn_z < 1e-4:
            continue
        boxes.append((mn_x, mn_y, mn_z, mx_x, mx_y, mx_z))

    lines = [
        "    private static final double[][] MODEL_BOXES = {",
    ]
    for b in boxes:
        lines.append(
            "            {{{0:.4f}D, {1:.4f}D, {2:.4f}D, {3:.4f}D, {4:.4f}D, {5:.4f}D}},".format(*b)
        )
    lines.append("    };")
    OUT_BOXES.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(f"Wrote {OUT_BLOCK / 'arena_portal.json'} ({len(elements)} elements)")
    print(f"Wrote texture {OUT_TEX / 'arena_portal.png'}")
    print(f"Wrote {OUT_BOXES} ({len(boxes)} boxes)")
    ys = [b[1] for b in boxes] + [b[4] for b in boxes]
    print(f"Y range pixels: {min(ys):.2f} .. {max(ys):.2f}")


if __name__ == "__main__":
    main()
