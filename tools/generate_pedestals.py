"""Generates the resources for every vanilla stone and wood pedestal (Pedestals.Material): block states, block and
item models, recipes, loot tables, the mining tags and the English names. Run it from this folder after adding a
material to Pedestals.Material:

    python tools/generate_pedestals.py

Stone, cobblestone and blackstone have their own drawn textures (their columns fluted in the drawing) and hand-made
files, which this leaves alone; it still lists them in the tags. Every other material uses its vanilla block's own
textures: the cap and base sides
from "side", their tops from "top", the underside from "bottom", and the column from "column", round the gold trim and
the well (whose floor is the material's top too). Every pedestal, the hand-made ones too (whose models take the same
template), has a diamond carved on its column rather than drawn into its texture: a rim of four bars turned 45 degrees, standing a little proud of each side round a
sunken diamond panel, with a small raised gem in its middle, inlaid in the gold trim so it stands out from any material; its
edges are straight and the game's face shading lights one side of the rim and shades the other
(models/block/pedestal_template_chiseled.json, which is pedestal_template.json with that carving added: written from
it, so edit the template and rerun this). The well's floor is an overlay on every pedestal
(textures/block/pedestal/well_imprint.png, drawn here): see-through shading, so it takes the material's colours, of the
well's walls and the glass lens the hologram rises from, on a face over the well in the template; the pedestal's
renderer lights the lens up in its rank's color (well_glow.png, drawn here too) while it shows something. Recipes are six of the material's block round a gold
ingot. Every generated pedestal (and the custom pedestal, drawn from the same model) also has its column fluted as the
drawn ones are: an overlay of see-through shading over its four sides (textures/block/pedestal/column_flutes.png, drawn
here at twice a block texture's resolution: bands at the top and bottom, and between them rounded grooves, each lit
along its lip and shaded into its hollow), so the flutes take the material's colours and blend with its texture, on a face just
outside the column (models/block/pedestal_template_fluted.json, the chiseled template with that face added). It needs
Pillow.
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'common', 'src', 'main', 'resources')
ASSETS = os.path.join(ROOT, 'assets', 'trophypedestals')
DATA = os.path.join(ROOT, 'data')

# The hand-made ones: listed in the tags, files left alone.
CUSTOM = ['stone', 'cobblestone', 'blackstone']


def stone(side, column=None, top=None, bottom=None, ingredient=None):
    return {'side': side, 'top': top or side, 'bottom': bottom or top or side, 'column': column or side,
            'ingredient': ingredient or side, 'wood': False}


def wood(name, column):
    planks = name + '_planks'
    return {'side': planks, 'top': planks, 'bottom': planks, 'column': column, 'ingredient': planks, 'wood': True}


# Material id -> textures (vanilla block textures) and the block its recipe takes.
MATERIALS = {
    'deepslate': stone('polished_deepslate', 'deepslate_tiles'),
    'sandstone': stone('cut_sandstone', 'sandstone', top='sandstone_top', bottom='sandstone_bottom', ingredient='sandstone'),
    'quartz': stone('quartz_block_side', 'quartz_pillar', top='quartz_block_top', bottom='quartz_block_bottom', ingredient='quartz_block'),
    'mossy_cobblestone': stone('mossy_cobblestone', 'mossy_stone_bricks'),
    'stone_bricks': stone('stone_bricks', 'chiseled_stone_bricks'),
    'granite': stone('polished_granite', 'granite'),
    'diorite': stone('polished_diorite', 'diorite'),
    'andesite': stone('polished_andesite', 'andesite'),
    'cobbled_deepslate': stone('cobbled_deepslate', 'deepslate_bricks'),
    'tuff': stone('polished_tuff', 'tuff_bricks'),
    'calcite': stone('calcite'),
    'basalt': stone('polished_basalt_side', 'basalt_side', top='polished_basalt_top', ingredient='polished_basalt'),
    'red_sandstone': stone('red_sandstone', 'cut_red_sandstone', top='red_sandstone_top', bottom='red_sandstone_bottom'),
    'prismarine': stone('prismarine_bricks', 'prismarine'),
    'dark_prismarine': stone('dark_prismarine', 'prismarine_bricks'),
    'end_stone': stone('end_stone_bricks', 'end_stone'),
    'purpur': stone('purpur_block', 'purpur_pillar'),
    'bricks': stone('bricks'),
    'nether_bricks': stone('nether_bricks'),
    'red_nether_bricks': stone('red_nether_bricks'),
    'mud_bricks': stone('mud_bricks'),
    'resin_bricks': stone('resin_bricks', 'chiseled_resin_bricks'),
    'oak': wood('oak', 'oak_log'),
    'spruce': wood('spruce', 'spruce_log'),
    'birch': wood('birch', 'birch_log'),
    'jungle': wood('jungle', 'jungle_log'),
    'acacia': wood('acacia', 'acacia_log'),
    'dark_oak': wood('dark_oak', 'dark_oak_log'),
    'mangrove': wood('mangrove', 'mangrove_log'),
    'cherry': wood('cherry', 'cherry_log'),
    'pale_oak': wood('pale_oak', 'pale_oak_log'),
    'bamboo': wood('bamboo', 'bamboo_block'),
    'crimson': wood('crimson', 'crimson_stem'),
    'warped': wood('warped', 'warped_stem'),
}


def write(path, value):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(value, f, indent=2)
        f.write('\n')


def title(name):
    return ' '.join(word.capitalize() for word in name.split('_'))


def trophy_item_model(block):
    with open(os.path.join(ASSETS, 'models', 'item', 'deepslate_pedestal_trophy.json'), encoding='utf-8') as f:
        model = json.load(f)
    model['parent'] = 'trophypedestals:block/' + block
    return model


def item_definition(block):
    trophy_model = 'trophypedestals:item/' + block + '_trophy'
    trophy = {'type': 'minecraft:composite', 'models': [
        {'type': 'minecraft:model', 'model': trophy_model},
        {'type': 'minecraft:select', 'property': 'minecraft:display_context',
         'cases': [{'when': 'gui', 'model': {'type': 'minecraft:special', 'base': trophy_model, 'model': {'type': 'trophypedestals:trophy_icon'}}}],
         'fallback': {'type': 'minecraft:special', 'base': trophy_model, 'model': {'type': 'trophypedestals:trophy_icon', 'spin': True}}}]}
    return {'model': {'type': 'minecraft:condition', 'property': 'minecraft:has_component', 'component': 'trophypedestals:pedestal',
                      'on_true': trophy,
                      'on_false': {'type': 'minecraft:condition', 'property': 'minecraft:has_component', 'component': 'trophypedestals:mob_trophy',
                                   'on_true': trophy, 'on_false': {'type': 'minecraft:model', 'model': 'trophypedestals:block/' + block}}}}


def loot_table(block):
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'trophypedestals:' + block, 'functions': [
        {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['trophypedestals:pedestal', 'trophypedestals:mob_trophy', 'minecraft:item_name']}]}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}


def recipe(block, ingredient):
    return {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'group': 'trophypedestals:pedestal',
            'key': {'G': 'minecraft:gold_ingot', 'S': 'minecraft:' + ingredient}, 'pattern': ['SGS', ' S ', 'SSS'],
            'result': {'count': 1, 'id': 'trophypedestals:' + block}}


# The well's lens, in clean pixel discs (each row's first and last column): its bezel's outside, 12 wide, and its
# glass, 10 wide.
LENS_BEZEL = {2: (6, 9), 3: (4, 11), 4: (3, 12), 5: (3, 12), 6: (2, 13), 7: (2, 13), 8: (2, 13), 9: (2, 13),
              10: (3, 12), 11: (3, 12), 12: (4, 11), 13: (6, 9)}
LENS_GLASS = {3: (6, 9), 4: (5, 10), 5: (4, 11), 6: (3, 12), 7: (3, 12), 8: (3, 12), 9: (3, 12), 10: (4, 11),
              11: (5, 10), 12: (6, 9)}
# The shine on the glass, inside its bevel: a crescent along its upper left, and a glint.
LENS_SHINE = ((6, 5), (7, 5), (5, 6), (5, 7))
LENS_GLINT = (6, 6)
NEIGHBOURS = ((1, 0), (-1, 0), (0, 1), (0, -1))


def in_disc(disc, x, y):
    return y in disc and disc[y][0] <= x <= disc[y][1]


def write_well_imprint():
    """The well's floor and its lens. The floor is an overlay over the material (see-through, so it takes the material's
    colours): its walls in shadow at its upper and left edges and lit at its lower and right. The lens is its own voxels
    (lens_voxels()): its glass (lens.png, dark cool glass bevelled down from the bezel, lit from the upper left, with a
    shine) recessed in a raised gold bezel; and its glow (well_glow.png, drawn by the pedestal's renderer on the glass's
    own faces in its rank's color, fading in and out as it turns on and off), the glass glowing from within, brightest
    in its middle, keeping its bevel and shine as shades of white."""
    folder = os.path.join(ASSETS, 'textures', 'block', 'pedestal')
    dormant = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    glass = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    lit = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

    def put(image, x, y, rgb, alpha):
        image.putpixel((x, y), rgb + (round(alpha * 255),))

    def glow(x, y, level):
        value = round(255 * level)
        put(lit, x, y, (value, value, value), 1.0)

    for y in range(16):
        for x in range(16):
            if x == 0 or y == 0:
                put(dormant, x, y, (0, 0, 0), 0.40)
            elif x == 1 or y == 1:
                put(dormant, x, y, (0, 0, 0), 0.16)
            elif x == 15 or y == 15:
                put(dormant, x, y, (255, 255, 255), 0.14)
            if not in_disc(LENS_GLASS, x, y):
                continue
            light = (x - 7.5) + (y - 7.5)
            # Its rim (where a neighbour is outside the glass) a bevel sloping down into it: in shadow along its upper
            # left, facing away from the light, and catching it along its lower right.
            rim = not all(in_disc(LENS_GLASS, x + dx, y + dy) for dx, dy in NEIGHBOURS)
            middle = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 / 5.0
            if rim and light < 0:
                put(glass, x, y, (10, 12, 18), 1.0)
                glow(x, y, 0.45)
            elif rim:
                put(glass, x, y, (70, 82, 108), 1.0)
                glow(x, y, 0.95)
            else:
                put(glass, x, y, (22, 27, 40), 1.0)
                glow(x, y, 1.0 - 0.3 * min(1.0, middle))
    for x, y in LENS_SHINE:
        put(glass, x, y, (92, 108, 145), 1.0)
        glow(x, y, 1.0)
    put(glass, *LENS_GLINT, (170, 185, 220), 1.0)
    glow(*LENS_GLINT, 1.0)
    dormant.save(os.path.join(folder, 'well_imprint.png'))
    glass.save(os.path.join(folder, 'lens.png'))
    lit.save(os.path.join(folder, 'well_glow.png'))


def write_bezel():
    """The lens's gold bezel (bezel.png, 32x32): aged gold drawn pixel by pixel round the ring, lit from the upper left
    like the glass (bright and glinting there, deepening to shadow round to the lower right), its outer edge a step
    lighter than its inner, with a sparse grain. The left half is its tops, at one texture pixel per lens pixel; the right
    half its sides, each a step or two darker than the top over it."""
    highlight, light, gold, shadow, deep = (0xF2DC92, 0xDDB95E, 0xC29A40, 0x936E2C, 0x634919)
    glint, grain = 0xFFF4C8, 0xAD8834
    ramp = [glint, highlight, light, gold, shadow, deep]
    image = Image.new('RGBA', (32, 32), (0, 0, 0, 0))

    def rgb(color):
        return ((color >> 16) & 255, (color >> 8) & 255, color & 255, 255)

    for y in range(16):
        for x in range(16):
            if not in_disc(LENS_BEZEL, x, y) or in_disc(LENS_GLASS, x, y):
                continue
            dx, dy = x - 7.5, y - 7.5
            # How far round from the light (the upper left, -1) to the shadow (the lower right, 1).
            facing = (dx + dy) / ((dx * dx + dy * dy) ** 0.5 * 2 ** 0.5)
            # In bands, dithered pixel by pixel where one meets the next.
            shade = (facing + 1) / 2 * 3.4
            step = 1 + int(shade) + (1 if shade % 1 > 0.5 and (x + y) % 2 == 0 else 0)
            # The outer edge catches more light than the inner, where the bezel drops to the glass.
            outer = not all(in_disc(LENS_BEZEL, x + ex, y + ey) for ex, ey in NEIGHBOURS)
            if not outer:
                step += 1
            # A glint where the light hits square on, and a few grain pixels on the lit side's plain gold.
            if outer and -0.95 > facing:
                step = 0
            step = max(0, min(len(ramp) - 1, step))
            color = ramp[step]
            if color == gold and (x * 3 + y * 5) % 7 == 0:
                color = grain
            image.putpixel((x, y), rgb(color))
            image.putpixel((x + 16, y), rgb(ramp[min(len(ramp) - 1, step + 2)]))
    image.save(os.path.join(ASSETS, 'textures', 'block', 'pedestal', 'bezel.png'))


# The lens's voxels, in model units: the well's floor (and where its texture's pixels fall on it), how high the glass
# and the bezel round it stand over the floor.
WELL_FLOOR, WELL_LOW, PIXEL = 14.0, 4.0, 0.5
GLASS_TOP, BEZEL_TOP = 14.25, 14.5


def runs(disc, minus=None):
    """Each row's runs of pixels in the disc (and not in minus), as (row, first, last)."""
    out = []
    for y in sorted(disc):
        start = None
        for x in range(17):
            inside = x < 16 and in_disc(disc, x, y) and not (minus and in_disc(minus, x, y))
            if inside and start is None:
                start = x
            elif not inside and start is not None:
                out.append((y, start, x - 1))
                start = None
    return out


def lens_voxels():
    """The lens as its own voxels: one box per row of its glass, in its own texture, recessed in a raised bezel, one box
    per run of the bezel's pixels, in its own gold texture. Each box's top shows its pixels of the texture; its sides their edge."""
    elements = []

    def box(y, x0, x1, top, texture):
        frm = [WELL_LOW + x0 * PIXEL, WELL_FLOOR, WELL_LOW + y * PIXEL]
        to = [WELL_LOW + (x1 + 1) * PIXEL, top, WELL_LOW + (y + 1) * PIXEL]
        uv = [x0, y, x1 + 1, y + 1]
        faces = {'up': {'uv': uv, 'texture': texture}}
        for side in ('north', 'south', 'east', 'west'):
            faces[side] = {'uv': [x0, y, x1 + 1, y + 1] if side in ('north', 'south') else [x0, y, x0 + 1, y + 1], 'texture': texture}
        if texture == '#bezel':
            # The bezel's own texture (bezel.png, see write_bezel()): its tops (the left half) each show their own
            # pixels of the ring, its sides (the right half) a darker copy of their edge's.
            def at(px0, py, px1):
                return [px0 * PIXEL, py * PIXEL, (px1 + 1) * PIXEL, (py + 1) * PIXEL]
            faces['up']['uv'] = at(x0, y, x1)
            for side, (px0, px1) in (('north', (x0, x1)), ('south', (x0, x1)), ('west', (x0, x0)), ('east', (x1, x1))):
                uv = at(px0, y, px1)
                faces[side]['uv'] = [uv[0] + 8, uv[1], uv[2] + 8, uv[3]]
        return {'from': frm, 'to': to, 'faces': faces}

    for y, x0, x1 in runs(LENS_GLASS):
        elements.append(box(y, x0, x1, GLASS_TOP, '#lens'))
    for y, x0, x1 in runs(LENS_BEZEL, LENS_GLASS):
        elements.append(box(y, x0, x1, BEZEL_TOP, '#bezel'))
    return elements


# The column, in model units; the diamond's middle on each side, how far its corners reach from it, how wide its rim's
# bars are, how far the rim and the gem stand out, and how far the gem's corners reach.
COLUMN_LOW, COLUMN_HIGH, COLUMN_BOTTOM, COLUMN_TOP = 4.0, 12.0, 3.5, 12.0
DIAMOND_REACH, RIM_WIDTH, RIM_DEPTH = 2.4, 0.55, 0.35
GEM_REACH, GEM_DEPTH = 0.65, 0.3
ROOT2 = 2 ** 0.5
# The carving's regions of gold_trim.png (drawn by tools/generate_gold_trim.py at four times a block texture's
# resolution, in the square the column hides): bars across and down (a lit edge, a bright stripe, engraved grained gold,
# a shadow edge), the bars' edges (deep shadow), the gem (faceted, with a glint) and its edges (a lit bevel). Each face
# takes its region at its own length; across a bar's narrow width its four detail pixels are fitted in.
BAR_ACROSS_MIDDLE, BAR_ACROSS_V, BAR_DOWN_U, BAR_DOWN_MIDDLE = 7.5, 4.0, 11.0, 8.0
BAR_EDGE_V, GEM_UV, GEM_EDGE_UV = 11.0, [6, 6, 8, 8], [6, 9, 8, 10]


def carving():
    """Each side's diamond: a rim of four bars and a gem, each a box turned 45 degrees about the side's outward axis."""
    # In the gold trim, so it stands out from the column as an inlay whatever its material.
    texture = '#gold'
    middle = (COLUMN_LOW + COLUMN_HIGH) / 2
    height = (COLUMN_BOTTOM + COLUMN_TOP) / 2
    # A square turned 45 degrees reaches its corners at half its side times the square root of two.
    rim_half = DIAMOND_REACH / ROOT2
    gem_half = GEM_REACH / ROOT2
    elements = []
    # Each side: its outward direction, the axis it faces along, where its surface is and which way is out, the axis
    # running across it, and the directions of that axis's low and high ends.
    sides = [('north', 2, COLUMN_LOW, -1, 0, 'west', 'east'), ('south', 2, COLUMN_HIGH, 1, 0, 'west', 'east'),
             ('west', 0, COLUMN_LOW, -1, 2, 'north', 'south'), ('east', 0, COLUMN_HIGH, 1, 2, 'north', 'south')]
    for side, out_axis, surface, out, across, low_end, high_end in sides:
        def box(a0, a1, y0, y1, depth, gem=False):
            """A box over the side, from just inside its surface to depth out, turned about the diamond's middle."""
            frm, to = [0.0, y0, 0.0], [0.0, y1, 0.0]
            frm[across], to[across] = middle + a0, middle + a1
            inner, outer = surface - out * 0.05, surface + out * depth
            frm[out_axis], to[out_axis] = min(inner, outer), max(inner, outer)
            origin = [middle, height, middle]
            origin[out_axis] = surface
            # Each face takes its own region of the trim (see above), so the carving reads as standing proud of the
            # column: lit faces, shadowed edges.
            length, tall = a1 - a0, y1 - y0
            r4 = lambda values: [round(v, 4) for v in values]
            if gem:
                face_uv, long_edge, short_edge = GEM_UV, GEM_EDGE_UV, GEM_EDGE_UV
            elif length >= tall:
                face_uv = r4([BAR_ACROSS_MIDDLE - length / 2, BAR_ACROSS_V, BAR_ACROSS_MIDDLE + length / 2, BAR_ACROSS_V + 1])
                long_edge = r4([BAR_ACROSS_MIDDLE - length / 2, BAR_EDGE_V, BAR_ACROSS_MIDDLE + length / 2, BAR_EDGE_V + 1])
                short_edge = r4([4, BAR_EDGE_V, 5, BAR_EDGE_V + 1])
            else:
                face_uv = r4([BAR_DOWN_U, BAR_DOWN_MIDDLE - tall / 2, BAR_DOWN_U + 1, BAR_DOWN_MIDDLE + tall / 2])
                long_edge = r4([4, BAR_EDGE_V, 5, BAR_EDGE_V + 1])
                short_edge = r4([4, BAR_EDGE_V, 4 + tall, BAR_EDGE_V + 1])
            faces = {side: {'uv': face_uv, 'texture': texture}}
            for edge in ('up', 'down'):
                faces[edge] = {'uv': long_edge, 'texture': texture}
            for edge in (low_end, high_end):
                faces[edge] = {'uv': short_edge, 'texture': texture}
            return {'from': [round(v, 4) for v in frm], 'to': [round(v, 4) for v in to],
                    'rotation': {'origin': [round(v, 4) for v in origin], 'axis': 'x' if out_axis == 0 else 'z', 'angle': 45},
                    'faces': faces}
        r, w = rim_half, RIM_WIDTH
        # The rim: its top and bottom bars run its full width, its sides fit between them.
        elements.append(box(-r, r, height + r - w, height + r, RIM_DEPTH))
        elements.append(box(-r, r, height - r, height - r + w, RIM_DEPTH))
        elements.append(box(-r, -r + w, height - r + w, height + r - w, RIM_DEPTH))
        elements.append(box(r - w, r, height - r + w, height + r - w, RIM_DEPTH))
        elements.append(box(-gem_half, gem_half, height - gem_half, height + gem_half, GEM_DEPTH, gem=True))
    return elements


# The flutes' shading, drawn at twice a block texture's resolution so it can fade smoothly: across each groove (eight
# pixels, four grooves to a side) its lit lip, then its shadow, deepest at the lip's foot and fading across the hollow;
# down the column the bands at its top and bottom (their lit edge, then an undercut's shadow), and a soft shadow where
# the grooves run into them. Positive is light (white), negative shade (black), each as the overlay's alpha.
FLUTE_ACROSS = [0.20, 0.09, -0.30, -0.23, -0.16, -0.10, -0.05, -0.015]
FLUTE_TOP_BAND = [0.24, 0.10, -0.08, -0.26]
FLUTE_BOTTOM_BAND = [0.20, 0.07, -0.10, -0.28]
FLUTE_UNDER_BAND = [-0.12, -0.07, -0.03]


def write_column_flutes():
    """The column's flutes, as see-through shading over the material (so they take its colours and blend with its own
    texture): bands at the top and bottom, and between them four rounded grooves, each lit along its lip and shaded
    into its hollow, a little deeper where it meets the bands."""
    size = 32
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    band = len(FLUTE_TOP_BAND)
    for y in range(size):
        for x in range(size):
            if y < band:
                shade = FLUTE_TOP_BAND[y]
            elif y >= size - band:
                shade = FLUTE_BOTTOM_BAND[y - (size - band)]
            else:
                shade = FLUTE_ACROSS[x % len(FLUTE_ACROSS)]
                # Under the top band and over the bottom one, the grooves sink into shadow.
                from_top, from_bottom = y - band, (size - band - 1) - y
                if from_top < len(FLUTE_UNDER_BAND):
                    shade += FLUTE_UNDER_BAND[from_top]
                if from_bottom < len(FLUTE_UNDER_BAND):
                    shade += FLUTE_UNDER_BAND[from_bottom] * 0.7
            shade = max(-0.6, min(0.6, shade))
            if abs(shade) < 0.005:
                continue
            rgb = (255, 255, 255) if shade > 0 else (0, 0, 0)
            image.putpixel((x, y), rgb + (round(abs(shade) * 255),))
    image.save(os.path.join(ASSETS, 'textures', 'block', 'pedestal', 'column_flutes.png'))


def write_fluted_template():
    """The chiseled template with the flutes over the column's four sides, on a face just outside each (under the
    carving, which stands out further)."""
    with open(os.path.join(ASSETS, 'models', 'block', 'pedestal_template_chiseled.json'), encoding='utf-8') as f:
        template = json.load(f)
    template['textures']['column_flutes'] = 'trophypedestals:block/pedestal/column_flutes'
    out = 0.01
    flutes = {'from': [COLUMN_LOW - out, COLUMN_BOTTOM, COLUMN_LOW - out], 'to': [COLUMN_HIGH + out, COLUMN_TOP, COLUMN_HIGH + out],
              'faces': {side: {'uv': [0, 0, 16, 16], 'texture': '#column_flutes'} for side in ('north', 'south', 'east', 'west')}}
    column = next(e for e in template['elements'] if e['faces']['north']['texture'] == '#column')
    at = template['elements'].index(column)
    template['elements'].insert(at + 1, flutes)
    write(os.path.join(ASSETS, 'models', 'block', 'pedestal_template_fluted.json'), template)


def write_chiseled_template():
    """The pedestal template with the diamond carved on each side of its column."""
    with open(os.path.join(ASSETS, 'models', 'block', 'pedestal_template.json'), encoding='utf-8') as f:
        template = json.load(f)
    column = next(e for e in template['elements'] if e['faces']['north']['texture'] == '#column')
    at = template['elements'].index(column)
    template['elements'][at + 1:at + 1] = carving()
    # The lens's voxels, over the well's floor.
    template['textures']['lens'] = 'trophypedestals:block/pedestal/lens'
    template['textures']['bezel'] = 'trophypedestals:block/pedestal/bezel'
    template['elements'].extend(lens_voxels())
    write(os.path.join(ASSETS, 'models', 'block', 'pedestal_template_chiseled.json'), template)


def main():
    write_well_imprint()
    write_bezel()
    write_chiseled_template()
    write_column_flutes()
    write_fluted_template()
    # The custom pedestal (a data pack's material, any stone or wood) takes either tool.
    # (The trophy cabinets and shelves, tools/generate_cabinet.py, are wood: an axe.)
    pickaxe, axe = ['trophypedestals:custom_pedestal'], ['trophypedestals:custom_pedestal'] + [f'trophypedestals:{w}_trophy_{k}' for w in ('oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'pale_oak', 'bamboo', 'crimson', 'warped') for k in ('cabinet', 'shelf')]
    names = {}
    for name in CUSTOM:
        pickaxe.append('trophypedestals:' + name + '_pedestal')
    for name, material in MATERIALS.items():
        block = name + '_pedestal'
        (axe if material['wood'] else pickaxe).append('trophypedestals:' + block)
        names['block.trophypedestals.' + block] = title(name) + ' Pedestal'
        textures = {key: 'minecraft:block/' + material[key] for key in ('side', 'top', 'bottom', 'column')}
        textures['gold'] = 'trophypedestals:block/pedestal/gold_trim'
        # The well inside the cap in the material's own top, so the inside matches (the lit lens covers it when shown).
        textures['well'] = textures['top']
        write(os.path.join(ASSETS, 'models', 'block', block + '.json'), {'parent': 'trophypedestals:block/pedestal_template_fluted', 'textures': textures})
        model = 'trophypedestals:block/' + block
        write(os.path.join(ASSETS, 'blockstates', block + '.json'), {'variants': {'lit=false': {'model': model}, 'lit=true': {'model': model}}})
        write(os.path.join(ASSETS, 'models', 'item', block + '_trophy.json'), trophy_item_model(block))
        write(os.path.join(ASSETS, 'items', block + '.json'), item_definition(block))
        write(os.path.join(DATA, 'trophypedestals', 'loot_table', 'blocks', block + '.json'), loot_table(block))
        write(os.path.join(DATA, 'trophypedestals', 'recipe', block + '.json'), recipe(block, material['ingredient']))
    write(os.path.join(DATA, 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'), {'values': pickaxe})
    write(os.path.join(DATA, 'minecraft', 'tags', 'block', 'mineable', 'axe.json'), {'values': axe})

    # The names, after the hand-made ones' in the English lang file (kept in its order, others' entries untouched).
    lang_path = os.path.join(ASSETS, 'lang', 'en_us.json')
    with open(lang_path, encoding='utf-8') as f:
        text = f.read()
    lang = json.loads(text)
    # The keys with a blank line before them (the file's groups), kept so.
    lines = text.split('\n')
    spaced = {line.strip().split('"')[1] for before, line in zip(lines, lines[1:]) if not before.strip() and line.strip().startswith('"')}
    merged = {}
    anchor = 'block.trophypedestals.' + CUSTOM[-1] + '_pedestal'
    for key, value in lang.items():
        if key in names:
            continue
        merged[key] = value
        if key == anchor:
            merged.update(names)
    # (At the end if the last hand-made one's name isn't there.)
    for key, value in names.items():
        merged.setdefault(key, value)
    out = []
    for line in json.dumps(merged, indent=2, ensure_ascii=False).split('\n'):
        if line.startswith('  "') and line.strip().split('"')[1] in spaced:
            out.append('')
        out.append(line)
    with open(lang_path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(out) + '\n')
    print(f'{len(MATERIALS)} generated pedestals, {len(pickaxe)} pickaxe and {len(axe)} axe')


if __name__ == '__main__':
    main()
