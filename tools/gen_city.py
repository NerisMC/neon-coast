#!/usr/bin/env python3
"""Building blocks, street props and the All City world type for Neon Coast."""
import json, os, random, sys
from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
MOD = "neoncoast"
A = f"{ROOT}/src/main/resources/assets/{MOD}"
D = f"{ROOT}/src/main/resources/data"
random.seed(21)


def mk(p):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def wj(p, o):
    json.dump(o, open(mk(p), "w"), indent=2)


def cl(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def sh(c, d):
    return cl((c[0] + d, c[1] + d, c[2] + d))


def nz(c, a=6):
    return sh(c, random.randint(-a, a))


def tile(name, fn, alpha=None):
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            c = fn(x, y)
            a = alpha(x, y) if alpha else 255
            im.putpixel((x, y), tuple(c[:3]) + (a,))
    im.save(mk(f"{A}/textures/block/{name}.png"))


# ---------------------------------------------------------------- building block textures
def textures():
    tile("concrete_panel", lambda x, y: (150, 150, 156) if (x == 0 or y == 0) else nz((205, 205, 210), 4))
    tile("dark_panel", lambda x, y: (40, 40, 46) if (x == 0 or y == 0) else nz((78, 80, 88), 4))
    def marble(x, y):
        v = (x * 3 + y * 5) % 23 == 0 or (x * 7 - y * 2) % 29 == 0
        return (180, 180, 186) if v else nz((236, 236, 240), 3)
    tile("marble", marble)
    def brick(x, y):
        row = y // 4
        off = 4 if row % 2 else 0
        if y % 4 == 3 or (x + off) % 8 == 7: return (200, 195, 185)
        return nz((150, 64, 48), 10)
    tile("brick_facade", brick)
    tile("metal_cladding", lambda x, y: sh((170, 172, 178), 25 if x % 4 == 0 else -15 if x % 4 == 2 else 0))
    tile("roof_gravel", lambda x, y: nz((110, 108, 104), 18))
    tile("garage_door", lambda x, y: (90, 92, 98) if y % 3 == 2 else nz((150, 152, 158), 4))
    tile("shop_awning", lambda x, y: (220, 40, 40) if (x // 4) % 2 == 0 else (240, 240, 235))
    def frame(x, y): return x in (0, 15) or y in (0, 15)
    tile("office_window", lambda x, y: (55, 58, 66) if (frame(x, y) or x == 7 or y == 7) else (70, 110, 150) if (x + y) % 9 else (190, 210, 230),
         lambda x, y: 255 if (frame(x, y) or x == 7 or y == 7) else 150)
    tile("glass_curtain_wall", lambda x, y: (180, 185, 195) if (x == 0 or y == 0) else (60, 120, 170) if (x - y) % 7 else (200, 225, 245),
         lambda x, y: 255 if (x == 0 or y == 0) else 165)
    asph = lambda: nz((48, 48, 52), 7)
    tile("crosswalk", lambda x, y: (235, 235, 230) if (x // 3) % 2 == 0 else asph())
    def manhole(x, y):
        d = (x - 7.5) ** 2 + (y - 7.5) ** 2
        if d < 36: return (60, 58, 56) if (x + y) % 3 else (85, 82, 78)
        if d < 44: return (35, 35, 35)
        return asph()
    tile("manhole", manhole)
    neon = {"red": (255, 40, 60), "blue": (40, 120, 255), "pink": (255, 70, 200), "green": (40, 255, 120), "yellow": (255, 220, 40)}
    for name, c in neon.items():
        def nfn(x, y, c=c):
            tube = x in (2, 13) or y in (2, 13) or (x == y and 4 <= x <= 11) or (x + y == 15 and 4 <= x <= 11)
            glow = x in (1, 3, 12, 14) or y in (1, 3, 12, 14)
            if tube: return sh(c, 60)
            if glow: return cl((c[0] * 0.45, c[1] * 0.45, c[2] * 0.45))
            return (18, 16, 22)
        tile(f"neon_{name}", nfn)
    # props: front / side / top
    tile("vending_machine_front", lambda x, y: (40, 120, 210) if 2 <= x <= 9 and 2 <= y <= 11 and (x % 3 and y % 3) else
         (230, 230, 230) if 2 <= x <= 9 and 2 <= y <= 11 else (30, 30, 30) if 11 <= x <= 13 and 5 <= y <= 9 else nz((200, 30, 30), 5))
    tile("vending_machine_side", lambda x, y: nz((190, 28, 28), 5))
    tile("dumpster_side", lambda x, y: (30, 80, 40) if y in (0, 3) else nz((45, 120, 60), 6))
    tile("dumpster_top", lambda x, y: (25, 25, 25) if x == 8 else nz((35, 90, 45), 5))
    tile("ac_unit_side", lambda x, y: (60, 62, 66) if y % 2 == 0 and 2 <= x <= 13 and 2 <= y <= 13 else nz((175, 177, 182), 4))
    tile("ac_unit_top", lambda x, y: (40, 40, 44) if ((x - 7.5) ** 2 + (y - 7.5) ** 2 < 25 and (x + y) % 2) else nz((165, 167, 172), 4))
    tile("fire_hydrant", lambda x, y: (240, 220, 60) if y in (0, 1) else nz((210, 30, 30), 8))
    def tl_front(x, y):
        for cy, col in ((3, (255, 50, 40)), (8, (255, 200, 40)), (13, (60, 230, 90))):
            if (x - 7.5) ** 2 + (y - cy) ** 2 < 5: return col
        return (28, 28, 30)
    tile("traffic_light_front", tl_front)
    tile("traffic_light_side", lambda x, y: nz((35, 35, 38), 3))
    tile("bench", lambda x, y: (60, 60, 62) if x in (1, 14) else nz((150, 105, 60), 8) if y % 4 else (110, 75, 40))
    tile("trash_can", lambda x, y: (40, 40, 42) if y in (0, 1) else nz((90, 92, 96), 6) if x % 3 else (70, 72, 76))


# ---------------------------------------------------------------- models / blockstates / data
CUBES = ["concrete_panel", "dark_panel", "marble", "brick_facade", "metal_cladding", "roof_gravel", "garage_door",
         "shop_awning", "office_window", "glass_curtain_wall", "crosswalk", "manhole"] + \
        [f"neon_{c}" for c in ["red", "blue", "pink", "green", "yellow"]]
# name: (x1,y1,z1,x2,y2,z2), front texture, side texture, top texture
PROPS = {
    "vending_machine": ((1, 0, 2, 15, 16, 14), "vending_machine_front", "vending_machine_side", "vending_machine_side"),
    "dumpster": ((0, 0, 2, 16, 12, 14), "dumpster_side", "dumpster_side", "dumpster_top"),
    "ac_unit": ((1, 0, 1, 15, 10, 15), "ac_unit_side", "ac_unit_side", "ac_unit_top"),
    "fire_hydrant": ((5, 0, 5, 11, 11, 11), "fire_hydrant", "fire_hydrant", "fire_hydrant"),
    "traffic_light": ((5, 0, 5, 11, 16, 11), "traffic_light_front", "traffic_light_side", "traffic_light_side"),
    "bench": ((0, 0, 4, 16, 7, 12), "bench", "bench", "bench"),
    "trash_can": ((4, 0, 4, 12, 12, 12), "trash_can", "trash_can", "trash_can"),
}
NAMES = {"concrete_panel": "Concrete Panel", "dark_panel": "Dark Panel", "marble": "Marble", "brick_facade": "Brick Facade",
         "metal_cladding": "Metal Cladding", "roof_gravel": "Roof Gravel", "garage_door": "Garage Door", "shop_awning": "Shop Awning",
         "office_window": "Office Window", "glass_curtain_wall": "Glass Curtain Wall", "crosswalk": "Crosswalk", "manhole": "Manhole",
         "neon_red": "Red Neon", "neon_blue": "Blue Neon", "neon_pink": "Pink Neon", "neon_green": "Green Neon", "neon_yellow": "Yellow Neon",
         "vending_machine": "Vending Machine", "dumpster": "Dumpster", "ac_unit": "AC Unit", "fire_hydrant": "Fire Hydrant",
         "traffic_light": "Traffic Light", "bench": "Bench", "trash_can": "Trash Can"}


def data():
    t = lambda n: f"{MOD}:block/{n}"
    for b in CUBES:
        wj(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": t(b)}})
        wj(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"{MOD}:block/{b}"}}})
        wj(f"{A}/models/item/{b}.json", {"parent": f"{MOD}:block/{b}"})
    for b, (box, front, side, top) in PROPS.items():
        x1, y1, z1, x2, y2, z2 = box
        faces = {"north": {"texture": "#front"}, "south": {"texture": "#side"}, "east": {"texture": "#side"},
                 "west": {"texture": "#side"}, "up": {"texture": "#top"}, "down": {"texture": "#top", "cullface": "down"}}
        wj(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/block",
            "textures": {"particle": t(side), "front": t(front), "side": t(side), "top": t(top)},
            "elements": [{"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces}]})
        wj(f"{A}/blockstates/{b}.json", {"variants": {
            "facing=north": {"model": f"{MOD}:block/{b}"}, "facing=east": {"model": f"{MOD}:block/{b}", "y": 90},
            "facing=south": {"model": f"{MOD}:block/{b}", "y": 180}, "facing=west": {"model": f"{MOD}:block/{b}", "y": 270}}})
        wj(f"{A}/models/item/{b}.json", {"parent": f"{MOD}:block/{b}"})
    allb = CUBES + list(PROPS)
    for b in allb:
        wj(f"{D}/{MOD}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    lp = f"{A}/lang/en_us.json"
    lang = json.load(open(lp))
    for b in allb:
        lang[f"block.{MOD}.{b}"] = NAMES[b]
    lang["generator.neoncoast.all_city"] = "All City"
    lang["biome.neoncoast.city"] = "City"
    wj(lp, lang)
    tp = f"{D}/minecraft/tags/block/mineable/pickaxe.json"
    tag = json.load(open(tp))
    for b in allb:
        if b not in ("bench", "shop_awning") and f"{MOD}:{b}" not in tag["values"]:
            tag["values"].append(f"{MOD}:{b}")
    wj(tp, tag)
    # recipes: stonecutter for the stone-ish blocks, crafting for neon and props
    R = f"{D}/{MOD}/recipe"
    def cut(name, src, n=1):
        wj(f"{R}/{name}.json", {"type": "minecraft:stonecutting", "ingredient": {"item": src}, "result": {"id": f"{MOD}:{name}", "count": n}})
    cut("concrete_panel", "minecraft:white_concrete"); cut("dark_panel", "minecraft:gray_concrete")
    cut("marble", "minecraft:quartz_block"); cut("brick_facade", "minecraft:bricks"); cut("roof_gravel", "minecraft:gravel")
    cut("crosswalk", f"{MOD}:asphalt"); cut("manhole", f"{MOD}:asphalt")
    def shaped(name, pat, key, n=1):
        wj(f"{R}/{name}.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pat,
            "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()}, "result": {"id": f"{MOD}:{name}", "count": n}})
    shaped("metal_cladding", ["II", "II"], {"I": "minecraft:iron_ingot"}, 8)
    shaped("garage_door", ["III", "I I", "III"], {"I": "minecraft:iron_ingot"}, 4)
    shaped("shop_awning", ["RWR"], {"R": "minecraft:red_wool", "W": "minecraft:white_wool"}, 3)
    shaped("office_window", ["GIG", "IGI", "GIG"], {"G": "minecraft:glass", "I": "minecraft:iron_nugget"}, 5)
    shaped("glass_curtain_wall", ["GGG", "GBG", "GGG"], {"G": "minecraft:glass", "B": "minecraft:light_blue_dye"}, 8)
    for c, dye in [("red", "red"), ("blue", "blue"), ("pink", "pink"), ("green", "lime"), ("yellow", "yellow")]:
        shaped(f"neon_{c}", ["GDG", "DSD", "GDG"], {"G": "minecraft:glass", "D": f"minecraft:{dye}_dye", "S": "minecraft:glowstone"}, 4)
    shaped("vending_machine", ["III", "IGI", "IRI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "R": "minecraft:redstone"})
    shaped("dumpster", ["I I", "IGI", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:green_dye"})
    shaped("ac_unit", ["III", "IRI", "III"], {"I": "minecraft:iron_nugget", "R": "minecraft:redstone"})
    shaped("fire_hydrant", [" I ", "IRI", " I "], {"I": "minecraft:iron_ingot", "R": "minecraft:red_dye"})
    shaped("traffic_light", ["IRI", "IYI", "IGI"], {"I": "minecraft:iron_ingot", "R": "minecraft:red_dye", "Y": "minecraft:yellow_dye", "G": "minecraft:lime_dye"})
    shaped("bench", ["PPP", "I I"], {"P": "#minecraft:planks", "I": "minecraft:iron_ingot"}, 2)
    shaped("trash_can", ["I I", "I I", "III"], {"I": "minecraft:iron_nugget"})

    # ---- All City world type: flat ground, a "city" biome whose only feature is wall-to-wall city
    wj(f"{D}/{MOD}/worldgen/configured_feature/all_city.json", {"type": f"{MOD}:all_city", "config": {}})
    wj(f"{D}/{MOD}/worldgen/placed_feature/all_city.json", {"feature": f"{MOD}:all_city", "placement": []})
    wj(f"{D}/{MOD}/worldgen/biome/city.json", {
        "has_precipitation": True, "temperature": 0.8, "downfall": 0.4,
        "effects": {"sky_color": 7907327, "fog_color": 12638463, "water_color": 4159204, "water_fog_color": 329011,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}},
        "spawners": {}, "spawn_costs": {}, "carvers": {},
        "features": [[], [], [], [], [f"{MOD}:all_city"], [], [], [], [], [], []]})
    wj(f"{D}/{MOD}/worldgen/world_preset/all_city.json", {"dimensions": {
        "minecraft:overworld": {"type": "minecraft:overworld", "generator": {"type": "minecraft:flat", "settings": {
            "biome": f"{MOD}:city", "features": True, "lakes": False, "structure_overrides": [],
            "layers": [{"block": "minecraft:bedrock", "height": 1}, {"block": "minecraft:stone", "height": 127},
                       {"block": "minecraft:grass_block", "height": 1}]}}},
        "minecraft:the_nether": {"type": "minecraft:the_nether", "generator": {"type": "minecraft:noise",
            "biome_source": {"type": "minecraft:multi_noise", "preset": "minecraft:nether"}, "settings": "minecraft:nether"}},
        "minecraft:the_end": {"type": "minecraft:the_end", "generator": {"type": "minecraft:noise",
            "biome_source": {"type": "minecraft:the_end"}, "settings": "minecraft:end"}}}})
    wj(f"{D}/minecraft/tags/worldgen/world_preset/normal.json", {"replace": False, "values": [f"{MOD}:all_city"]})


textures()
data()
print("city blocks + All City preset done")
