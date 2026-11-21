"""Gera assets/craftlevel/defaults/skills.json a partir das regras atuais do SkillsParser."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/craftlevel/defaults/skills.json"

# id -> maxLevel (SkillCatalog)
SKILLS: dict[str, int] = {
    "sense_of_prey": 5,
    "ghost_saturation": 3,
    "soul_anchor": 5,
    "spectral_climb": 3,
    "imbue_master": 5,
    "vampire_attack": 3,
    "spirit_return": 3,
    "carnage": 3,
    "supernova": 3,
    "protective_shockwave": 3,
    "soul_render": 3,
    "light_speed": 2,
    "darkness_light": 3,
    "miner_frenzy": 3,
    "last_chance": 1,
    "reinforced_bones": 3,
    "mind_fury": 2,
    "nether_elevation": 3,
    "nether_rush": 3,
    "prospector": 3,
    "frozen_mind": 3,
    "frozen_rush": 3,
    "ender_backpack": 1,
    "vampire_blood": 2,
    "quick_survive": 1,
    "blood_rush": 3,
    "strength_recovery": 3,
    "portable_crafting": 1,
    "goodnight": 1,
    "recall": 1,
    "sky_walker": 1,
    "inferno_bolt": 5,
    "soul_lance": 3,
    "blink_step": 2,
    "crippling_aura": 5,
    "terror_pulse": 2,
}


def min_player_level(skill_id: str, level: int) -> int:
    level = max(1, level)
    table: dict[str, dict[int, int] | int] = {
        "sense_of_prey": {1: 20, 2: 25, 3: 30, 4: 35},
        "ghost_saturation": 30,
        "soul_anchor": {1: 20, 2: 25, 3: 30, 4: 35},
        "spectral_climb": {1: 15, 2: 20},
        "imbue_master": {1: 20, 2: 25, 3: 30, 4: 35},
        "vampire_attack": {1: 20, 2: 25},
        "spirit_return": {1: 18, 2: 24},
        "carnage": {1: 22, 2: 28},
        "supernova": {1: 25, 2: 32},
        "protective_shockwave": {1: 18, 2: 24},
        "soul_render": {1: 20, 2: 26},
        "light_speed": {1: 20, 2: 28},
        "darkness_light": {1: 15, 2: 22},
        "miner_frenzy": {1: 18, 2: 24},
        "last_chance": 35,
        "reinforced_bones": {1: 12, 2: 20},
        "mind_fury": {1: 20, 2: 28},
        "nether_elevation": {1: 22, 2: 28},
        "nether_rush": {1: 22, 2: 28},
        "frozen_rush": {1: 22, 2: 28},
        "prospector": {1: 16, 2: 24},
        "frozen_mind": {1: 20, 2: 26},
        "ender_backpack": 15,
        "portable_crafting": 10,
        "goodnight": 12,
        "recall": 16,
        "sky_walker": 20,
        "inferno_bolt": {1: 20, 2: 26, 3: 32, 4: 38},
        "soul_lance": {1: 22, 2: 28},
        "blink_step": {1: 18, 2: 28},
        "crippling_aura": {1: 16, 2: 22, 3: 28, 4: 34},
        "terror_pulse": {1: 22, 2: 30},
        "vampire_blood": {1: 22, 2: 30},
        "quick_survive": 18,
        "blood_rush": {1: 18, 2: 24},
        "strength_recovery": {1: 16, 2: 22},
    }
    entry = table.get(skill_id)
    if entry is None:
        return 1 + max(0, level - 1) * 5
    if isinstance(entry, int):
        return entry
    defaults = {
        "sense_of_prey": 40,
        "soul_anchor": 40,
        "spectral_climb": 25,
        "imbue_master": 40,
        "vampire_attack": 30,
        "spirit_return": 30,
        "carnage": 34,
        "supernova": 40,
        "protective_shockwave": 30,
        "soul_render": 32,
        "darkness_light": 30,
        "miner_frenzy": 30,
        "reinforced_bones": 28,
        "nether_elevation": 34,
        "nether_rush": 34,
        "frozen_rush": 34,
        "prospector": 32,
        "frozen_mind": 32,
        "inferno_bolt": 45,
        "soul_lance": 35,
        "crippling_aura": 40,
        "blood_rush": 30,
        "strength_recovery": 28,
    }
    return entry.get(level, defaults.get(skill_id, 1 + max(0, level - 1) * 5))


def main() -> None:
    root: dict[str, list[dict[str, int]]] = {}
    for skill_id, max_level in SKILLS.items():
        rows = []
        for level in range(1, max_level + 1):
            rows.append(
                {
                    "level": level,
                    "skill_point_cost": 1,
                    "min_player_level": min_player_level(skill_id, level),
                }
            )
        root[skill_id] = rows
    OUT.write_text(json.dumps(root, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("wrote", OUT.relative_to(ROOT), "skills=", len(root))


if __name__ == "__main__":
    main()
