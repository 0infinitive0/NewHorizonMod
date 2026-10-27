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
    "sand": (247, 203, 164),
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

    for i, name in enumerate(("cargo-drone", "cargo-hauler", "cargo-freighter"), start=1):
        air_unit(name, 24 + i * 8, i)
    for i, name in enumerate(("cargo-skiff", "cargo-barge", "cargo-carrier"), start=1):
        naval_unit(name, 24 + i * 8, i)


if __name__ == "__main__":
    main()
