#!/usr/bin/env python3
"""Upgrades generated resources to the Minecraft 26.1 formats. Safe to run repeatedly.

Usage: python3 tools/port26.py <project root>
 - recipes: {"item": "x"} / {"tag": "t"} ingredients -> "x" / "#t"
 - writes assets/<ns>/items/<name>.json item model definitions for every models/item/<name>.json
 - armor: textures/models/armor/<m>_layer_1/2.png -> equipment asset + entity/equipment textures
"""
import json, pathlib, shutil, sys

root = pathlib.Path(sys.argv[1]) / "src/main/resources"


def ingredient(v):
    if isinstance(v, dict):
        if "item" in v and len(v) == 1:
            return v["item"]
        if "tag" in v and len(v) == 1:
            return "#" + v["tag"]
        return v
    if isinstance(v, list):
        flat = [ingredient(x) for x in v]
        if all(isinstance(x, str) and not x.startswith("#") for x in flat):
            return flat if len(flat) > 1 else flat[0]
        return flat
    return v


for f in root.glob("data/*/recipe/**/*.json"):
    r = json.loads(f.read_text())
    if "key" in r:
        r["key"] = {k: ingredient(v) for k, v in r["key"].items()}
    if "ingredients" in r:
        r["ingredients"] = [ingredient(v) for v in r["ingredients"]]
    for k in ("ingredient", "base", "addition", "template"):
        if k in r:
            r[k] = ingredient(r[k])
    if "result" in r and isinstance(r["result"], dict) and "item" in r["result"]:
        r["result"]["id"] = r["result"].pop("item")
    f.write_text(json.dumps(r, indent=2))

for ns in (root / "assets").iterdir():
    models = ns / "models/item"
    if models.is_dir():
        out = ns / "items"
        out.mkdir(exist_ok=True)
        for m in models.glob("*.json"):
            target = out / m.name
            if not target.exists():
                target.write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"{ns.name}:item/{m.stem}"}}, indent=2))
    armor = ns / "textures/models/armor"
    if armor.is_dir():
        for l1 in armor.glob("*_layer_1.png"):
            mat = l1.name[: -len("_layer_1.png")]
            l2 = armor / f"{mat}_layer_2.png"
            for kind, src in (("humanoid", l1), ("humanoid_leggings", l2)):
                d = ns / "textures/entity/equipment" / kind
                d.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(src, d / f"{mat}.png")
            eq = ns / "equipment"
            eq.mkdir(exist_ok=True)
            (eq / f"{mat}.json").write_text(json.dumps({"layers": {
                "humanoid": [{"texture": f"{ns.name}:{mat}"}],
                "humanoid_leggings": [{"texture": f"{ns.name}:{mat}"}]}}, indent=2))
print("port26: resources upgraded")
