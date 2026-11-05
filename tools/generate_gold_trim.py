"""Draws the pedestals' gold trim (textures/block/pedestal/gold_trim.png): aged gold at twice a block texture's
resolution, so the small gold parts (bars half a block pixel wide, a gem under one, bands half a pixel tall) still show
a little shading, in pixels half a block pixel big (any finer and they'd look out of place on the blocks). Each part of a pedestal takes its own region (the models' UVs, in tools/generate_pedestals.py and
models/block/pedestal_template.json, point at them; UVs are in sixteenths, a sixteenth being two pixels here):

    band sides     v 0.5 to 1.5     a light row over a shadow row
    band tops      the frame round u, v 3 to 13 (its outer sixteenth): light outside, shadow inside, where it meets
                   the column; the column covers the square inside it, which holds the carving's regions:
    bars across    u 4 to 11, v 4 to 5     a lit row with a few glints over a shadow row
    bars down      u 11 to 12, v 4 to 12   the same, turned
    gem            u 6 to 8, v 6 to 8      a faceted face with a glint
    gem edges      u 6 to 8, v 9 to 10     its lit bevel
    bar edges      u 4 to 11, v 11 to 12   deep shadow

Everywhere else is the plain grained gold. Run it from this folder; it needs Pillow.
"""
import os

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'common', 'src', 'main', 'resources', 'assets', 'trophypedestals', 'textures',
                   'block', 'pedestal', 'gold_trim.png')
SIZE, SCALE = 32, 2

HIGHLIGHT, LIGHT, GOLD, SHADOW, DEEP = 0xF2DC92, 0xDDB95E, 0xC29A40, 0x936E2C, 0x634919
GRAIN_DARK, GRAIN_LIGHT, GLINT = 0xAD8834, 0xD3AE54, 0xFFF4C8

image = Image.new('RGBA', (SIZE, SIZE))
pixels = image.load()


def put(x, y, color):
    pixels[x, y] = ((color >> 16) & 255, (color >> 8) & 255, color & 255, 255)


def grained(x, y):
    """The plain gold, with a sparse even grain (a fixed pattern, not noise, so it never speckles)."""
    if (x * 3 + y * 5) % 9 == 0:
        return GRAIN_DARK
    if (x * 5 + y * 3) % 11 == 0:
        return GRAIN_LIGHT
    return GOLD


def px(uv):
    return int(round(uv * SCALE))


# Everything starts as the plain grained gold.
for y in range(SIZE):
    for x in range(SIZE):
        put(x, y, grained(x, y))

# Band sides: a light row over a shadow row.
for x in range(SIZE):
    put(x, px(0.5), LIGHT)
    put(x, px(0.5) + 1, SHADOW)

# Band tops: the frame round the square from 3 to 13, two pixels: light outside, shadow inside (where it meets the
# column).
low, high = px(3), px(13) - 1
for y in range(low, high + 1):
    for x in range(low, high + 1):
        depth = min(x - low, high - x, y - low, high - y)
        if depth < 2:
            put(x, y, (LIGHT, SHADOW)[depth])

# Bars across: a lit row (a glint every few pixels) over a shadow row; bars down, the same turned.
for x in range(px(4), px(11)):
    put(x, px(4), HIGHLIGHT if x % 5 == 0 else LIGHT)
    put(x, px(4) + 1, SHADOW)
for y in range(px(4), px(12)):
    put(px(11), y, HIGHLIGHT if y % 5 == 0 else LIGHT)
    put(px(11) + 1, y, SHADOW)

# The gem: four pixels square, its facets lit from the top left, with a glint.
GEM = [[HIGHLIGHT, HIGHLIGHT, HIGHLIGHT, LIGHT],
       [HIGHLIGHT, GLINT, LIGHT, GOLD],
       [HIGHLIGHT, LIGHT, GOLD, SHADOW],
       [LIGHT, GOLD, SHADOW, DEEP]]
for y in range(4):
    for x in range(4):
        put(px(6) + x, px(6) + y, GEM[y][x])
# Its edges: a lit bevel; the bars' edges: shadow.
for x in range(px(6), px(8)):
    put(x, px(9), LIGHT)
    put(x, px(9) + 1, GOLD)
for x in range(px(4), px(11)):
    put(x, px(11), SHADOW)
    put(x, px(11) + 1, DEEP)

image.save(OUT)
print('drew the gold trim')
