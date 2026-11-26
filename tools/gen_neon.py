#!/usr/bin/env python3
"""Generates Neon Coast car models (Java), all textures, sounds and JSON data."""
import os
# Since 2.0.0 (Minecraft 26.1.2) the Java model classes are maintained by hand;
# the templates here are the old 1.21.1 versions and only the textures/JSON are still generated.
WRITE_JAVA = False
import json, math, os, random, subprocess, sys
import numpy as np
from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
MOD = "neoncoast"
A = f"{ROOT}/src/main/resources/assets/{MOD}"
D = f"{ROOT}/src/main/resources/data"
J = f"{ROOT}/src/main/java/com/neoncoast/client"
random.seed(3)

COLORS = {"red": (200, 28, 28), "blue": (28, 70, 200), "black": (32, 32, 36), "white": (232, 232, 236), "yellow": (240, 190, 20)}


def mk(p):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def wj(p, o):
    json.dump(o, open(mk(p), "w"), indent=2)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, d):
    return clamp((c[0] + d, c[1] + d, c[2] + d))


def noisy(c, a=5):
    return shade(c, random.randint(-a, a)) + (255,)


# ------------------------------------------------------------------ car models
# cuboid: (x, y, z, w, h, d, role, dilation); pivots in root space: ground y=0, up = -y, front = -z, +x = car's LEFT.
# The cabin is open (frame + tinted glass) so the interior shows: dashboard with gauges, steering wheel, seats.
def wheel(r=6, w=4):
    return [(-w // 2, -r, -4, w, 2 * r, 8, "tire", 0), (-w // 2, -4, -r, w, 8, 2 * r, "tire", 0),
            (-w // 2, -4, -4, w, 8, 8, "hub", 0.2)]


def steering_wheel():
    return [(-3, -3, 0, 6, 1, 1, "wheel", 0), (-3, 2, 0, 6, 1, 1, "wheel", 0),
            (-3, -2, 0, 1, 4, 1, "wheel", 0), (2, -2, 0, 1, 4, 1, "wheel", 0),
            (-2, 0, 0, 4, 1, 1, "wheel", 0), (-1, -1, -1, 2, 2, 1, "hub_dark", 0)]


def cabin(belt, roof, z_ws, z_rw):
    """Open cabin between beltline `belt` and roof height `roof` (negative y), windshield at z_ws, rear window at z_rw."""
    h = belt - roof
    mid = (z_ws + z_rw) // 2
    return [
        (-14, roof - 1, z_ws, 28, 1, z_rw - z_ws + 1, "paint", 0),             # roof
        (-14, roof, z_ws, 1, h, 2, "paint", 0), (13, roof, z_ws, 1, h, 2, "paint", 0),    # A pillars
        (-14, roof, mid, 1, h, 2, "paint", 0), (13, roof, mid, 1, h, 2, "paint", 0),      # B pillars
        (-14, roof, z_rw - 1, 1, h, 2, "paint", 0), (13, roof, z_rw - 1, 1, h, 2, "paint", 0),  # C pillars
        (-13, roof, z_ws, 26, h, 1, "glass", 0),                                # windshield
        (-13, roof, z_rw, 26, h, 1, "glass", 0),                                # rear window
        (-14, roof, z_ws + 2, 1, h, mid - z_ws - 2, "glass", 0),                # side windows
        (13, roof, z_ws + 2, 1, h, mid - z_ws - 2, "glass", 0),
        (-14, roof, mid + 2, 1, h, z_rw - 1 - mid - 2, "glass", 0),
        (13, roof, mid + 2, 1, h, z_rw - 1 - mid - 2, "glass", 0),
    ]


def interior(belt, roof, z_dash, z_seat, z_rear):
    seat_top = roof + 3                      # seat backs stop below the roof, headrests just under it
    sh = belt - seat_top
    return [
        (-12, belt - 3, z_dash, 24, 3, 4, "dash", 0),        # dashboard (gauges face the driver)
        (-1, belt - 2, z_dash + 4, 2, 2, 7, "console", 0),   # centre console
        (0, belt - 4, z_dash + 7, 1, 2, 1, "chrome", 0),     # gear stick
        (2, seat_top, z_seat, 9, sh, 2, "seat", 0),          # driver seat (left side)
        (4, roof + 1, z_seat, 5, 2, 2, "seat", 0),           # headrest
        (-11, seat_top, z_seat, 9, sh, 2, "seat", 0),        # passenger seat
        (-9, roof + 1, z_seat, 5, 2, 2, "seat", 0),
        (-12, seat_top + 1, z_rear, 24, sh - 1, 2, "seat", 0),  # rear bench
    ]


def sedan_parts(police=False):
    belt, roof = -14, -22
    body = [
        (-15, -12, -30, 30, 8, 60, "paint", 0),          # main body
        (-14, belt, -30, 28, 2, 19, "paint", 0),          # hood
        (-14, belt, 16, 28, 2, 14, "paint", 0),           # trunk lid
        (-14, -5, -10, 28, 1, 20, "trim", 0),             # side skirts
        (-15, -6, -31, 30, 3, 1, "trim", 0),              # front bumper
        (-15, -6, 30, 30, 3, 1, "trim", 0),               # rear bumper
        (-13, -11, -31, 6, 2, 1, "headlight", 0), (7, -11, -31, 6, 2, 1, "headlight", 0),
        (-6, -11, -31, 12, 3, 1, "grille", 0),
        (-3, -8, -32, 6, 2, 1, "plate", 0),
        (-14, -11, 30, 6, 2, 1, "taillight", 0), (8, -11, 30, 6, 2, 1, "taillight", 0),
        (-3, -9, 31, 6, 2, 1, "plate", 0),
        (-11, -5, 30, 2, 1, 2, "chrome", 0),              # exhaust
        (15, belt - 2, -10, 2, 2, 2, "trim", 0), (-17, belt - 2, -10, 2, 2, 2, "trim", 0),  # mirrors
    ] + cabin(belt, roof, -11, 15) + interior(belt, roof, -10, 3, 11)
    if police:
        body += [(-10, -12, -33, 20, 7, 2, "trim", 0),    # push bar
                 (-9, roof - 3, 1, 9, 2, 4, "light_red", 0), (0, roof - 3, 1, 9, 2, 4, "light_blue", 0)]
    return body


def sports_parts():
    belt, roof = -12, -19
    return [
        (-15, -10, -31, 30, 6, 62, "paint", 0),
        (-14, belt, -31, 28, 2, 23, "paint", 0),          # long hood
        (-14, belt, 18, 28, 2, 13, "paint", 0),
        (-15, -5, -12, 30, 1, 26, "trim", 0),
        (-15, -6, -32, 30, 2, 1, "trim", 0),
        (-15, -6, 31, 30, 2, 1, "trim", 0),
        (-13, -9, -32, 7, 2, 1, "headlight", 0), (6, -9, -32, 7, 2, 1, "headlight", 0),
        (-5, -8, -32, 10, 2, 1, "grille", 0),
        (-14, -9, 31, 7, 2, 1, "taillight", 0), (7, -9, 31, 7, 2, 1, "taillight", 0),
        (-12, -5, 31, 2, 1, 2, "chrome", 0), (10, -5, 31, 2, 1, 2, "chrome", 0),
        (15, -9, 4, 1, 3, 6, "trim", 0), (-16, -9, 4, 1, 3, 6, "trim", 0),   # side intakes
        (-10, belt - 3, 27, 2, 3, 2, "trim", 0), (8, belt - 3, 27, 2, 3, 2, "trim", 0),   # spoiler posts
        (-15, belt - 4, 26, 30, 1, 5, "paint", 0),       # spoiler wing
        (15, belt - 2, -7, 2, 2, 2, "trim", 0), (-17, belt - 2, -7, 2, 2, 2, "trim", 0),
    ] + cabin(belt, roof, -8, 17) + interior(belt, roof, -7, 6, 14)


def car_spec(cls, layer, body, wz, tex, steer_at):
    sx, sy, sz = steer_at
    return {"class": cls, "layer": layer, "tex": tex, "parts": [
        ("body", "root", (0, 0, 0, 0), body),
        ("steering", "body", (sx, sy, sz, -0.45), steering_wheel()),
        ("front_left", "root", (13, -6, -wz, 0), wheel()),
        ("front_right", "root", (-13, -6, -wz, 0), wheel()),
        ("rear_left", "root", (13, -6, wz, 0), wheel()),
        ("rear_right", "root", (-13, -6, wz, 0), wheel()),
    ]}


SEDAN = car_spec("SedanModel", "sedan", sedan_parts(), 19, (256, 256), (6, -16, -6))
SPORTS = car_spec("SportsCarModel", "sports_car", sports_parts(), 20, (256, 256), (6, -13, -3))
POLICE = car_spec("PoliceCarModel", "police_car", sedan_parts(True), 19, (256, 256), (6, -16, -6))


def pack(model):
    tw, th = model["tex"]
    out, cache = [], {}
    x = y = shelf = 0
    items = [(p[0], c) for p in model["parts"] for c in p[3]]
    items.sort(key=lambda it: -(it[1][4] + it[1][5]))
    for pname, c in items:
        key = c if c[6] in ("tire", "hub", "wheel", "hub_dark") else None
        if key is not None and key in cache:
            out.append((pname, c) + cache[key])
            continue
        w, h, d = c[3], c[4], c[5]
        fw, fh = 2 * (w + d), d + h
        if x + fw > tw:
            x, y, shelf = 0, y + shelf, 0
        if y + fh > th:
            raise SystemExit("texture too small for " + model["class"])
        out.append((pname, c, x, y))
        if key is not None:
            cache[key] = (x, y)
        x += fw
        shelf = max(shelf, fh)
    return out


def paint(img, u, v, w, h, d, role, base, police=False, flash=False):
    px = img.load()
    faces = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "side1": (u, v + d, d, h),
             "front": (u + d, v + d, w, h), "side2": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
    for name, (fx, fy, fw, fh) in faces.items():
        for i in range(fw):
            for j in range(fh):
                a = 255
                c = base
                edge = i == 0 or j == 0 or i == fw - 1 or j == fh - 1
                if role == "paint":
                    if name == "top":
                        c = shade(base, 20)
                    elif name == "bottom":
                        c = (28, 28, 30)
                    if police:
                        c = (240, 240, 240) if (name == "top" or j < fh // 2) else (22, 22, 28)
                    if name in ("side1", "side2") and fw > 30:
                        if i in (fw // 3, (2 * fw) // 3):
                            c = shade(c, -45)                      # door shut lines
                        if j == 2 and (i == fw // 3 - 3 or i == (2 * fw) // 3 - 3):
                            c = (200, 200, 205)                    # door handles
                    if edge:
                        c = shade(c, -14)
                elif role == "glass":
                    c, a = (90, 130, 170), 95
                    if j == 0 or (i + j) % 11 == 0:
                        c, a = (200, 220, 240), 130                # reflections
                elif role == "trim":
                    c = (40, 40, 44)
                elif role == "grille":
                    c = (18, 18, 20) if j % 2 else (70, 70, 76)
                elif role == "headlight":
                    c = (255, 248, 215)
                elif role == "taillight":
                    c = (215, 22, 22)
                elif role == "plate":
                    c = (240, 240, 230) if not edge else (40, 40, 120)
                elif role == "chrome":
                    c = (200, 200, 208)
                elif role == "tire":
                    c = (30, 30, 30) if (i + j) % 3 else (48, 48, 48)
                elif role == "hub":
                    c = (195, 195, 202)
                elif role == "seat":
                    c = (44, 40, 38) if (j % 3) else (34, 30, 28)
                elif role == "console":
                    c = (28, 28, 30)
                elif role == "wheel":
                    c = (22, 22, 24)
                elif role == "hub_dark":
                    c = (60, 60, 66)
                elif role == "dash":
                    c = (30, 30, 32)
                    if name == "back":                             # the face the driver looks at
                        c = (14, 14, 16)
                        for gx in (fw - 7, fw - 3):
                            if (i - gx) ** 2 + (j - fh // 2) ** 2 <= 2:
                                c = (230, 230, 230)
                        if i == fw - 6 and j == fh // 2:
                            c = (230, 40, 30)                      # speedo needle
                        if fw // 2 - 3 <= i <= fw // 2 + 2 and 0 < j < fh - 1:
                            c = (40, 110, 200)                     # centre screen
                elif role == "light_red":
                    c = (255, 60, 60) if flash else (120, 20, 20)
                elif role == "light_blue":
                    c = (70, 120, 255) if not flash else (20, 30, 110)
                c = noisy(c, 4 if role == "paint" else 2)
                px[fx + i, fy + j] = c[:3] + (a,)


MODEL_TEMPLATE = """package com.neoncoast.client;

import com.neoncoast.NeonCoast;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Generated by tools/gen_neon.py - shapes and texture layout must match. Rendered translucent for the tinted glass. */
public class CLS extends SinglePartEntityModel<CarEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(NeonCoast.id("LAYERNAME"), "main");
    private final ModelPart top;
    private final ModelPart body;
    private final ModelPart steering;
    private final ModelPart frontLeft, frontRight, rearLeft, rearRight;

    public CLS(ModelPart top) {
        super(RenderLayer::getEntityTranslucent);
        this.top = top;
        ModelPart root = top.getChild("root");
        this.body = root.getChild("body");
        this.steering = body.getChild("steering");
        this.frontLeft = root.getChild("front_left");
        this.frontRight = root.getChild("front_right");
        this.rearLeft = root.getChild("rear_left");
        this.rearRight = root.getChild("rear_right");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot().addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, 24.0f, 0.0f));
PARTS
        return TexturedModelData.of(data, TW, TH);
    }

    @Override
    public ModelPart getPart() {
        return top;
    }

    @Override
    public void setAngles(CarEntity car, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        float td = MathHelper.clamp(animationProgress - car.age, 0.0f, 1.0f);
        float steer = car.getSteerAngle(td) * MathHelper.RADIANS_PER_DEGREE;
        float spin = car.getWheelSpin(td);
        body.roll = car.getRoll(td) * MathHelper.RADIANS_PER_DEGREE;
        body.pitch = car.getBodyPitch(td) * MathHelper.RADIANS_PER_DEGREE;
        steering.roll = -steer * 3.0f;   // the steering wheel turns about three times as far as the tyres
        frontLeft.yaw = steer;
        frontRight.yaw = steer;
        frontLeft.pitch = spin;
        frontRight.pitch = spin;
        rearLeft.pitch = spin;
        rearRight.pitch = spin;
    }
}
"""


def gen_model(model, colors, police=False):
    placed = pack(model)
    tw, th = model["tex"]
    for cname, base in colors.items():
        for flash in ([False, True] if police else [False]):
            img = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
            random.seed(hash((cname, flash)) & 0xffff)
            for pname, c, u, v in placed:
                paint(img, u, v, c[3], c[4], c[5], c[6], base, police, flash)
            name = model["layer"] + ("" if police else "_" + cname) + ("_flash" if flash else "")
            img.save(mk(f"{A}/textures/entity/{name}.png"))
    f = lambda v: f"{float(v)}f"
    lines = []
    for pname, parent, pivot, cubs in model["parts"]:
        chain = "ModelPartBuilder.create()"
        for (pp, c, u, v) in placed:
            if pp != pname:
                continue
            x, y, z, w, h, d, role, dil = c
            dil_s = f", new Dilation({f(dil)})" if dil else ""
            chain += f".uv({u}, {v}).cuboid({f(x)}, {f(y)}, {f(z)}, {f(w)}, {f(h)}, {f(d)}{dil_s})"
        prefix = "ModelPartData bodyData = " if pname == "body" else ""
        par = "bodyData" if parent == "body" else parent
        lines.append(f"        {prefix}{par}.addChild(\"{pname}\", {chain},\n"
                     f"                ModelTransform.of({f(pivot[0])}, {f(pivot[1])}, {f(pivot[2])}, {f(pivot[3])}, 0.0f, 0.0f));")
    src = (MODEL_TEMPLATE.replace("CLS", model["class"]).replace("LAYERNAME", model["layer"])
           .replace("PARTS", "\n".join(lines)).replace("TW", str(tw)).replace("TH", str(th)))
    (open(os.devnull, "w") if not WRITE_JAVA else open(mk(f"{J}/{model['class']}.java"), "w")).write(src)


# ------------------------------------------------------------------ police officer skin (64x64 player layout)
def police_skin():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    navy, dark, skin, badge, white, black = (28, 38, 80), (18, 24, 52), (214, 170, 130), (230, 190, 40), (235, 235, 235), (20, 20, 20)

    def box(u, v, w, h, d, fn):
        faces = [(u + d, v, w, d, "top"), (u + d + w, v, w, d, "bottom"), (u, v + d, d, h, "right"),
                 (u + d, v + d, w, h, "front"), (u + d + w, v + d, d, h, "left"), (u + 2 * d + w, v + d, w, h, "back")]
        for fx, fy, fw, fh, name in faces:
            for i in range(fw):
                for j in range(fh):
                    c = fn(name, i, j, fw, fh)
                    if c is not None:
                        img.putpixel((fx + i, fy + j), noisy(c, 4))

    def head(n, i, j, fw, fh):
        if n == "top": return (60, 40, 25)
        if n == "front":
            if j in (3, 4) and i in (1, 2, 5, 6): return white if i in (1, 5) else (40, 60, 120)
            if j == 6 and 2 <= i <= 5: return (150, 90, 80)
        if j < 2 and n != "bottom": return (60, 40, 25)
        return skin
    box(0, 0, 8, 8, 8, head)

    def cap(n, i, j, fw, fh):          # hat layer = police cap
        if n == "bottom": return None
        if n in ("top",): return navy
        if j < 3: return navy if not (n == "front" and j == 2 and 3 <= i <= 4) else badge
        if j == 3 and n == "front": return black
        return None
    box(32, 0, 8, 8, 8, cap)

    def torso(n, i, j, fw, fh):
        if n == "front" and j == 3 and i == 2: return badge
        if n == "front" and i == 4: return dark
        if j >= 10: return black
        return navy
    box(16, 16, 8, 12, 4, torso)

    def arm(n, i, j, fw, fh):
        if j >= 10 and n != "top": return skin
        return navy
    box(40, 16, 4, 12, 4, arm)
    box(32, 48, 4, 12, 4, arm)

    def leg(n, i, j, fw, fh):
        if j >= 10: return black
        return dark
    box(0, 16, 4, 12, 4, leg)
    box(16, 48, 4, 12, 4, leg)
    img.save(mk(f"{A}/textures/entity/police_officer.png"))


# ------------------------------------------------------------------ 16x16 item & block textures
def img16():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), tuple(c) + ((255,) if len(c) == 3 else ()))


def rect(im, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1): put(im, x, y, c)


def key_icon(color):
    im = img16()
    rect(im, 2, 3, 7, 12, color); rect(im, 3, 4, 6, 11, shade(color, 30))
    rect(im, 4, 6, 5, 7, (240, 240, 240))
    rect(im, 8, 7, 14, 8, (190, 190, 196)); rect(im, 12, 9, 12, 10, (190, 190, 196)); rect(im, 14, 9, 14, 10, (190, 190, 196))
    return im


def textures():
    key_icon((50, 90, 200)).save(mk(f"{A}/textures/item/sedan_key.png"))
    key_icon((200, 30, 30)).save(mk(f"{A}/textures/item/sports_car_key.png"))
    key_icon((25, 25, 30)).save(mk(f"{A}/textures/item/police_car_key.png"))
    im = img16(); rect(im, 2, 5, 13, 13, (200, 30, 30)); rect(im, 5, 3, 10, 4, (60, 60, 66)); rect(im, 7, 7, 8, 11, (240, 240, 240)); rect(im, 5, 9, 10, 9, (240, 240, 240))
    im.save(mk(f"{A}/textures/item/repair_kit.png"))
    im = img16(); rect(im, 2, 4, 13, 13, (176, 132, 82)); rect(im, 2, 4, 13, 5, (150, 110, 66)); rect(im, 7, 4, 8, 13, (210, 190, 140))
    im.save(mk(f"{A}/textures/item/package.png"))
    im = img16()
    for x in range(16):
        for y in range(16): put(im, x, y, (60, 60, 66) if (x % 8 < 2 or y % 8 < 2) else (120, 170, 220) if (x + y) % 5 else (200, 200, 210))
    im.save(mk(f"{A}/textures/item/city_kit.png"))

    def tile(fn, name):
        t = img16()
        for x in range(16):
            for y in range(16): put(t, x, y, fn(x, y))
        t.save(mk(f"{A}/textures/block/{name}.png"))
    tile(lambda x, y: noisy((48, 48, 52), 7)[:3], "asphalt")
    tile(lambda x, y: (235, 190, 30) if 6 <= x <= 9 else noisy((48, 48, 52), 7)[:3], "road_line")
    tile(lambda x, y: (120, 120, 126) if (x % 8 == 0 or y % 8 == 0) else noisy((172, 172, 178), 5)[:3], "sidewalk")
    tile(lambda x, y: noisy((60, 64, 70), 4)[:3], "shop_terminal_side")
    def shop_front(x, y):
        if 2 <= x <= 13 and 2 <= y <= 7: return (30, 140, 60) if (x + y) % 2 else (40, 170, 80)
        if 3 <= x <= 12 and 9 <= y <= 13: return (180, 180, 186) if (x % 3) else (90, 90, 96)
        return noisy((60, 64, 70), 4)[:3]
    tile(shop_front, "shop_terminal_front")
    def board(x, y):
        if x in (0, 15) or y in (0, 15): return (100, 70, 40)
        if y in (3, 7, 11) and 3 <= x <= 12: return (40, 40, 40)
        if 4 <= x <= 7 and 4 <= y <= 6: return (240, 240, 230)
        if 9 <= x <= 12 and 8 <= y <= 10: return (240, 230, 160)
        return noisy((176, 140, 92), 5)[:3]
    tile(board, "job_board")
    def chop(x, y):
        if y in (2, 3) and 2 <= x <= 13: return (220, 40, 40)
        if 5 <= y <= 14 and x % 3 == 0: return (70, 70, 76)
        return noisy((130, 130, 136), 5)[:3]
    tile(chop, "chop_shop")


# ------------------------------------------------------------------ sounds
def to_ogg(sig, name, sr=44100):
    pcm = (np.clip(sig, -1, 1) * 32767).astype(np.int16)
    raw = f"/tmp/{name}.raw"
    pcm.tofile(raw)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(sr), "-ac", "1", "-i", raw,
                    "-c:a", "libvorbis", "-q:a", "5", mk(f"{A}/sounds/{name}.ogg")], check=True)
    os.remove(raw)


def sounds():
    sr = 44100
    t = np.arange(sr) / sr
    # V8-ish burble: 40 Hz firing with uneven pulses, seamless 1 s loop
    f0 = 40.0
    ph = (t * f0) % 1.0
    pulses = np.exp(-ph * 6.0) * (1.0 + 0.35 * np.sin(2 * np.pi * 10 * t))
    sig = 0.5 * pulses
    for k, a in [(1, 0.35), (2, 0.3), (3, 0.15), (4, 0.12), (6, 0.06)]:
        sig += a * np.sin(2 * np.pi * f0 * k * t + k)
    rng = np.random.default_rng(1)
    n = rng.normal(0, 1, sr)
    n = np.real(np.fft.ifft(np.fft.fft(n) * np.fft.fft(np.ones(12) / 12, sr)))
    sig += 0.25 * n * (0.3 + pulses)
    sig = np.tanh(sig * 1.4)
    sig -= sig.mean()
    to_ogg(sig * 0.85 / np.max(np.abs(sig)), "car_engine")
    # siren: 2 s wail loop (frequency sweeps 650 -> 1350 -> 650 Hz)
    n2 = 2 * sr
    t2 = np.arange(n2) / sr
    freq = 1000 - 350 * np.cos(2 * np.pi * t2 / 2.0)
    phase = 2 * np.pi * np.cumsum(freq) / sr
    phase *= (np.round(phase[-1] / (2 * np.pi)) * 2 * np.pi) / phase[-1]  # whole cycles -> clean loop
    siren = 0.6 * np.sin(phase) + 0.25 * np.sin(2 * phase) + 0.1 * np.sin(3 * phase)
    to_ogg(np.tanh(siren * 1.5) * 0.8, "siren")


# ------------------------------------------------------------------ json data
def data():
    tex = lambda n: f"{MOD}:block/{n}"
    for b in ["asphalt", "road_line", "sidewalk", "job_board", "chop_shop"]:
        wj(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex(b)}})
        wj(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"{MOD}:block/{b}"}}})
        wj(f"{A}/models/item/{b}.json", {"parent": f"{MOD}:block/{b}"})
    wj(f"{A}/models/block/shop_terminal.json", {"parent": "minecraft:block/orientable",
        "textures": {"front": tex("shop_terminal_front"), "side": tex("shop_terminal_side"), "top": tex("shop_terminal_side")}})
    wj(f"{A}/blockstates/shop_terminal.json", {"variants": {
        "facing=north": {"model": f"{MOD}:block/shop_terminal"},
        "facing=east": {"model": f"{MOD}:block/shop_terminal", "y": 90},
        "facing=south": {"model": f"{MOD}:block/shop_terminal", "y": 180},
        "facing=west": {"model": f"{MOD}:block/shop_terminal", "y": 270}}})
    wj(f"{A}/models/item/shop_terminal.json", {"parent": f"{MOD}:block/shop_terminal"})
    for n in ["sedan_key", "sports_car_key", "police_car_key", "repair_kit", "package", "city_kit"]:
        wj(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{n}"}})
    wj(f"{A}/sounds.json", {"car_engine": {"sounds": [f"{MOD}:car_engine"], "subtitle": f"subtitles.{MOD}.car_engine"},
                            "siren": {"sounds": [f"{MOD}:siren"], "subtitle": f"subtitles.{MOD}.siren"}})
    wj(f"{A}/lang/en_us.json", {
        f"itemGroup.{MOD}": "Neon Coast",
        f"entity.{MOD}.sedan": "Sedan", f"entity.{MOD}.sports_car": "Sports Car", f"entity.{MOD}.police_car": "Police Car",
        f"entity.{MOD}.police_officer": "Police Officer",
        f"item.{MOD}.sedan_key": "Sedan Key", f"item.{MOD}.sports_car_key": "Sports Car Key", f"item.{MOD}.police_car_key": "Police Car Key",
        f"item.{MOD}.repair_kit": "Repair Kit", f"item.{MOD}.package": "Delivery Package", f"item.{MOD}.city_kit": "City Kit",
        f"block.{MOD}.asphalt": "Asphalt", f"block.{MOD}.road_line": "Road Line", f"block.{MOD}.sidewalk": "Sidewalk",
        f"block.{MOD}.shop_terminal": "Shop Terminal", f"block.{MOD}.job_board": "Job Board", f"block.{MOD}.chop_shop": "Chop Shop",
        f"subtitles.{MOD}.car_engine": "Engine revs", f"subtitles.{MOD}.siren": "Police siren"})
    for b in ["asphalt", "road_line", "sidewalk", "shop_terminal", "job_board", "chop_shop"]:
        wj(f"{D}/{MOD}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    wj(f"{D}/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"{MOD}:{b}" for b in
        ["asphalt", "road_line", "sidewalk", "shop_terminal", "chop_shop"]]})
    wj(f"{D}/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": [f"{MOD}:job_board"]})
    R = f"{D}/{MOD}/recipe"
    def shaped(name, pat, key, res, n=1, cat="misc"):
        wj(f"{R}/{name}.json", {"type": "minecraft:crafting_shaped", "category": cat, "pattern": pat,
            "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()}, "result": {"id": res, "count": n}})
    shaped("asphalt", ["GC", "CG"], {"G": "minecraft:gravel", "C": "minecraft:coal"}, f"{MOD}:asphalt", 8, "building")
    shaped("road_line", ["A", "Y"], {"A": f"{MOD}:asphalt", "Y": "minecraft:yellow_dye"}, f"{MOD}:road_line", 1, "building")
    shaped("sidewalk", ["SS", "SS"], {"S": "minecraft:smooth_stone"}, f"{MOD}:sidewalk", 4, "building")
    shaped("repair_kit", [" I ", "IRI", " I "], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, f"{MOD}:repair_kit")
    shaped("shop_terminal", ["III", "IGI", "IEI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "E": "minecraft:emerald"}, f"{MOD}:shop_terminal")
    shaped("job_board", ["PPP", "PBP", "PPP"], {"P": "#minecraft:planks", "B": "minecraft:book"}, f"{MOD}:job_board")
    shaped("chop_shop", ["III", "IAI", "III"], {"I": "minecraft:iron_ingot", "A": "minecraft:anvil"}, f"{MOD}:chop_shop")
    wj(f"{D}/{MOD}/worldgen/configured_feature/city.json", {"type": f"{MOD}:city", "config": {}})
    wj(f"{D}/{MOD}/worldgen/placed_feature/city.json", {"feature": f"{MOD}:city", "placement": []})


def main():
    gen_model(SEDAN, COLORS)
    gen_model(SPORTS, COLORS)
    gen_model(POLICE, {"x": (240, 240, 240)}, police=True)
    police_skin()
    textures()
    sounds()
    data()
    icon = Image.open(f"{A}/textures/item/sports_car_key.png").resize((128, 128), Image.NEAREST)
    icon.save(mk(f"{A}/icon.png"))
    print("neon coast assets done")
    # building blocks + All City world type (also re-adds their lang/tag entries)
    subprocess.run([sys.executable, os.path.join(os.path.dirname(os.path.abspath(__file__)), "gen_city.py"), ROOT], check=True)


main()
