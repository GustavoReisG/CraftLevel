"""Insere craftlevel:death_merchant em defaults/villager.json."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "src/main/resources/assets/craftlevel/defaults/villager.json"

EMPTY_SLOT = {
    "item": "",
    "min": 0,
    "max": 0,
    "whitelist": [],
    "blacklist": [],
    "nbt": "",
}

SPAWNER_MOBS = [
    "zombie",
    "skeleton",
    "creeper",
    "spider",
    "cave_spider",
    "blaze",
    "magma_cube",
    "enderman",
    "witch",
    "drowned",
    "husk",
    "stray",
]

SPAWN_EGGS = [
    "minecraft:zombie_spawn_egg",
    "minecraft:skeleton_spawn_egg",
    "minecraft:creeper_spawn_egg",
    "minecraft:spider_spawn_egg",
    "minecraft:cave_spider_spawn_egg",
    "minecraft:blaze_spawn_egg",
    "minecraft:magma_cube_spawn_egg",
    "minecraft:enderman_spawn_egg",
    "minecraft:witch_spawn_egg",
    "minecraft:drowned_spawn_egg",
    "minecraft:husk_spawn_egg",
    "minecraft:stray_spawn_egg",
    "minecraft:phantom_spawn_egg",
    "minecraft:guardian_spawn_egg",
    "minecraft:hoglin_spawn_egg",
    "minecraft:piglin_brute_spawn_egg",
]


def mark_cost(min_cost: int, max_cost: int | None = None) -> dict:
    hi = min_cost if max_cost is None else max_cost
    return {
        "item": "craftlevel:darkness_mark",
        "min": min_cost,
        "max": hi,
        "whitelist": [],
        "blacklist": [],
        "nbt": "",
    }


def output_item(
    item_id: str,
    *,
    nbt: str = "",
    whitelist: list[str] | None = None,
) -> dict:
    return {
        "item": item_id,
        "min": 1,
        "max": 1,
        "whitelist": list(whitelist or []),
        "blacklist": [],
        "nbt": nbt,
    }


def trade(
    min_cost: int,
    max_cost: int,
    item_id: str,
    *,
    nbt: str = "",
    whitelist: list[str] | None = None,
    limit: int = 0,
) -> dict:
    return {
        "limit": limit,
        "input1": mark_cost(min_cost, max_cost),
        "input2": dict(EMPTY_SLOT),
        "output": output_item(item_id, nbt=nbt, whitelist=whitelist),
    }


def spawner_entry(mob: str) -> str:
    entity = f"minecraft:{mob}"
    nbt = (
        "{BlockEntityTag:{SpawnData:{entity:{id:\""
        + entity
        + "\"}},SpawnPotentials:[{weight:1,data:{entity:{id:\""
        + entity
        + "\"}}}]}}"
    )
    return f"minecraft:spawner{nbt}"


def main() -> None:
    data = json.loads(PATH.read_text(encoding="utf-8"))
    professions = data.setdefault("professions", {})
    trades = data.setdefault("trades", {})

    professions["craftlevel:death_merchant"] = {
        "station": "craftlevel:arena_portal"
    }

    trades["craftlevel:death_merchant"] = [
        trade(8, 12, "minecraft:wither_skeleton_skull"),
        trade(8, 12, "minecraft:phantom_membrane"),
        trade(10, 14, "minecraft:echo_shard"),
        trade(12, 16, "minecraft:shulker_shell"),
        trade(16, 20, "minecraft:zombie_spawn_egg", whitelist=SPAWN_EGGS),
        trade(20, 25, "minecraft:elytra"),
        trade(20, 25, "minecraft:trident"),
        trade(20, 25, "minecraft:sculk_catalyst"),
        trade(
            30,
            40,
            "minecraft:spawner",
            whitelist=[spawner_entry(mob) for mob in SPAWNER_MOBS],
        ),
    ]

    PATH.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("updated", PATH)
    print("trades:", len(trades["craftlevel:death_merchant"]))


if __name__ == "__main__":
    main()
