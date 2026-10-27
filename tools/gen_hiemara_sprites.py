#!/usr/bin/env python3
"""
Generates the placeholder sprites of the planet Hiemara into assets/sprites.

    python3 tools/gen_hiemara_sprites.py [--vanilla path/to/deep-water.png]

The art is procedural (shapes drawn at 4x and downsampled), so everything is playable without hand-drawn art.
To replace a sprite, overwrite the PNG in assets/sprites and remove the matching call at the bottom.

The rich water floors are the vanilla deep water sprite with specks of the ore on it. Get the vanilla file from
https://github.com/Anuken/Mindustry/blob/v160.7/core/assets-raw/sprites/blocks/environment/deep-water.png
"""
import argparse
import os
import random

from PIL import Image, ImageDraw

SS = 4
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets", "sprites")
BLOCK_DIR = os.path.join(ROOT, "blocks", "hiemara")
UNIT_DIR = os.path.join(ROOT, "unit", "hiemara")
ENV_DIR = os.path.join(ROOT, "blocks", "environment")

HULL_D = (38, 52, 66)
HULL = (74, 98, 120)
HULL_L = (128, 160, 186)
ICE = (181, 228, 255)
ICE_L = (225, 245, 255)
WARM = (255, 160, 80)

ORES = {
    "scrap": (150, 150, 150),
    "copper": (217, 157, 115),
    "lead": (140, 127, 169),
    "coal": (90, 90, 96),
    "titanium": (141, 161, 227),
    "beryllium": (58, 190, 120),
    "thorium": (249, 163, 199),
    "tungsten": (150, 175, 190),
}


def canvas(size):
    return Image.new("RGBA", (size * SS, size * SS), (0, 0, 0, 0))


def finish(img, size, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.resize((size, size), Image.LANCZOS).save(path)


def plate(d, size, inset=2, color=HULL, outline=HULL_D):
    """A beveled square plate that covers the block."""
    s = size * SS
    i = inset * SS
    d.rounded_rectangle([i, i, s - i - 1, s - i - 1], radius=4 * SS, fill=color, outline=outline, width=2 * SS)
    d.rounded_rectangle([i + 3 * SS, i + 3 * SS, s - i - 3 * SS - 1, s - i - 3 * SS - 1], radius=3 * SS,
                        fill=tuple(min(255, c + 14) for c in color))


def rivets(d, size, inset=5):
    s = size * SS
    for x, y in ((inset, inset), (size - inset, inset), (inset, size - inset), (size - inset, size - inset)):
        d.ellipse([x * SS - 2 * SS, y * SS - 2 * SS, x * SS + 2 * SS, y * SS + 2 * SS], fill=HULL_L, outline=HULL_D)


def rich_water(vanilla):
    base = Image.open(vanilla).convert("RGBA")
    for name, color in ORES.items():
        rng = random.Random(name)
        img = base.copy()
        px = img.load()
        for _ in range(26):
            x, y = rng.randrange(32), rng.randrange(32)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                if x + dx < 32 and y + dy < 32 and rng.random() < 0.8:
                    light = rng.uniform(0.85, 1.15)
                    px[x + dx, y + dy] = tuple(min(255, int(c * light)) for c in color) + (255,)
        img.save(os.path.join(ENV_DIR, "rich-water-%s.png" % name))


def filter_block(name, size, tier):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    plate(d, size)
    rivets(d, size)
    # a mesh with as many bars as the tier has
    m = 7 * SS
    d.rounded_rectangle([m, m, s - m, s - m], radius=3 * SS, fill=(24, 40, 62), outline=HULL_D, width=SS)
    bars = 3 + tier * 2
    for i in range(1, bars):
        x = m + (s - 2 * m) * i // bars
        d.line([x, m, x, s - m], fill=ICE, width=SS)
    for i in range(1, bars):
        y = m + (s - 2 * m) * i // bars
        d.line([m, y, s - m, y], fill=(90, 150, 190), width=SS)
    # the number of tier marks on the corner
    for t in range(tier):
        d.rectangle([(9 + t * 5) * SS, 3 * SS, (12 + t * 5) * SS, 5 * SS], fill=WARM)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def port(name, size):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    plate(d, size)
    rivets(d, size)
    c = s // 2
    d.ellipse([c - 14 * SS, c - 14 * SS, c + 14 * SS, c + 14 * SS], fill=(26, 40, 58), outline=HULL_D, width=2 * SS)
    d.ellipse([c - 9 * SS, c - 9 * SS, c + 9 * SS, c + 9 * SS], fill=ICE, outline=HULL_D, width=SS)
    # an arrow that goes out
    d.polygon([(c - 5 * SS, c + 3 * SS), (c + 1 * SS, c + 3 * SS), (c + 1 * SS, c + 6 * SS), (c + 7 * SS, c),
               (c + 1 * SS, c - 6 * SS), (c + 1 * SS, c - 3 * SS), (c - 5 * SS, c - 3 * SS)], fill=HULL_D)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def depot(name, size):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    plate(d, size)
    rivets(d, size)
    c = s // 2
    d.rounded_rectangle([c - 14 * SS, c - 14 * SS, c + 14 * SS, c + 14 * SS], radius=2 * SS, fill=(26, 40, 58),
                        outline=HULL_D, width=2 * SS)
    for dx in (-9, 9):
        for dy in (-9, 9):
            d.rectangle([c + (dx - 5) * SS, c + (dy - 5) * SS, c + (dx + 5) * SS, c + (dy + 5) * SS], fill=HULL_L,
                        outline=HULL_D, width=SS)
    # an arrow that comes in
    d.polygon([(c + 6 * SS, c + 3 * SS), (c, c + 3 * SS), (c, c + 6 * SS), (c - 6 * SS, c), (c, c - 6 * SS),
               (c, c - 3 * SS), (c + 6 * SS, c - 3 * SS)], fill=ICE)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def pad(name, size, naval):
    for suffix in ("", "-out", "-top"):
        img = canvas(size)
        d = ImageDraw.Draw(img)
        s = size * SS
        if suffix == "":
            plate(d, size, color=(60, 84, 108))
            rivets(d, size, 6)
            m = 9 * SS
            d.rounded_rectangle([m, m, s - m, s - m], radius=3 * SS, fill=(30, 46, 66), outline=HULL_D, width=2 * SS)
            if naval:
                d.rectangle([m, s // 2 - 5 * SS, s - m, s // 2 + 5 * SS], fill=(40, 100, 170))
            else:
                c = s // 2
                d.ellipse([c - 12 * SS, c - 12 * SS, c + 12 * SS, c + 12 * SS], outline=ICE, width=2 * SS)
        elif suffix == "-out":
            # the opening on the side the unit leaves through
            d.rectangle([s - 8 * SS, s // 2 - 7 * SS, s - 2 * SS, s // 2 + 7 * SS], fill=(24, 40, 62), outline=HULL_D, width=SS)
        else:
            d.polygon([(s // 2 - 5 * SS, 12 * SS), (s // 2 + 5 * SS, 12 * SS), (s // 2, 6 * SS)], fill=WARM)
        finish(img, size, os.path.join(BLOCK_DIR, name + suffix + ".png"))


def cap_block(name, size, dock):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    plate(d, size)
    rivets(d, size, 6)
    c = s // 2
    if dock:
        for i in range(3):
            y = (14 + i * 17) * SS
            d.rectangle([10 * SS, y, s - 10 * SS, y + 8 * SS], fill=HULL_L, outline=HULL_D, width=SS)
        d.polygon([(c - 6 * SS, 8 * SS), (c + 6 * SS, 8 * SS), (c, 3 * SS)], fill=WARM)
    else:
        d.ellipse([c - 28 * SS, c - 28 * SS, c + 28 * SS, c + 28 * SS], fill=(40, 58, 80), outline=HULL_D, width=2 * SS)
        d.ellipse([c - 18 * SS, c - 18 * SS, c + 18 * SS, c + 18 * SS], fill=ICE, outline=HULL_D, width=2 * SS)
        d.line([c - 18 * SS, c, c + 18 * SS, c], fill=HULL_D, width=2 * SS)
        d.line([c, c - 18 * SS, c, c + 18 * SS], fill=HULL_D, width=2 * SS)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def heater(name, size):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    plate(d, size, color=(86, 70, 66))
    rivets(d, size)
    c = s // 2
    for r, col in ((20, (120, 50, 30)), (14, (230, 110, 50)), (8, WARM)):
        d.ellipse([c - r * SS, c - r * SS, c + r * SS, c + r * SS], fill=col, outline=HULL_D, width=SS)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def generator(name, size):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    plate(d, size)
    rivets(d, size)
    c = s // 2
    d.ellipse([c - 18 * SS, c - 18 * SS, c + 18 * SS, c + 18 * SS], fill=(26, 40, 58), outline=HULL_D, width=2 * SS)
    for k in range(4):
        x, y = (1, 0) if k % 2 == 0 else (0, 1)
        d.rectangle([c - (14 if x else 3) * SS, c - (14 if y else 3) * SS, c + (14 if x else 3) * SS,
                     c + (14 if y else 3) * SS], fill=ICE)
    d.ellipse([c - 5 * SS, c - 5 * SS, c + 5 * SS, c + 5 * SS], fill=WARM, outline=HULL_D, width=SS)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def node(name, size):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    d.rounded_rectangle([6 * SS, 6 * SS, s - 6 * SS, s - 6 * SS], radius=3 * SS, fill=HULL, outline=HULL_D, width=2 * SS)
    d.ellipse([c - 6 * SS, c - 6 * SS, c + 6 * SS, c + 6 * SS], fill=ICE, outline=HULL_D, width=SS)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def air_unit(name, size, tier):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    w = (6 + tier * 2) * SS
    d.polygon([(c, 2 * SS), (c + w, s - 5 * SS), (c, s - 9 * SS), (c - w, s - 5 * SS)], fill=(150, 200, 235), outline=HULL_D)
    d.polygon([(c, 2 * SS), (c + 3 * SS, s // 2), (c - 3 * SS, s // 2)], fill=ICE_L)
    # a cargo box on the back for every tier
    for t in range(tier):
        d.rectangle([c - 3 * SS, (s // 2 + t * 3 * SS), c + 3 * SS, (s // 2 + t * 3 * SS + 2 * SS)], fill=WARM)
    finish(img, size, os.path.join(UNIT_DIR, name + ".png"))


def naval_unit(name, size, tier):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    w = (5 + tier * 2) * SS
    d.polygon([(c, 1 * SS), (c + w, 8 * SS), (c + w, s - 3 * SS), (c - w, s - 3 * SS), (c - w, 8 * SS)], fill=(74, 110, 150),
              outline=HULL_D)
    d.rectangle([c - w + 2 * SS, s // 3, c + w - 2 * SS, s - 6 * SS], fill=(40, 62, 90))
    for t in range(tier):
        d.rectangle([c - w + 3 * SS + t * 7 * SS, s // 3 + 2 * SS, c - w + 8 * SS + t * 7 * SS, s - 8 * SS], fill=WARM,
                    outline=HULL_D)
    finish(img, size, os.path.join(UNIT_DIR, name + ".png"))


def hub(name, size, tier, kind):
    """Improved ports (arrows out of a crate), distributors (a hub with spokes) and the liquid port and depot."""
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    color = (74, 98, 120) if kind != "liquid" else (60, 92, 118)
    plate(d, size, color=color)
    rivets(d, size)
    c = s // 2
    r = c - 11 * SS
    d.ellipse([c - r, c - r, c + r, c + r], fill=(26, 40, 58), outline=HULL_D, width=2 * SS)
    if kind == "improved":
        for k in range(4):
            dx, dy = ((1, 0), (0, 1), (-1, 0), (0, -1))[k]
            d.polygon([(c + dx * (r - 2 * SS), c + dy * (r - 2 * SS)),
                       (c + dx * (r - 9 * SS) - dy * 4 * SS, c + dy * (r - 9 * SS) + dx * 4 * SS),
                       (c + dx * (r - 9 * SS) + dy * 4 * SS, c + dy * (r - 9 * SS) - dx * 4 * SS)], fill=ICE)
        d.ellipse([c - 5 * SS, c - 5 * SS, c + 5 * SS, c + 5 * SS], fill=ICE_L, outline=HULL_D, width=SS)
    elif kind == "distributor":
        for k in range(8):
            ang = k * 3.14159 / 4
            import math
            d.line([c, c, c + math.cos(ang) * (r - 2 * SS), c + math.sin(ang) * (r - 2 * SS)], fill=WARM, width=2 * SS)
        d.ellipse([c - 6 * SS, c - 6 * SS, c + 6 * SS, c + 6 * SS], fill=WARM, outline=HULL_D, width=SS)
    else:
        d.ellipse([c - r + 4 * SS, c - r + 4 * SS, c + r - 4 * SS, c + r - 4 * SS], fill=(40, 120, 190))
        d.polygon([(c, c - 8 * SS), (c + 6 * SS, c + 2 * SS), (c - 6 * SS, c + 2 * SS)], fill=ICE_L)
        d.ellipse([c - 6 * SS, c - 1 * SS, c + 6 * SS, c + 8 * SS], fill=ICE_L)
    for t in range(tier):
        d.rectangle([(8 + t * 5) * SS, 3 * SS, (11 + t * 5) * SS, 5 * SS], fill=WARM)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def crafter(name, size, tint, motif):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    plate(d, size, color=tint)
    rivets(d, size)
    c = s // 2
    r = c - 11 * SS
    d.rounded_rectangle([c - r, c - r, c + r, c + r], radius=3 * SS, fill=(26, 40, 58), outline=HULL_D, width=2 * SS)
    if motif == "spore":
        for dx, dy in ((-5, -4), (4, -5), (0, 4), (6, 5), (-6, 6)):
            d.ellipse([c + (dx - 4) * SS, c + (dy - 4) * SS, c + (dx + 4) * SS, c + (dy + 4) * SS], fill=(150, 110, 200), outline=HULL_D)
    elif motif == "oil":
        d.ellipse([c - r + 3 * SS, c - r + 3 * SS, c + r - 3 * SS, c + r - 3 * SS], fill=(30, 26, 28), outline=HULL_D, width=SS)
        d.ellipse([c - 4 * SS, c - 6 * SS, c + 4 * SS, c + 2 * SS], fill=(90, 80, 90))
    elif motif == "coal":
        for dx, dy in ((-5, -3), (3, -5), (-1, 4), (6, 3)):
            d.polygon([(c + (dx - 4) * SS, c + dy * SS), (c + dx * SS, c + (dy - 4) * SS), (c + (dx + 4) * SS, c + dy * SS),
                       (c + dx * SS, c + (dy + 4) * SS)], fill=(60, 60, 66), outline=HULL_D)
    elif motif == "pyratite":
        d.ellipse([c - 7 * SS, c - 7 * SS, c + 7 * SS, c + 7 * SS], fill=(230, 120, 40), outline=HULL_D, width=SS)
        d.ellipse([c - 3 * SS, c - 3 * SS, c + 3 * SS, c + 3 * SS], fill=(255, 220, 120))
    elif motif == "silicon":
        d.polygon([(c, c - 8 * SS), (c + 8 * SS, c), (c, c + 8 * SS), (c - 8 * SS, c)], fill=(150, 160, 180), outline=HULL_D)
        d.line([c - 8 * SS, c, c + 8 * SS, c], fill=ICE_L, width=SS)
    elif motif == "sand":
        d.rectangle([c - r + 3 * SS, c, c + r - 3 * SS, c + r - 3 * SS], fill=(230, 195, 150))
        d.rectangle([c - r + 3 * SS, c - 4 * SS, c + r - 3 * SS, c], fill=(40, 120, 190))
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def ship(name, size, tier, tank=False, big=False):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    w = int(s * (0.2 if not big else 0.18))
    d.polygon([(c, 2 * SS), (c + w, s // 6), (c + w, s - 3 * SS), (c - w, s - 3 * SS), (c - w, s // 6)],
              fill=(74, 110, 150), outline=HULL_D)
    d.rectangle([c - w + 2 * SS, s // 4, c + w - 2 * SS, s - 7 * SS], fill=(40, 62, 90))
    if tank:
        n = tier + 1
        for t in range(n):
            y = s // 4 + 2 * SS + t * ((s - 7 * SS - s // 4 - 4 * SS) // n)
            h = (s - 7 * SS - s // 4 - 4 * SS) // n - 2 * SS
            d.ellipse([c - w + 3 * SS, y, c + w - 3 * SS, y + h], fill=(60, 150, 210), outline=HULL_D)
    else:
        n = 2 + tier
        for t in range(n):
            x = c - w + 3 * SS + (t % 3) * ((2 * w - 6 * SS) // 3)
            y = s // 4 + 2 * SS + (t // 3) * 9 * SS
            d.rectangle([x, y, x + (2 * w - 6 * SS) // 3 - 2 * SS, y + 7 * SS], fill=WARM, outline=HULL_D)
    d.rectangle([c - 3 * SS, s // 6, c + 3 * SS, s // 6 + 4 * SS], fill=ICE_L)
    finish(img, size, os.path.join(UNIT_DIR, name + ".png"))


def power(name, size, motif):
    """Generators, batteries, nodes and the reactor."""
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    if motif == "node":
        d.rounded_rectangle([4 * SS, 4 * SS, s - 4 * SS, s - 4 * SS], radius=3 * SS, fill=HULL, outline=HULL_D, width=2 * SS)
        d.ellipse([c - 7 * SS, c - 7 * SS, c + 7 * SS, c + 7 * SS], fill=ICE, outline=HULL_D, width=SS)
        d.line([c, c - 12 * SS, c, c + 12 * SS], fill=WARM, width=2 * SS)
        finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))
        return
    plate(d, size, color=(70, 90, 112) if motif != "reactor" else (60, 70, 96))
    rivets(d, size)
    r = c - 10 * SS
    if motif == "wind":
        import math
        for k in range(3):
            a = k * 2.0944
            d.polygon([(c, c), (c + math.cos(a) * r, c + math.sin(a) * r),
                       (c + math.cos(a + 0.35) * r * 0.8, c + math.sin(a + 0.35) * r * 0.8)], fill=ICE_L, outline=HULL_D)
        d.ellipse([c - 3 * SS, c - 3 * SS, c + 3 * SS, c + 3 * SS], fill=WARM, outline=HULL_D)
    elif motif == "hydro":
        d.rounded_rectangle([c - r, c - r, c + r, c + r], radius=3 * SS, fill=(26, 60, 100), outline=HULL_D, width=2 * SS)
        for k in range(4):
            y = c - r + (k + 1) * (2 * r) // 5
            d.line([c - r + 2 * SS, y, c + r - 2 * SS, y], fill=(110, 170, 230), width=2 * SS)
        d.ellipse([c - 5 * SS, c - 5 * SS, c + 5 * SS, c + 5 * SS], fill=WARM, outline=HULL_D)
    elif motif == "solar":
        d.rounded_rectangle([c - r, c - r, c + r, c + r], radius=2 * SS, fill=(24, 40, 86), outline=HULL_D, width=SS)
        for k in range(1, 4):
            x = c - r + k * (2 * r) // 4
            d.line([x, c - r, x, c + r], fill=(80, 110, 190), width=SS)
            d.line([c - r, x, c + r, x], fill=(80, 110, 190), width=SS)
    elif motif == "battery":
        d.rounded_rectangle([c - r, c - r, c + r, c + r], radius=2 * SS, fill=(34, 52, 70), outline=HULL_D, width=SS)
        d.rectangle([c - r // 2, c - r // 2, c + r // 2, c + r // 2], fill=WARM)
    elif motif == "tower":
        d.polygon([(c, c - r), (c + r, c + r), (c - r, c + r)], fill=HULL_L, outline=HULL_D)
        d.ellipse([c - 4 * SS, c - r - 2 * SS, c + 4 * SS, c - r + 6 * SS], fill=WARM, outline=HULL_D)
    elif motif == "reactor":
        d.ellipse([c - r, c - r, c + r, c + r], fill=(20, 26, 44), outline=HULL_D, width=3 * SS)
        d.ellipse([c - r + 6 * SS, c - r + 6 * SS, c + r - 6 * SS, c + r - 6 * SS], fill=(40, 70, 130), outline=ICE, width=2 * SS)
        d.ellipse([c - r // 3, c - r // 3, c + r // 3, c + r // 3], fill=ICE_L)
    elif motif == "separator":
        d.rounded_rectangle([c - r, c - r, c + r, c + r], radius=3 * SS, fill=(30, 46, 66), outline=HULL_D, width=2 * SS)
        d.ellipse([c - r + 3 * SS, c - r + 3 * SS, c, c + r - 3 * SS], fill=(90, 150, 220))
        d.ellipse([c, c - r + 3 * SS, c + r - 3 * SS, c + r - 3 * SS], fill=(230, 210, 120))
    elif motif == "pump":
        d.ellipse([c - r, c - r, c + r, c + r], fill=(26, 60, 100), outline=HULL_D, width=2 * SS)
        d.ellipse([c - r // 2, c - r // 2, c + r // 2, c + r // 2], fill=ICE, outline=HULL_D, width=SS)
    elif motif == "tank":
        d.ellipse([c - r, c - r, c + r, c + r], fill=(50, 120, 190), outline=HULL_D, width=2 * SS)
        d.ellipse([c - r // 2, c - r // 2, c + r // 2, c + r // 2], fill=(110, 180, 235))
    elif motif == "harvester":
        d.rounded_rectangle([c - r, c - r, c + r, c + r], radius=3 * SS, fill=(30, 46, 66), outline=HULL_D, width=2 * SS)
        d.polygon([(c, c - r + 4 * SS), (c + r - 4 * SS, c), (c, c + r - 4 * SS), (c - r + 4 * SS, c)], fill=ICE, outline=HULL_D)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--vanilla", help="path to the vanilla deep-water.png")
    args = parser.parse_args()

    if args.vanilla:
        rich_water(args.vanilla)

    filter_block("water-filter-1", 64, 1)
    filter_block("water-filter-2", 96, 2)
    filter_block("water-filter-3", 96, 3)
    filter_block("water-filter-4", 128, 4)
    port("cargo-port", 64)
    depot("cargo-depot", 64)
    pad("cargo-air-pad", 96, False)
    pad("cargo-boat-yard", 96, True)
    cap_block("cargo-hangar", 96, False)
    cap_block("cargo-dock", 96, True)
    heater("floating-heater", 64)
    generator("floating-generator", 64)
    node("floating-node", 32)

    hub("improved-port-1", 96, 1, "improved")
    hub("improved-port-2", 96, 2, "improved")
    hub("improved-port-3", 128, 3, "improved")
    hub("distributor-1", 64, 1, "distributor")
    hub("distributor-2", 96, 2, "distributor")
    hub("distributor-3", 128, 3, "distributor")
    hub("liquid-port", 64, 1, "liquid")
    hub("liquid-depot", 64, 2, "liquid")
    crafter("sand-dredger", 96, (110, 100, 80), "sand")
    crafter("floating-cultivator", 64, (70, 96, 84), "spore")
    crafter("floating-spore-press", 64, (84, 80, 100), "oil")
    crafter("floating-coal-centrifuge", 64, (74, 80, 90), "coal")
    crafter("floating-pyratite-mixer", 64, (100, 78, 70), "pyratite")
    crafter("floating-silicon-smelter", 64, (80, 86, 98), "silicon")
    crafter("floating-silicon-crucible", 96, (80, 86, 98), "silicon")

    power("wind-turbine", 64, "wind")
    power("hydro-generator", 96, "hydro")
    power("floating-solar-panel", 96, "solar")
    power("floating-battery-1", 32, "battery")
    power("floating-battery-2", 64, "battery")
    power("floating-battery-3", 96, "battery")
    power("floating-node-large", 64, "node")
    power("floating-surge-tower", 64, "tower")
    power("electric-separator", 96, "separator")
    power("fusion-reactor", 160, "reactor")
    power("floating-pump", 32, "pump")
    power("floating-rotary-pump", 64, "pump")
    power("floating-liquid-container", 64, "tank")
    power("floating-liquid-tank", 96, "tank")
    power("floating-ice-harvester", 96, "harvester")

    ship("cargo-galleon", 64, 4, big=True)
    ship("cargo-leviathan", 88, 5, big=True)
    ship("cargo-titan", 120, 6, big=True)
    ship("tanker-cutter", 32, 1, tank=True)
    ship("tanker-bulk", 48, 2, tank=True)
    ship("tanker-super", 72, 3, tank=True)

    for i, name in enumerate(("cargo-drone", "cargo-hauler", "cargo-freighter"), start=1):
        air_unit(name, 24 + i * 8, i)
    for i, name in enumerate(("cargo-skiff", "cargo-barge", "cargo-carrier"), start=1):
        naval_unit(name, 24 + i * 8, i)


if __name__ == "__main__":
    main()
