#!/usr/bin/env python3
"""Pip's Party Palace assets: every texture, model, blockstate, sound, loot table, recipe and lang entry.
Run from the project root: python3 tools/gen_assets.py"""
import json, math, os, random, subprocess, tempfile
import numpy as np
from PIL import Image, ImageDraw

MOD = "pipsparty"
RES = "common/src/main/resources"
A = f"{RES}/assets/{MOD}"
D = f"{RES}/data/{MOD}"
rng = random.Random(1987)


def mk(p):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def wj(p, o):
    json.dump(o, open(mk(p), "w"), indent=2)


def cl(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def sh(c, d):
    return cl((c[0] + d, c[1] + d, c[2] + d)) + ((c[3],) if len(c) == 4 else ())


def noise(img, amt=10, seed=0):
    r = random.Random(seed)
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            p = px[x, y]
            if len(p) == 4 and p[3] == 0:
                continue
            d = r.randint(-amt, amt)
            px[x, y] = sh(p, d)
    return img


def tex(name, size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def save_block(img, name):
    img.save(mk(f"{A}/textures/block/{name}.png"))


def save_item(img, name):
    img.save(mk(f"{A}/textures/item/{name}.png"))


def fillr(img, x0, y0, x1, y1, c):
    ImageDraw.Draw(img).rectangle([x0, y0, x1, y1], fill=c)


# ====================================================================== block textures
RED, BLUE, PURPLE = (190, 40, 50), (40, 70, 170), (110, 50, 150)
BLACK, WHITE = (24, 24, 28), (236, 234, 228)


def solid(c, seed, amt=8):
    img = Image.new("RGBA", (16, 16), c + (255,))
    return noise(img, amt, seed)


def block_textures():
    # checker floor: 4px checks
    img = tex("x")
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (BLACK if ((x // 4) + (y // 4)) % 2 else WHITE) + (255,))
    save_block(noise(img, 6, 1), "checker_floor")

    for name, c, seed in [("party_wall_red", RED, 2), ("party_wall_blue", BLUE, 3), ("party_wall_purple", PURPLE, 4)]:
        img = solid(c, seed, 6)
        d = ImageDraw.Draw(img)
        for i in range(0, 16, 4):   # vertical wallpaper stripes
            d.line([(i, 0), (i, 15)], fill=sh(c, -18) + (255,))
        # little stars
        for sx, sy in [(2, 3), (10, 9), (6, 13)]:
            star = (250, 210, 60, 255)
            for dx, dy in [(0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)]:
                img.putpixel(((sx + dx) % 16, (sy + dy) % 16), star)
        save_block(img, name)

    img = tex("x")
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (BLACK if ((x // 2) + (y // 2)) % 2 else WHITE) + (255,))
    fillr(img, 0, 0, 15, 1, RED + (255,))
    fillr(img, 0, 14, 15, 15, RED + (255,))
    save_block(img, "party_wall_trim")

    img = solid(WHITE, 5, 4)
    r = random.Random(5)
    for _ in range(40):
        img.putpixel((r.randrange(16), r.randrange(16)), rng.choice([(230, 60, 60), (60, 120, 230), (250, 200, 40), (60, 190, 90), (200, 80, 210)]) + (255,))
    save_block(img, "confetti_wall")

    img = solid((214, 210, 196), 6, 5)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(160, 156, 146, 255))
    for _ in range(14):
        img.putpixel((r.randrange(1, 15), r.randrange(1, 15)), (180, 176, 164, 255))
    img.putpixel((11, 4), (150, 120, 70, 255)); img.putpixel((12, 4), (150, 120, 70, 255)); img.putpixel((12, 5), (160, 130, 80, 255))  # water stain
    save_block(img, "ceiling_tile")

    img = solid((120, 122, 126), 7, 6)
    d = ImageDraw.Draw(img)
    for y in (0, 8):
        d.line([(0, y), (15, y)], fill=(90, 92, 96, 255))
    d.line([(0, 0), (0, 7)], fill=(90, 92, 96, 255)); d.line([(8, 8), (8, 15)], fill=(90, 92, 96, 255))
    save_block(img, "office_wall")

    img = solid((150, 100, 56), 8, 7)
    d = ImageDraw.Draw(img)
    for y in (3, 7, 11, 15):
        d.line([(0, y), (15, y)], fill=(105, 66, 34, 255))
    for x, y in [(5, 0), (12, 4), (3, 8), (9, 12)]:
        img.putpixel((x, y), (105, 66, 34, 255))
    save_block(img, "stage_floor")

    for name, c, stars in [("stage_curtain", (165, 20, 34), False), ("star_curtain", (70, 30, 110), True)]:
        img = tex("x")
        for x in range(16):
            fold = int(26 * math.sin(x / 16 * math.pi * 4))
            for y in range(16):
                img.putpixel((x, y), sh(c, fold) + (255,))
        if stars:
            for sx, sy in [(3, 3), (11, 6), (6, 11), (13, 13)]:
                for dx, dy in [(0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)]:
                    img.putpixel(((sx + dx) % 16, (sy + dy) % 16), (250, 215, 70, 255))
        else:
            fillr(img, 0, 0, 15, 1, (220, 180, 50, 255))
        save_block(noise(img, 4, 9), name)

    img = Image.new("RGBA", (16, 16), (30, 30, 40, 255))
    d = ImageDraw.Draw(img)
    cols = [(230, 50, 50), (50, 120, 230), (250, 210, 40), (60, 200, 90), (220, 80, 220)]
    for yy in range(-2, 16, 4):
        for xx in range(-2, 16, 4):
            ox = xx + (2 if (yy // 4) % 2 else 0)
            c = r.choice(cols)
            d.ellipse([ox, yy, ox + 3, yy + 3], fill=c + (255,))
            img.putpixel((max(0, min(15, ox + 1)), max(0, min(15, yy + 1))), (255, 255, 255, 255))
    save_block(img, "ball_pit")

    img = tex("x")
    d = ImageDraw.Draw(img)
    for (cx, cy, c) in [(4, 4, (230, 50, 60)), (11, 3, (60, 130, 230)), (8, 8, (250, 200, 40))]:
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 4], fill=c + (255,))
        img.putpixel((cx - 1, cy - 1), (255, 255, 255, 255))
        d.line([(cx, cy + 5), (8, 15)], fill=(220, 220, 220, 255))
    save_block(img, "party_balloons")

    # tablecloth (party table top) and furniture
    img = tex("x")
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), ((210, 40, 50) if ((x // 4) + (y // 4)) % 2 else (240, 236, 230)) + (255,))
    save_block(noise(img, 4, 10), "party_table")
    save_block(solid((70, 70, 78), 11, 4), "metal_leg")
    img = solid((40, 90, 200), 12, 5)
    ImageDraw.Draw(img).rectangle([0, 0, 15, 15], outline=(25, 60, 150, 255))
    save_block(img, "party_chair")

    # arcade cabinet
    img = solid((26, 24, 34), 13, 4)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 8], fill=(20, 140, 160, 255), outline=(200, 200, 210, 255))
    for x, y, c in [(4, 4, (250, 230, 60)), (9, 5, (240, 60, 80)), (6, 6, (90, 240, 90)), (11, 3, (250, 250, 250))]:
        img.putpixel((x, y), c + (255,))
    d.rectangle([2, 10, 13, 12], fill=(60, 58, 70, 255))
    img.putpixel((5, 11), (240, 40, 40, 255)); img.putpixel((9, 11), (40, 100, 240, 255)); img.putpixel((11, 11), (250, 210, 40, 255))
    save_block(img, "arcade_front")
    img = solid((120, 40, 160), 14, 5)
    ImageDraw.Draw(img).line([(0, 2), (15, 14)], fill=(250, 210, 40, 255), width=2)
    save_block(img, "arcade_side")

    img = solid((200, 160, 60), 15, 5)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, 14, 9], fill=(170, 210, 230, 255), outline=(120, 90, 40, 255))
    for x, y, c in [(3, 6, (230, 60, 60)), (6, 4, (60, 120, 230)), (9, 7, (60, 200, 90)), (12, 5, (230, 90, 220))]:
        fillr(img, x, y, x + 1, y + 2, c + (255,))
    save_block(img, "prize_counter")

    img = solid((230, 120, 30), 16, 5)
    d = ImageDraw.Draw(img)
    d.rectangle([3, 2, 12, 5], fill=(20, 20, 20, 255)); d.text((4, 1), "", fill=(255, 0, 0, 255))
    fillr(img, 4, 3, 11, 4, (240, 60, 40, 255))
    fillr(img, 5, 9, 10, 10, (30, 30, 30, 255))
    for y in range(11, 16, 2):
        fillr(img, 6, y, 9, y, (250, 230, 120, 255))
    save_block(img, "ticket_machine")

    img = solid((150, 70, 50), 17, 10)
    d = ImageDraw.Draw(img)
    for y in (3, 7, 11):
        d.line([(0, y), (15, y)], fill=(110, 50, 40, 255))
    d.ellipse([2, 6, 13, 17], fill=(20, 10, 8, 255))
    d.ellipse([4, 10, 11, 17], fill=(250, 140, 30, 255))
    d.ellipse([6, 12, 9, 17], fill=(255, 230, 120, 255))
    save_block(img, "pizza_oven")
    save_block(solid((150, 70, 50), 18, 10), "brick")

    img = solid((190, 194, 200), 19, 3)
    ImageDraw.Draw(img).line([(0, 0), (15, 0)], fill=(230, 232, 236, 255))
    save_block(img, "steel")
    img = solid((170, 174, 180), 20, 3)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 3, 14, 14], outline=(120, 124, 130, 255)); d.line([(6, 8), (9, 8)], fill=(90, 90, 95, 255))
    save_block(img, "kitchen_front")

    img = solid((210, 40, 150), 21, 4)
    fillr(img, 7, 0, 8, 15, (250, 220, 60, 255))
    save_block(img, "gift_box")
    img = solid((210, 40, 150), 22, 4)
    fillr(img, 7, 0, 8, 15, (250, 220, 60, 255)); fillr(img, 0, 7, 15, 8, (250, 220, 60, 255))
    save_block(img, "gift_box_top")

    save_block(solid((90, 92, 98), 23, 4), "desk_metal")
    img = solid((120, 90, 60), 24, 4)
    d = ImageDraw.Draw(img)
    for _ in range(8):
        x, y = r.randrange(14), r.randrange(14)
        fillr(img, x, y, x + 2, y + 1, (235, 232, 220, 255))   # papers
    img.putpixel((3, 12), (200, 60, 60, 255)); img.putpixel((4, 12), (200, 60, 60, 255))   # cupcake wrapper
    save_block(img, "desk_top")

    img = tex("x")
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 14, 14], outline=(170, 170, 176, 255))
    for a in range(0, 360, 120):
        x = 7.5 + 5 * math.cos(math.radians(a)); y = 7.5 + 5 * math.sin(math.radians(a))
        d.line([(7.5, 7.5), (x, y)], fill=(80, 80, 90, 255), width=2)
    d.ellipse([6, 6, 9, 9], fill=(50, 50, 56, 255))
    save_block(img, "fan_front")

    img = solid((40, 42, 46), 25, 3)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, 14, 11], fill=(16, 40, 24, 255))
    for y in range(2, 11, 2):
        d.line([(2, y), (13, y)], fill=(40, 110, 60, 255))
    d.text((3, 2), "", fill=(0, 255, 0, 255))
    fillr(img, 3, 4, 6, 6, (90, 220, 110, 255))
    save_block(img, "monitor_front")

    # posters (wall props) - 16x16 art
    for name, bg, face in [("poster_pip", (110, 50, 150), "pip"), ("poster_rusty", (190, 110, 40), "rusty"),
                           ("poster_marla", (200, 120, 150), "marla"), ("poster_gnash", (40, 120, 80), "gnash")]:
        img = solid(bg, hash(name) % 99, 4)
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 15, 15], outline=(250, 215, 60, 255))
        draw_face(img, face, 3, 3, 10)
        d.line([(2, 14), (13, 14)], fill=(255, 255, 255, 255))
        save_block(img, name)
    img = solid((240, 236, 222), 30, 3)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(200, 40, 50, 255))
    fillr(img, 2, 2, 13, 3, (200, 40, 50, 255))
    for y in range(5, 14, 2):
        d.line([(2, y), (2 + r.randrange(8, 12), y)], fill=(60, 60, 70, 255))
    save_block(img, "poster_rules")
    img = solid((40, 40, 46), 31, 3)
    d = ImageDraw.Draw(img)
    fillr(img, 1, 5, 14, 10, (240, 200, 40, 255))
    for x in range(2, 14, 2):
        fillr(img, x, 7, x, 8, (30, 30, 30, 255))
    save_block(img, "employees_only_sign")
    img = Image.new("RGBA", (16, 16), (20, 150, 60, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 4, 15, 11], fill=(30, 200, 80, 255))
    for x, pat in zip(range(2, 14, 3), ["E", "X", "I", "T"]):
        fillr(img, x, 6, x + 1, 9, (240, 255, 240, 255))
    save_block(img, "exit_sign")

    # security door: heavy shutter
    img = solid((120, 124, 130), 32, 4)
    d = ImageDraw.Draw(img)
    for y in range(1, 16, 3):
        d.line([(0, y), (15, y)], fill=(80, 82, 88, 255))
    for y in range(13, 16):
        for x in range(16):
            img.putpixel((x, y), ((230, 190, 30) if ((x + y) // 2) % 2 else (30, 30, 30)) + (255,))
    save_block(img, "security_door")

    for name, c, label in [("door_button", (220, 30, 40), "DOOR"), ("light_button", (240, 240, 230), "LIGHT")]:
        img = solid((80, 82, 88), 33 if name[0] == "d" else 34, 4)
        d = ImageDraw.Draw(img)
        d.rectangle([2, 2, 13, 13], fill=(56, 58, 62, 255), outline=(40, 40, 44, 255))
        d.ellipse([4, 4, 11, 11], fill=c + (255,))
        d.ellipse([5, 5, 7, 7], fill=sh(c, 40) + (255,))
        save_block(img, name)

    for lit in (False, True):
        img = solid((70, 72, 78), 35, 3)
        d = ImageDraw.Draw(img)
        d.rectangle([2, 2, 13, 13], fill=(255, 248, 210, 255) if lit else (110, 110, 104, 255), outline=(40, 40, 44, 255))
        if lit:
            d.rectangle([4, 4, 11, 11], fill=(255, 255, 240, 255))
        save_block(img, "hall_light" + ("_on" if lit else ""))

    img = solid((150, 150, 156), 36, 4)
    d = ImageDraw.Draw(img)
    d.ellipse([3, 1, 12, 10], fill=(240, 236, 226, 255), outline=(40, 40, 44, 255))
    d.line([(7.5, 5.5), (7.5, 2.5)], fill=(20, 20, 20, 255)); d.line([(7.5, 5.5), (10, 5.5)], fill=(20, 20, 20, 255))
    fillr(img, 5, 12, 10, 13, (30, 30, 30, 255))   # card slot
    save_block(img, "time_clock")

    img = solid((40, 150, 80), 37, 5)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 6], fill=(15, 30, 20, 255)); fillr(img, 3, 3, 8, 4, (120, 255, 140, 255))
    for y in (8, 10, 12):
        for x in (3, 6, 9, 12):
            img.putpixel((x, y), (230, 230, 230, 255))
    save_block(img, "register_front")
    save_block(solid((40, 150, 80), 38, 5), "register_side")

    img = solid((110, 50, 150), 39, 4)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, 14, 14], outline=(250, 215, 60, 255))
    draw_face(img, "pip", 4, 3, 8)
    fillr(img, 3, 12, 12, 13, (250, 215, 60, 255))
    save_block(img, "catalog_cover")
    save_block(solid((235, 228, 205), 40, 3), "catalog_pages")


def draw_face(img, who, x0, y0, s):
    """A tiny portrait of one of the band, s px wide."""
    d = ImageDraw.Draw(img)
    pal = CHAR[who]
    d.rectangle([x0, y0 + s // 5, x0 + s - 1, y0 + s - 1], fill=pal["fur"] + (255,))
    if who == "pip":
        d.polygon([(x0, y0 + s // 5), (x0 + 2, y0), (x0 + 3, y0 + s // 5)], fill=pal["fur"] + (255,))
        d.polygon([(x0 + s - 4, y0 + s // 5), (x0 + s - 3, y0), (x0 + s - 1, y0 + s // 5)], fill=pal["fur"] + (255,))
        d.rectangle([x0, y0 + s // 2 - 1, x0 + s - 1, y0 + s // 2 + 1], fill=pal["dark"] + (255,))
    if who == "rusty":
        d.rectangle([x0 - 1, y0 + s // 5, x0, y0 + s - 2], fill=pal["dark"] + (255,))
        d.rectangle([x0 + s - 1, y0 + s // 5, x0 + s, y0 + s - 2], fill=pal["dark"] + (255,))
    ey = y0 + s // 2
    for ex in (x0 + s // 4, x0 + s - 1 - s // 4):
        img.putpixel((ex, ey), pal["eye"] + (255,))
    d.rectangle([x0 + s // 3, y0 + s - 3, x0 + s - 1 - s // 3, y0 + s - 2], fill=pal["snout"] + (255,))
    if who == "gnash":
        for tx in range(x0 + 1, x0 + s - 1, 2):
            img.putpixel((tx, y0 + s - 2), (250, 250, 240, 255))


# ====================================================================== the band
CHAR = {
    "pip":   {"fur": (128, 128, 140), "dark": (44, 42, 52), "belly": (200, 198, 206), "snout": (220, 218, 224), "eye": (90, 240, 255),
              "accent": (140, 60, 200), "name": "Pip the Raccoon"},
    "rusty": {"fur": (172, 92, 44), "dark": (100, 52, 26), "belly": (210, 160, 110), "snout": (200, 150, 100), "eye": (255, 180, 40),
              "accent": (200, 30, 40), "name": "Rusty the Robot Dog"},
    "marla": {"fur": (118, 84, 72), "dark": (70, 48, 40), "belly": (170, 130, 110), "snout": (240, 140, 170), "eye": (255, 60, 50),
              "accent": (250, 200, 40), "name": "Marla the Mole"},
    "gnash": {"fur": (64, 124, 62), "dark": (36, 78, 38), "belly": (220, 210, 130), "snout": (70, 135, 66), "eye": (250, 230, 40),
              "accent": (40, 40, 160), "name": "Gnash the Gator"},
}


class Skin:
    def __init__(self, size=64):
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.eyes = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    def rect(self, x, y, w, h, c, target=None):
        if w <= 0 or h <= 0:
            return
        ImageDraw.Draw(target or self.img).rectangle([x, y, x + w - 1, y + h - 1], fill=c + (255,) if len(c) == 3 else c)

    def box(self, u, v, w, h, d, base, front=None, top=None, bottom=None, back=None):
        """Paints a model box's UV net. Faces: top, bottom, right, front, left, back."""
        self.rect(u + d, v, w, d, top or sh(base, 12))
        self.rect(u + d + w, v, w, d, bottom or sh(base, -14))
        self.rect(u, v + d, d, h, sh(base, -6))
        self.rect(u + d, v + d, w, h, front or base)
        self.rect(u + d + w, v + d, d, h, sh(base, -6))
        self.rect(u + 2 * d + w, v + d, w, h, back or sh(base, -4))
        return (u + d, v + d)   # front face origin


def band_skin(who):
    p = CHAR[who]
    s = Skin()
    fur, dark, belly, snout, eye, acc = p["fur"], p["dark"], p["belly"], p["snout"], p["eye"], p["accent"]

    # head 8x7x8 at (0,0)
    fx, fy = s.box(0, 0, 8, 7, 8, fur)
    if who == "pip":
        s.rect(fx, fy + 1, 8, 4, dark)                  # bandit mask
    if who == "marla":
        s.rect(fx + 1, fy - 8, 6, 3, acc, None)         # hard hat on top face
        s.rect(fx - 0, fy - 2, 8, 2, acc)
    if who == "rusty":
        for x, y in [(fx, fy), (fx + 7, fy), (fx, fy + 6), (fx + 7, fy + 6)]:
            s.rect(x, y, 1, 1, (210, 210, 200))         # rivets
    for ex in (fx + 1, fx + 5):
        s.rect(ex, fy + 2, 2, 2, (18, 18, 20))          # sockets
        s.rect(ex + (1 if ex == fx + 1 else 0), fy + 2, 1, 1, eye)
        s.rect(ex + (1 if ex == fx + 1 else 0), fy + 2, 1, 1, eye, s.eyes)
        s.rect(ex, fy + 3, 2, 1, sh(eye, -60), s.eyes) if who == "pip" else None
    # jaw / snout
    if who == "gnash":
        jx, jy = s.box(0, 48, 6, 2, 7, belly)
        for x in range(0, 6, 2):
            s.rect(jx + x, jy, 1, 1, (250, 250, 240))
        tx, ty = s.box(32, 6, 6, 3, 7, fur)
        for x in range(0, 6, 2):
            s.rect(tx + x, ty + 2, 1, 1, (250, 250, 240))
        s.rect(tx + 1, ty - 7, 1, 1, (20, 30, 20)); s.rect(tx + 4, ty - 7, 1, 1, (20, 30, 20))   # nostrils on top face
    else:
        jx, jy = s.box(32, 0, 6, 2, 3, sh(snout, -20))
        s.rect(jx, jy, 6, 1, (240, 236, 226))           # teeth
        dims = {"pip": (4, 3, 3), "rusty": (4, 3, 4), "marla": (5, 3, 3)}[who]
        tx, ty = s.box(32, 6, *dims, snout)
        s.rect(tx + dims[0] // 2 - 1, ty, 2, 1, (30, 24, 26) if who != "marla" else (230, 80, 120))   # nose
    # ears / eye bumps at (56,16)
    if who == "gnash":
        ex, ey = s.box(56, 16, 3, 2, 2, fur)
        s.rect(ex + 1, ey, 1, 1, eye); s.rect(ex + 1, ey, 1, 1, eye, s.eyes)
    elif who == "pip":
        s.box(56, 16, 2, 3, 1, dark, front=(220, 220, 228))
    elif who == "rusty":
        s.box(56, 16, 1, 5, 3, dark)
    else:
        s.box(56, 16, 2, 1, 1, sh(fur, 20))
    # body 10x10x6 at (0,16)
    bx, by = s.box(0, 16, 10, 10, 6, fur)
    s.rect(bx + 2, by + 2, 6, 7, belly)
    if who == "pip":
        s.rect(bx + 3, by, 4, 2, acc); s.rect(bx + 4, by, 2, 2, sh(acc, -40))   # bow tie
    elif who == "rusty":
        s.rect(bx, by, 10, 1, acc); s.rect(bx + 4, by + 1, 2, 2, (240, 200, 60))  # collar + tag
        s.rect(bx + 3, by + 4, 4, 3, (150, 154, 160))                              # chest panel
    elif who == "marla":
        s.rect(bx + 1, by + 1, 8, 8, (60, 90, 170)); s.rect(bx + 2, by + 1, 1, 3, (60, 90, 170))   # overalls
        s.rect(bx + 2, by + 3, 6, 6, (70, 100, 180)); s.rect(bx + 3, by + 4, 1, 1, acc); s.rect(bx + 6, by + 4, 1, 1, acc)
    elif who == "gnash":
        for y in range(2, 10, 2):
            s.rect(bx + 2, by + y, 6, 1, sh(belly, -25))
        s.rect(bx + 3, by, 4, 1, acc)
    # arms
    for u in (32, 44):
        ax, ay = s.box(u, 16, 3, 10, 3, fur)
        s.rect(ax, ay + 8, 3, 2, belly if who != "rusty" else (150, 154, 160))
        s.rect(ax, ay + 3, 3, 1, sh(fur, -30))          # elbow seam
    if who == "marla":
        s.box(20, 48, 4, 2, 4, (240, 236, 220))           # claws
    # legs
    for u in (0, 16):
        lx, ly = s.box(u, 32, 4, 9, 4, fur, bottom=dark)
        s.rect(lx, ly + 7, 4, 2, dark)
        s.rect(lx, ly + 4, 4, 1, sh(fur, -30))
    # tail
    if who == "pip":
        s.box(32, 32, 3, 3, 9, fur)
        for z in (1, 4, 7):   # dark rings all the way round
            s.rect(41, 32 + z, 3, 1, dark); s.rect(44, 32 + z, 3, 1, dark)
            s.rect(32 + z, 41, 1, 3, dark); s.rect(44 + 8 - z, 41, 1, 3, dark)
        s.rect(53, 41, 3, 3, dark)
    elif who == "rusty":
        s.box(32, 32, 2, 2, 5, (150, 154, 160))
    elif who == "marla":
        s.box(32, 32, 2, 2, 2, snout)
    else:
        s.box(32, 32, 4, 3, 12, fur, top=sh(fur, -20))
        for z in range(0, 12, 2):
            s.rect(32 + 12 + 1, 32 + z, 2, 1, (40, 70, 40))   # ridge spikes on top

    # wear and tear: grime, scuffs and the odd stain
    img = noise(s.img, 9, hash(who) % 1000)
    px = img.load()
    r = random.Random(who)
    for _ in range(70):
        x, y = r.randrange(64), r.randrange(64)
        if px[x, y][3]:
            px[x, y] = sh(px[x, y], -35)
    for _ in range(6):
        x, y = r.randrange(64), r.randrange(60)
        for k in range(r.randrange(2, 5)):
            if y + k < 64 and px[x, y + k][3]:
                px[x, y + k] = cl((px[x, y + k][0] * 0.55 + 40, px[x, y + k][1] * 0.45, px[x, y + k][2] * 0.45)) + (255,)
    img.save(mk(f"{A}/textures/entity/{who}.png"))
    s.eyes.save(mk(f"{A}/textures/entity/{who}_eyes.png"))


def camera_skin():
    s = Skin(32)
    fx, fy = s.box(0, 0, 4, 4, 7, (220, 220, 214))
    s.rect(fx + 3, fy, 1, 1, (240, 30, 30))
    lx, ly = s.box(0, 12, 3, 3, 1, (30, 30, 34))
    s.rect(lx + 1, ly + 1, 1, 1, (90, 140, 220))
    s.box(24, 0, 1, 8, 1, (90, 92, 98))
    s.box(0, 17, 3, 1, 3, (90, 92, 98))
    s.img.save(mk(f"{A}/textures/entity/security_camera.png"))


# ====================================================================== items
def item_textures():
    img = tex("x"); d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], fill=(230, 180, 40, 255), outline=(160, 110, 20, 255))
    d.ellipse([4, 4, 11, 11], outline=(255, 220, 100, 255))
    for x, y in [(6, 5), (6, 6), (6, 7), (6, 8), (6, 9), (7, 5), (8, 5), (9, 6), (8, 7), (7, 7)]:
        img.putpixel((x, y), (140, 90, 10, 255))
    save_item(img, "pip_coin")

    img = tex("x"); d = ImageDraw.Draw(img)
    d.rounded_rectangle([1, 3, 14, 13], 2, fill=(36, 36, 40, 255))
    d.rectangle([3, 5, 12, 11], fill=(30, 70, 40, 255))
    for y in range(5, 12, 2):
        d.line([(3, y), (12, y)], fill=(50, 110, 60, 255))
    img.putpixel((11, 6), (240, 40, 40, 255))
    save_item(img, "camera_tablet")

    img = tex("x"); d = ImageDraw.Draw(img)
    d.rectangle([7, 1, 8, 5], fill=(90, 92, 98, 255))
    d.rectangle([2, 5, 12, 10], fill=(220, 220, 214, 255), outline=(150, 150, 146, 255))
    d.rectangle([12, 6, 14, 9], fill=(30, 30, 34, 255)); img.putpixel((13, 7), (90, 140, 220, 255))
    img.putpixel((4, 7), (240, 30, 30, 255))
    save_item(img, "security_camera")

    img = tex("x"); d = ImageDraw.Draw(img)
    d.rectangle([3, 3, 12, 12], fill=(30, 120, 60, 255), outline=(20, 70, 36, 255))
    d.rectangle([6, 6, 9, 9], fill=(30, 30, 34, 255))
    for i in range(4, 12, 2):
        img.putpixel((i, 1), (220, 190, 80, 255)); img.putpixel((i, 2), (220, 190, 80, 255))
        img.putpixel((i, 13), (220, 190, 80, 255)); img.putpixel((i, 14), (220, 190, 80, 255))
    img.putpixel((7, 7), (255, 60, 60, 255))
    save_item(img, "override_chip")

    img = tex("x"); d = ImageDraw.Draw(img)
    d.rectangle([1, 4, 14, 14], fill=(200, 150, 90, 255), outline=(140, 100, 60, 255))
    d.polygon([(1, 4), (4, 1), (14, 1), (14, 4)], fill=(220, 170, 110, 255))
    draw_face(img, "pip", 5, 6, 6)
    save_item(img, "pizzeria_kit")

    img = tex("x"); d = ImageDraw.Draw(img)
    d.polygon([(2, 3), (14, 3), (8, 14)], fill=(250, 200, 80, 255))
    d.line([(2, 3), (14, 3)], fill=(190, 120, 50, 255), width=2)
    for x, y in [(6, 6), (10, 6), (8, 9)]:
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(200, 40, 40, 255))
    save_item(img, "pizza_slice")

    img = tex("x"); d = ImageDraw.Draw(img)
    d.polygon([(4, 5), (12, 5), (11, 15), (5, 15)], fill=(230, 50, 60, 255))
    d.rectangle([4, 8, 12, 10], fill=(250, 250, 250, 255))
    d.rectangle([3, 4, 13, 5], fill=(240, 240, 240, 255))
    d.line([(9, 4), (11, 0)], fill=(60, 140, 240, 255))
    save_item(img, "party_soda")

    for who in CHAR:
        img = tex("x")
        draw_face(img, who, 2, 2, 12)
        save_item(img, f"{who}_animatronic")


# ====================================================================== models and blockstates
CUBES = ["checker_floor", "party_wall_red", "party_wall_blue", "party_wall_purple", "party_wall_trim", "confetti_wall",
         "ceiling_tile", "office_wall", "stage_floor", "stage_curtain", "star_curtain", "ball_pit", "door_button", "light_button"]
WALL_PROPS = ["poster_pip", "poster_rusty", "poster_marla", "poster_gnash", "poster_rules", "employees_only_sign", "exit_sign"]


def face(t, uv=None, cull=None):
    f = {"texture": t}
    if uv:
        f["uv"] = uv
    return f


def el(fr, to, tex_all="#side", **over):
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        faces[f] = {"texture": over.get(f, tex_all)}
    return {"from": fr, "to": to, "faces": faces}


def model(name, elements, textures):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    wj(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/block", "textures": textures, "elements": elements})


def facing_state(name, mdl=None):
    mdl = mdl or f"{MOD}:block/{name}"
    wj(f"{A}/blockstates/{name}.json", {"variants": {
        "facing=north": {"model": mdl}, "facing=east": {"model": mdl, "y": 90},
        "facing=south": {"model": mdl, "y": 180}, "facing=west": {"model": mdl, "y": 270}}})


def simple_state(name, mdl=None):
    wj(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": mdl or f"{MOD}:block/{name}"}}})


def T(n):
    return f"{MOD}:block/{n}"


def models():
    for n in CUBES:
        wj(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": T(n)}})
        simple_state(n)
    wj(f"{A}/models/block/party_balloons.json", {"parent": "minecraft:block/cross", "textures": {"cross": T("party_balloons")}, "render_type": "cutout"})
    simple_state("party_balloons")

    for n in WALL_PROPS:
        model(n, [el([0, 0, 15], [16, 16, 16], "#back", north="#front")], {"front": T(n), "back": T("office_wall"), "particle": T(n)})
        facing_state(n)

    S = "#side"
    model("party_table", [el([0, 11, 0], [16, 12, 16], "#top"),
                          el([1, 0, 1], [3, 11, 3], "#leg"), el([13, 0, 1], [15, 11, 3], "#leg"),
                          el([1, 0, 13], [3, 11, 15], "#leg"), el([13, 0, 13], [15, 11, 15], "#leg")],
          {"top": T("party_table"), "leg": T("metal_leg")})
    model("party_chair", [el([3, 7, 3], [13, 8, 13], "#seat"), el([3, 8, 12], [13, 16, 13], "#seat"),
                          el([3, 0, 3], [4, 7, 4], "#leg"), el([12, 0, 3], [13, 7, 4], "#leg"),
                          el([3, 0, 12], [4, 7, 13], "#leg"), el([12, 0, 12], [13, 7, 13], "#leg")],
          {"seat": T("party_chair"), "leg": T("metal_leg")})
    model("arcade_cabinet", [el([1, 0, 1], [15, 16, 15], S, north="#front", up="#top")],
          {"front": T("arcade_front"), "side": T("arcade_side"), "top": T("desk_metal")})
    model("prize_counter", [el([0, 0, 0], [16, 16, 16], S, north="#front", up="#top")],
          {"front": T("prize_counter"), "side": T("register_side"), "top": T("steel")})
    model("ticket_machine", [el([3, 0, 3], [13, 16, 13], S, north="#front")],
          {"front": T("ticket_machine"), "side": T("desk_metal")})
    model("pizza_oven", [el([0, 0, 0], [16, 16, 16], S, north="#front")], {"front": T("pizza_oven"), "side": T("brick")})
    model("kitchen_counter", [el([0, 0, 0], [16, 16, 16], S, north="#front", up="#top")],
          {"front": T("kitchen_front"), "side": T("steel"), "top": T("steel")})
    model("gift_box", [el([2, 0, 2], [14, 12, 14], S, up="#top"), el([3, 12, 3], [13, 13, 13], "#top")],
          {"side": T("gift_box"), "top": T("gift_box_top")})
    model("security_desk", [el([0, 12, 0], [16, 13, 16], "#side", up="#top"), el([0, 0, 13], [16, 12, 15], "#side")],
          {"top": T("desk_top"), "side": T("desk_metal")})
    model("desk_fan", [el([6, 0, 6], [10, 1, 10], S), el([7.5, 1, 7.5], [8.5, 6, 8.5], S),
                       el([4, 5, 7], [12, 13, 9], S, north="#front", south="#front")],
          {"front": T("fan_front"), "side": T("desk_metal")})
    model("office_monitor", [el([2, 0, 3], [14, 12, 13], S, north="#front")], {"front": T("monitor_front"), "side": T("desk_metal")})
    model("time_clock", [el([3, 0, 3], [13, 16, 13], S, north="#front")], {"front": T("time_clock"), "side": T("desk_metal")})
    model("pizzeria_register", [el([2, 0, 3], [14, 12, 13], S, north="#front", up="#front")],
          {"front": T("register_front"), "side": T("register_side")})
    model("franchise_catalog", [el([2, 0, 2], [14, 3, 14], "#pages", up="#cover"), el([2, 3, 6], [14, 15, 7], "#pages", north="#cover", south="#cover")],
          {"cover": T("catalog_cover"), "pages": T("catalog_pages")})
    for n in ["party_table", "party_chair", "arcade_cabinet", "prize_counter", "ticket_machine", "pizza_oven", "kitchen_counter",
              "gift_box", "security_desk", "desk_fan", "office_monitor", "time_clock", "pizzeria_register", "franchise_catalog"]:
        facing_state(n)

    # security door: closed slab vs rolled-up shutter
    model("security_door_closed", [el([0, 0, 6], [16, 16, 10], "#door")], {"door": T("security_door")})
    model("security_door_open", [el([0, 14, 6], [16, 16, 10], "#door")], {"door": T("security_door")})
    wj(f"{A}/blockstates/security_door.json", {"variants": {
        "powered=true": {"model": f"{MOD}:block/security_door_closed"}, "powered=false": {"model": f"{MOD}:block/security_door_open"}}})
    for on in ("", "_on"):
        wj(f"{A}/models/block/hall_light{on}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": T("hall_light" + on)}})
    wj(f"{A}/blockstates/hall_light.json", {"variants": {"lit=false": {"model": f"{MOD}:block/hall_light"}, "lit=true": {"model": f"{MOD}:block/hall_light_on"}}})

    # item models and 26.1 item definitions
    block_items = CUBES + WALL_PROPS + ["party_balloons", "party_table", "party_chair", "arcade_cabinet", "prize_counter", "ticket_machine",
                                         "pizza_oven", "kitchen_counter", "gift_box", "security_desk", "desk_fan", "office_monitor", "time_clock",
                                         "pizzeria_register", "franchise_catalog", "security_door", "hall_light"]
    flat_block_items = {"party_balloons", *WALL_PROPS}
    for n in block_items:
        if n in flat_block_items:
            wj(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": T(n)}})
        else:
            parent = {"security_door": "security_door_closed"}.get(n, n)
            wj(f"{A}/models/item/{n}.json", {"parent": f"{MOD}:block/{parent}"})
    items = ["pip_coin", "camera_tablet", "security_camera", "override_chip", "pizzeria_kit", "pizza_slice", "party_soda"] + [f"{w}_animatronic" for w in CHAR]
    for n in items:
        wj(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{n}"}})
    for n in block_items + items:
        wj(f"{A}/items/{n}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{n}"}})
    return block_items, items


# ====================================================================== sounds
SR = 44100


def to_ogg(sig, name):
    sig = np.clip(sig, -1, 1)
    raw = tempfile.mktemp(suffix=".raw")
    (sig * 32000).astype("<i2").tofile(raw)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(SR), "-ac", "1", "-i", raw,
                    "-c:a", "libvorbis", "-q:a", "4", mk(f"{A}/sounds/{name}.ogg")], check=True)
    os.remove(raw)


def t_(sec):
    return np.arange(int(SR * sec)) / SR


def env(n, a=0.005, rel=0.2, total=None):
    t = np.arange(n) / SR
    total = total or n / SR
    e = np.minimum(1, t / a) * np.clip((total - t) / rel, 0, 1)
    return e


def lowpass(x, k):
    if k <= 1:
        return x
    return np.convolve(x, np.ones(k) / k, mode="same")


def sounds():
    R = np.random.default_rng(7)
    # jumpscare: a tearing, detuned metal shriek
    t = t_(1.5)
    f = 700 + 500 * np.sin(2 * np.pi * 7 * t) + 300 * t
    ph = 2 * np.pi * np.cumsum(f) / SR
    saw = ((ph / (2 * np.pi)) % 1) * 2 - 1
    saw2 = (((ph * 1.47) / (2 * np.pi)) % 1) * 2 - 1
    sig = 0.45 * saw + 0.35 * saw2 + 0.5 * R.normal(0, 1, len(t)) * np.exp(-t * 1.2)
    sig = np.tanh(sig * 2.5) * env(len(t), 0.003, 0.35)
    to_ogg(sig * 0.9, "jumpscare")

    # servo step: whir + clunk
    t = t_(0.4)
    whir = np.sin(2 * np.pi * (300 + 900 * t) * t) * np.exp(-t * 9) * 0.3
    clunk = lowpass(R.normal(0, 1, len(t)), 30) * np.exp(-np.maximum(0, t - 0.12) * 30) * (t > 0.12) * 1.6
    to_ogg(whir + clunk, "servo_step")

    # heavy door slam and bang
    for name, length, decay, k in [("door_slam", 0.8, 9, 22), ("door_bang", 0.45, 14, 14)]:
        t = t_(length)
        boom = np.sin(2 * np.pi * 55 * t) * np.exp(-t * decay) * 0.8
        hit = lowpass(R.normal(0, 1, len(t)), k) * np.exp(-t * decay * 2) * 2.0
        ring = np.sin(2 * np.pi * 410 * t) * np.exp(-t * 6) * 0.15
        to_ogg(np.tanh((boom + hit + ring) * 1.5) * 0.85, name)

    # light buzz: mains hum
    t = t_(0.9)
    hum = sum(np.sin(2 * np.pi * 60 * h * t) / h for h in (1, 2, 3, 5, 7)) * 0.25
    hum += 0.08 * np.sign(np.sin(2 * np.pi * 120 * t))
    to_ogg(hum * env(len(t), 0.01, 0.1), "light_buzz")

    # music box: an original little waltz in A minor (Pip's song)
    notes = [("A5", 1), ("C6", 1), ("E6", 1), ("D6", 2), ("C6", 1), ("B5", 1), ("G5", 1), ("B5", 1), ("A5", 3),
             ("E5", 1), ("A5", 1), ("C6", 1), ("B5", 2), ("G#5", 1), ("A5", 1), ("E6", 1), ("D6", 1), ("C6", 2), ("B5", 1), ("A5", 3)]
    names = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}

    def hz(n):
        return 440 * 2 ** ((names[n[:-1]] + 12 * int(n[-1]) - 57) / 12)
    beat = 0.36
    total = sum(b for _, b in notes) * beat + 1.5
    out = np.zeros(int(SR * total))
    pos = 0.0
    for n, b in notes:
        i = int(pos * SR)
        tt = t_(1.4)
        tone = (np.sin(2 * np.pi * hz(n) * tt) + 0.3 * np.sin(2 * np.pi * hz(n) * 2.01 * tt) + 0.12 * np.sin(2 * np.pi * hz(n) * 4.2 * tt)) * np.exp(-tt * 3.2)
        out[i:i + len(tone)] += tone[:len(out) - i] * 0.33
        pos += b * beat
    wobble = 1 + 0.004 * np.sin(2 * np.pi * 0.7 * t_(total))   # a slightly worn spring
    idx = np.clip(np.cumsum(wobble) - 1, 0, len(out) - 1).astype(int)
    to_ogg(out[idx], "music_box")

    # 6 AM chime: bells
    t = t_(3.2)
    sig = np.zeros(len(t))
    for k, f0 in enumerate([523.25, 659.25, 783.99, 1046.5]):
        st = int(k * 0.35 * SR)
        tt = t[: len(t) - st]
        bell = sum(a * np.sin(2 * np.pi * f0 * m * tt) for m, a in [(1, 1), (2.76, 0.4), (5.4, 0.2)]) * np.exp(-tt * 1.6)
        sig[st:] += bell * 0.25
    to_ogg(sig, "chime_6am")

    # camera flip: click + static burst
    t = t_(0.28)
    sig = R.normal(0, 1, len(t)) * 0.35 * np.exp(-t * 14)
    sig[:200] += np.hanning(200) * 1.2
    to_ogg(sig, "camera_flip")

    # power down: descending whine and a final clunk
    t = t_(2.6)
    f = 900 * np.exp(-t * 1.4) + 30
    sig = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.45 * np.clip(2.3 - t, 0, 1)
    k = int(2.3 * SR)
    sig[k:] += lowpass(R.normal(0, 1, len(t) - k), 25) * np.exp(-(t[k:] - 2.3) * 20) * 1.5
    to_ogg(sig, "power_down")

    # stage band: a cheerful (slightly off) organ loop
    bpm = 132
    b = 60 / bpm
    t = t_(b * 16)
    sig = np.zeros(len(t))
    prog = [(220, 277.2, 329.6), (196, 246.9, 293.7), (174.6, 220, 261.6), (196, 246.9, 293.7)]
    for bar, chord in enumerate(prog):
        for beatn in range(4):
            st = int((bar * 4 + beatn) * b * SR)
            tt = t_(b * 0.9)
            ch = sum(np.sign(np.sin(2 * np.pi * f * 1.004 * tt)) * 0.06 + np.sin(2 * np.pi * f * tt) * 0.1 for f in chord) * np.exp(-tt * 2)
            bass = np.sin(2 * np.pi * chord[0] / 2 * tt) * 0.3 * np.exp(-tt * 5)
            kick = np.sin(2 * np.pi * (60 + 120 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 14) * (0.6 if beatn % 2 == 0 else 0)
            seg = ch + bass + kick
            sig[st:st + len(seg)] += seg[: len(sig) - st]
    to_ogg(np.tanh(sig * 1.2) * 0.8, "stage_band")

    # gator run: fast heavy thumps getting closer
    t = t_(1.6)
    sig = np.zeros(len(t))
    for k in range(10):
        st = int(k * 0.15 * SR)
        tt = t_(0.14)
        thump = (np.sin(2 * np.pi * 70 * tt) + lowpass(R.normal(0, 1, len(tt)), 18)) * np.exp(-tt * 28) * (0.25 + k * 0.07)
        sig[st:st + len(tt)] += thump[: len(sig) - st]
    to_ogg(sig, "gator_run")

    # giggle: a creepy, pitched-down "hee-hee-hee"
    sig = np.zeros(int(SR * 1.0))
    for k in range(4):
        tt = t_(0.16)
        f0 = 420 - k * 25
        voice = sum(np.sin(2 * np.pi * f0 * h * tt) / h for h in range(1, 8))
        form = np.sin(2 * np.pi * 2600 * tt) * 0.3 + np.sin(2 * np.pi * 900 * tt) * 0.4
        seg = voice * (0.6 + form) * np.sin(np.pi * tt / 0.16) * 0.22
        st = int(k * 0.2 * SR)
        sig[st:st + len(seg)] += seg
    to_ogg(np.tanh(sig * 1.4), "giggle")


SUBS = {"jumpscare": "Animatronic screams", "servo_step": "Servos whir", "door_slam": "Security door slams", "door_bang": "Something bangs on the door",
        "light_buzz": "Hall light buzzes", "music_box": "A music box plays", "chime_6am": "6 AM chime", "camera_flip": "Camera feed switches",
        "power_down": "Power runs out", "stage_band": "The band performs", "gator_run": "Heavy footsteps sprint", "giggle": "Someone giggles"}


# ====================================================================== data
def data(block_items, items):
    for n in block_items:
        wj(f"{D}/loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{n}"}],
                                                                                    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    R_ = f"{D}/recipe"

    def shaped(name, pattern, key, count=1):
        wj(f"{R_}/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key,
                                 "result": {"id": f"{MOD}:{name}", "count": count}})
    shaped("franchise_catalog", ["PE", "BP"], {"P": "minecraft:paper", "E": "minecraft:emerald", "B": "minecraft:book"})
    shaped("pizzeria_register", ["III", "GRG", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:gold_ingot", "R": "minecraft:redstone"})
    shaped("time_clock", [" I ", "ICI", " I "], {"I": "minecraft:iron_ingot", "C": "minecraft:clock"})
    shaped("camera_tablet", ["III", "IGI", "IRI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "R": "minecraft:redstone"})
    shaped("security_camera", ["I", "G", "R"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "R": "minecraft:redstone"}, 2)
    shaped("checker_floor", ["WB", "BW"], {"W": "minecraft:white_concrete", "B": "minecraft:black_concrete"}, 4)
    shaped("pizza_slice", ["T", "B"], {"T": "minecraft:beetroot", "B": "minecraft:bread"}, 2)

    lp = f"{A}/lang/en_us.json"
    lang = {}
    for n in block_items:
        lang[f"block.{MOD}.{n}"] = n.replace("_", " ").title()
    lang[f"block.{MOD}.pizzeria_register"] = "The Register"
    lang[f"block.{MOD}.party_wall_trim"] = "Checkered Party Trim"
    nice = {"pip_coin": "Pip Coin", "camera_tablet": "Camera Tablet", "security_camera": "Security Camera", "override_chip": "Override Chip",
            "pizzeria_kit": "Pizzeria Kit", "pizza_slice": "Pizza Slice", "party_soda": "Party Soda"}
    for k, v in nice.items():
        lang[f"item.{MOD}.{k}"] = v
    for w, p in CHAR.items():
        lang[f"item.{MOD}.{w}_animatronic"] = p["name"]
        lang[f"entity.{MOD}.{w}"] = p["name"]
    lang[f"entity.{MOD}.security_camera"] = "Security Camera"
    for k, v in SUBS.items():
        lang[f"subtitles.{MOD}.{k}"] = v
    wj(lp, lang)
    wj(f"{A}/sounds.json", {k: {"sounds": [{"name": f"{MOD}:{k}", "stream": k in ("music_box", "stage_band")}], "subtitle": f"subtitles.{MOD}.{k}"}
                             for k in SUBS})


def main():
    block_textures()
    item_textures()
    for who in CHAR:
        band_skin(who)
    camera_skin()
    bi, it = models()
    sounds()
    data(bi, it)
    print("pip's party palace assets done:", len(bi), "block items,", len(it), "items")


main()
