#!/usr/bin/env python3
"""
Turns an image into a Mindustry map made only of floor tiles (one pixel -> one tile).

    python3 tools/image_to_map.py picture.png                      # -> picture.msav
    python3 tools/image_to_map.py picture.png -o art.msav --width 200 --dither floyd --preview art_preview.png
    python3 tools/image_to_map.py --list-floors                    # show the palette that would be used

Put the resulting .msav into the game's `maps` folder, or open it from the map editor
(Editor -> Import / Open map), then play or edit it like any other map.

How it works
    * The image is resized (default: fitted into 250x250 tiles, never upscaled).
    * Every pixel is matched to the closest floor colour in CIELAB space, optionally with
      Floyd-Steinberg error diffusion (--dither floyd) so gradients survive the small palette.
    * Fully transparent pixels become the --background floor.
    * A save-version-7 .msav is written (the same version as the maps in assets/maps, which the
      current game still loads). It contains only floors; there are no blocks, ores or buildings.

Palettes (--palette)
    vanilla     62 stock Mindustry floors, colours measured from the game's own floor sprites
    newhorizon  this mod's floors; colours come from assets/sprites/blocks/environment when the
                script sits in the repo, otherwise from a built-in table (so the file works alone)
    all         both (default). A map that uses a mod floor lists the mod in its metadata, and
                needs the mod installed - otherwise those tiles load as plain stone.
    Use --palette-file to add or replace floors from a JSON file: {"floor-name": "#rrggbb", ...}
    (names of floors from other mods must include the mod prefix, e.g. "some-mod-floor-name").
    Use --only / --exclude to restrict the palette, e.g. --exclude molten-slag,space,tar

Requires Pillow and numpy.
"""
import argparse
import io
import json
import os
import re
import struct
import sys
import time
import zlib

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SPRITE_DIR = os.path.join(ROOT, "assets", "sprites", "blocks", "environment")

SAVE_VERSION = 7
GAME_BUILD = 160
TILE_SIZE = 8
MAX_DIMENSION = 65535  # map size is stored as an unsigned short
BLOCK_CONTENT_TYPE = 1  # mindustry.ctype.ContentType.block

MOD_NAME = "new-horizon"
MOD_VERSION = None  # read from mod.hjson when available

# Stock floors. Colours are the alpha-weighted average of the floor's sprites (all variants).
VANILLA_FLOORS = {
    "deep-water": "#505fa6", "shallow-water": "#5c6dbb", "tainted-water": "#614b95",
    "deep-tainted-water": "#45366c", "darksand-tainted-water": "#4b4161", "sand-water": "#97889f",
    "darksand-water": "#454969", "tar": "#1d201f", "pooled-cryofluid": "#6ecdec",
    "molten-slag": "#ff8142", "space": "#070018", "stone": "#57575d", "crater-stone": "#57575c",
    "char": "#4e4e56", "basalt": "#403d3d", "hotrock": "#5a4a40", "magmarock": "#835e45",
    "sand-floor": "#dab491", "darksand": "#403d3d", "dirt": "#684037", "mud": "#351f1b",
    "dacite": "#9a9bad", "rhyolite": "#74493b", "rhyolite-crater": "#73483a",
    "rough-rhyolite": "#6a4138", "regolith": "#9b6948", "yellow-stone": "#976547",
    "carbon-stone": "#2c3038", "ferric-stone": "#4a4240", "ferric-craters": "#4d4441",
    "beryllic-stone": "#353c37", "crystalline-stone": "#363039", "crystal-floor": "#493b53",
    "yellow-stone-plates": "#a57649", "red-stone": "#b44e55", "dense-red-stone": "#bb5857",
    "red-ice": "#fae1d9", "arkycite-floor": "#6f934d", "arkyic-stone": "#495f42",
    "redmat": "#5a3937", "bluemat": "#3b3354", "grass": "#6eac5f", "salt": "#f5f6f9",
    "snow": "#e8eef4", "ice": "#d3d2fd", "ice-snow": "#d0d2f4", "shale": "#605c83",
    "moss": "#704678", "core-zone": "#8b7662", "spore-moss": "#704988",
    "metal-floor": "#6f717e", "metal-floor-damaged": "#6b6d79", "metal-floor-2": "#686a75",
    "metal-floor-3": "#757784", "metal-floor-4": "#7a7d8a", "metal-floor-5": "#8d7363",
    "dark-panel-1": "#4f515d", "dark-panel-2": "#55515b", "dark-panel-3": "#4d4f5b",
    "dark-panel-4": "#585a67", "dark-panel-5": "#5b5862", "dark-panel-6": "#665a63",
}

# New Horizon floors (without the "new-horizon-" prefix) -> sprite base name in SPRITE_DIR.
# Only plain single-tile floors; the grooved / data floors need extra tile data to look right.
MOD_FLOORS = {
    "armor-clear": "armor-clear",
    "armor-ancient": "armor-ancient",
    "armor-quantum": "armor-quantum",
    "armor-light": "armor-light",
    "conglomerate-rock": "conglomerate-rock",
    "metal-ground": "metal-ground",
    "metal-ground-heat": "metal-ground-heat",
    "metal-ground-quantum": "metal-ground-quantum",
    "quantum-field": "quantum-field",
    "quantum-field-deep": "quantum-field-deep",
    "lab-floor-light": "lab-floor-light",
    "lab-floor-dark": "lab-floor-dark",
    "plating-metal-floor": "plating-metal-floor",
}

# Colours measured from those sprites, used when the sprites are not next to the script
# (so the file also works when downloaded on its own). Regenerate if the sprites change.
MOD_FLOOR_COLORS = {
    "armor-clear": "#3f4049", "armor-ancient": "#686251", "armor-quantum": "#4f4961",
    "armor-light": "#595961", "conglomerate-rock": "#4f4e51", "metal-ground": "#444651",
    "metal-ground-heat": "#5f5675", "metal-ground-quantum": "#524c65", "quantum-field": "#ab7ce9",
    "quantum-field-deep": "#915bcf", "lab-floor-light": "#555666", "lab-floor-dark": "#4d4e58",
    "plating-metal-floor": "#3f4049",
}


class Floor:
    def __init__(self, name, rgb, mod=None):
        self.name = name  # full content name, as stored in the map
        self.rgb = tuple(int(c) for c in rgb)
        self.mod = mod

    def __repr__(self):
        return "Floor(%s, #%02x%02x%02x)" % ((self.name,) + self.rgb)


# ----------------------------------------------------------------------------------------------
# Palette
# ----------------------------------------------------------------------------------------------

def parse_hex(value):
    m = re.fullmatch(r"#?([0-9a-fA-F]{6})", value.strip())
    if not m:
        raise ValueError("not a #rrggbb colour: %r" % value)
    n = int(m.group(1), 16)
    return (n >> 16) & 255, (n >> 8) & 255, n & 255


def sprite_average(sprite_dir, base):
    """Alpha-weighted mean colour of base.png / base1.png / base2.png ... or None if none exist."""
    pattern = re.compile(r"^%s\d*\.png$" % re.escape(base))
    total = np.zeros(3)
    weight = 0.0
    for fn in sorted(os.listdir(sprite_dir)):
        if not pattern.match(fn):
            continue
        a = np.asarray(Image.open(os.path.join(sprite_dir, fn)).convert("RGBA"), dtype=float)
        w = a[..., 3:] / 255.0
        total += (a[..., :3] * w).sum((0, 1))
        weight += w.sum()
    if weight < 1:
        return None
    return tuple(int(round(v)) for v in total / weight)


def mod_floors(sprite_dir=SPRITE_DIR):
    """Mod floors: measured from the sprites when they are available, else the embedded colours."""
    have_sprites = os.path.isdir(sprite_dir)
    floors = []
    for name, base in MOD_FLOORS.items():
        rgb = sprite_average(sprite_dir, base) if have_sprites else None
        if rgb is None:
            rgb = parse_hex(MOD_FLOOR_COLORS[name])
        floors.append(Floor("%s-%s" % (MOD_NAME, name), rgb, MOD_NAME))
    return floors


def build_palette(groups=("vanilla", "newhorizon"), only=None, exclude=None, palette_file=None,
                  sprite_dir=SPRITE_DIR):
    floors = {}
    if "vanilla" in groups:
        for n, c in VANILLA_FLOORS.items():
            floors[n] = Floor(n, parse_hex(c))
    if "newhorizon" in groups:
        for f in mod_floors(sprite_dir):
            floors[f.name] = f
    if palette_file:
        with open(palette_file, encoding="utf-8") as fh:
            for n, c in json.load(fh).items():
                floors[n] = Floor(n, parse_hex(c), floors[n].mod if n in floors else None)
    if only:
        floors = {n: f for n, f in floors.items() if matches(n, only)}
    if exclude:
        floors = {n: f for n, f in floors.items() if not matches(n, exclude)}
    if not floors:
        raise SystemExit("error: the palette is empty (check --palette / --only / --exclude)")
    return list(floors.values())


def matches(name, wanted):
    """A wanted entry matches the full name, or the name without the mod prefix."""
    short = name[len(MOD_NAME) + 1:] if name.startswith(MOD_NAME + "-") else name
    return name in wanted or short in wanted


def split_names(value):
    return [s.strip() for s in value.split(",") if s.strip()] if value else None


# ----------------------------------------------------------------------------------------------
# Colour matching
# ----------------------------------------------------------------------------------------------

def srgb_to_lab(rgb):
    """rgb: (..., 3) array in 0..255 -> CIELAB (D65)."""
    c = np.asarray(rgb, dtype=float) / 255.0
    lin = np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    m = np.array([[0.4124564, 0.3575761, 0.1804375],
                  [0.2126729, 0.7151522, 0.0721750],
                  [0.0193339, 0.1191920, 0.9503041]])
    xyz = lin @ m.T / np.array([0.95047, 1.0, 1.08883])
    f = np.where(xyz > 216 / 24389, np.cbrt(xyz), (24389 / 27 * xyz + 16) / 116)
    return np.stack([116 * f[..., 1] - 16, 500 * (f[..., 0] - f[..., 1]),
                     200 * (f[..., 1] - f[..., 2])], axis=-1)


def nearest(lab_pixels, lab_palette):
    """lab_pixels (n, 3) -> index of the closest palette entry for each."""
    out = np.empty(len(lab_pixels), dtype=np.int32)
    step = max(1, 4_000_000 // max(1, len(lab_palette)))
    for i in range(0, len(lab_pixels), step):
        chunk = lab_pixels[i:i + step]
        d = ((chunk[:, None, :] - lab_palette[None, :, :]) ** 2).sum(-1)
        out[i:i + step] = d.argmin(1)
    return out


def quantize(rgb, opaque, palette, dither="none", strength=0.75):
    """
    rgb: (h, w, 3) uint8, opaque: (h, w) bool -> (h, w) int32 palette indices (-1 where not opaque).
    """
    h, w, _ = rgb.shape
    lab_pal = srgb_to_lab(np.array([f.rgb for f in palette]))
    lab = srgb_to_lab(rgb)
    result = np.full((h, w), -1, dtype=np.int32)

    if dither == "none":
        result[opaque] = nearest(lab[opaque], lab_pal)
        return result

    # Floyd-Steinberg, serpentine scan, error carried in Lab space
    work = lab.copy()
    for y in range(h):
        xs = range(w) if y % 2 == 0 else range(w - 1, -1, -1)
        direction = 1 if y % 2 == 0 else -1
        for x in xs:
            if not opaque[y, x]:
                continue
            old = work[y, x]
            idx = int(((lab_pal - old) ** 2).sum(1).argmin())
            result[y, x] = idx
            err = (old - lab_pal[idx]) * strength
            for dx, dy, f in ((direction, 0, 7 / 16), (-direction, 1, 3 / 16),
                              (0, 1, 5 / 16), (direction, 1, 1 / 16)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and ny < h and opaque[ny, nx]:
                    work[ny, nx] += err * f
    return result


# ----------------------------------------------------------------------------------------------
# Image -> grid
# ----------------------------------------------------------------------------------------------

def target_size(src_w, src_h, width=None, height=None, max_size=250):
    if width and height:
        w, h = width, height
    elif width:
        w, h = width, round(src_h * width / src_w)
    elif height:
        w, h = round(src_w * height / src_h), height
    else:
        scale = min(1.0, max_size / max(src_w, src_h))
        w, h = round(src_w * scale), round(src_h * scale)
    w, h = max(1, int(w)), max(1, int(h))
    if w > MAX_DIMENSION or h > MAX_DIMENSION:
        raise SystemExit("error: a map can be at most %d tiles on a side" % MAX_DIMENSION)
    return w, h


def load_image(path, width=None, height=None, max_size=250, resample="auto"):
    img = Image.open(path)
    img.load()
    img = img.convert("RGBA")
    w, h = target_size(img.width, img.height, width, height, max_size)
    if (w, h) != img.size:
        if resample == "auto":
            resample = "box" if w * h < img.width * img.height else "nearest"
        filt = {"nearest": Image.NEAREST, "box": Image.BOX, "lanczos": Image.LANCZOS,
                "bilinear": Image.BILINEAR}[resample]
        # premultiply so transparent pixels do not bleed their colour into the edges
        arr = np.asarray(img, dtype=float)
        pre = Image.fromarray(np.concatenate([arr[..., :3] * arr[..., 3:] / 255.0, arr[..., 3:]],
                                             axis=-1).round().astype(np.uint8), "RGBA")
        pre = pre.resize((w, h), filt)
        a = np.asarray(pre, dtype=float)
        alpha = a[..., 3:]
        rgb = np.where(alpha > 0, a[..., :3] * 255.0 / np.maximum(alpha, 1), 0)
        img = Image.fromarray(np.concatenate([rgb, alpha], axis=-1).clip(0, 255).round()
                              .astype(np.uint8), "RGBA")
    return img


def image_to_grid(img, palette, background, dither="none", strength=0.75, alpha_cutoff=128):
    """-> (h, w) array of palette indices, top row first (image orientation)."""
    a = np.asarray(img)
    opaque = a[..., 3] >= alpha_cutoff
    grid = quantize(a[..., :3], opaque, palette, dither, strength)
    bg = next((i for i, f in enumerate(palette) if matches(f.name, [background])), None)
    if bg is None:
        raise SystemExit("error: background floor %r is not in the palette" % background)
    grid[~opaque] = bg
    return grid


def render_preview(grid, palette, scale=1):
    colors = np.array([f.rgb for f in palette], dtype=np.uint8)
    img = Image.fromarray(colors[grid], "RGB")
    if scale != 1:
        img = img.resize((grid.shape[1] * scale, grid.shape[0] * scale), Image.NEAREST)
    return img


# ----------------------------------------------------------------------------------------------
# .msav writing / reading (save version 7, floors only)
# ----------------------------------------------------------------------------------------------

def java_utf(s):
    """Java DataOutput.writeUTF: 2 byte length + modified UTF-8."""
    out = bytearray()
    for unit in struct.unpack("<%dH" % (len(s.encode("utf-16-le")) // 2), s.encode("utf-16-le")):
        if 0 < unit < 0x80:
            out.append(unit)
        elif unit < 0x800:
            out += bytes((0xC0 | unit >> 6, 0x80 | unit & 0x3F))
        else:
            out += bytes((0xE0 | unit >> 12, 0x80 | (unit >> 6) & 0x3F, 0x80 | unit & 0x3F))
    if len(out) > 0xFFFF:
        raise ValueError("string too long for the map format")
    return struct.pack(">H", len(out)) + bytes(out)


def read_java_utf(buf):
    n = struct.unpack(">H", buf.read(2))[0]
    raw = buf.read(n)
    # modified UTF-8 -> UTF-16 code units -> str
    units = []
    i = 0
    while i < len(raw):
        b = raw[i]
        if b < 0x80:
            units.append(b)
            i += 1
        elif b >> 5 == 0b110:
            units.append((b & 0x1F) << 6 | raw[i + 1] & 0x3F)
            i += 2
        else:
            units.append((b & 0x0F) << 12 | (raw[i + 1] & 0x3F) << 6 | raw[i + 2] & 0x3F)
            i += 3
    return struct.pack("<%dH" % len(units), *units).decode("utf-16-le", "surrogatepass")


def region(payload):
    return struct.pack(">i", len(payload)) + payload


def read_mod_version():
    try:
        with open(os.path.join(ROOT, "mod.hjson"), encoding="utf-8") as fh:
            m = re.search(r'^\s*version\s*:\s*"?([^"\n]+)"?', fh.read(), re.M)
        return m.group(1).strip() if m else None
    except OSError:
        return None


def encode_msav(grid, palette, name="Image Map", author="", description="", rules=None):
    """
    grid: (h, w) palette indices, top row first. Returns the compressed .msav bytes.
    Only palette entries that are actually used are written to the content header.
    """
    h, w = grid.shape
    used = sorted(set(int(i) for i in np.unique(grid)))
    names = ["air"] + [palette[i].name for i in used]
    block_id = {palette_index: n + 1 for n, palette_index in enumerate(used)}  # 0 is air
    mods = sorted({palette[i].mod for i in used if palette[i].mod})
    mod_version = MOD_VERSION or read_mod_version() or "0"

    # --- meta
    meta = {
        "name": name, "author": author, "description": description,
        "saved": str(int(time.time() * 1000)), "playtime": "0", "build": str(GAME_BUILD),
        "wave": "1", "wavetime": "0.0", "tick": "0.0", "stats": "{}", "genfilters": "[]",
        "rules": json.dumps(rules if rules is not None else {"waves": False, "infiniteResources": True},
                            separators=(",", ":")),
        "viewpos": "(%s,%s)" % (w * TILE_SIZE / 2, h * TILE_SIZE / 2),
        "width": str(w), "height": str(h),
    }
    if mods:
        meta["mods"] = "[" + ",".join("%s:%s" % (m, mod_version) for m in mods) + "]"
    meta_bytes = struct.pack(">H", len(meta)) + b"".join(java_utf(k) + java_utf(v) for k, v in meta.items())

    # --- content header (blocks only; the game tolerates the other types being absent)
    content = struct.pack(">BBH", 1, BLOCK_CONTENT_TYPE, len(names)) + b"".join(java_utf(n) for n in names)

    # --- map: floors, then blocks. Tile index = y * w + x with y = 0 at the BOTTOM of the map.
    lookup = np.zeros(len(palette), dtype=np.int32)
    for pi, bid in block_id.items():
        lookup[pi] = bid
    flat = lookup[grid[::-1].reshape(-1)]

    out = bytearray(struct.pack(">HH", w, h))
    total = w * h
    i = 0
    while i < total:  # floor id, overlay id (0 = none), run length - 1
        j = i + 1
        while j < total and j - i <= 255 and flat[j] == flat[i]:
            j += 1
        out += struct.pack(">hhB", int(flat[i]), 0, j - i - 1)
        i = j
    i = 0
    while i < total:  # every tile is air: block id 0, no entity/data flags, run length - 1
        run = min(256, total - i)
        out += struct.pack(">hBB", 0, 0, run - 1)
        i += run

    entities = struct.pack(">hii", 0, 0, 0)  # no custom entity ids, no team plans, no units
    custom = struct.pack(">i", 0)           # no custom chunks

    raw = (b"MSAV" + struct.pack(">i", SAVE_VERSION) + region(meta_bytes) + region(content)
           + region(bytes(out)) + region(entities) + region(custom))
    return zlib.compress(raw, 9)


def read_msav(data):
    """
    Minimal reader for the files this tool writes (also reads any version-7 map without buildings).
    Returns (meta dict, grid of floor names top row first, grid of overlay names or '' ).
    """
    buf = io.BytesIO(zlib.decompress(data))
    if buf.read(4) != b"MSAV":
        raise ValueError("not a Mindustry save")
    version = struct.unpack(">i", buf.read(4))[0]
    if version != SAVE_VERSION:
        raise ValueError("only save version %d is supported (got %d)" % (SAVE_VERSION, version))

    def chunk():
        n = struct.unpack(">i", buf.read(4))[0]
        return io.BytesIO(buf.read(n))

    m = chunk()
    meta = {}
    for _ in range(struct.unpack(">H", m.read(2))[0]):
        k = read_java_utf(m)
        meta[k] = read_java_utf(m)

    c = chunk()
    block_names = []
    for _ in range(c.read(1)[0]):
        ctype = c.read(1)[0]
        names = [read_java_utf(c) for _ in range(struct.unpack(">H", c.read(2))[0])]
        if ctype == BLOCK_CONTENT_TYPE:
            block_names = names

    mp = chunk()
    w, h = struct.unpack(">HH", mp.read(4))
    floors, overlays = [], []
    while len(floors) < w * h:
        fid, oid, run = struct.unpack(">hhB", mp.read(5))
        floors += [block_names[fid]] * (run + 1)
        overlays += [block_names[oid]] * (run + 1)
    count = 0
    while count < w * h:  # blocks must all be plain air in files we understand
        bid, packed = struct.unpack(">hB", mp.read(3))
        if packed != 0 or block_names[bid] != "air":
            raise ValueError("map contains buildings or tile data; this reader does not support that")
        count += mp.read(1)[0] + 1
    if count != w * h or mp.read(1):
        raise ValueError("corrupt map region")
    for _ in range(2):
        chunk()  # entities, custom (version 7 has no patches / markers regions)
    if buf.read(1):
        raise ValueError("unexpected trailing data")

    def to_grid(flat):
        g = np.empty((h, w), dtype=object)
        arr = np.array(flat, dtype=object).reshape(h, w)  # row 0 is the bottom of the map
        g[:] = arr[::-1]
        return g

    return meta, to_grid(floors), to_grid(overlays)


# ----------------------------------------------------------------------------------------------
# Command line
# ----------------------------------------------------------------------------------------------

def main(argv=None):
    p = argparse.ArgumentParser(description="Turn an image into a Mindustry map made of floor tiles.")
    p.add_argument("image", nargs="?", help="input image (png, jpg, gif, ...)")
    p.add_argument("-o", "--output", help="output .msav (default: <image name>.msav)")
    p.add_argument("--width", type=int, help="map width in tiles (height follows the aspect ratio)")
    p.add_argument("--height", type=int, help="map height in tiles")
    p.add_argument("--max-size", type=int, default=250,
                   help="without --width/--height, shrink to fit this many tiles (default 250)")
    p.add_argument("--resample", choices=["auto", "nearest", "box", "bilinear", "lanczos"], default="auto",
                   help="resize filter; auto = box when shrinking, nearest otherwise (good for pixel art)")
    p.add_argument("--dither", choices=["none", "floyd"], default="none",
                   help="floyd = error diffusion, keeps gradients and photos readable")
    p.add_argument("--dither-strength", type=float, default=0.75, help="0..1, default 0.75")
    p.add_argument("--palette", choices=["vanilla", "newhorizon", "all"], default="all")
    p.add_argument("--palette-file", help='JSON {"floor-name": "#rrggbb"} to add or override floors')
    p.add_argument("--only", help="comma separated floor names to use exclusively")
    p.add_argument("--exclude", help="comma separated floor names to leave out")
    p.add_argument("--background", default="stone", help="floor for transparent pixels (default stone)")
    p.add_argument("--name", help="map name (default: image file name)")
    p.add_argument("--author", default="", help="map author")
    p.add_argument("--description", default="", help="map description")
    p.add_argument("--preview", help="also write a PNG of the floors as they will appear in the map")
    p.add_argument("--preview-scale", type=int, default=4, help="preview pixels per tile (default 4)")
    p.add_argument("--list-floors", action="store_true", help="print the palette and exit")
    args = p.parse_args(argv)

    groups = ("vanilla", "newhorizon") if args.palette == "all" else (args.palette,)
    palette = build_palette(groups, split_names(args.only), split_names(args.exclude), args.palette_file)

    if args.list_floors:
        for f in palette:
            print("#%02x%02x%02x  %s" % (f.rgb + (f.name,)))
        print("%d floors" % len(palette), file=sys.stderr)
        return 0
    if not args.image:
        p.error("an input image is required")

    img = load_image(args.image, args.width, args.height, args.max_size, args.resample)
    grid = image_to_grid(img, palette, args.background, args.dither, args.dither_strength)
    stem = os.path.splitext(os.path.basename(args.image))[0]
    out = args.output or stem + ".msav"
    data = encode_msav(grid, palette, args.name or stem, args.author, args.description)
    with open(out, "wb") as fh:
        fh.write(data)
    used = np.unique(grid)
    mods = sorted({palette[i].mod for i in used if palette[i].mod})
    print("wrote %s: %dx%d tiles, %d different floors, %d bytes" %
          (out, grid.shape[1], grid.shape[0], len(used), len(data)))
    if mods:
        print("note: this map uses floors from the mod(s): %s" % ", ".join(mods))
    if args.preview:
        render_preview(grid, palette, args.preview_scale).save(args.preview)
        print("preview: %s" % args.preview)
    return 0


if __name__ == "__main__":
    sys.exit(main())
