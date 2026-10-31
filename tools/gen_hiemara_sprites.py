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


# Tint of the water and light colour of the specks, one per ore. Not the item colours: lead, scrap and tungsten are all grey.
RICH = {
    "scrap": ((170, 150, 120), (235, 150, 70)),
    "copper": ((225, 120, 55), (255, 190, 110)),
    "lead": ((125, 95, 205), (200, 175, 255)),
    "coal": ((38, 38, 46), (120, 120, 135)),
    "titanium": ((60, 130, 255), (170, 220, 255)),
    "beryllium": ((35, 200, 115), (170, 255, 195)),
    "thorium": ((235, 70, 150), (255, 175, 215)),
    "tungsten": ((70, 190, 190), (200, 245, 240)),
}


def rich_shape(name, d, x, y, light, dark, rng):
    """One speck of the ore, each ore with a shape of its own. Drawn on a 4x canvas."""
    k = SS
    if name == "copper":      # round nuggets
        r = rng.choice((2, 3))
        d.ellipse([(x - r) * k, (y - r) * k, (x + r) * k, (y + r) * k], fill=light, outline=dark, width=k // 2)
    elif name == "lead":      # cubes
        r = rng.choice((2, 3))
        d.rectangle([(x - r) * k, (y - r) * k, (x + r) * k, (y + r) * k], fill=light, outline=dark, width=k // 2)
        d.rectangle([(x - r) * k, (y - r) * k, (x) * k, (y) * k], fill=tuple(min(255, c + 25) for c in light))
    elif name == "scrap":     # bars at an angle
        n = rng.choice((3, 4))
        d.line([(x - n) * k, (y + n) * k, (x + n) * k, (y - n) * k], fill=dark, width=3 * k)
        d.line([(x - n) * k, (y + n) * k, (x + n) * k, (y - n) * k], fill=light, width=2 * k)
    elif name == "coal":      # chunky blocks
        r = rng.choice((2, 3))
        d.polygon([((x - r) * k, (y - r + 1) * k), ((x - 1) * k, (y - r - 1) * k), ((x + r) * k, (y - 1) * k),
                   ((x + r - 1) * k, (y + r) * k), ((x - r + 1) * k, (y + r) * k)], fill=light, outline=dark)
    elif name == "titanium":  # diamonds
        r = rng.choice((3, 4))
        d.polygon([(x * k, (y - r) * k), ((x + r) * k, y * k), (x * k, (y + r) * k), ((x - r) * k, y * k)],
                  fill=light, outline=dark)
    elif name == "beryllium":  # thin crystal needles
        n = rng.choice((3, 4))
        d.line([x * k, (y + n) * k, x * k, (y - n) * k], fill=dark, width=2 * k)
        d.line([x * k, (y + n) * k, x * k, (y - n) * k], fill=light, width=k)
        d.line([(x - 2) * k, (y + n - 1) * k, (x - 1) * k, (y - 1) * k], fill=light, width=k)
    elif name == "thorium":   # glowing dots with a halo
        d.ellipse([(x - 3) * k, (y - 3) * k, (x + 3) * k, (y + 3) * k], fill=dark)
        d.ellipse([(x - 1) * k, (y - 1) * k, (x + 1) * k, (y + 1) * k], fill=light)
    elif name == "tungsten":  # small hexagons
        r = 3
        d.polygon([((x - r) * k, y * k), ((x - 1) * k, (y - r) * k), ((x + 1) * k, (y - r) * k), ((x + r) * k, y * k),
                   ((x + 1) * k, (y + r) * k), ((x - 1) * k, (y + r) * k)], fill=light, outline=dark)


def rich_water(vanilla):
    base = Image.open(vanilla).convert("RGBA")
    for name, (tint, light) in RICH.items():
        rng = random.Random(name)
        # the vanilla water pulled towards the tint, the wave detail stays
        img = base.copy()
        px = img.load()
        for x in range(32):
            for y in range(32):
                r, g, b, a = px[x, y]
                px[x, y] = tuple(int(c * 0.45 + t * 0.55 * (0.7 + 0.3 * c / 255.0)) for c, t in zip((r, g, b), tint)) + (a,)
        big = img.resize((32 * SS, 32 * SS), Image.NEAREST)
        d = ImageDraw.Draw(big)
        dark = tuple(int(c * 0.35) for c in tint)
        placed = []
        for _ in range(60):
            if len(placed) >= 9:
                break
            x, y = rng.randrange(4, 28), rng.randrange(4, 28)
            if any((x - px_) ** 2 + (y - py_) ** 2 < 36 for px_, py_ in placed):
                continue
            placed.append((x, y))
            rich_shape(name, d, x, y, light, dark, rng)
        big.resize((32, 32), Image.LANCZOS).save(os.path.join(ENV_DIR, "rich-water-%s.png" % name))


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


# Core colour of the factories that make a base game resource, by the resource.
FACTORY_COLORS = {
    "graphite": (178, 198, 210), "metaglass": (235, 238, 245), "plastanium": (203, 217, 127), "phase": (244, 186, 110),
    "surge": (243, 233, 121), "blast": (255, 121, 94), "oxide": (228, 255, 214), "carbide": (137, 118, 154),
    "slag": (255, 161, 102), "cryo": (110, 205, 236), "nitrogen": (239, 227, 255), "arkycite": (132, 169, 75),
    "cyanogen": (137, 232, 182), "neoplasm": (223, 80, 100),
}


def factory_sprites():
    for name, size, motif in (("floating-graphite-press", 64, "graphite"), ("floating-kiln", 64, "metaglass"),
                              ("floating-plastanium-compressor", 64, "plastanium"), ("floating-phase-weaver", 64, "phase"),
                              ("floating-surge-smelter", 96, "surge"), ("floating-blast-mixer", 64, "blast"),
                              ("floating-oxidation-chamber", 96, "oxide"), ("floating-carbide-crucible", 96, "carbide"),
                              ("floating-melter", 64, "slag"), ("floating-cryofluid-mixer", 64, "cryo"),
                              ("floating-air-condenser", 96, "nitrogen"), ("floating-arkycite-fermenter", 64, "arkycite"),
                              ("floating-cyanogen-synthesizer", 96, "cyanogen"), ("floating-neoplasm-culture", 96, "neoplasm")):
        crafter(name, size, tuple(int(v * 0.45 + 60) for v in FACTORY_COLORS[motif]), motif)


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
    elif motif in FACTORY_COLORS:
        col = FACTORY_COLORS[motif]
        d.ellipse([c - r + 3 * SS, c - r + 3 * SS, c + r - 3 * SS, c + r - 3 * SS], fill=tuple(int(v * 0.35) for v in col), outline=HULL_D, width=SS)
        d.polygon([(c, c - 8 * SS), (c + 6 * SS, c + 2 * SS), (c + 3 * SS, c + 8 * SS), (c - 3 * SS, c + 8 * SS), (c - 6 * SS, c + 2 * SS)], fill=col, outline=HULL_D)
        d.ellipse([c - 2 * SS, c - 2 * SS, c + 2 * SS, c + 2 * SS], fill=ICE_L)
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


def turret(name, size, kind):
    """Turret heads. They point up, the game turns them."""
    import math
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    r = int(s * 0.36)
    d.ellipse([c - r, c - r, c + r, c + r], fill=HULL, outline=HULL_D, width=2 * SS)
    d.ellipse([c - r + 3 * SS, c - r + 3 * SS, c + r - 3 * SS, c + r - 3 * SS], fill=(86, 112, 136))
    if kind == "arc":
        for dx in (-1, 1):
            d.line([c + dx * 4 * SS, c, c + dx * 4 * SS, 3 * SS], fill=(150, 215, 255), width=2 * SS)
            d.ellipse([c + dx * 4 * SS - 2 * SS, 2 * SS, c + dx * 4 * SS + 2 * SS, 6 * SS], fill=ICE_L, outline=HULL_D)
        d.ellipse([c - 3 * SS, c - 3 * SS, c + 3 * SS, c + 3 * SS], fill=(120, 200, 255), outline=HULL_D)
    elif kind == "lance":
        d.rounded_rectangle([c - 3 * SS, 2 * SS, c + 3 * SS, c], radius=SS, fill=HULL_L, outline=HULL_D, width=SS)
        d.polygon([(c - 3 * SS, 2 * SS), (c, 0), (c + 3 * SS, 2 * SS)], fill=(150, 215, 255), outline=HULL_D)
        d.ellipse([c - 5 * SS, c - 5 * SS, c + 5 * SS, c + 5 * SS], fill=(80, 180, 255), outline=HULL_D, width=SS)
    elif kind == "cannon":
        d.polygon([(c - 4 * SS, c), (c - 6 * SS, 3 * SS), (c + 6 * SS, 3 * SS), (c + 4 * SS, c)], fill=(60, 130, 190), outline=HULL_D)
        d.rectangle([c - 3 * SS, 3 * SS, c + 3 * SS, 7 * SS], fill=(24, 40, 62))
        d.ellipse([c - 4 * SS, c - 4 * SS, c + 4 * SS, c + 4 * SS], fill=(90, 190, 240), outline=HULL_D, width=SS)
    else:  # mortar
        d.rounded_rectangle([c - 6 * SS, 2 * SS, c + 6 * SS, c + 2 * SS], radius=2 * SS, fill=(70, 96, 120), outline=HULL_D, width=SS)
        d.ellipse([c - 4 * SS, 3 * SS, c + 4 * SS, 9 * SS], fill=(20, 30, 44), outline=HULL_D)
        d.ellipse([c - 5 * SS, c - 3 * SS, c + 5 * SS, c + 7 * SS], fill=(110, 180, 230), outline=HULL_D, width=SS)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def vortex_gen(name, size):
    import math
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    plate(d, size, color=(60, 86, 112))
    rivets(d, size)
    r = c - 9 * SS
    d.ellipse([c - r, c - r, c + r, c + r], fill=(18, 44, 78), outline=HULL_D, width=2 * SS)
    pts = []
    for i in range(70):
        a = i * 0.28
        rr = r * (0.1 + 0.88 * i / 70.0)
        pts.append((c + math.cos(a) * rr, c + math.sin(a) * rr))
    d.line(pts, fill=(130, 215, 255), width=2 * SS)
    d.ellipse([c - 3 * SS, c - 3 * SS, c + 3 * SS, c + 3 * SS], fill=ICE_L, outline=HULL_D)
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def mine(name, size):
    import math
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    r = int(s * 0.26)
    for k in range(8):
        a = k * math.pi / 4
        d.line([c + math.cos(a) * r, c + math.sin(a) * r, c + math.cos(a) * (r + 5 * SS), c + math.sin(a) * (r + 5 * SS)],
               fill=HULL_D, width=3 * SS)
    d.ellipse([c - r, c - r, c + r, c + r], fill=(52, 62, 76), outline=HULL_D, width=2 * SS)
    d.ellipse([c - 2 * SS, c - 2 * SS, c + 2 * SS, c + 2 * SS], fill=(255, 80, 70))
    finish(img, size, os.path.join(BLOCK_DIR, name + ".png"))


def warship(name, size, kind):
    """Warships point up. The hull is the same family as the cargo ships, in grey and blue with red-orange trim."""
    import math
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    hull, dark, deck = (96, 120, 144), (44, 58, 76), (66, 86, 108)
    if kind == "abyss":
        d.ellipse([c - int(s * 0.17), 2 * SS, c + int(s * 0.17), s - 2 * SS], fill=(52, 70, 94), outline=HULL_D, width=2 * SS)
        d.rounded_rectangle([c - 4 * SS, s // 3, c + 4 * SS, s // 3 + int(s * 0.2)], radius=2 * SS, fill=deck, outline=HULL_D)
        d.rectangle([c - SS, s // 3 - 3 * SS, c + SS, s // 3 + 2 * SS], fill=ICE_L)
        for dx in (-1, 1):
            d.rectangle([c + dx * int(s * 0.17) - SS, s - int(s * 0.22), c + dx * int(s * 0.17) + SS, s - int(s * 0.08)], fill=HULL_D)
    elif kind == "carrier":
        w = int(s * 0.3)
        d.polygon([(c, 2 * SS), (c + w, s // 5), (c + w, s - 4 * SS), (c - w, s - 4 * SS), (c - w, s // 5)], fill=hull, outline=HULL_D)
        d.rectangle([c - w + 3 * SS, s // 4, c + w - 3 * SS, s - 8 * SS], fill=deck)
        for i in range(5):
            y = s // 4 + 4 * SS + i * ((s - 8 * SS - s // 4 - 8 * SS) // 5)
            d.rectangle([c - SS, y, c + SS, y + 4 * SS], fill=ICE_L)
        d.line([c - w + 5 * SS, s // 4 + 3 * SS, c - w + 5 * SS, s - 11 * SS], fill=WARM, width=SS)
        d.rectangle([c + w - 11 * SS, s // 3, c + w - 3 * SS, s // 3 + 14 * SS], fill=(54, 70, 92), outline=HULL_D)
    else:
        w = int(s * {"patrol": 0.2, "corvette": 0.22, "torpedo": 0.15, "mortar": 0.22, "frigate": 0.21, "longbow": 0.2}.get(kind, 0.2))
        d.polygon([(c, 2 * SS), (c + w, s // 5), (c + w, s - 3 * SS), (c - w, s - 3 * SS), (c - w, s // 5)], fill=hull, outline=HULL_D)
        d.rectangle([c - w + 2 * SS, s // 4, c + w - 2 * SS, s - 6 * SS], fill=deck)
        d.rectangle([c - 3 * SS, s // 2, c + 3 * SS, s // 2 + 6 * SS], fill=ICE_L, outline=HULL_D)
        if kind == "patrol":
            d.ellipse([c - 3 * SS, s // 4, c + 3 * SS, s // 4 + 6 * SS], fill=WARM, outline=HULL_D)
        elif kind == "corvette":
            for dx in (-1, 1):
                d.rectangle([c + dx * 6 * SS - 3 * SS, s // 4, c + dx * 6 * SS + 3 * SS, s // 4 + 8 * SS], fill=WARM, outline=HULL_D)
        elif kind == "torpedo":
            for dx in (-1, 1):
                d.rectangle([c + dx * 3 * SS - SS, s // 6, c + dx * 3 * SS + SS, s // 3], fill=(200, 230, 255), outline=HULL_D)
        elif kind == "mortar":
            d.ellipse([c - 6 * SS, s // 3 - 2 * SS, c + 6 * SS, s // 3 + 10 * SS], fill=(40, 52, 68), outline=HULL_D, width=SS)
            d.ellipse([c - 3 * SS, s // 3 + SS, c + 3 * SS, s // 3 + 7 * SS], fill=(14, 20, 30))
        elif kind == "frigate":
            for dy in (0, 1):
                for dx in (-1, 1):
                    d.rectangle([c + dx * 7 * SS - 4 * SS, s // 4 + 3 * SS + dy * 12 * SS, c + dx * 7 * SS + 4 * SS, s // 4 + 3 * SS + dy * 12 * SS + 9 * SS],
                                fill=WARM, outline=HULL_D)
        elif kind == "longbow":
            for dx in (-1, 1):
                d.ellipse([c + dx * 7 * SS - 5 * SS, s // 5, c + dx * 7 * SS + 5 * SS, s // 5 + 10 * SS], fill=(52, 66, 84), outline=HULL_D)
                d.rectangle([c + dx * 7 * SS - SS, 0, c + dx * 7 * SS + SS, s // 5 + 2 * SS], fill=HULL_L, outline=HULL_D)
    finish(img, size, os.path.join(UNIT_DIR, name + ".png"))


def interceptor(name, size):
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    d.polygon([(c, 2 * SS), (c + int(s * 0.42), s - 4 * SS), (c, s - 8 * SS), (c - int(s * 0.42), s - 4 * SS)], fill=(110, 140, 168), outline=HULL_D)
    d.polygon([(c, s // 4), (c + 3 * SS, s // 2), (c - 3 * SS, s // 2)], fill=ICE_L, outline=HULL_D)
    d.rectangle([c - SS, s - 12 * SS, c + SS, s - 6 * SS], fill=WARM)
    finish(img, size, os.path.join(UNIT_DIR, name + ".png"))


def battery_top(name, size):
    """The lid of a battery that the game draws over the body (DrawRegion("-top")): a light frame and a shine."""
    img = canvas(size)
    d = ImageDraw.Draw(img)
    s = size * SS
    c = s // 2
    r = int(s * 0.5) - 4 * SS
    d.rounded_rectangle([c - r, c - r, c + r, c + r], radius=2 * SS, outline=(190, 210, 228, 200), width=SS)
    h = r // 2
    d.rectangle([c - h, c - h, c + h, c + h], outline=(255, 240, 200, 230), width=SS)
    d.line([c - h + SS, c - h + SS, c - SS, c - h + SS], fill=(255, 255, 255, 220), width=SS)
    finish(img, size, os.path.join(BLOCK_DIR, name + "-top.png"))


def defense_sprites():
    turret("arc-coil", 64, "arc")
    turret("pulse-lance", 96, "lance")
    turret("tide-cannon", 64, "cannon")
    turret("whirl-mortar", 96, "mortar")
    vortex_gen("vortex-generator", 96)
    mine("naval-mine", 32)
    pad("warship-yard", 128, True)
    for name, size, kind in (("patrol-boat", 28, "patrol"), ("corvette", 36, "corvette"), ("torpedo-boat", 28, "torpedo"),
                             ("mortar-skiff", 34, "mortar"), ("missile-frigate", 54, "frigate"), ("abyss", 44, "abyss"),
                             ("longbow", 74, "longbow"), ("aircraft-carrier", 108, "carrier")):
        warship(name, size, kind)
    interceptor("interceptor", 24)


def shrink(name, size=32, folder=None):
    """Port and depot are one tile. They are drawn on a 64px plate and scaled down."""
    path = os.path.join(folder or BLOCK_DIR, name + ".png")
    Image.open(path).convert("RGBA").resize((size, size), Image.LANCZOS).save(path)


def planet_icon(name):
    """The icon of the planet in the tech tree and the database: a sphere of deep and shallow water with an ice cap."""
    import math
    size = 32
    img = canvas(size)
    px = img.load()
    n = size * SS
    c = n / 2.0
    r = c - 1.5 * SS
    rng = random.Random(name)
    blobs = [(rng.uniform(-0.8, 0.8), rng.uniform(-0.8, 0.8), rng.uniform(0.18, 0.4)) for _ in range(9)]
    for y in range(n):
        for x in range(n):
            dx, dy = (x + 0.5 - c) / r, (y + 0.5 - c) / r
            q = dx * dx + dy * dy
            if q > 1:
                continue
            dz = math.sqrt(1 - q)
            col = (29, 79, 149)
            if any((dx - bx) ** 2 + (dy - by) ** 2 < br * br for bx, by, br in blobs):
                col = (74, 159, 220)
            if dy < -0.55 + 0.12 * math.sin(dx * 7.0):
                col = (227, 244, 255)
            light = 0.55 + 0.6 * (-dx * 0.45 - dy * 0.55 + dz * 0.7)
            col = tuple(max(0, min(255, int(v * light))) for v in col)
            px[x, y] = col + (255,)
    # a thin atmosphere ring
    d = ImageDraw.Draw(img)
    d.ellipse([c - r, c - r, c + r, c + r], outline=(143, 201, 242, 255), width=SS)
    finish(img, size, os.path.join(ROOT, "others", name + ".png"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--vanilla", help="path to the vanilla deep-water.png")
    args = parser.parse_args()

    if args.vanilla:
        rich_water(args.vanilla)

    planet_icon("hiemara")
    defense_sprites()
    factory_sprites()
    filter_block("water-filter-1", 64, 1)
    filter_block("water-filter-2", 96, 2)
    filter_block("water-filter-3", 96, 3)
    filter_block("water-filter-4", 128, 4)
    port("cargo-port", 64)
    shrink("cargo-port")
    depot("cargo-depot", 64)
    shrink("cargo-depot")
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
    shrink("liquid-port")
    hub("liquid-depot", 64, 2, "liquid")
    shrink("liquid-depot")
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
