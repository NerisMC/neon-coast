#!/usr/bin/env python3
"""Builds Pip's Party Palace for Bedrock: a behavior pack + resource pack, zipped as one .mcaddon.
Textures and sounds come from the Java mod's generated assets (../pips)."""
import json, os, shutil, uuid, zipfile
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
JAVA = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "pipsparty")
OUT = os.path.join(HERE, "out")
BP, RP = os.path.join(OUT, "PipsPartyPalace_BP"), os.path.join(OUT, "PipsPartyPalace_RP")
NS = "pipsparty"
VERSION = [1, 0, 0]
MIN_ENGINE = [1, 21, 90]
U = lambda s: str(uuid.uuid5(uuid.NAMESPACE_DNS, "pipsparty.bedrock." + s))   # stable UUIDs across rebuilds


def mk(p):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def wj(p, o):
    json.dump(o, open(mk(p), "w"), indent=1)


def copy(src, dst):
    shutil.copyfile(src, mk(dst))


# ====================================================================== manifests
def manifests():
    wj(f"{RP}/manifest.json", {"format_version": 2, "header": {"name": "Pip's Party Palace (Resources)", "description": "Textures, models and sounds for Pip's Party Palace",
                                                                "uuid": U("rp"), "version": VERSION, "min_engine_version": MIN_ENGINE},
                               "modules": [{"type": "resources", "uuid": U("rp-mod"), "version": VERSION}]})
    wj(f"{BP}/manifest.json", {"format_version": 2, "header": {"name": "Pip's Party Palace", "description": "Night shifts, animatronics, cameras and your own pizzeria franchise.",
                                                                "uuid": U("bp"), "version": VERSION, "min_engine_version": MIN_ENGINE},
                               "modules": [{"type": "data", "uuid": U("bp-data"), "version": VERSION},
                                           {"type": "script", "language": "javascript", "uuid": U("bp-script"), "version": VERSION, "entry": "scripts/main.js"}],
                               "dependencies": [{"uuid": U("rp"), "version": VERSION},
                                                {"module_name": "@minecraft/server", "version": "2.0.0"},
                                                {"module_name": "@minecraft/server-ui", "version": "2.0.0"}]})
    icon = Image.open(f"{JAVA}/textures/block/catalog_cover.png").resize((128, 128), Image.NEAREST)
    icon.save(mk(f"{BP}/pack_icon.png")); icon.save(mk(f"{RP}/pack_icon.png"))


# ====================================================================== blocks
# geometry elements: (from, to, {face: material}) in Java block coordinates (0..16), default material "*"
def E(fr, to, **faces):
    return (fr, to, faces)


PROPS = {
    "party_table": ([E([0, 11, 0], [16, 12, 16], north="top", south="top", east="top", west="top", up="top", down="top"),
                     E([1, 0, 1], [3, 11, 3]), E([13, 0, 1], [15, 11, 3]), E([1, 0, 13], [3, 11, 15]), E([13, 0, 13], [15, 11, 15])],
                    {"*": "metal_leg", "top": "party_table"}, [16, 12, 16]),
    "party_chair": ([E([3, 7, 3], [13, 8, 13], **{f: "seat" for f in "north south east west up down".split()}),
                     E([3, 8, 12], [13, 16, 13], **{f: "seat" for f in "north south east west up down".split()}),
                     E([3, 0, 3], [4, 7, 4]), E([12, 0, 3], [13, 7, 4]), E([3, 0, 12], [4, 7, 13]), E([12, 0, 12], [13, 7, 13])],
                    {"*": "metal_leg", "seat": "party_chair"}, [10, 14, 10]),
    "arcade_cabinet": ([E([1, 0, 1], [15, 16, 15], north="front", up="top")], {"*": "arcade_side", "front": "arcade_front", "top": "desk_metal"}, [14, 16, 14]),
    "prize_counter": ([E([0, 0, 0], [16, 16, 16], north="front", up="top")], {"*": "register_side", "front": "prize_counter", "top": "steel"}, [16, 16, 16]),
    "ticket_machine": ([E([3, 0, 3], [13, 16, 13], north="front")], {"*": "desk_metal", "front": "ticket_machine"}, [10, 16, 10]),
    "pizza_oven": ([E([0, 0, 0], [16, 16, 16], north="front")], {"*": "brick", "front": "pizza_oven"}, [16, 16, 16]),
    "kitchen_counter": ([E([0, 0, 0], [16, 16, 16], north="front", up="top")], {"*": "steel", "front": "kitchen_front", "top": "steel"}, [16, 16, 16]),
    "gift_box": ([E([2, 0, 2], [14, 12, 14], up="top"), E([3, 12, 3], [13, 13, 13], **{f: "top" for f in "north south east west up down".split()})],
                 {"*": "gift_box", "top": "gift_box_top"}, [12, 13, 12]),
    "security_desk": ([E([0, 12, 0], [16, 13, 16], up="top"), E([0, 0, 13], [16, 12, 15])], {"*": "desk_metal", "top": "desk_top"}, [16, 13, 16]),
    "desk_fan": ([E([6, 0, 6], [10, 1, 10]), E([7.5, 1, 7.5], [8.5, 6, 8.5]), E([4, 5, 7], [12, 13, 9], north="front", south="front")],
                 {"*": "desk_metal", "front": "fan_front"}, [8, 13, 8]),
    "office_monitor": ([E([2, 0, 3], [14, 12, 13], north="front")], {"*": "desk_metal", "front": "monitor_front"}, [12, 12, 10]),
    "time_clock": ([E([3, 0, 3], [13, 16, 13], north="front")], {"*": "desk_metal", "front": "time_clock"}, [10, 16, 10]),
    "pizzeria_register": ([E([2, 0, 3], [14, 12, 13], north="front", up="front")], {"*": "register_side", "front": "register_front"}, [12, 12, 10]),
    "franchise_catalog": ([E([2, 0, 2], [14, 3, 14], up="cover"), E([2, 3, 6], [14, 15, 7], north="cover", south="cover")],
                          {"*": "catalog_pages", "cover": "catalog_cover"}, [12, 15, 12]),
}
WALL_PROPS = ["poster_pip", "poster_rusty", "poster_marla", "poster_gnash", "poster_rules", "employees_only_sign", "exit_sign"]
CUBES = ["checker_floor", "party_wall_red", "party_wall_blue", "party_wall_purple", "party_wall_trim", "confetti_wall", "ceiling_tile", "office_wall", "stage_floor"]
PASSABLE = ["stage_curtain", "star_curtain", "ball_pit"]
SCRIPTED = {"door_button": "door_button", "light_button": "light_button", "time_clock": "time_clock", "pizzeria_register": "register", "franchise_catalog": "catalog"}
LIGHT = {"arcade_cabinet": 7, "prize_counter": 5, "pizza_oven": 10, "office_monitor": 6, "exit_sign": 9}
NAMES = {"pizzeria_register": "The Register", "party_wall_trim": "Checkered Party Trim"}


def geo(name, elements, size=16):
    bones = []
    cubes = []
    for fr, to, faces in elements:
        w, h, d = (to[0] - fr[0], to[1] - fr[1], to[2] - fr[2])
        uv = {}
        for f in ("north", "south", "east", "west", "up", "down"):
            fw, fh = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}[f]
            uv[f] = {"uv": [0, 0], "uv_size": [max(fw, 0.01), max(fh, 0.01)], "material_instance": faces.get(f, "*")}
        cubes.append({"origin": [fr[0] - 8, fr[1], fr[2] - 8], "size": [w, h, d], "uv": uv})
    bones.append({"name": "root", "pivot": [0, 0, 0], "cubes": cubes})
    wj(f"{RP}/models/blocks/{name}.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NS}.{name}", "texture_width": size, "texture_height": size,
                        "visible_bounds_width": 2, "visible_bounds_height": 2, "visible_bounds_offset": [0, 0.5, 0]}, "bones": bones}]})
    return f"geometry.{NS}.{name}"


def tex_key(n):
    return f"{NS}_{n}"


TERRAIN = {}
ITEMS_TEX = {}


def block_tex(n):
    if n not in TERRAIN:
        copy(f"{JAVA}/textures/block/{n}.png", f"{RP}/textures/blocks/{NS}/{n}.png")
        TERRAIN[tex_key(n)] = {"textures": f"textures/blocks/{NS}/{n}"}
    return tex_key(n)


def mats(m, method="alpha_test"):
    return {k: {"texture": block_tex(v), "render_method": method} for k, v in m.items()}


FACING_PERMS = [{"condition": f"q.block_state('minecraft:cardinal_direction') == '{d}'", "components": {"minecraft:transformation": {"rotation": [0, r, 0]}}}
                for d, r in (("north", 0), ("west", 90), ("south", 180), ("east", -90))]
FACING_TRAIT = {"minecraft:placement_direction": {"enabled_states": ["minecraft:cardinal_direction"], "y_rotation_offset": 180}}


def box(size):
    w, h, d = size
    return {"origin": [-w / 2, 0, -d / 2], "size": [w, h, d]}


def write_block(name, components, category="construction", states=None, traits=None, perms=None, group=None):
    desc = {"identifier": f"{NS}:{name}", "menu_category": {"category": category}}
    if group:
        desc["menu_category"]["group"] = group
    if states:
        desc["states"] = states
    if traits:
        desc["traits"] = traits
    comps = {"minecraft:destructible_by_mining": {"seconds_to_destroy": 1.0}, "minecraft:destructible_by_explosion": {"explosion_resistance": 6}}
    comps.update(components)
    if name in LIGHT:
        comps["minecraft:light_emission"] = LIGHT[name]
    if name in SCRIPTED:
        comps[f"{NS}:{SCRIPTED[name]}"] = {}
    body = {"description": desc, "components": comps}
    if perms:
        body["permutations"] = perms
    wj(f"{BP}/blocks/{name}.json", {"format_version": "1.21.90", "minecraft:block": body})


def blocks():
    for n in CUBES:
        write_block(n, {"minecraft:geometry": "minecraft:geometry.full_block", "minecraft:material_instances": mats({"*": n}, "opaque")})
    for n in ["door_button", "light_button"]:
        write_block(n, {"minecraft:geometry": "minecraft:geometry.full_block", "minecraft:material_instances": mats({"*": n}, "opaque")}, "items")
    for n in PASSABLE:
        write_block(n, {"minecraft:geometry": "minecraft:geometry.full_block", "minecraft:material_instances": mats({"*": n}, "opaque"),
                        "minecraft:collision_box": False})
    g = geo("cross", [(E([0, 0, 8], [16, 16, 8.01]))])
    cross = json.load(open(f"{RP}/models/blocks/cross.geo.json"))
    cross["minecraft:geometry"][0]["bones"] = [
        {"name": "a", "pivot": [0, 0, 0], "rotation": [0, 45, 0], "cubes": [{"origin": [-8, 0, 0], "size": [16, 16, 0], "uv": {"north": {"uv": [0, 0], "uv_size": [16, 16]}, "south": {"uv": [0, 0], "uv_size": [16, 16]}}}]},
        {"name": "b", "pivot": [0, 0, 0], "rotation": [0, -45, 0], "cubes": [{"origin": [-8, 0, 0], "size": [16, 16, 0], "uv": {"north": {"uv": [0, 0], "uv_size": [16, 16]}, "south": {"uv": [0, 0], "uv_size": [16, 16]}}}]}]
    wj(f"{RP}/models/blocks/cross.geo.json", cross)
    write_block("party_balloons", {"minecraft:geometry": g, "minecraft:material_instances": mats({"*": "party_balloons"}),
                                   "minecraft:collision_box": False, "minecraft:light_dampening": 0}, "items")

    for n, (els, m, size) in PROPS.items():
        write_block(n, {"minecraft:geometry": geo(n, els), "minecraft:material_instances": mats(m),
                        "minecraft:collision_box": box(size), "minecraft:selection_box": box(size), "minecraft:light_dampening": 0},
                    "items", traits=FACING_TRAIT, perms=FACING_PERMS)
    for n in WALL_PROPS:
        write_block(n, {"minecraft:geometry": geo(n, [E([0, 0, 15], [16, 16, 16], north="front")]),
                        "minecraft:material_instances": mats({"*": "office_wall", "front": n}),
                        "minecraft:collision_box": False, "minecraft:selection_box": {"origin": [-8, 0, 7], "size": [16, 16, 1]},
                        "minecraft:light_dampening": 0}, "items", traits=FACING_TRAIT, perms=FACING_PERMS)

    # security door: rolls down when closed
    closed_geo = geo("security_door_closed", [E([0, 0, 6], [16, 16, 10])])
    open_geo = geo("security_door_open", [E([0, 14, 6], [16, 16, 10])])
    write_block("security_door", {"minecraft:geometry": open_geo, "minecraft:material_instances": mats({"*": "security_door"}),
                                  "minecraft:collision_box": False, "minecraft:selection_box": {"origin": [-8, 14, -2], "size": [16, 2, 4]},
                                  "minecraft:light_dampening": 0, "minecraft:destructible_by_explosion": {"explosion_resistance": 1200}},
                "items", states={f"{NS}:closed": [False, True]},
                perms=[{"condition": f"q.block_state('{NS}:closed')", "components": {
                    "minecraft:geometry": closed_geo, "minecraft:collision_box": {"origin": [-8, 0, -2], "size": [16, 16, 4]},
                    "minecraft:selection_box": {"origin": [-8, 0, -2], "size": [16, 16, 4]}}}])
    write_block("hall_light", {"minecraft:geometry": "minecraft:geometry.full_block", "minecraft:material_instances": mats({"*": "hall_light"}, "opaque")},
                "items", states={f"{NS}:lit": [False, True]},
                perms=[{"condition": f"q.block_state('{NS}:lit')", "components": {
                    "minecraft:light_emission": 15, "minecraft:material_instances": mats({"*": "hall_light_on"}, "opaque")}}])
    wj(f"{RP}/textures/terrain_texture.json", {"resource_pack_name": NS, "texture_name": "atlas.terrain", "padding": 8, "num_mip_levels": 4,
                                               "texture_data": TERRAIN})
    all_blocks = CUBES + ["door_button", "light_button"] + PASSABLE + ["party_balloons"] + list(PROPS) + WALL_PROPS + ["security_door", "hall_light"]
    return all_blocks


# ====================================================================== items
ITEMS = {
    "pip_coin": ("Pip Coin", {"minecraft:max_stack_size": 64}),
    "camera_tablet": ("Camera Tablet", {"minecraft:max_stack_size": 1}),
    "override_chip": ("Override Chip", {"minecraft:max_stack_size": 1}),
    "pizzeria_kit": ("Pizzeria Kit", {"minecraft:max_stack_size": 1}),
    "security_camera": ("Security Camera", {"minecraft:max_stack_size": 16, "minecraft:entity_placer": {"entity": f"{NS}:security_camera"}}),
    "pizza_slice": ("Pizza Slice", {"minecraft:max_stack_size": 64, "minecraft:food": {"nutrition": 6, "saturation_modifier": 0.6},
                                    "minecraft:use_animation": "eat", "minecraft:use_modifiers": {"use_duration": 1.6, "movement_modifier": 0.35}}),
    "party_soda": ("Party Soda", {"minecraft:max_stack_size": 16, "minecraft:food": {"nutrition": 2, "saturation_modifier": 0.3, "can_always_eat": True},
                                  "minecraft:use_animation": "drink", "minecraft:use_modifiers": {"use_duration": 1.6, "movement_modifier": 0.35}}),
}


def items():
    for n, (_, comps) in ITEMS.items():
        copy(f"{JAVA}/textures/item/{n}.png", f"{RP}/textures/items/{NS}/{n}.png")
        ITEMS_TEX[tex_key(n)] = {"textures": f"textures/items/{NS}/{n}"}
        c = {"minecraft:icon": tex_key(n)}
        c.update(comps)
        wj(f"{BP}/items/{n}.json", {"format_version": "1.21.90", "minecraft:item": {
            "description": {"identifier": f"{NS}:{n}", "menu_category": {"category": "items" if n not in ("pizza_slice", "party_soda") else "nature"}},
            "components": c}})
    for w in BAND:
        copy(f"{JAVA}/textures/item/{w}_animatronic.png", f"{RP}/textures/items/{NS}/{w}_egg.png")
        ITEMS_TEX[tex_key(w + "_egg")] = {"textures": f"textures/items/{NS}/{w}_egg"}
    wj(f"{RP}/textures/item_texture.json", {"resource_pack_name": NS, "texture_name": "atlas.items", "texture_data": ITEMS_TEX})


# ====================================================================== the band (converted from the Java rig)
BAND = ["pip", "rusty", "marla", "gnash"]
INFO = {"pip": ("Pip the Raccoon", "#808090", "#8c3cc8", 0.24, 1.0), "rusty": ("Rusty the Robot Dog", "#ac5c2c", "#c81e28", 0.26, 1.05),
        "marla": ("Marla the Mole", "#765448", "#f08caa", 0.25, 1.0), "gnash": ("Gnash the Gator", "#407c3e", "#dcd282", 0.27, 1.9)}


def java_parts(c):
    """Mirror of AnimatronicModel.create: name -> (parent, pivot(offset from parent), rot(rad), [(tex u,v, x,y,z, w,h,d, mirror)])"""
    gator = c == "gnash"
    P = {}
    P["head"] = ("root", (0, 5, 0), (0, 0, 0), [(0, 0, -4, -7, -4, 8, 7, 8, False)])
    P["jaw"] = ("head", (0, -1, -4), (0, 0, 0), [(0, 48, -3, 0, -7, 6, 2, 7, False)] if gator else [(32, 0, -3, 0, -3, 6, 2, 3, False)])
    snout = {"pip": (-2, -1, -3, 4, 3, 3), "rusty": (-2, -1, -4, 4, 3, 4), "marla": (-2.5, -1, -3, 5, 3, 3), "gnash": (-3, -1, -7, 6, 3, 7)}[c]
    P["snout"] = ("head", (0, -4 if gator else -3, -4), (0, 0, 0), [(32, 6, *snout, False)])
    ears = {
        "pip": [((-3, -7, 0), (0, 0, -0.25), (-1, -3, -0.5, 2, 3, 1)), ((3, -7, 0), (0, 0, 0.25), (-1, -3, -0.5, 2, 3, 1))],
        "rusty": [((-4, -6, 0), (0, 0, 0.15), (-1, 0, -1.5, 1, 5, 3)), ((4, -6, 0), (0, 0, -0.15), (0, 0, -1.5, 1, 5, 3))],
        "marla": [((-3, -7, 0), (0, 0, 0), (-1, -1, -0.5, 2, 1, 1)), ((3, -7, 0), (0, 0, 0), (-1, -1, -0.5, 2, 1, 1))],
        "gnash": [((-2, -7, -2), (0, 0, 0), (-1.5, -2, -1, 3, 2, 2)), ((2, -7, -2), (0, 0, 0), (-1.5, -2, -1, 3, 2, 2))],
    }[c]
    P["right_ear"] = ("head", ears[0][0], ears[0][1], [(56, 16, *ears[0][2], False)])
    P["left_ear"] = ("head", ears[1][0], ears[1][1], [(56, 16, *ears[1][2], True)])
    P["body"] = ("root", (0, 5, 0), (0, 0, 0), [(0, 16, -5, 0, -3, 10, 10, 6, False)])
    tail = {"pip": ((0, 8, 3), (-0.5, 0, 0), (-1.5, -1.5, 0, 3, 3, 9)), "rusty": ((0, 7, 3), (0.6, 0, 0), (-1, -1, 0, 2, 2, 5)),
            "marla": ((0, 8, 3), (0, 0, 0), (-1, -1, 0, 2, 2, 2)), "gnash": ((0, 8, 3), (-0.9, 0, 0), (-2, -1.5, 0, 4, 3, 12))}[c]
    P["tail"] = ("body", tail[0], tail[1], [(32, 32, *tail[2], False)])
    ra = [(32, 16, -1.5, -1, -1.5, 3, 10, 3, False)]
    la = [(44, 16, -1.5, -1, -1.5, 3, 10, 3, False)]
    if c == "marla":
        ra.append((20, 48, -2, 9, -2, 4, 2, 4, False))
        la.append((20, 48, -2, 9, -2, 4, 2, 4, True))
    P["right_arm"] = ("root", (-6.5, 6, 0), (0, 0, 0), ra)
    P["left_arm"] = ("root", (6.5, 6, 0), (0, 0, 0), la)
    P["right_leg"] = ("root", (-2.5, 15, 0), (0, 0, 0), [(0, 32, -2, 0, -2, 4, 9, 4, False)])
    P["left_leg"] = ("root", (2.5, 15, 0), (0, 0, 0), [(16, 32, -2, 0, -2, 4, 9, 4, False)])
    return P


def band_geo(c):
    import math
    P = java_parts(c)
    absp = {"root": (0, 0, 0)}

    def ab(n):
        if n in absp:
            return absp[n]
        par, off, _, _ = P[n]
        pp = ab(par)
        absp[n] = (pp[0] + off[0], pp[1] + off[1], pp[2] + off[2])
        return absp[n]
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    for n, (par, off, rot, cubes) in P.items():
        px, py, pz = ab(n)
        bone = {"name": n, "parent": par, "pivot": [-px, 24 - py, pz]}
        if any(rot):
            bone["rotation"] = [math.degrees(rot[0]), -math.degrees(rot[1]), -math.degrees(rot[2])]
        bone["cubes"] = []
        for (u, v, x, y, z, w, h, d, mir) in cubes:
            cube = {"origin": [-(px + x + w), 24 - (py + y + h), pz + z], "size": [w, h, d], "uv": [u, v]}
            if mir:
                cube["mirror"] = True
            bone["cubes"].append(cube)
        bones.append(bone)
    wj(f"{RP}/models/entity/{c}.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NS}.{c}", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 3, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]}, "bones": bones}]})


def band_texture(c):
    """Skin with the eyes layer folded into the alpha channel: entity_emissive_alpha makes low-alpha pixels glow."""
    skin = Image.open(f"{JAVA}/textures/entity/{c}.png").convert("RGBA")
    eyes = Image.open(f"{JAVA}/textures/entity/{c}_eyes.png").convert("RGBA")
    px, ep = skin.load(), eyes.load()
    for y in range(skin.height):
        for x in range(skin.width):
            r, g, b, a = px[x, y]
            if ep[x, y][3] > 0:
                er, eg, eb, _ = ep[x, y]
                px[x, y] = (er, eg, eb, 0)        # full glow
            elif a == 0:
                px[x, y] = (0, 0, 0, 255)         # unused UV space: opaque, no glow
            else:
                px[x, y] = (r, g, b, 255)
    skin.save(mk(f"{RP}/textures/entity/{NS}/{c}.png"))


def band_entities():
    for c in BAND:
        name, base, over, speed, hunt = INFO[c]
        band_geo(c)
        band_texture(c)
        wj(f"{RP}/entity/{c}.entity.json", {"format_version": "1.10.0", "minecraft:client_entity": {"description": {
            "identifier": f"{NS}:{c}",
            "materials": {"default": "entity_emissive_alpha"},
            "textures": {"default": f"textures/entity/{NS}/{c}"},
            "geometry": {"default": f"geometry.{NS}.{c}"},
            "render_controllers": [f"controller.render.{NS}.default"],
            "spawn_egg": {"texture": tex_key(c + "_egg")},
            "animations": {"walk": f"animation.{NS}.walk", "look": f"animation.{NS}.look", "scare": f"animation.{NS}.scare",
                           "perform": f"animation.{NS}.perform", "hunt": f"animation.{NS}.hunt", "tail": f"animation.{NS}.tail"},
            "scripts": {"animate": ["walk", "tail", {"look": f"q.property('{NS}:mode') != 3"},
                                    {"scare": f"q.property('{NS}:mode') == 3"},
                                    {"perform": f"q.property('{NS}:mode') == 0 && q.modified_move_speed < 0.05"},
                                    {"hunt": f"q.property('{NS}:mode') == 2 || q.has_rider"}]}}}})
        groups = {
            f"{NS}:hunt": {
                "minecraft:behavior.nearest_attackable_target": {"priority": 2, "must_see": False, "reselect_targets": True, "within_radius": 32,
                                                                 "entity_types": [{"filters": {"all_of": [{"test": "is_family", "subject": "other", "value": "player"},
                                                                                                          {"test": "has_ability", "subject": "other", "operator": "!=", "value": "instabuild"}]},
                                                                                   "max_dist": 32}]},
                "minecraft:behavior.melee_attack": {"priority": 1, "speed_multiplier": hunt, "track_target": True},
                "minecraft:behavior.random_stroll": {"priority": 6, "speed_multiplier": 0.8},
            },
            f"{NS}:frozen": {"minecraft:movement": {"value": 0}},
            f"{NS}:possessed": {"minecraft:rideable": {"seat_count": 1, "family_types": ["player"], "interact_text": "Possess", "pull_in_entities": False,
                                                       "seats": {"position": [0, 1.05, 0]}},
                                "minecraft:input_ground_controlled": {}, "minecraft:movement": {"value": 0.3}},
        }
        wj(f"{BP}/entities/{c}.json", {"format_version": "1.21.0", "minecraft:entity": {
            "description": {"identifier": f"{NS}:{c}", "is_spawnable": True, "is_summonable": True,
                            "properties": {f"{NS}:mode": {"type": "int", "range": [0, 4], "default": 0, "client_sync": True}}},
            "component_groups": groups,
            "components": {
                "minecraft:type_family": {"family": ["animatronic", "monster", c]},
                "minecraft:health": {"value": 300, "max": 300},
                "minecraft:damage_sensor": {"triggers": [
                    {"on_damage": {"filters": {"test": "has_ability", "subject": "other", "value": "instabuild"}}, "deals_damage": True},
                    {"cause": "void", "deals_damage": True},
                    {"cause": "all", "deals_damage": False}]},
                "minecraft:attack": {"damage": 1},
                "minecraft:movement": {"value": speed},
                "minecraft:navigation.walk": {"can_path_over_water": True, "avoid_damage_blocks": True, "can_open_doors": True, "can_pass_doors": True},
                "minecraft:movement.basic": {}, "minecraft:jump.static": {}, "minecraft:can_climb": {},
                "minecraft:physics": {}, "minecraft:pushable": {"is_pushable": False, "is_pushable_by_piston": True},
                "minecraft:knockback_resistance": {"value": 1.0},
                "minecraft:collision_box": {"width": 0.6, "height": 1.6},
                "minecraft:scale": {"value": 1.45},
                "minecraft:persistent": {}, "minecraft:nameable": {}, "minecraft:fire_immune": {},
                "minecraft:breathable": {"total_supply": 15, "suffocate_time": 0, "breathes_water": True},
                "minecraft:behavior.look_at_player": {"priority": 8, "look_distance": 12},
                "minecraft:behavior.random_look_around": {"priority": 9},
                "minecraft:interact": {"interactions": [{"on_interact": {"filters": {"all_of": [
                    {"test": "has_equipment", "subject": "other", "domain": "hand", "value": f"{NS}:override_chip"},
                    {"test": "is_family", "subject": "other", "value": "player"}]}}, "interact_text": "Possess"}]},
            },
            "events": {
                f"{NS}:night": {"add": {"component_groups": [f"{NS}:hunt"]}},
                f"{NS}:day": {"remove": {"component_groups": [f"{NS}:hunt", f"{NS}:frozen"]}},
                f"{NS}:freeze": {"add": {"component_groups": [f"{NS}:frozen"]}},
                f"{NS}:unfreeze": {"remove": {"component_groups": [f"{NS}:frozen"]}},
                f"{NS}:possess": {"remove": {"component_groups": [f"{NS}:hunt", f"{NS}:frozen"]}, "add": {"component_groups": [f"{NS}:possessed"]}},
                f"{NS}:release": {"remove": {"component_groups": [f"{NS}:possessed"]}},
            }}})

    # animations shared by the whole band (bone names match the rig)
    swing = "math.cos(q.modified_distance_moved * 38.17) * 55 * q.modified_move_speed"
    wj(f"{RP}/animations/band.animation.json", {"format_version": "1.8.0", "animations": {
        f"animation.{NS}.walk": {"loop": True, "bones": {
            "right_leg": {"rotation": [swing, 0, 0]}, "left_leg": {"rotation": [f"-{swing}", 0, 0]},
            "right_arm": {"rotation": [f"-{swing} * 0.7", 0, 0]}, "left_arm": {"rotation": [f"{swing} * 0.7", 0, 0]}}},
        f"animation.{NS}.look": {"loop": True, "bones": {"head": {"rotation": ["q.target_x_rotation", "q.target_y_rotation", 0]}}},
        f"animation.{NS}.tail": {"loop": True, "bones": {"tail": {"rotation": [0, "math.sin(q.life_time * 140) * 14", 0]}}},
        f"animation.{NS}.scare": {"loop": True, "bones": {
            "right_arm": {"rotation": ["-132 + math.sin(q.life_time * 2400) * 8", 0, 20]},
            "left_arm": {"rotation": ["-132 - math.sin(q.life_time * 2400) * 8", 0, -20]},
            "jaw": {"rotation": [43, 0, 0]},
            "head": {"rotation": [-14, 0, "math.sin(q.life_time * 3800) * 7"]}}},
        f"animation.{NS}.perform": {"loop": True, "bones": {
            "head": {"rotation": [0, 0, "math.sin(q.life_time * 290) * 7"]},
            "right_arm": {"rotation": ["-34 + math.sin(q.life_time * 570) * 23", 0, 0]},
            "left_arm": {"rotation": ["-17 - math.sin(q.life_time * 570) * 17", 0, 0]},
            "jaw": {"rotation": ["math.max(0, math.sin(q.life_time * 1030)) * 20", 0, 0]}}},
        f"animation.{NS}.hunt": {"loop": True, "bones": {
            "right_arm": {"rotation": [-50, 0, 0]}, "left_arm": {"rotation": [-50, 0, 0]},
            "jaw": {"rotation": ["9 + math.max(0, math.sin(q.life_time * 460)) * 9", 0, 0]},
            "head": {"rotation": [0, 0, "math.sin(q.life_time * 80) * 10"]}}},
    }})
    wj(f"{RP}/render_controllers/{NS}.render_controllers.json", {"format_version": "1.8.0", "render_controllers": {
        f"controller.render.{NS}.default": {"geometry": "Geometry.default", "materials": [{"*": "Material.default"}], "textures": ["Texture.default"]}}})
    wj(f"{RP}/sounds.json", {"entity_sounds": {"entities": {f"{NS}:{c}": {"volume": 0.5, "pitch": [0.8, 1.0],
                                                                         "events": {"step": {"sound": f"{NS}.servo_step", "volume": 0.35}}} for c in BAND}}})


def camera_entity():
    s = Image.open(f"{JAVA}/textures/entity/security_camera.png")
    s.save(mk(f"{RP}/textures/entity/{NS}/security_camera.png"))
    # mirror of SecurityCameraRenderer.create (Java y-down, centred on the entity at +0.2)
    cubes = [((0, 0), (-2, -2, -4), (4, 4, 7)), ((0, 12), (-1.5, -1.5, -5), (3, 3, 1))]
    mount = [((24, 0), (-0.5, -10, -0.5), (1, 8, 1)), ((0, 17), (-1.5, -11, -1.5), (3, 1, 3))]
    to_b = lambda uv, o, s_: {"origin": [-(o[0] + s_[0]), 3.2 - (o[1] + s_[1]), o[2]], "size": list(s_), "uv": list(uv)}
    wj(f"{RP}/models/entity/security_camera.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NS}.security_camera", "texture_width": 32, "texture_height": 32,
                        "visible_bounds_width": 1.5, "visible_bounds_height": 1.5, "visible_bounds_offset": [0, 0.5, 0]},
        "bones": [{"name": "mount", "pivot": [0, 3.2, 0], "cubes": [to_b(*m) for m in mount]},
                  {"name": "cam", "pivot": [0, 3.2, 0], "cubes": [to_b(*c) for c in cubes]}]}]})
    wj(f"{RP}/animations/camera.animation.json", {"format_version": "1.8.0", "animations": {
        f"animation.{NS}.camera_pitch": {"loop": True, "bones": {"cam": {"rotation": ["q.body_x_rotation", 0, 0]}}}}})
    wj(f"{RP}/entity/security_camera.entity.json", {"format_version": "1.10.0", "minecraft:client_entity": {"description": {
        "identifier": f"{NS}:security_camera", "materials": {"default": "entity_alphatest"},
        "textures": {"default": f"textures/entity/{NS}/security_camera"}, "geometry": {"default": f"geometry.{NS}.security_camera"},
        "render_controllers": [f"controller.render.{NS}.default"], "animations": {"pitch": f"animation.{NS}.camera_pitch"},
        "scripts": {"animate": ["pitch"]}}}})
    wj(f"{BP}/entities/security_camera.json", {"format_version": "1.21.0", "minecraft:entity": {
        "description": {"identifier": f"{NS}:security_camera", "is_spawnable": False, "is_summonable": True},
        "components": {
            "minecraft:type_family": {"family": ["security_camera", "inanimate"]},
            "minecraft:health": {"value": 1, "max": 1},
            "minecraft:collision_box": {"width": 0.4, "height": 0.4},
            "minecraft:physics": {"has_gravity": False, "has_collision": False},
            "minecraft:pushable": {"is_pushable": False, "is_pushable_by_piston": False},
            "minecraft:knockback_resistance": {"value": 1.0},
            "minecraft:persistent": {}, "minecraft:nameable": {"always_show": False, "allow_name_tag_renaming": True},
            "minecraft:loot": {"table": "loot_tables/entities/security_camera.json"},
            "minecraft:damage_sensor": {"triggers": [{"on_damage": {"filters": {"test": "is_family", "subject": "other", "value": "player"}}, "deals_damage": True},
                                                     {"cause": "all", "deals_damage": False}]},
        }}})
    wj(f"{BP}/loot_tables/entities/security_camera.json", {"pools": [{"rolls": 1, "entries": [{"type": "item", "name": f"{NS}:security_camera"}]}]})


# ====================================================================== sounds, recipes, text
SOUNDS = {"jumpscare": "hostile", "servo_step": "hostile", "door_slam": "block", "door_bang": "hostile", "light_buzz": "block", "music_box": "hostile",
          "chime_6am": "ui", "camera_flip": "ui", "power_down": "block", "stage_band": "record", "gator_run": "hostile", "giggle": "hostile"}


def sounds():
    defs = {}
    for s, cat in SOUNDS.items():
        copy(f"{JAVA}/sounds/{s}.ogg", f"{RP}/sounds/{NS}/{s}.ogg")
        entry = {"name": f"sounds/{NS}/{s}"}
        if s in ("music_box", "stage_band"):
            entry["stream"] = True
        defs[f"{NS}.{s}"] = {"category": cat, "min_distance": 1.0, "max_distance": 48.0 if s in ("jumpscare", "music_box", "gator_run") else 24.0, "sounds": [entry]}
    wj(f"{RP}/sounds/sound_definitions.json", {"format_version": "1.20.20", "sound_definitions": defs})


def recipes():
    def shaped(name, pattern, key, count=1):
        wj(f"{BP}/recipes/{name}.json", {"format_version": "1.20.10", "minecraft:recipe_shaped": {
            "description": {"identifier": f"{NS}:{name}"}, "tags": ["crafting_table"], "pattern": pattern,
            "key": {k: {"item": v} for k, v in key.items()}, "result": {"item": f"{NS}:{name}", "count": count}}})
    shaped("franchise_catalog", ["PE", "BP"], {"P": "minecraft:paper", "E": "minecraft:emerald", "B": "minecraft:book"})
    shaped("pizzeria_register", ["III", "GRG", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:gold_ingot", "R": "minecraft:redstone"})
    shaped("time_clock", [" I ", "ICI", " I "], {"I": "minecraft:iron_ingot", "C": "minecraft:clock"})
    shaped("camera_tablet", ["III", "IGI", "IRI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "R": "minecraft:redstone"})
    shaped("security_camera", ["I", "G", "R"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "R": "minecraft:redstone"}, 2)
    shaped("checker_floor", ["WB", "BW"], {"W": "minecraft:white_concrete", "B": "minecraft:black_concrete"}, 4)
    shaped("pizza_slice", ["T", "B"], {"T": "minecraft:beetroot", "B": "minecraft:bread"}, 2)


def lang(all_blocks):
    lines = ["pack.name=Pip's Party Palace", "pack.description=Night shifts, animatronics, cameras and your own pizzeria franchise."]
    for b in all_blocks:
        lines.append(f"tile.{NS}:{b}.name={NAMES.get(b, b.replace('_', ' ').title())}")
    for n, (nice, _) in ITEMS.items():
        lines.append(f"item.{NS}:{n}={nice}")
        lines.append(f"item.{NS}:{n}.name={nice}")
    for c in BAND:
        lines.append(f"entity.{NS}:{c}.name={INFO[c][0]}")
        lines.append(f"item.spawn_egg.entity.{NS}:{c}.name={INFO[c][0]}")
    lines.append(f"entity.{NS}:security_camera.name=Security Camera")
    lines.append(f"action.interact.{NS}.possess=Possess")
    for pack in (BP, RP):
        open(mk(f"{pack}/texts/en_US.lang"), "w").write("\n".join(lines) + "\n")
        wj(f"{pack}/texts/languages.json", ["en_US"])


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    manifests()
    shutil.copytree(os.path.join(HERE, "src", "BP", "scripts"), f"{BP}/scripts")
    all_blocks = blocks()
    items()
    band_entities()
    camera_entity()
    sounds()
    recipes()
    lang(all_blocks)
    out = os.path.join(OUT, "PipsPartyPalace.mcaddon")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for pack in (BP, RP):
            for root, _, files in os.walk(pack):
                for f in files:
                    full = os.path.join(root, f)
                    z.write(full, os.path.relpath(full, OUT))
    n = sum(len(f) for _, _, f in os.walk(OUT))
    print("built", out, os.path.getsize(out) // 1024, "KB,", n, "files")


main()
