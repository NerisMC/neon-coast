#!/usr/bin/env python3
"""Neon Coast 2.1 assets: citizen skins, guns, ammo, gun sounds, and their JSON/lang entries.
Run by gen_city.py (which gen_neon.py runs), or on its own: python3 tools/gen_life.py <project root>."""
import json, os, random, subprocess, sys
import numpy as np
from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
MOD = "neoncoast"
A = f"{ROOT}/src/main/resources/assets/{MOD}"
D = f"{ROOT}/src/main/resources/data"
rng = random.Random(21)


def mk(p):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def wj(p, o):
    json.dump(o, open(mk(p), "w"), indent=2)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, d):
    return clamp((c[0] + d, c[1] + d, c[2] + d))


# ------------------------------------------------------------------ citizen skins (64x64 player layout)
SKIN_TONES = [(255, 219, 172), (241, 194, 125), (224, 172, 105), (198, 134, 66), (141, 85, 36), (99, 57, 30), (236, 188, 150), (180, 120, 80)]
HAIR = [(20, 16, 14), (60, 38, 22), (110, 70, 35), (200, 160, 90), (150, 40, 30), (90, 90, 95), (230, 220, 200)]
SHIRTS = [(200, 40, 40), (40, 80, 180), (240, 240, 240), (30, 30, 34), (60, 140, 70), (240, 190, 40), (130, 60, 150), (230, 120, 40), (90, 160, 200), (220, 110, 150)]
PANTS = [(45, 70, 130), (35, 50, 95), (190, 165, 120), (30, 30, 34), (90, 90, 96), (110, 80, 55)]
SHOES = [(240, 240, 240), (25, 25, 25), (120, 70, 40), (200, 40, 40)]


def skin(i):
    r = random.Random(100 + i)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    tone = SKIN_TONES[r.randrange(len(SKIN_TONES))]
    hair = HAIR[r.randrange(len(HAIR))]
    shirt = SHIRTS[r.randrange(len(SHIRTS))]
    pants = PANTS[r.randrange(len(PANTS))]
    shoes = SHOES[r.randrange(len(SHOES))]
    outfit = r.choice(["tee", "tee", "hoodie", "suit", "tank", "jacket"])
    long_hair = r.random() < 0.4
    bald = not long_hair and r.random() < 0.12
    cap = not long_hair and r.random() < 0.25
    cap_col = SHIRTS[r.randrange(len(SHIRTS))]
    shorts = outfit in ("tee", "tank") and r.random() < 0.35
    eye = r.choice([(60, 40, 25), (40, 90, 160), (60, 120, 60), (30, 30, 30)])

    def px(x, y, c, a=4):
        if c is None:
            return
        img.putpixel((x, y), shade(c, r.randint(-a, a)) + (255,))

    def box(u, v, w, h, d, fn):
        faces = [(u + d, v, w, d, "top"), (u + d + w, v, w, d, "bottom"), (u, v + d, d, h, "right"),
                 (u + d, v + d, w, h, "front"), (u + d + w, v + d, d, h, "left"), (u + 2 * d + w, v + d, w, h, "back")]
        for fx, fy, fw, fh, name in faces:
            for a in range(fw):
                for b in range(fh):
                    px(fx + a, fy + b, fn(name, a, b, fw, fh))

    def head(n, a, b, fw, fh):
        top_hair = None if bald else hair
        if n == "top":
            return top_hair or tone
        if n == "bottom":
            return tone
        if n == "front":
            if b == 0 or (b == 1 and not bald and a not in (3, 4)):
                return top_hair or tone
            if b == 3 and a in (1, 2, 5, 6) and not bald:
                return shade(hair, -10) if a in (1, 2, 5, 6) else tone     # eyebrows
            if b == 4 and a in (1, 2, 5, 6):
                return (245, 245, 245) if a in (1, 6) else eye
            if b == 5 and a in (3, 4):
                return shade(tone, -25)                                     # nose
            if b == 6 and 2 <= a <= 5:
                return (150, 80, 70)                                         # mouth
            if long_hair and a in (0, 7) and b <= 5:
                return hair
            return tone
        if n == "back":
            if bald:
                return tone
            return hair if (b < (8 if long_hair else 5)) else tone
        # sides
        if bald:
            return tone
        return hair if b < (7 if long_hair else 3) or (b < 4 and (a < 2 if n == "right" else a > 5)) else tone

    box(0, 0, 8, 8, 8, head)

    def hat(n, a, b, fw, fh):
        if cap:
            if n == "bottom":
                return None
            if n == "top":
                return cap_col
            if b < 3:
                return cap_col
            if n == "front" and b == 3:
                return shade(cap_col, -40)
            return None
        if long_hair and n in ("back", "left", "right") and b < 7:
            return hair
        if long_hair and n == "top":
            return hair
        return None

    box(32, 0, 8, 8, 8, hat)

    def torso(n, a, b, fw, fh):
        if b >= 10:
            return (40, 30, 25) if outfit == "suit" else shade(pants, -10)   # belt line
        if outfit == "suit":
            if n == "front":
                if a in (3, 4) and b < 9:
                    return (180, 30, 40) if a == 3 or b > 1 else (240, 240, 240)   # tie
                if a in (2, 5) and b < 4:
                    return (240, 240, 240)
                return (35, 38, 48)
            return (35, 38, 48)
        if outfit == "tank" and n == "front" and b < 2 and 2 <= a <= 5:
            return tone
        if outfit == "hoodie" and n == "front" and 6 <= b <= 8 and 2 <= a <= 5:
            return shade(shirt, -25)                                        # pocket
        if outfit == "jacket":
            if n == "front" and a in (3, 4):
                return (240, 240, 240) if b < 9 else shade(shirt, -40)
            return shade(shirt, -60)
        if outfit == "tee" and n == "front" and b == 0 and 3 <= a <= 4:
            return tone
        return shirt

    box(16, 16, 8, 12, 4, torso)

    def arm(n, a, b, fw, fh):
        if n == "bottom":
            return tone
        sleeve = {"tee": 4, "tank": 0, "hoodie": 11, "suit": 11, "jacket": 11}[outfit]
        if b < sleeve:
            if outfit == "suit":
                return (35, 38, 48)
            if outfit == "jacket":
                return shade(shirt, -60)
            return shirt
        return tone

    box(40, 16, 4, 12, 4, arm)
    box(32, 48, 4, 12, 4, arm)

    def leg(n, a, b, fw, fh):
        if b >= 10 or n == "bottom":
            return shoes
        if shorts and b >= 5:
            return tone
        if outfit == "suit":
            return (35, 38, 48)
        return pants

    box(0, 16, 4, 12, 4, leg)
    box(16, 48, 4, 12, 4, leg)
    img.save(mk(f"{A}/textures/entity/citizen/citizen_{i}.png"))


# ------------------------------------------------------------------ gun sprites (32x32, barrel pointing right)
METAL, METAL_D, METAL_L = (52, 54, 60), (30, 31, 36), (92, 95, 104)
WOOD, WOOD_D = (128, 84, 46), (92, 58, 30)
BLACK = (22, 22, 24)


class Sprite:
    def __init__(self, size=32):
        self.im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.n = size

    def rect(self, x0, y0, x1, y1, c, outline=False):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                if 0 <= x < self.n and 0 <= y < self.n:
                    edge = outline and (x in (x0, x1) or y in (y0, y1))
                    cc = shade(c, -22) if edge else shade(c, rng.randint(-3, 3))
                    self.im.putpixel((x, y), cc + (255,))

    def line_h(self, x0, x1, y, c):
        self.rect(x0, y, x1, y, c)

    def save(self, name):
        self.im.save(mk(f"{A}/textures/item/{name}.png"))


def guns():
    # pistol
    s = Sprite()
    s.rect(9, 11, 25, 15, METAL, True)          # slide
    s.line_h(10, 24, 11, METAL_L)              # slide highlight
    for x in range(19, 24, 2):
        s.rect(x, 12, x, 14, METAL_D)          # serrations
    s.rect(24, 12, 25, 13, BLACK)              # muzzle
    s.rect(10, 16, 21, 17, METAL_D)            # frame
    for k in range(7):
        s.rect(10 + k // 3, 17 + k, 14 + k // 3, 17 + k, METAL_D if k % 2 else (40, 40, 44))   # raked grip
    s.rect(15, 18, 18, 18, METAL_D); s.rect(18, 18, 18, 20, METAL_D); s.rect(15, 20, 18, 20, METAL_D)  # trigger guard
    s.rect(16, 18, 16, 19, METAL_L)            # trigger
    s.rect(24, 10, 24, 10, METAL_L); s.rect(10, 10, 11, 10, METAL_L)   # sights
    s.save("pistol")

    # SMG
    s = Sprite()
    s.rect(7, 11, 24, 16, METAL, True)
    s.line_h(8, 23, 11, METAL_L)
    s.rect(24, 12, 29, 14, METAL_D, True)      # barrel shroud
    s.rect(29, 13, 30, 13, BLACK)
    s.rect(14, 17, 17, 26, METAL_D, True)      # long magazine
    s.rect(9, 17, 12, 23, (40, 40, 44), True)  # pistol grip
    s.rect(1, 12, 7, 13, METAL_D); s.rect(1, 12, 2, 17, METAL_D)   # folding stock
    s.rect(20, 9, 21, 10, METAL_D)             # sight
    s.save("smg")

    # assault rifle
    s = Sprite()
    s.rect(0, 12, 7, 18, (38, 38, 42), True)    # stock
    s.rect(7, 11, 18, 16, METAL, True)          # receiver
    s.line_h(8, 17, 11, METAL_L)
    s.rect(18, 11, 26, 15, (60, 62, 56), True)  # handguard
    s.rect(26, 12, 31, 13, METAL_D)             # barrel
    s.rect(31, 11, 31, 14, BLACK)               # muzzle brake
    for k in range(9):
        s.rect(13 + k // 3, 17 + k, 16 + k // 3, 17 + k, METAL_D)   # curved magazine
    s.rect(9, 17, 11, 22, (40, 40, 44), True)   # grip
    s.rect(11, 8, 17, 10, BLACK, True)          # optic
    s.rect(17, 9, 17, 9, (90, 170, 220))
    s.save("rifle")

    # shotgun
    s = Sprite()
    s.rect(0, 13, 9, 18, WOOD, True)            # wooden stock
    s.rect(9, 12, 16, 16, METAL, True)          # receiver
    s.rect(16, 12, 31, 13, METAL_D)             # barrel
    s.rect(16, 14, 29, 14, METAL)               # magazine tube
    s.rect(19, 14, 25, 16, WOOD_D, True)        # pump
    s.rect(11, 17, 13, 18, METAL_D)             # trigger guard
    s.rect(30, 11, 30, 11, METAL_L)             # bead sight
    s.save("shotgun")

    # sniper rifle
    s = Sprite()
    s.rect(0, 13, 10, 19, (70, 74, 60), True)   # stock
    s.rect(2, 12, 6, 12, (70, 74, 60))          # cheek rest
    s.rect(10, 13, 19, 16, METAL, True)         # action
    s.rect(19, 13, 31, 14, METAL_D)             # long barrel
    s.rect(30, 12, 31, 15, BLACK)               # muzzle brake
    s.rect(10, 7, 21, 10, BLACK, True)          # scope
    s.rect(9, 7, 9, 10, METAL_D); s.rect(21, 7, 22, 10, METAL_D)
    s.rect(22, 8, 22, 9, (90, 170, 220))
    s.rect(13, 11, 13, 12, METAL_D); s.rect(18, 11, 18, 12, METAL_D)   # scope rings
    s.rect(15, 16, 16, 17, METAL_L)             # bolt handle
    s.rect(12, 17, 14, 18, METAL_D)             # trigger guard
    s.rect(22, 15, 22, 19, METAL_D); s.rect(21, 19, 23, 19, METAL_D)   # bipod leg
    s.save("sniper")

    # ammo (16x16)
    def box_ammo(name, bullet, tip, n, tall, case):
        s = Sprite(16)
        s.rect(1, 9, 14, 14, case, True)
        s.rect(2, 10, 13, 10, shade(case, 30))
        for k in range(n):
            x = 2 + k * (12 // n)
            s.rect(x, 9 - tall, x + 1, 8, bullet)
            s.rect(x, 9 - tall, x + 1, 9 - tall, tip)
        s.save(name)
    box_ammo("pistol_ammo", (205, 160, 60), (170, 110, 60), 4, 3, (60, 90, 60))
    box_ammo("rifle_ammo", (205, 160, 60), (150, 150, 155), 4, 6, (90, 80, 50))
    s = Sprite(16)
    for k, x in enumerate((2, 6, 10)):
        s.rect(x, 3, x + 3, 10, (190, 30, 30), True)
        s.rect(x, 11, x + 3, 13, (205, 160, 60), True)
    s.save("shotgun_shells")


# ------------------------------------------------------------------ sounds
SR = 44100


def to_ogg(sig, name):
    sig = np.asarray(sig, dtype=np.float64)
    sig = sig / max(1e-6, np.max(np.abs(sig))) * 0.95
    pcm = (np.clip(sig, -1, 1) * 32767).astype(np.int16)
    raw = f"/tmp/{name}.raw"
    pcm.tofile(raw)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(SR), "-ac", "1", "-i", raw,
                    "-c:a", "libvorbis", "-q:a", "5", mk(f"{A}/sounds/{name}.ogg")], check=True)
    os.remove(raw)


def lowpass(x, k):
    k = max(1, int(k))
    return np.convolve(x, np.ones(k) / k, mode="same")


def gunshot(name, crack_tau, boom_hz, boom_tau, tail_s, brightness, seed):
    g = np.random.default_rng(seed)
    n = int(SR * (tail_s + 0.1))
    t = np.arange(n) / SR
    noise = g.normal(0, 1, n)
    crack = noise * np.exp(-t / crack_tau)
    crack = crack * brightness + lowpass(crack, 6) * (1 - brightness)
    boom = np.sin(2 * np.pi * boom_hz * t * np.exp(-t * 3)) * np.exp(-t / boom_tau)
    body = lowpass(noise, 14) * np.exp(-t / (boom_tau * 1.5)) * 0.8
    # short room/street echo: a few delayed, darker copies
    sig = crack * 1.0 + boom * 0.9 + body
    out = sig.copy()
    for delay, gain in [(0.045, 0.35), (0.11, 0.22), (0.19, 0.14), (0.31, 0.08)]:
        d = int(delay * SR)
        if d < n:
            out[d:] += lowpass(sig[: n - d], 10) * gain
    fade = np.ones(n)
    fade[-int(0.08 * SR):] = np.linspace(1, 0, int(0.08 * SR))
    to_ogg(np.tanh(out * 1.6) * fade, name)


def clicks(name, times, seed, tone=0.5):
    g = np.random.default_rng(seed)
    n = int(SR * (max(times) + 0.15))
    out = np.zeros(n)
    for i, tt in enumerate(times):
        s = int(tt * SR)
        m = int(0.03 * SR)
        tseg = np.arange(m) / SR
        burst = g.normal(0, 1, m) * np.exp(-tseg / 0.004)
        burst = lowpass(burst, 2 + i * 2) * (1.0 if i % 2 == 0 else 0.7)
        ring = np.sin(2 * np.pi * (1800 + 600 * i) * tseg) * np.exp(-tseg / 0.01) * tone
        out[s:s + m] += (burst + ring)[: max(0, min(m, n - s))]
    to_ogg(out, name)


def sounds():
    gunshot("gun_pistol", 0.012, 90, 0.05, 0.35, 0.6, 1)
    gunshot("gun_smg", 0.008, 110, 0.035, 0.25, 0.7, 2)
    gunshot("gun_rifle", 0.010, 80, 0.06, 0.5, 0.8, 3)
    gunshot("gun_shotgun", 0.020, 55, 0.12, 0.7, 0.4, 4)
    gunshot("gun_sniper", 0.014, 50, 0.12, 1.1, 0.85, 5)
    clicks("gun_reload", [0.0, 0.22, 0.48, 0.62], 6)
    clicks("gun_empty", [0.0], 7, tone=0.8)


# ------------------------------------------------------------------ json
GUN_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 100], "translation": [0, 3.5, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -100], "translation": [0, 3.5, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 5], "translation": [1.13, 3.6, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -5], "translation": [1.13, 3.6, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
}

GUNS = {"pistol": "Pistol", "smg": "SMG", "shotgun": "Shotgun", "rifle": "Assault Rifle", "sniper": "Sniper Rifle"}
AMMO = {"pistol_ammo": "Pistol/SMG Ammo", "rifle_ammo": "Rifle Ammo", "shotgun_shells": "Shotgun Shells"}


def data():
    for g in GUNS:
        wj(f"{A}/models/item/{g}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{g}"},
                                         "display": GUN_DISPLAY})
    for a in AMMO:
        wj(f"{A}/models/item/{a}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{a}"}})

    sp = f"{A}/sounds.json"
    sj = json.load(open(sp)) if os.path.exists(sp) else {}
    subs = {"gun_pistol": "Pistol shot", "gun_smg": "SMG fire", "gun_rifle": "Rifle fire", "gun_shotgun": "Shotgun blast",
            "gun_sniper": "Sniper shot", "gun_reload": "Gun reloads", "gun_empty": "Gun clicks empty"}
    for k, v in subs.items():
        sj[k] = {"sounds": [f"{MOD}:{k}"], "subtitle": f"subtitles.{MOD}.{k}"}
    wj(sp, sj)

    lp = f"{A}/lang/en_us.json"
    lang = json.load(open(lp)) if os.path.exists(lp) else {}
    lang[f"entity.{MOD}.citizen"] = "Citizen"
    for k, v in {**GUNS, **AMMO}.items():
        lang[f"item.{MOD}.{k}"] = v
    for k, v in subs.items():
        lang[f"subtitles.{MOD}.{k}"] = v
    wj(lp, lang)

    R = f"{D}/{MOD}/recipe"
    wj(f"{R}/pistol_ammo.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": ["minecraft:iron_nugget", "minecraft:iron_nugget", "minecraft:gunpowder"], "result": {"id": f"{MOD}:pistol_ammo", "count": 12}})
    wj(f"{R}/rifle_ammo.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": ["minecraft:iron_ingot", "minecraft:gunpowder", "minecraft:copper_ingot"], "result": {"id": f"{MOD}:rifle_ammo", "count": 12}})
    wj(f"{R}/shotgun_shells.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": ["minecraft:paper", "minecraft:gunpowder", "minecraft:iron_nugget"], "result": {"id": f"{MOD}:shotgun_shells", "count": 6}})


def main():
    for i in range(16):
        skin(i)
    guns()
    sounds()
    data()
    print("neon coast life assets done")


main()
