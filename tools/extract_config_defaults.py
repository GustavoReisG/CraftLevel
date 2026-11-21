"""Extrai DEFAULT_* text blocks de ConfigManager.java para assets/.../defaults/*.json."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/grgs/craftlevel/config/ConfigManager.java"
OUT = ROOT / "src/main/resources/assets/craftlevel/defaults"

NAME_MAP = {
    "DEFAULT_CONFIG": "config.json",
    "DEFAULT_PLAYER_STATS": "player_stats.json",
    "DEFAULT_MOB_STATS": "mob_stats.json",
    "DEFAULT_MOB_OVERRIDES": "mob_overrides.json",
    "DEFAULT_MOB_LEVELS": "mob_levels.json",
    "DEFAULT_CLIENT": "client.json",
    "DEFAULT_ECONOMY": "economy.json",
    "DEFAULT_SOULS": "souls.json",
    "DEFAULT_GLORY_KILLS": "glory_kills.json",
    "DEFAULT_ALTAR_CONVERSION": "altar_conversion.json",
    "DEFAULT_ALTAR_OF_DEATH": "altar_of_death.json",
}

PATTERN = re.compile(
    r"(?:public |private )?static final String (DEFAULT_\w+) = \"\"\"\n(.*?)\"\"\";",
    re.S,
)


def main() -> None:
    text = JAVA.read_text(encoding="utf-8")
    OUT.mkdir(parents=True, exist_ok=True)
    found = 0
    for name, body in PATTERN.findall(text):
        fname = NAME_MAP.get(name)
        if not fname:
            print("skip", name)
            continue
        data = json.loads(body)
        path = OUT / fname
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print("wrote", path.relative_to(ROOT))
        found += 1
    if found != len(NAME_MAP):
        raise SystemExit(f"expected {len(NAME_MAP)} files, wrote {found}")


if __name__ == "__main__":
    main()
