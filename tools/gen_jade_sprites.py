#!/usr/bin/env python3
"""
Generates the placeholder sprites of the Jade Accord faction into assets/sprites.

    python3 tools/gen_jade_sprites.py [--preview preview.png]

The art is procedural (faceted, shaded shapes drawn at 4x and downsampled) so the faction is fully
playable without hand-drawn art. To replace a sprite, just overwrite the PNG in assets/sprites and
remove the matching function call below so it is not regenerated.
"""
import argparse
import math
import os

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

SS = 4
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets", "sprites")
BLOCK_DIR = os.path.join(ROOT, "blocks", "jade")
UNIT_DIR = os.path.join(ROOT, "unit", "jade")
ITEM_DIR = os.path.join(ROOT, "item", "jade")
STATUS_DIR = os.path.join(ROOT, "status")

HULL_D = (34, 52, 47)
HULL = (62, 92, 82)
HULL_L = (104, 142, 128)
JADE = (63, 191, 143)
JADE_L = (180, 245, 214)
JADE_D = (29, 107, 83)
POLLEN = (246, 216, 107)
INFECT = (155, 217, 74)
INFECT_D = (74, 122, 36)
RESIN = (217, 165, 58)
RESIN_D = (140, 95, 30)
WHITE = (255, 255, 255)

generated = []


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def rrect(x0, y0, x1, y1, r, n=6):
    pts = []
    for cx, cy, a0 in ((x1 - r, y0 + r, -90), (x1 - r, y1 - r, 0), (x0 + r, y1 - r, 90), (x0 + r, y0 + r, 180)):
        for i in range(n + 1):
            a = math.radians(a0 + 90 * i / n)
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def ellipse(cx, cy, rx, ry, n=36):
    return [(cx + rx * math.cos(2 * math.pi * i / n), cy + ry * math.sin(2 * math.pi * i / n)) for i in range(n)]


def mirror(half, cx):
    """Half polygon (right side, listed top->bottom) -> full symmetric polygon around x = cx."""
    return half + [(2 * cx - x, y) for x, y in reversed(half)]


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))

    def _mask(self, pts):
        m = Image.new("L", self.img.size, 0)
        ImageDraw.Draw(m).polygon([(x * SS, y * SS) for x, y in pts], fill=255)
        return m

    def part(self, pts, base, light=None, dark=None, rim=0.55, shine=1.28, alpha=255):
        """Shaded polygon: light top-left -> dark bottom-right, darker rim, bright inner top-left edge."""
        light = light or lerp(base, WHITE, 0.3)
        dark = dark or lerp(base, (0, 0, 0), 0.35)
        mask = self._mask(pts)
        m = np.asarray(mask, dtype=np.float32) / 255.0
        ys, xs = np.nonzero(m)
        if len(xs) == 0:
            return
        x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
        gy, gx = np.mgrid[0:self.img.size[1], 0:self.img.size[0]].astype(np.float32)
        t = ((gx - x0) / max(1, x1 - x0) + (gy - y0) / max(1, y1 - y0)) * 0.5
        t = np.clip(t, 0, 1)[..., None]
        b, l, d = (np.array(c, dtype=np.float32) for c in (base, light, dark))
        col = np.where(t < 0.5, b + (l - b) * (1 - 2 * t), b + (d - b) * (2 * t - 1))

        k = SS * 2 + 1
        eroded = mask.filter(ImageFilter.MinFilter(k))
        band = np.asarray(ImageChops.subtract(mask, eroded), dtype=np.float32)[..., None] / 255.0
        col = col * (1 - band * (1 - rim))
        inner = ImageChops.subtract(eroded, ImageChops.offset(eroded, SS, SS))
        hl = np.asarray(inner, dtype=np.float32)[..., None] / 255.0
        col = np.minimum(255.0, col * (1 + hl * (shine - 1)))

        rgba = np.concatenate([col, (m * alpha)[..., None]], axis=2).astype(np.uint8)
        self.img = Image.alpha_composite(self.img, Image.fromarray(rgba, "RGBA"))

    def flat(self, pts, color, alpha=255):
        layer = Image.new("RGBA", self.img.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).polygon([(x * SS, y * SS) for x, y in pts], fill=tuple(color) + (alpha,))
        self.img = Image.alpha_composite(self.img, layer)

    def line(self, pts, color, width=1.0, alpha=255):
        layer = Image.new("RGBA", self.img.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).line([(x * SS, y * SS) for x, y in pts], fill=tuple(color) + (alpha,),
                                   width=max(1, int(width * SS)), joint="curve")
        self.img = Image.alpha_composite(self.img, layer)

    def glow(self, cx, cy, r, color, alpha=170):
        layer = Image.new("RGBA", self.img.size, (0, 0, 0, 0))
        d = ImageDraw.Draw(layer)
        d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], fill=tuple(color) + (alpha,))
        layer = layer.filter(ImageFilter.GaussianBlur(r * SS * 0.45))
        self.img = Image.alpha_composite(self.img, layer)

    def circle(self, cx, cy, r, base, **kw):
        self.part(ellipse(cx, cy, r, r, 28), base, **kw)

    def outline(self, color=(20, 30, 27), width=1):
        """Dark 1px silhouette outline (used for blocks and items; units are outlined by the game)."""
        a = self.img.split()[3]
        grown = a.filter(ImageFilter.MaxFilter(SS * 2 * width + 1))
        edge = ImageChops.subtract(grown, a)
        solid = Image.new("RGBA", self.img.size, tuple(color) + (255,))
        solid.putalpha(edge)
        self.img = Image.alpha_composite(solid, self.img)

    def result(self):
        return self.img.resize((self.w, self.h), Image.LANCZOS)

    def save(self, directory, name):
        os.makedirs(directory, exist_ok=True)
        path = os.path.join(directory, name + ".png")
        self.result().save(path)
        generated.append(path)


def leaf(c, cx, cy, length, width, angle, base=JADE, **kw):
    """Pointed leaf from (cx, cy) growing towards `angle` (degrees, 0 = up)."""
    a = math.radians(angle)
    ux, uy = math.sin(a), -math.cos(a)
    px, py = -uy, ux
    pts = []
    for i in range(0, 9):
        s = i / 8
        w = math.sin(math.pi * s) ** 0.8 * width / 2
        pts.append((cx + ux * length * s + px * w, cy + uy * length * s + py * w))
    for i in range(8, -1, -1):
        s = i / 8
        w = math.sin(math.pi * s) ** 0.8 * width / 2
        pts.append((cx + ux * length * s - px * w, cy + uy * length * s - py * w))
    c.part(pts, base, **kw)


def flower(c, cx, cy, r, petals=6, petal=POLLEN, center=WHITE, rot=0):
    for i in range(petals):
        leaf(c, cx, cy, r, r * 0.62, rot + i * 360 / petals, base=petal)
    c.circle(cx, cy, r * 0.28, center)


# ---------------------------------------------------------------------------------------------
# Items (32x32)
# ---------------------------------------------------------------------------------------------
def item_resin():
    c = Canvas(32, 32)
    c.part(mirror([(16, 4), (19, 11), (22, 16), (23, 21), (20, 26), (16, 28)], 16), RESIN, light=lerp(RESIN, WHITE, 0.55), dark=RESIN_D)
    c.circle(12.5, 19, 2.2, lerp(RESIN, WHITE, 0.7), rim=1.0, shine=1.0)
    c.outline()
    c.save(ITEM_DIR, "verdant-resin")


def item_silk():
    c = Canvas(32, 32)
    for off, ang in ((-6, -25), (0, 0), (6, 25)):
        pts = [(16 + off + 9 * math.sin(math.radians(ang)) * math.sin(math.pi * i / 10) * 1.0 + (i - 5) * ang / 30.0,
                4 + i * 2.4) for i in range(11)]
        pts2 = [(x + 4, y) for x, y in reversed(pts)]
        c.part(pts + pts2, JADE, light=JADE_L, dark=JADE_D)
    c.part(ellipse(16, 16, 11, 5, 30), JADE_L, light=WHITE, dark=JADE)
    c.part(ellipse(16, 16, 6, 11, 30), JADE, light=JADE_L, dark=JADE_D, alpha=210)
    c.outline()
    c.save(ITEM_DIR, "jade-silk")


def item_core():
    c = Canvas(32, 32)
    for a in (-60, 60, 180):
        leaf(c, 16, 16, 13, 8, a, base=JADE, light=JADE_L, dark=JADE_D)
    c.glow(16, 16, 11, POLLEN, 150)
    flower(c, 16, 16, 12, petals=6, petal=POLLEN, center=WHITE)
    c.outline()
    c.save(ITEM_DIR, "everbloom-core")


# ---------------------------------------------------------------------------------------------
# Status effect icons (32x32)
# ---------------------------------------------------------------------------------------------
def status_icons():
    c = Canvas(32, 32)
    flower(c, 16, 16, 13, petals=5, petal=JADE, center=POLLEN, rot=18)
    c.outline()
    c.save(STATUS_DIR, "bloom")

    r = Canvas(32, 32)
    r.part(rrect(14, 14, 18, 29, 1.5), JADE_D, light=JADE, dark=HULL_D)
    leaf(r, 16, 20, 13, 9, -50, base=JADE, light=JADE_L, dark=JADE_D)
    leaf(r, 16, 17, 13, 9, 50, base=JADE, light=JADE_L, dark=JADE_D)
    r.part([(14.5, 3), (17.5, 3), (17.5, 8.5), (23, 8.5), (23, 11.5), (17.5, 11.5), (17.5, 17), (14.5, 17), (14.5, 11.5), (9, 11.5), (9, 8.5), (14.5, 8.5)], POLLEN, light=WHITE, dark=RESIN)
    r.outline()
    r.save(STATUS_DIR, "regrowth")

    i = Canvas(32, 32)
    i.part(rrect(14, 16, 18, 29, 1.5), JADE_D, light=JADE, dark=HULL_D)
    i.part(ellipse(16, 12, 9, 9, 28), INFECT_D, light=INFECT, dark=HULL_D)
    for a in range(0, 360, 60):
        leaf(i, 16, 12, 13, 5, a, base=INFECT_D, light=INFECT, dark=HULL_D)
    i.circle(16, 12, 4, INFECT, light=WHITE, dark=INFECT_D, rim=0.8)
    i.outline()
    i.save(STATUS_DIR, "infested")


# ---------------------------------------------------------------------------------------------
# Blocks
# ---------------------------------------------------------------------------------------------
def plate(c, size_px, inset=2, radius=5, base=HULL):
    s = size_px
    c.part(rrect(inset, inset, s - inset, s - inset, radius), base, light=HULL_L, dark=HULL_D)
    c.part(rrect(inset + 4, inset + 4, s - inset - 4, s - inset - 4, radius - 1), lerp(base, (0, 0, 0), 0.15),
           light=lerp(base, WHITE, 0.08), dark=HULL_D, rim=0.7, shine=1.1)
    for x in (inset + 3, s - inset - 3):
        for y in (inset + 3, s - inset - 3):
            c.circle(x, y, 1.4, JADE, rim=0.6)


def vines(c, size_px, count=3):
    s = size_px
    for i in range(count):
        t = (i + 1) / (count + 1)
        leaf(c, s * t, s - 3, s * 0.18, s * 0.09, -30 + 60 * (i % 2), base=JADE_D, light=JADE, dark=HULL_D)


def blk_resin_tapper():
    c = Canvas(64, 64)
    plate(c, 64)
    c.circle(32, 32, 17, HULL_D, rim=0.6)
    c.circle(32, 32, 14, RESIN, light=lerp(RESIN, WHITE, 0.5), dark=RESIN_D)
    c.part(rrect(30, 8, 34, 24, 1.5), HULL_L, light=lerp(HULL_L, WHITE, 0.3))
    c.circle(32, 32, 5, lerp(RESIN, WHITE, 0.75), rim=0.9, shine=1.0)
    leaf(c, 14, 50, 12, 6, -35, base=JADE, light=JADE_L, dark=JADE_D)
    leaf(c, 50, 50, 12, 6, 35, base=JADE, light=JADE_L, dark=JADE_D)
    c.outline()
    c.save(BLOCK_DIR, "resin-tapper")


def blk_silk_loom():
    c = Canvas(96, 96)
    plate(c, 96, radius=7)
    c.part(rrect(14, 14, 82, 82, 4), HULL_D, rim=0.6)
    for i in range(7):
        x = 20 + i * 9.3
        c.line([(x, 16), (x, 80)], JADE, 2.2)
        c.line([(x - 0.8, 16), (x - 0.8, 80)], JADE_L, 0.8)
    c.part(rrect(10, 40, 86, 54, 3), HULL_L, light=lerp(HULL_L, WHITE, 0.3), dark=HULL)
    for x in (22, 48, 74):
        c.circle(x, 47, 4, JADE, light=JADE_L, dark=JADE_D)
    c.outline()
    c.save(BLOCK_DIR, "silk-loom")
    t = Canvas(96, 96)
    for i in range(7):
        x = 20 + i * 9.3
        t.line([(x, 16), (x, 80)], JADE_L, 1.4, alpha=110)
    t.part(rrect(10, 40, 86, 54, 3), HULL_L, light=lerp(HULL_L, WHITE, 0.3), dark=HULL)
    for x in (22, 48, 74):
        t.circle(x, 47, 4, JADE, light=JADE_L, dark=JADE_D)
    t.outline()
    t.save(BLOCK_DIR, "silk-loom-top")


def blk_conservatory():
    c = Canvas(96, 96)
    plate(c, 96, radius=7)
    c.circle(48, 48, 33, HULL_D, rim=0.6)
    c.circle(48, 48, 29, lerp(JADE_D, HULL_D, 0.4), light=JADE_D, dark=HULL_D)
    flower(c, 48, 48, 22, petals=8, petal=JADE, center=POLLEN, rot=11)
    flower(c, 48, 48, 12, petals=6, petal=POLLEN, center=WHITE)
    c.outline()
    c.save(BLOCK_DIR, "bloom-conservatory")
    t = Canvas(96, 96)
    t.part(ellipse(48, 48, 29, 29, 40), JADE_L, light=WHITE, dark=JADE, alpha=60)
    t.line([(32, 30), (44, 24)], WHITE, 2.4, alpha=170)
    t.line([(30, 38), (33, 33)], WHITE, 1.6, alpha=140)
    t.save(BLOCK_DIR, "bloom-conservatory-top")


def blk_spore_furnace():
    c = Canvas(64, 64)
    plate(c, 64)
    c.circle(32, 32, 18, HULL_D, rim=0.6)
    c.glow(32, 32, 16, POLLEN, 120)
    c.circle(32, 32, 14, lerp(HULL, HULL_D, 0.2))
    for a in range(0, 360, 60):
        leaf(c, 32, 32, 13, 7, a, base=lerp(JADE, POLLEN, 0.3 if a % 120 else 0.0), light=JADE_L, dark=JADE_D)
    c.circle(32, 32, 4.5, POLLEN, light=WHITE, rim=0.8)
    c.outline()
    c.save(BLOCK_DIR, "spore-furnace")


def blk_wall(size):
    px = 32 * size
    c = Canvas(px, px)
    c.part(rrect(1, 1, px - 1, px - 1, 4), JADE_D, light=JADE, dark=HULL_D)
    c.part(rrect(4, 4, px - 4, px - 4, 3), lerp(JADE, HULL, 0.45), light=JADE_L, dark=JADE_D, rim=0.7)
    mid = px / 2
    for i in range(size):
        cy = 8 + i * 32
        c.part(mirror([(mid, cy + 4), (mid + 10, cy + 12), (mid + 10, cy + 17), (mid, cy + 9)], mid), JADE_D, light=JADE, dark=HULL_D, rim=0.7)
    c.outline()
    c.save(BLOCK_DIR, "jade-wall" if size == 1 else "jade-wall-large")


def blk_pylon():
    c = Canvas(64, 64)
    plate(c, 64)
    c.circle(32, 32, 15, HULL_D, rim=0.6)
    for a in range(0, 360, 90):
        leaf(c, 32, 32, 17, 8, a + 45, base=JADE_D, light=JADE, dark=HULL_D)
    c.outline()
    c.save(BLOCK_DIR, "lifebloom-pylon")
    t = Canvas(64, 64)
    t.glow(32, 32, 12, JADE_L, 140)
    flower(t, 32, 32, 10, petals=5, petal=JADE_L, center=WHITE)
    t.save(BLOCK_DIR, "lifebloom-pylon-top")


def turret_head(name, px, barrels, body_w, body_h, accent=JADE, tube=None):
    """Turrets are drawn pointing up; `barrels` is a list of (x, length, width)."""
    c = Canvas(px, px)
    cx = px / 2
    for bx, ln, bw in barrels:
        c.part(rrect(bx - bw / 2, px * 0.5 - ln, bx + bw / 2, px * 0.5, 1.5), HULL_L, light=lerp(HULL_L, WHITE, 0.35), dark=HULL_D)
        c.part([(bx - bw / 2 - 1, px * 0.5 - ln), (bx, px * 0.5 - ln - bw * 0.9), (bx + bw / 2 + 1, px * 0.5 - ln)], accent, light=JADE_L, dark=JADE_D)
    c.part(mirror([(cx + body_w * 0.28, px * 0.5 - body_h * 0.5), (cx + body_w / 2, px * 0.5 - body_h * 0.2),
                   (cx + body_w / 2, px * 0.5 + body_h * 0.25), (cx + body_w * 0.3, px * 0.5 + body_h * 0.5)], cx), HULL, light=HULL_L, dark=HULL_D)
    c.part(mirror([(cx + body_w * 0.18, px * 0.5 - body_h * 0.3), (cx + body_w * 0.32, px * 0.5 + body_h * 0.05),
                   (cx + body_w * 0.2, px * 0.5 + body_h * 0.32)], cx), JADE_D, light=JADE, dark=HULL_D)
    c.circle(cx, px * 0.5, body_w * 0.17, POLLEN, light=WHITE, rim=0.75)
    c.outline()
    c.save(BLOCK_DIR, name)


def blk_turrets():
    turret_head("thorn-spitter", 64, [(24, 22, 5), (32, 26, 6), (40, 22, 5)], 34, 34)
    turret_head("briar-lance", 96, [(48, 44, 14)], 52, 52)
    turret_head("canopy-mortar", 128, [(52, 50, 14), (76, 50, 14)], 74, 70)


def blk_catapult():
    """Spore Catapult head (points up): a wide basket cradling a glowing spore pod."""
    px = 96
    c = Canvas(px, px)
    cx = px / 2
    c.part(mirror([(cx + 16, 52), (cx + 30, 60), (cx + 32, 80), (cx + 18, 90)], cx), HULL, light=HULL_L, dark=HULL_D)
    for sgn in (-1, 1):
        c.part(rrect(cx + sgn * 21 - 4, 16, cx + sgn * 21 + 4, 62, 2), HULL_L, light=lerp(HULL_L, WHITE, 0.35), dark=HULL_D)
        leaf(c, cx + sgn * 21, 20, 22, 9, sgn * 12, base=JADE_D, light=JADE, dark=HULL_D)
    c.part(rrect(cx - 15, 36, cx + 15, 70, 6), HULL_D, rim=0.6)
    c.glow(cx, 34, 17, INFECT, 150)
    c.circle(cx, 34, 11, INFECT_D, light=INFECT, dark=HULL_D)
    for a in range(0, 360, 60):
        leaf(c, cx, 34, 12, 5, a, base=INFECT_D, light=INFECT, dark=HULL_D)
    c.circle(cx, 34, 4.5, INFECT, light=WHITE, dark=INFECT_D, rim=0.8)
    c.circle(cx, 66, 5, POLLEN, light=WHITE, rim=0.75)
    c.outline()
    c.save(BLOCK_DIR, "spore-catapult")


def blk_clinic():
    """Verdant Clinic: a plate (-base) with a rotating bloom emitter head (points up)."""
    base = Canvas(64, 64)
    plate(base, 64)
    base.circle(32, 32, 13, HULL_D, rim=0.6)
    base.outline()
    base.save(BLOCK_DIR, "verdant-clinic-base")

    head = Canvas(64, 64)
    head.part(rrect(28, 8, 36, 34, 2.5), HULL_L, light=lerp(HULL_L, WHITE, 0.35), dark=HULL_D)
    head.part([(26, 12), (32, 2), (38, 12)], JADE, light=JADE_L, dark=JADE_D)
    head.circle(32, 34, 10, JADE_D, light=JADE, dark=HULL_D)
    for a in range(0, 360, 72):
        leaf(head, 32, 34, 10, 5, a + 36, base=JADE, light=JADE_L, dark=JADE_D)
    head.glow(32, 34, 7, JADE_L, 140)
    head.circle(32, 34, 3.2, WHITE, light=WHITE, dark=JADE_L, rim=0.9, shine=1.0)
    head.outline()
    head.save(BLOCK_DIR, "verdant-clinic")


def blk_unit_blocks():
    for name, px in (("jade-nursery", 96), ("verdant-cultivator", 96), ("grove-cultivator", 160)):
        c = Canvas(px, px)
        plate(c, px, radius=8)
        c.part(rrect(px * 0.16, px * 0.16, px * 0.84, px * 0.84, 5), HULL_D, rim=0.6)
        cx = px / 2
        n = 3 if px == 96 else 5
        for i in range(n):
            a = i * 360 / n
            leaf(c, cx, cx, px * 0.25, px * 0.11, a, base=JADE_D, light=JADE, dark=HULL_D)
        flower(c, cx, cx, px * 0.11, petals=6, petal=JADE_L, center=POLLEN)
        c.outline()
        c.save(BLOCK_DIR, name)

        top = Canvas(px, px)
        top.part(rrect(3, px * 0.2, 9, px * 0.8, 2), HULL_L, light=lerp(HULL_L, WHITE, 0.3))
        top.part(rrect(px - 9, px * 0.2, px - 3, px * 0.8, 2), HULL_L, light=lerp(HULL_L, WHITE, 0.3))
        top.line([(px * 0.2, 6), (px * 0.8, 6)], JADE, 2)
        top.line([(px * 0.2, px - 6), (px * 0.8, px - 6)], JADE, 2)
        top.outline()
        top.save(BLOCK_DIR, name + "-top")

        # Payload openings: input on the left edge, output on the right edge (block faces right at rotation 0).
        for suffix, side in (("-in", -1), ("-out", 1)):
            o = Canvas(px, px)
            x_edge = 2 if side < 0 else px - 2
            dx = 1 if side < 0 else -1
            half = px * 0.22
            o.part([(x_edge, px / 2 - half), (x_edge + dx * 9, px / 2 - half + 4), (x_edge + dx * 9, px / 2 + half - 4), (x_edge, px / 2 + half)],
                   HULL_D, light=HULL, dark=HULL_D, rim=0.6)
            for k in range(3):
                tip = x_edge + dx * (3 + k * 2.2)
                o.line([(tip - dx * 3, px / 2 - 5), (tip, px / 2), (tip - dx * 3, px / 2 + 5)], POLLEN if side > 0 else JADE_L, 1.4)
            o.save(BLOCK_DIR, name + suffix)


# ---------------------------------------------------------------------------------------------
# Units (drawn pointing up; the game rotates them and draws the outline)
# ---------------------------------------------------------------------------------------------
def unit_mech(name, px, scale):
    s = scale
    c = Canvas(px, px)
    cx = px / 2
    cy = px / 2
    hull = mirror([(cx + 4 * s, cy - 13 * s), (cx + 9 * s, cy - 6 * s), (cx + 10 * s, cy + 6 * s), (cx + 6 * s, cy + 13 * s)], cx)
    c.part(hull, JADE_D, light=JADE, dark=HULL_D)
    c.part(mirror([(cx + 3 * s, cy - 10 * s), (cx + 6.5 * s, cy - 3 * s), (cx + 6.5 * s, cy + 6 * s), (cx + 4 * s, cy + 10 * s)], cx), JADE, light=JADE_L, dark=JADE_D)
    leaf(c, cx, cy + 5 * s, 9 * s, 6 * s, 180, base=JADE_D, light=JADE, dark=HULL_D)
    c.glow(cx, cy - 1 * s, 5 * s, POLLEN, 130)
    c.circle(cx, cy - 1 * s, 2.6 * s, POLLEN, light=WHITE, rim=0.8)
    c.save(UNIT_DIR, name)

    base = Canvas(px, px)
    base.part(ellipse(cx, cy, 8 * s, 6 * s, 24), HULL, light=HULL_L, dark=HULL_D)
    base.save(UNIT_DIR, name + "-base")

    leg = Canvas(px, px)
    leg.part(rrect(cx - 3 * s, cy - 2 * s, cx + 3 * s, cy + 12 * s, 2 * s), HULL, light=HULL_L, dark=HULL_D)
    leg.part(ellipse(cx, cy + 12 * s, 4 * s, 3 * s, 20), JADE_D, light=JADE, dark=HULL_D)
    leg.save(UNIT_DIR, name + "-leg")


def unit_sporeling():
    """A small spore bulb on a stalk: round body, spikes, glowing infectious core."""
    px = 24
    c = Canvas(px, px)
    cx = cy = px / 2
    for a in range(0, 360, 60):
        leaf(c, cx, cy, 11, 4.5, a + 30, base=INFECT_D, light=INFECT, dark=HULL_D)
    c.part(ellipse(cx, cy, 6.5, 7.5, 28), JADE_D, light=JADE, dark=HULL_D)
    c.glow(cx, cy, 5, INFECT, 150)
    c.circle(cx, cy, 3.2, INFECT, light=WHITE, dark=INFECT_D, rim=0.8)
    c.save(UNIT_DIR, "sporeling")

    base = Canvas(px, px)
    base.part(ellipse(cx, cy, 5, 4, 20), HULL, light=HULL_L, dark=HULL_D)
    base.save(UNIT_DIR, "sporeling-base")

    leg = Canvas(px, px)
    leg.part(rrect(cx - 2, cy - 1, cx + 2, cy + 8, 1.5), HULL, light=HULL_L, dark=HULL_D)
    leg.save(UNIT_DIR, "sporeling-leg")


def unit_gardener():
    """Flying builder: a round body with four leaf rotors and a glowing build tip."""
    w = h = 44
    c = Canvas(w, h)
    cx = cy = w / 2
    for a in (45, 135, 225, 315):
        leaf(c, cx, cy, 17, 8, a, base=JADE_D, light=JADE, dark=HULL_D)
    c.part(ellipse(cx, cy, 8, 10, 30), HULL, light=HULL_L, dark=HULL_D)
    c.part(mirror([(cx + 3, cy - 12), (cx + 6, cy - 5), (cx + 5, cy + 3)], cx), JADE, light=JADE_L, dark=JADE_D)
    c.glow(cx, cy - 12, 5, POLLEN, 150)
    c.circle(cx, cy - 12, 2.6, POLLEN, light=WHITE, rim=0.8)
    c.circle(cx, cy + 2, 3, JADE_L, light=WHITE, dark=JADE, rim=0.8)
    c.save(UNIT_DIR, "gardener")


def unit_herbalist():
    """Flying healer: a soft round body with two big petal wings and a glowing heart."""
    w, h = 44, 46
    c = Canvas(w, h)
    cx = w / 2
    for sgn in (-1, 1):
        leaf(c, cx + sgn * 5, 26, 20, 13, sgn * 75, base=JADE_L, light=WHITE, dark=JADE)
        leaf(c, cx + sgn * 4, 30, 14, 8, sgn * 125, base=JADE, light=JADE_L, dark=JADE_D)
    c.part(ellipse(cx, 24, 7, 11, 30), HULL, light=HULL_L, dark=HULL_D)
    c.glow(cx, 24, 7, RESIN, 120)
    c.part([(cx - 3, 22), (cx - 1, 20), (cx, 21), (cx + 1, 20), (cx + 3, 22), (cx, 28)], (230, 90, 100), light=(255, 170, 170), dark=(150, 40, 60))
    c.save(UNIT_DIR, "herbalist")


def unit_air(name, w, h, wing, accent=POLLEN):
    c = Canvas(w, h)
    cx = w / 2
    # wings as petals swept back
    for sgn in (-1, 1):
        leaf(c, cx + sgn * w * 0.08, h * 0.45, wing, wing * 0.5, sgn * 62 + 180 * 0, base=JADE_D, light=JADE, dark=HULL_D)
    c.part(mirror([(cx + w * 0.06, h * 0.08), (cx + w * 0.16, h * 0.32), (cx + w * 0.17, h * 0.62), (cx + w * 0.08, h * 0.9)], cx), JADE, light=JADE_L, dark=JADE_D)
    c.part(mirror([(cx + w * 0.03, h * 0.18), (cx + w * 0.08, h * 0.4), (cx + w * 0.06, h * 0.7)], cx), HULL, light=HULL_L, dark=HULL_D)
    c.glow(cx, h * 0.5, w * 0.12, accent, 140)
    c.circle(cx, h * 0.5, w * 0.06, accent, light=WHITE, rim=0.8)
    c.save(UNIT_DIR, name)


def weapon(name, w, h, tip=JADE, wide=None):
    c = Canvas(w, h)
    cx = w / 2
    ww = wide or w * 0.34
    c.part(rrect(cx - ww / 2, h * 0.22, cx + ww / 2, h * 0.92, 1.5), HULL, light=HULL_L, dark=HULL_D)
    c.part([(cx - ww / 2 - 0.5, h * 0.24), (cx, 0.5), (cx + ww / 2 + 0.5, h * 0.24)], tip, light=JADE_L, dark=JADE_D)
    c.circle(cx, h * 0.62, ww * 0.28, POLLEN, light=WHITE, rim=0.8)
    c.save(UNIT_DIR, name)


def units():
    unit_mech("sprig", 32, 1.0)
    unit_mech("thornback", 48, 1.5)
    unit_mech("briarlord", 80, 2.5)
    unit_sporeling()
    unit_gardener()
    unit_herbalist()
    unit_air("wisp", 32, 32, 12)
    unit_air("luminar", 56, 60, 24)
    unit_air("verdance", 96, 108, 44)
    weapon("sprig-needler", 10, 20)
    weapon("thornback-thorn", 14, 28)
    weapon("briarlord-mortar", 28, 40, wide=14)
    weapon("luminar-pod", 12, 22)
    weapon("verdance-launcher", 18, 34, wide=9)


def preview(path):
    items = sorted(generated)
    cell = 176
    cols = 6
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (46, 50, 56, 255))
    d = ImageDraw.Draw(sheet)
    for i, p in enumerate(items):
        im = Image.open(p).convert("RGBA")
        scale = max(1, min(cell - 24, 160) // max(im.size))
        scale = min(scale, 4)
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
        if im.width > cell - 8 or im.height > cell - 20:
            r = min((cell - 8) / im.width, (cell - 20) / im.height)
            im = im.resize((int(im.width * r), int(im.height * r)), Image.LANCZOS)
        x, y = (i % cols) * cell, (i // cols) * cell
        sheet.alpha_composite(im, (x + (cell - im.width) // 2, y + 4 + (cell - 20 - im.height) // 2))
        d.text((x + 4, y + cell - 14), os.path.basename(p)[:-4], fill=(210, 220, 215, 255))
    sheet.save(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", help="also write a contact sheet of every generated sprite")
    args = ap.parse_args()

    item_resin()
    item_silk()
    item_core()
    status_icons()
    blk_resin_tapper()
    blk_silk_loom()
    blk_conservatory()
    blk_spore_furnace()
    blk_wall(1)
    blk_wall(2)
    blk_pylon()
    blk_turrets()
    blk_catapult()
    blk_clinic()
    blk_unit_blocks()
    units()
    print("wrote %d sprites" % len(generated))
    if args.preview:
        preview(args.preview)


if __name__ == "__main__":
    main()
