#!/usr/bin/env python3
"""Tests for tools/image_to_map.py:  python3 -m unittest tools/test_image_to_map.py -v"""
import io
import os
import struct
import sys
import tempfile
import unittest
import zlib

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import image_to_map as m  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")


def solid(color, size=(4, 4)):
    return Image.new("RGBA", size, color)


class PaletteTest(unittest.TestCase):
    def test_vanilla_names_unique_and_valid_colours(self):
        pal = m.build_palette(("vanilla",))
        self.assertEqual(len(pal), len(m.VANILLA_FLOORS))
        self.assertTrue(all(len(f.rgb) == 3 for f in pal))

    def test_mod_floors_are_prefixed_and_measured_from_sprites(self):
        pal = m.build_palette(("newhorizon",))
        self.assertEqual(len(pal), len(m.MOD_FLOORS))
        for f in pal:
            self.assertTrue(f.name.startswith("new-horizon-"), f.name)
            self.assertEqual(f.mod, "new-horizon")

    def test_works_without_the_sprites_folder(self):
        with tempfile.TemporaryDirectory() as d:
            pal = m.build_palette(("newhorizon",), sprite_dir=os.path.join(d, "missing"))
        self.assertEqual(len(pal), len(m.MOD_FLOORS))
        self.assertEqual(set(m.MOD_FLOOR_COLORS), set(m.MOD_FLOORS))

    def test_embedded_colours_match_the_sprites(self):
        if not os.path.isdir(m.SPRITE_DIR):
            self.skipTest("sprites not present")
        for f in m.mod_floors():
            short = f.name[len("new-horizon-"):]
            self.assertEqual(f.rgb, m.parse_hex(m.MOD_FLOOR_COLORS[short]), short)

    def test_sprite_pattern_does_not_mix_up_similar_names(self):
        d = SPRITE = m.SPRITE_DIR
        plain = m.sprite_average(d, "metal-ground")
        heat = m.sprite_average(d, "metal-ground-heat")
        self.assertNotEqual(plain, heat)  # metal-ground must not swallow metal-ground-heat1.png

    def test_only_exclude_accept_short_or_full_names(self):
        pal = m.build_palette(("vanilla", "newhorizon"), only=["stone", "metal-ground"])
        self.assertEqual({f.name for f in pal}, {"stone", "new-horizon-metal-ground"})
        pal = m.build_palette(("vanilla",), exclude=["stone"])
        self.assertNotIn("stone", [f.name for f in pal])

    def test_empty_palette_is_an_error(self):
        with self.assertRaises(SystemExit):
            m.build_palette(("vanilla",), only=["does-not-exist"])


class ConversionTest(unittest.TestCase):
    def setUp(self):
        self.pal = m.build_palette(("vanilla",))
        self.names = [f.name for f in self.pal]

    def convert(self, img, **kw):
        return m.image_to_grid(img, self.pal, kw.pop("background", "stone"), **kw)

    def test_exact_colours_map_to_their_floor(self):
        for name in ("grass", "snow", "deep-water", "molten-slag", "sand-floor"):
            rgb = m.parse_hex(m.VANILLA_FLOORS[name])
            grid = self.convert(solid(rgb + (255,)))
            self.assertEqual(self.names[grid[0, 0]], name)

    def test_transparent_pixels_use_background(self):
        img = solid((255, 0, 0, 0))
        grid = self.convert(img, background="snow")
        self.assertTrue(all(self.names[i] == "snow" for i in grid.reshape(-1)))

    def test_unknown_background_is_an_error(self):
        with self.assertRaises(SystemExit):
            self.convert(solid((1, 2, 3, 255)), background="nope")

    def test_dithering_mixes_floors_for_an_in_between_colour(self):
        # halfway between snow and grass should use more than one floor when dithered
        a, b = m.parse_hex(m.VANILLA_FLOORS["snow"]), m.parse_hex(m.VANILLA_FLOORS["deep-water"])
        mid = tuple((x + y) // 2 for x, y in zip(a, b))
        img = solid(mid + (255,), (24, 24))
        plain = self.convert(img)
        dith = self.convert(img, dither="floyd", strength=1.0)
        self.assertEqual(len(np.unique(plain)), 1)
        self.assertGreater(len(np.unique(dith)), 1)

    def test_sizes(self):
        self.assertEqual(m.target_size(100, 50), (100, 50))          # small images are not upscaled
        self.assertEqual(m.target_size(1000, 500), (250, 125))       # large ones fit max_size
        self.assertEqual(m.target_size(100, 50, width=200), (200, 100))
        self.assertEqual(m.target_size(100, 50, height=10), (20, 10))
        self.assertEqual(m.target_size(100, 50, width=7, height=9), (7, 9))

    def test_resize_does_not_bleed_transparent_colour(self):
        img = Image.new("RGBA", (4, 2), (255, 0, 0, 0))   # transparent red
        img.putpixel((0, 0), (0, 255, 0, 255))            # one green pixel
        out = m.load_image_from(img, 2, 1) if hasattr(m, "load_image_from") else None
        if out is None:  # go through a file
            with tempfile.TemporaryDirectory() as d:
                p = os.path.join(d, "a.png")
                img.save(p)
                out = m.load_image(p, width=2, height=1, resample="box")
        left = np.asarray(out)[0, 0]
        self.assertEqual(tuple(left[:3]), (0, 255, 0))   # not mixed with the red of transparent pixels


class FormatTest(unittest.TestCase):
    def roundtrip(self, names_grid_fn, w, h, **kw):
        pal = m.build_palette(("vanilla", "newhorizon"))
        by_name = {f.name: i for i, f in enumerate(pal)}
        grid = np.array([[by_name[names_grid_fn(x, y)] for x in range(w)] for y in range(h)])
        data = m.encode_msav(grid, pal, **kw)
        meta, floors, overlays = m.read_msav(data)
        expected = np.array([[names_grid_fn(x, y) for x in range(w)] for y in range(h)], dtype=object)
        self.assertTrue((floors == expected).all())
        self.assertTrue((overlays == "air").all())
        return meta, data

    def test_roundtrip_orientation_and_meta(self):
        # top row of the image must be the TOP of the map (tile y = height-1)
        meta, data = self.roundtrip(lambda x, y: "snow" if y == 0 else ("grass" if x % 2 else "stone"), 7, 5,
                                    name="Test", author="me", description="d")
        self.assertEqual((meta["width"], meta["height"]), ("7", "5"))
        self.assertEqual(meta["name"], "Test")
        self.assertNotIn("mods", meta)

        raw = zlib.decompress(data)
        self.assertEqual(raw[:8], b"MSAV" + struct.pack(">i", 7))
        # first floor entry belongs to tile (0, 0) = bottom-left = bottom row of the grid (row h-1)
        buf = io.BytesIO(raw)
        buf.seek(8)
        for _ in range(2):
            buf.seek(struct.unpack(">i", buf.read(4))[0], io.SEEK_CUR)
        buf.read(4)
        w, h = struct.unpack(">HH", buf.read(4))
        floor_id = struct.unpack(">h", buf.read(2))[0]
        self.assertEqual((w, h), (7, 5))
        self.assertNotEqual(floor_id, 0)

    def test_mod_floor_is_listed_in_mods(self):
        meta, _ = self.roundtrip(lambda x, y: "new-horizon-metal-ground" if x else "stone", 3, 2)
        self.assertTrue(meta["mods"].startswith("[new-horizon:"))

    def test_runs_longer_than_255_and_exact_multiples(self):
        for w, h in ((600, 1), (256, 1), (255, 2), (257, 3), (1, 1), (1, 700)):
            self.roundtrip(lambda x, y: "stone", w, h)
            self.roundtrip(lambda x, y: "stone" if (x + 3 * y) % 300 < 280 else "sand-floor", w, h)

    def test_non_ascii_and_astral_names(self):
        meta, _ = self.roundtrip(lambda x, y: "stone", 2, 2, name="要塞 🙂 \u0000x", description="é")
        self.assertEqual(meta["name"], "要塞 🙂 \u0000x")
        self.assertEqual(meta["description"], "é")

    def test_java_utf_matches_known_encodings(self):
        self.assertEqual(m.java_utf("a"), b"\x00\x01a")
        self.assertEqual(m.java_utf("\u0000"), b"\x00\x02\xc0\x80")
        self.assertEqual(m.java_utf("é"), b"\x00\x02\xc3\xa9")
        self.assertEqual(m.java_utf("要"), "要".encode("utf-8").join([b"\x00\x03", b""]))
        self.assertEqual(m.java_utf("\U0001F642"), b"\x00\x06\xed\xa0\xbd\xed\xb9\x82")  # surrogate pair

    def test_reader_understands_a_real_repo_header(self):
        # sanity check of the shared framing against a map the mod ships (it has buildings, so the
        # reader must refuse the tile data, but meta + content header must parse first)
        path = os.path.join(ROOT, "assets", "maps", "artillery-base.msav")
        if not os.path.exists(path):
            self.skipTest("map not present")
        with open(path, "rb") as fh:
            blob = fh.read()
        buf = io.BytesIO(zlib.decompress(blob))
        self.assertEqual(buf.read(4), b"MSAV")
        self.assertEqual(struct.unpack(">i", buf.read(4))[0], m.SAVE_VERSION)
        with self.assertRaises(ValueError):
            m.read_msav(blob)


class CliTest(unittest.TestCase):
    def test_end_to_end(self):
        with tempfile.TemporaryDirectory() as d:
            src = os.path.join(d, "pic.png")
            img = Image.new("RGBA", (40, 30), (0, 0, 0, 0))
            for x in range(40):
                for y in range(20):
                    img.putpixel((x, y), (x * 6, 255 - x * 6, 128, 255))
            img.save(src)
            out = os.path.join(d, "pic.msav")
            prev = os.path.join(d, "prev.png")
            rc = m.main([src, "-o", out, "--dither", "floyd", "--preview", prev, "--width", "20"])
            self.assertEqual(rc, 0)
            with open(out, "rb") as fh:
                meta, floors, _ = m.read_msav(fh.read())
            self.assertEqual((meta["width"], meta["height"]), ("20", "15"))
            self.assertEqual(floors.shape, (15, 20))
            self.assertEqual(meta["name"], "pic")
            self.assertEqual(Image.open(prev).size, (80, 60))
            # bottom rows were transparent -> background stone
            self.assertTrue((floors[-1] == "stone").all())

    def test_list_floors(self):
        import contextlib
        with contextlib.redirect_stdout(io.StringIO()) as out, contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(m.main(["--list-floors", "--palette", "vanilla"]), 0)
        self.assertIn("grass", out.getvalue())


if __name__ == "__main__":
    unittest.main()
