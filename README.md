# NEWHORIZON MOD

![Logo](github-pictures/logo.png)

****A Java Mindustry<V8, build 160+> mod for Android and PC.****

**Powered by *Yuria*, Maintained by *酪桦姬***

---
## Community
+ **Discord Server**
[![Discord](https://img.shields.io/discord/825801593466388520.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://discord.gg/gMD5KGpxQM)


+ **QQ Group | QQ群**

![QQ](github-pictures/qq-code.png)

---
## Mod Server
`175.178.22.6:6666`

---
## Tools
+ **Image to map** (`tools/image_to_map.py`, needs Python 3, Pillow, numpy)
  turns any image into a Mindustry map built only from floor tiles (1 pixel = 1 tile):
  ```
  python3 tools/image_to_map.py picture.png -o picture.msav --width 200 --dither floyd --preview preview.png
  ```
  Drop the `.msav` into the game's `maps` folder or open it from the map editor. Floors come from vanilla
  Mindustry plus this mod (`--palette vanilla|newhorizon|all`); see the header of the script for all options.
  Tests: `python3 -m unittest tools/test_image_to_map.py`
