"""Writes the trophy cabinets and trophy shelves, one of each in every wood: their block models, blockstates, item models,
loot tables, recipes and English names.

A cabinet is a shelf two blocks wide and half a block deep with a back board, a top and a post at each end; a shelf is one block wide, hung on a
wall: a board half a block deep held up by two brackets, with a short rim along its back. Both are trimmed in old
gold: the cabinet with one connected frame over the whole front edges of its opening, the shelf with a lip
along its board and a cap on its rim. Each wood's models are its
template's with that wood's planks.

The models face north (their open front at z = 0); the blockstates turn them. Looking at a cabinet's front, its left
half is the one placed and its right half beside it. Run it from this folder (and tools/generate_pedestals.py after it,
which writes the axe tag that lists these).
"""
import json
import os

HERE = os.path.dirname(__file__)
ASSETS = os.path.join(HERE, '..', 'common', 'src', 'main', 'resources', 'assets', 'trophypedestals')
DATA = os.path.join(HERE, '..', 'common', 'src', 'main', 'resources', 'data', 'trophypedestals')
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'pale_oak', 'bamboo', 'crimson', 'warped']
GOLD = 'trophypedestals:block/display_trim'
# The display trim texture (new gold, smooth), sixteen pixels, one to a model pixel: RAIL, its top
# two rows, twisted like a rope, laid along a rail two pixels high; CAP, its top row alone, for a rail one pixel high;
# POST, its first two columns, twisted the same way, laid down a post two pixels wide; CORNER, a corner cap's face,
# bevelled round a rivet.
RAIL = [0, 0, 16, 2]
CAP = [0, 0, 16, 1]
POST = [0, 4, 2, 16]
CORNER = [2, 4, 5, 7]


def element(frm, to, texture, front_uv=None):
    """A box. A wooden one takes its planks on every face. A gold one shows its pattern (front_uv, as big as its front)
    on its front and back, and carries it on round its edges onto its other faces, folded over each edge as a strip of
    paper would be: the pixels along each edge of the front go on round the corner, a texture pixel to a model pixel.
    (A block model lays a front's texture from its right, +x, edge; its top's and bottom's from their -x edge; its
    ends' from their back. So each face's uv runs the way that lines it up with the front.)"""
    faces = {}
    if texture != '#gold':
        for side in ('north', 'south', 'east', 'west', 'up', 'down'):
            faces[side] = {'texture': texture}
        return {'from': frm, 'to': to, 'faces': faces}
    u0, v0, u1, v1 = front_uv
    deep = to[2] - frm[2]
    faces['north'] = {'uv': front_uv, 'texture': texture}
    faces['south'] = {'uv': [u1, v0, u0, v1], 'texture': texture}
    faces['up'] = {'uv': [u1, v0, u0, v0 + deep], 'texture': texture}
    faces['down'] = {'uv': [u1, v1 - deep, u0, v1], 'texture': texture}
    faces['east'] = {'uv': [u0 + deep, v0, u0, v1], 'texture': texture}
    faces['west'] = {'uv': [u1, v0, u1 - deep, v1], 'texture': texture}
    return {'from': frm, 'to': to, 'faces': faces}


def corner(x, y, z):
    """A corner cap: a little gold block three pixels each way, its lower left front corner at (x, y, z), wrapped round the
    corner it guards (half a pixel proud of each face it meets), each face bevelled round a rivet."""
    return [element([x, y, z], [x + 3, y + 3, z + 3], '#gold', CORNER)]


def bare(elements):
    """The wooden elements alone."""
    return [e for e in elements if all(face['texture'] != '#gold' for face in e['faces'].values())]


def cabinet_half(outer, up=False, down=False, gold=True):
    """One half (outer: the x of its outer end, 0 or 16), half a block deep like the shelf (z 8-16, its back against the
    wall): the shelf, a thin back, the top and its post, and in front of them its share of the gold frame round the
    opening, a pixel proud and covering their whole front edges: a rail over the shelf's front (y 0-2) and one over the
    top's (y 14-16), each the half's full width so they run on unbroken into the other half's, and its post's front (2
    wide), with a corner cap on each of its outer corners.

    Stacked on another cabinet (down) or under one (up), it's joined to it: the boards they meet at make one divider, a
    plank of their wood (a wooden edge in place of the gold rail), the posts' gold runs on through, and there's no corner
    cap there. Without gold (its trim made of another block, which the renderer draws), it's the wood alone."""
    post = [14, 16] if outer == 16 else [0, 2]
    cap_x = 13.5 if outer == 16 else -0.5
    elements = [
        element([0, 0, 8], [16, 2, 16], '#wood'),
        element([0, 2, 15], [16, 14, 16], '#wood'),
        element([0, 14, 8], [16, 16, 16], '#wood'),
        element([post[0], 2, 8], [post[1], 14, 15], '#wood'),
    ]
    # Where it's joined, the board's front edge is wood (up to the post, whose trim runs on past it); elsewhere it's a
    # gold rail.
    inner = [0, 14] if outer == 16 else [2, 16]
    for y0, joined in ((0, down), (14, up)):
        if joined:
            elements.append(element([inner[0], y0, 7], [inner[1], y0 + 2, 8], '#wood'))
        elif gold:
            elements.append(element([0, y0, 7], [16, y0 + 2, 8], '#gold', RAIL))
    if gold:
        bottom, top = (0 if down else 2), (16 if up else 14)
        # (In pieces of up to eight pixels, so a joined post's rope keeps its twist without stretching.)
        for y0 in range(bottom, top, 8):
            y1 = min(top, y0 + 8)
            elements.append(element([post[0], y0, 7], [post[1], y1, 8], '#gold', [POST[0], POST[1], POST[2], POST[1] + y1 - y0]))
        if not down:
            elements += corner(cap_x, -0.5, 6.5)
        if not up:
            elements += corner(cap_x, 13.5, 6.5)
    return elements


def shelf(left=False, right=False):
    """The wall shelf: a board half a block deep, centred up the block (y 7-9) and set against the wall (z 8-16), its front
    under a gold lip; a short rim along its back, capped in gold, for the trophies to stand against; and under each end
    a bracket stepping out from the wall up to the board, like a quarter arch. Joined to a shelf on its left (+x, as seen
    from its front) or its right (-x), it has no corner cap on that end: the joined shelves' lips and rims run on as one."""
    elements = [
        element([0, 7, 8], [16, 9, 16], '#wood'),
        element([0, 7, 7], [16, 9, 8], '#gold', RAIL),
        element([0, 9, 15], [16, 11, 16], '#wood'),
        element([0, 11, 14.5], [16, 12, 16], '#gold', CAP),
    ]
    # The brackets: from the wall out under the board, each step shorter, so their undersides curve up to its front.
    for x0, x1 in ((1, 3), (13, 15)):
        for z0, y0 in ((14, 1), (12, 3), (10, 5), (9, 6)):
            elements.append(element([x0, y0, z0], [x1, 7, 16 if z0 == 14 else z0 + 2], '#wood'))
    # A corner cap on each end of the lip that isn't joined.
    if not right:
        elements += corner(-0.5, 6.5, 6.5)
    if not left:
        elements += corner(13.5, 6.5, 6.5)
    return elements


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def remove(path):
    if os.path.exists(path):
        os.remove(path)


FACINGS = (('north', 0), ('east', 90), ('south', 180), ('west', 270))
# A cabinet half's joins, (joined above, joined below), and the name its models take for each.
JOINS = {(False, False): '', (True, False): '_up', (False, True): '_down', (True, True): '_middle'}
# A shelf's joins, (joined on its left, joined on its right), and the name its models take for each.
SIDES = {(False, False): '', (True, False): '_left', (False, True): '_right', (True, True): '_both'}


def turns(parts):
    """A blockstate's variants: each facing, turned, with each of these (its other properties, its model)."""
    variants = {}
    for facing, y in FACINGS:
        for key, model in parts:
            variant = {'model': model}
            if y:
                variant['y'] = y
            variants[f'facing={facing}{key}'] = variant
    return {'variants': variants}


def title(name):
    return ' '.join(word.capitalize() for word in name.split('_'))


def main():
    models = os.path.join(ASSETS, 'models')
    gold = {'gold': GOLD}
    # The templates (#wood filled in by each wood's model): each half for each way it can be joined, with its gold trim
    # and without it (for a trim of another block, which the renderer draws from the gold elements of the trimmed
    # template), and the shelf with and without.
    for part, outer in (('left', 16), ('right', 0)):
        for (up, down), join in JOINS.items():
            write(os.path.join(models, 'block', f'trophy_cabinet_{part}{join}.json'), {'textures': gold, 'elements': cabinet_half(outer, up, down)})
            write(os.path.join(models, 'block', f'trophy_cabinet_{part}{join}_bare.json'), {'elements': cabinet_half(outer, up, down, gold=False)})
    # (The shelf's item is its block model, so it takes the block views.)
    for (left, right), side in SIDES.items():
        write(os.path.join(models, 'block', f'trophy_shelf{side}.json'), {'parent': 'minecraft:block/block', 'textures': gold, 'elements': shelf(left, right)})
        write(os.path.join(models, 'block', f'trophy_shelf{side}_bare.json'), {'parent': 'minecraft:block/block', 'elements': bare(shelf(left, right))})
    # Both halves side by side, centred on the block (x -8 to 24): a model's elements must stay within -16 to 32, which
    # the left half's corner caps (half a pixel proud of the cabinet) would leave if it were moved a whole block over.
    both = []
    for shift, half in ((8, cabinet_half(16)), (-8, cabinet_half(0))):
        for e in half:
            e = json.loads(json.dumps(e))
            e['from'][0] += shift
            e['to'][0] += shift
            both.append(e)
    cabinet_views = {
            'gui': {'rotation': [30, 225, 0], 'translation': [-1.5, -1, 0], 'scale': [0.42, 0.42, 0.42]},
            'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.3, 0.3, 0.3]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.3, 0.3, 0.3]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.3, 0.3, 0.3]},
    }
    # The cabinet's item, trimmed (gold, or its trim's block as the gold stands in for it) and untrimmed.
    write(os.path.join(models, 'item', 'trophy_cabinet.json'), {'parent': 'minecraft:block/block', 'textures': gold, 'elements': both,
                                                                 'display': cabinet_views})
    write(os.path.join(models, 'item', 'trophy_cabinet_bare.json'), {'parent': 'minecraft:block/block', 'elements': bare(both),
                                                                      'display': cabinet_views})
    # (The first cabinet, before there was one in every wood.)
    for path in (os.path.join(ASSETS, 'blockstates', 'trophy_cabinet.json'), os.path.join(ASSETS, 'items', 'trophy_cabinet.json'),
                 os.path.join(DATA, 'loot_table', 'blocks', 'trophy_cabinet.json'), os.path.join(DATA, 'recipe', 'trophy_cabinet.json')):
        remove(path)

    names = {}
    for wood in WOODS:
        textures = {'wood': f'minecraft:block/{wood}_planks', 'particle': f'minecraft:block/{wood}_planks'}
        cabinet, rack = f'{wood}_trophy_cabinet', f'{wood}_trophy_shelf'
        cabinet_parts = []
        for part in ('left', 'right'):
            for (up, down), join in JOINS.items():
                for trim, suffix in (('true', ''), ('false', '_bare')):
                    name = f'{cabinet}_{part}{join}{suffix}'
                    write(os.path.join(models, 'block', f'{name}.json'), {'parent': f'trophypedestals:block/trophy_cabinet_{part}{join}{suffix}', 'textures': textures})
                    cabinet_parts.append((f',part={part},up={str(up).lower()},down={str(down).lower()},gold_trim={trim}', f'trophypedestals:block/{name}'))
        shelf_parts = []
        for (left, right), side in SIDES.items():
            for trim, suffix in (('true', ''), ('false', '_bare')):
                name = f'{rack}{side}{suffix}'
                write(os.path.join(models, 'block', f'{name}.json'), {'parent': f'trophypedestals:block/trophy_shelf{side}{suffix}', 'textures': textures})
                shelf_parts.append((f',gold_trim={trim},left={str(left).lower()},right={str(right).lower()}', f'trophypedestals:block/{name}'))
        write(os.path.join(models, 'item', f'{cabinet}.json'), {'parent': 'trophypedestals:item/trophy_cabinet', 'textures': textures})
        write(os.path.join(models, 'item', f'{cabinet}_bare.json'), {'parent': 'trophypedestals:item/trophy_cabinet_bare', 'textures': textures})
        write(os.path.join(ASSETS, 'blockstates', f'{cabinet}.json'), turns(cabinet_parts))
        write(os.path.join(ASSETS, 'blockstates', f'{rack}.json'), turns(shelf_parts))
        # Plain wood, unless the item carries a trim (shown as gold, as a trimmed one's item is).
        for block in (cabinet, rack):
            model = f'trophypedestals:item/{cabinet}' if block == cabinet else f'trophypedestals:block/{rack}'
            write(os.path.join(ASSETS, 'items', f'{block}.json'), {'model': {
                'type': 'minecraft:condition', 'property': 'minecraft:has_component', 'component': 'trophypedestals:trim_material',
                'on_true': {'type': 'minecraft:model', 'model': model},
                'on_false': {'type': 'minecraft:model', 'model': model + '_bare'}}})
        # A cabinet drops once, from its left half; a shelf drops itself.
        conditions = [{'condition': 'minecraft:survives_explosion'}]
        # (Its trim's material goes with it.)
        keep = [{'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['trophypedestals:trim_material']}]
        write(os.path.join(DATA, 'loot_table', 'blocks', f'{cabinet}.json'), {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'trophypedestals:{cabinet}', 'functions': keep}],
                       'conditions': conditions + [{'condition': 'minecraft:block_state_property', 'block': f'trophypedestals:{cabinet}',
                                                    'properties': {'part': 'left'}}]}]})
        write(os.path.join(DATA, 'loot_table', 'blocks', f'{rack}.json'), {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'trophypedestals:{rack}', 'functions': keep}], 'conditions': conditions}]})
        write(os.path.join(DATA, 'recipe', f'{cabinet}.json'), {
            'type': 'minecraft:crafting_shaped', 'category': 'building', 'group': 'trophy_cabinet',
            'key': {'P': f'minecraft:{wood}_planks', 'G': 'minecraft:gold_ingot', 'S': f'minecraft:{wood}_slab'},
            'pattern': ['PPP', 'GSG', 'PPP'],
            'result': {'count': 1, 'id': f'trophypedestals:{cabinet}'}})
        write(os.path.join(DATA, 'recipe', f'{rack}.json'), {
            'type': 'minecraft:crafting_shaped', 'category': 'building', 'group': 'trophy_shelf',
            'key': {'G': 'minecraft:gold_nugget', 'S': f'minecraft:{wood}_slab'},
            'pattern': ['G G', 'SSS'],
            'result': {'count': 2, 'id': f'trophypedestals:{rack}'}})
        names[f'block.trophypedestals.{cabinet}'] = f'{title(wood)} Trophy Cabinet'
        names[f'block.trophypedestals.{rack}'] = f'{title(wood)} Trophy Shelf'

    # The names, after the trophies' own name in the English lang file, written in place so its other lines (and its
    # blank lines between groups) stay as they are.
    lang_path = os.path.join(ASSETS, 'lang', 'en_us.json')
    with open(lang_path, encoding='utf-8') as f:
        lines = f.read().split('\n')
    lines = [line for line in lines if not (line.startswith('  "block.trophypedestals.') and ('trophy_cabinet"' in line or 'trophy_shelf"' in line))]
    anchor = next(i for i, line in enumerate(lines) if line.startswith('  "trophypedestals.pedestal.trophy_name"'))
    new = [f'  {json.dumps(key)}: {json.dumps(value)},' for key, value in names.items()]
    lines[anchor + 1:anchor + 1] = new
    with open(lang_path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines))
    print(f'wrote {len(WOODS)} trophy cabinets and shelves')


main()
