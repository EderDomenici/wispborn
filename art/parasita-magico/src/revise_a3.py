"""Rebuild A3 geometry and pixel textures in the editable Blockbench project.

Run with /usr/bin/python3 (Pillow). Idempotent; preserves the existing rig and
unmodified animation channels. Exports mirror Blockbench's Bedrock X conversion.
"""
import base64
import copy
import io
import json
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def uid(name):
    return str(uuid5(NAMESPACE_URL, 'wispborn/a3/' + name))


def write_json(name, value):
    (ROOT / name).write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')


def main():
    model = json.loads((ROOT / 'wisp.bbmodel').read_text())
    groups = {g['name']: g for g in model['groups']}
    removed = {g['uuid'] for g in model['groups']
               if g['name'].startswith('ray_') or g['name'].startswith('halo_')}
    core_id, rays_id = groups['core']['uuid'], groups['rays']['uuid']
    removed_elements = set()

    def clear(nodes):
        kept = []
        for node in nodes:
            if isinstance(node, str):
                kept.append(node)
                continue
            if node['uuid'] in removed or node['uuid'] in (core_id, rays_id):
                def collect(children):
                    for child in children:
                        if isinstance(child, str):
                            removed_elements.add(child)
                        else:
                            collect(child['children'])
                collect(node['children'])
                if node['uuid'] in removed:
                    continue
                node['children'] = []
            else:
                node['children'] = clear(node['children'])
            kept.append(node)
        return kept

    model['outliner'] = clear(model['outliner'])
    model['elements'] = [e for e in model['elements'] if e['uuid'] not in removed_elements]
    model['groups'] = [g for g in model['groups'] if g['uuid'] not in removed]

    def node_for(identifier, nodes=None):
        for node in model['outliner'] if nodes is None else nodes:
            if isinstance(node, dict):
                if node['uuid'] == identifier:
                    return node
                found = node_for(identifier, node['children'])
                if found:
                    return found
        return None

    def cube(name, lower, upper, uv):
        element = {'name': name, 'type': 'cube', 'uuid': uid(name),
                   'box_uv': False, 'export': True, 'from': lower, 'to': upper,
                   'origin': [0, 12, 0], 'autouv': 0, 'color': 0,
                   'faces': {face: {'uv': uv, 'texture': 0}
                             for face in ('north', 'east', 'south', 'west', 'up', 'down')}}
        model['elements'].append(element)
        if name.startswith('core_'):
            x, y, z = lower[0], lower[1]-12, lower[2]
            X, Y, Z = upper[0], upper[1]-12, upper[2]
            # Planar UVs span the whole nucleus, preventing a framed tile per cap.
            spans = {'north': (x, y, X, Y), 'south': (x, y, X, Y),
                     'east': (z, y, Z, Y), 'west': (z, y, Z, Y),
                     'up': (x, z, X, Z), 'down': (x, z, X, Z)}
            for face, span in spans.items():
                element['faces'][face]['uv'] = [(v+2.5)*2 for v in span]
        return element['uuid']

    # Half-pixel voxel steps round the nucleus while preserving its 5px diameter.
    core_nodes = node_for(core_id)['children']
    step, radius = 0.5, 2.5
    runs = []
    for iy in range(10):
        y = -radius + (iy + 0.5) * step
        for iz in range(10):
            z = -radius + (iz + 0.5) * step
            occupied = [ix for ix in range(10)
                        if (-radius + (ix + 0.5) * step)**2 + y*y + z*z <= radius*radius]
            if not occupied:
                continue
            lower = [-radius + min(occupied)*step, 12-radius+iy*step, -radius+iz*step]
            upper = [-radius + (max(occupied)+1)*step, lower[1]+step, lower[2]+step]
            runs.append([lower, upper])
    # Merge adjacent runs without changing the surface or overlapping volumes.
    for axis in (2, 1):
        merged = []
        for lower, upper in runs:
            match = next((box for box in merged
                          if box[1][axis] == lower[axis]
                          and all(box[0][i] == lower[i] and box[1][i] == upper[i]
                                  for i in range(3) if i != axis)), None)
            if match is None:
                merged.append([lower[:], upper[:]])
            else:
                match[1][axis] = upper[axis]
        runs = merged
    for index, (lower, upper) in enumerate(runs):
        core_nodes.append(cube(f'core_slice_{index}', lower, upper, [0, 0, 10, 10]))

    for name, size, rotation, uv in (
            ('halo_inner', 7, [0, 0, 0], [0, 32, 16, 48]),
            ('halo_outer', 9, [0, 45, 0], [16, 32, 32, 48])):
        group = copy.deepcopy(groups['rays'])
        group.update(name=name, uuid=uid(name), rotation=rotation, children=[], selected=False)
        model['groups'].append(group)
        element_id = cube(name + '_shell', [-size/2, 12-size/2, -size/2],
                          [size/2, 12+size/2, size/2], uv)
        node_for(rays_id)['children'].append({'uuid': group['uuid'], 'isOpen': True, 'children': [element_id]})

    # Rebuild only changed atlas regions; satellite and wake texels stay intact.
    image = Image.open(ROOT / 'wisp.png').convert('RGBA')
    glow = Image.open(ROOT / 'wisp_glowmask.png').convert('RGBA')
    for target in (image, glow):
        for y in range(26, 49):
            for x in range(32):
                target.putpixel((x, y), (0, 0, 0, 0))
    for y in range(10):
        for x in range(10):
            edge = min(x, y, 9-x, 9-y)
            color = [(95, 230, 221, 255), (166, 250, 240, 255),
                     (212, 255, 249, 255), (244, 255, 253, 255)][min(edge, 3)]
            image.putpixel((x, y), color)
            glow.putpixel((x, y), color)
    for offset, outer in ((0, False), (16, True)):
        for y in range(16):
            for x in range(16):
                radius = (((x-7.5)/7.5)**2 + ((y-7.5)/7.5)**2)**0.5
                # Rounded stepped rim, open face center, no opaque cube corners.
                alpha = (0 if radius > 1.14 else 20 if radius > 1.02 else
                         86 if radius > .82 else 42 if radius > .62 else 9)
                if outer:
                    alpha = round(alpha * .48)
                color = (59, 231, 187, alpha) if outer else (102, 246, 240, alpha)
                image.putpixel((offset+x, 32+y), color)
                glow.putpixel((offset+x, 32+y), color[:3] + (round(alpha * (.5 if outer else .85)),))
    image.save(ROOT / 'wisp.png')
    glow.save(ROOT / 'wisp_glowmask.png')
    buffer = io.BytesIO()
    image.save(buffer, format='PNG')
    model['textures'][0]['source'] = 'data:image/png;base64,' + base64.b64encode(buffer.getvalue()).decode()
    model['textures'][0]['render_mode'] = 'default'
    model['textures'][0]['force_translucent'] = True

    for animation in model['animations']:
        valid_ids = {g['uuid'] for g in model['groups']}
        animation['animators'] = {k: v for k, v in animation['animators'].items()
                                 if k in valid_ids and v.get('keyframes')}
        if animation['name'] == 'animation.wisp.idle':
            animator = animation['animators'][rays_id]
            animator['keyframes'] = [k for k in animator['keyframes'] if k['channel'] != 'scale']
            for time, scale in ((0, 1), (1, 1.04), (2, 1.08), (3, 1.04), (4, 1)):
                animator['keyframes'].append({'channel': 'scale', 'time': time,
                    'data_points': [{axis: str(scale) for axis in 'xyz'}],
                    'uuid': uid(f'idle-halo-scale-{time}'), 'interpolation': 'linear',
                    'uniform': True, 'color': -1})
        if animation['name'] == 'animation.wisp.walking':
            for animator in animation['animators'].values():
                if animator['name'] == 'trail_1':
                    for key in animator['keyframes']:
                        if key['channel'] == 'position' and key['time'] in (.5, .95):
                            key['data_points'][0]['x'] = '0'
        if animation['name'] == 'animation.wisp.possess':
            # The old source had off-grid times; retain the delivered A1 timing.
            for animator in animation['animators'].values():
                for key in animator['keyframes']:
                    if animator['name'] == 'core' and key['time'] == .48:
                        key['time'] = .5
                    if animator['name'].startswith('trail_'):
                        key['time'] = {.53: .55, .56: .55, .59: .6, .62: .6}.get(key['time'], key['time'])

    # Export the supported cuboid-only subset, using existing Bedrock conventions.
    groups_by_id = {g['uuid']: g for g in model['groups']}
    elements = {e['uuid']: e for e in model['elements']}
    bones = []

    def export_tree(nodes, parent=None):
        for node in nodes:
            if isinstance(node, str):
                continue
            group = groups_by_id[node['uuid']]
            p = group['origin']
            bone = {'name': group['name'], 'pivot': [-p[0], p[1], p[2]]}
            if parent:
                bone['parent'] = parent
            r = group.get('rotation', [0, 0, 0])
            if any(r):
                bone['rotation'] = [-r[0], -r[1], r[2]]
            cubes = []
            for child in node['children']:
                if not isinstance(child, str):
                    continue
                e = elements[child]
                assert not any(e.get('rotation', [])), 'Rotated cubes require explicit export support'
                lo, hi = e['from'], e['to']
                uv = {}
                for face, data in e['faces'].items():
                    a, b, c, d = data['uv']
                    uv[face] = {'uv': [c, d], 'uv_size': [a-c, b-d]} if face in ('up', 'down') else {
                        'uv': [a, b], 'uv_size': [c-a, d-b]}
                cubes.append({'origin': [-hi[0], lo[1], lo[2]],
                              'size': [round(hi[i]-lo[i], 10) for i in range(3)], 'uv': uv})
            if cubes:
                bone['cubes'] = cubes
            bones.append(bone)
            export_tree(node['children'], group['name'])

    export_tree(model['outliner'])
    geo = json.loads((ROOT / 'wisp.geo.json').read_text())
    geo['minecraft:geometry'][0]['bones'] = bones
    exported = {'format_version': '1.8.0', 'animations': {}}
    for animation in model['animations']:
        clip = {'animation_length': animation['length'], 'bones': {}}
        if animation['loop'] == 'loop':
            clip['loop'] = True
        for animator in animation['animators'].values():
            if animator.get('type') != 'bone':
                continue
            channels = {}
            for key in sorted(animator['keyframes'], key=lambda k: k['time']):
                channel = key['channel']
                assert key['interpolation'] == 'linear'
                point = key['data_points'][0]
                value = [float(point[axis]) for axis in 'xyz']
                if channel == 'rotation':
                    value[0], value[1] = -value[0], -value[1]
                if channel == 'position':
                    value[0] = -value[0]
                channels.setdefault(channel, {})[str(float(key['time']))] = value
            for channel, keys in channels.items():
                if len(keys) == 1:
                    channels[channel] = next(iter(keys.values()))
            clip['bones'][animator['name']] = channels
        exported['animations'][animation['name']] = clip
    write_json('wisp.bbmodel', model)
    write_json('wisp.geo.json', geo)
    write_json('wisp.animation.json', exported)
    print(f'A3 exported: {len(bones)} bones, {len(model["elements"])} cubes, 64x64 RGBA textures.')


if __name__ == '__main__':
    main()
