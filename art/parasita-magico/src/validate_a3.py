"""Validate the A3 editable project, exported rig, animation and texture contracts."""
import base64
import io
import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def main():
    model = json.loads((ROOT/'wisp.bbmodel').read_text())
    geo = json.loads((ROOT/'wisp.geo.json').read_text())['minecraft:geometry'][0]
    animations = json.loads((ROOT/'wisp.animation.json').read_text())['animations']
    groups = {g['uuid']: g for g in model['groups']}
    bones = {b['name']: b for b in geo['bones']}
    required = {'root','body','core','rays','orbit','wake', 'halo_inner','halo_outer'}
    required |= {f'{prefix}_{i}' for prefix in ('satellite','trail') for i in range(1,5)}
    assert set(bones) == required == {g['name'] for g in groups.values()}
    assert len(bones) == len(geo['bones']) == len(groups)
    elements = {e['uuid']: e for e in model['elements']}
    seen = set()

    def tree(nodes, parent=None):
        for node in nodes:
            if isinstance(node,str):
                assert node in elements and node not in seen
                seen.add(node)
            else:
                group = groups[node['uuid']]
                bone = bones[group['name']]
                assert bone.get('parent') == parent
                assert bone['pivot'] == [-group['origin'][0], *group['origin'][1:]]
                tree(node['children'],group['name'])
    tree(model['outliner'])
    assert seen == set(elements)
    assert bones['rays']['pivot'] == [0,12,0]
    for name, size in [('halo_inner',7),('halo_outer',9)]:
        assert bones[name]['parent'] == 'rays'
        assert len(bones[name]['cubes']) == 1
        assert bones[name]['cubes'][0]['size'] == [size]*3
    assert bones['halo_outer']['rotation'] == [0,-45,0]
    for bone in bones.values():
        for cube in bone.get('cubes',[]):
            assert all(0 < n <= 9 for n in cube['size'])
            for face in cube['uv'].values():
                assert all(0 <= start <= 64 and 0 <= start+size <= 64
                           for start,size in zip(face['uv'],face['uv_size']))

    assert set(animations) == {'animation.wisp.'+n for n in ('idle','walking','possess')}
    for clip in model['animations']:
        exported = animations[clip['name']]
        assert exported['animation_length'] == clip['length']
        assert bool(exported.get('loop')) == (clip['loop'] == 'loop')
        assert set(exported['bones']) <= set(bones)
        for identifier, animator in clip['animators'].items():
            assert identifier in groups and groups[identifier]['name'] == animator['name']
            for key in animator['keyframes']:
                assert 0 <= key['time'] <= clip['length']
                value = [float(key['data_points'][0][a]) for a in 'xyz']
                assert all(math.isfinite(v) for v in value)
                assert all(abs(v) <= (720 if key['channel'] == 'rotation' else 32) for v in value)
                if key['channel'] == 'position':
                    value[0] *= -1
                if key['channel'] == 'rotation':
                    value[0] *= -1
                    value[1] *= -1
                track = exported['bones'][animator['name']][key['channel']]
                actual = track[str(float(key['time']))] if isinstance(track,dict) else track
                assert actual == value
    idle = animations['animation.wisp.idle']
    walking = animations['animation.wisp.walking']
    possess = animations['animation.wisp.possess']
    assert idle['animation_length'] == 4 and idle['loop']
    assert walking['animation_length'] == 1 and walking['loop']
    assert possess['animation_length'] == 1 and not possess.get('loop',False)
    assert all(1 <= n <= 1.08 for v in idle['bones']['rays']['scale'].values() for n in v)
    assert max(v[2] for v in walking['bones']['rays']['scale'].values()) <= 1.1
    assert possess['bones']['rays']['scale']['0.35'] == [.6]*3
    assert possess['bones']['body']['scale']['1.0'] == [.1]*3
    for bone in set(idle['bones']) | set(possess['bones']):
        for channel, default in [('position',[0]*3),('rotation',[0]*3),('scale',[1]*3)]:
            def initial(clip):
                value = clip['bones'].get(bone,{}).get(channel,default)
                return value['0.0'] if isinstance(value,dict) else value
            assert initial(idle) == initial(possess), (bone,channel)

    base = Image.open(ROOT/'wisp.png')
    glow = Image.open(ROOT/'wisp_glowmask.png')
    for image in (base,glow):
        assert image.mode == 'RGBA' and image.size == (64,64)
    embedded = Image.open(io.BytesIO(base64.b64decode(model['textures'][0]['source'].split(',')[1])))
    assert embedded.tobytes() == base.tobytes()
    assert all(base.getpixel((x,y))[3] == 255 for x in range(10) for y in range(10))
    for image in (base,glow):
        inner = [image.getpixel((x,y))[3] for x in range(16) for y in range(32,48)]
        outer = [image.getpixel((x,y))[3] for x in range(16,32) for y in range(32,48)]
        assert 0 < max(outer) < max(inner) < 255
    print('PASS: hierarchy, required bones, UV bounds, source/export keyframes, animation limits,')
    print('idle-to-possess pose, finite coordinates, RGBA atlas, embedded texture and halo alpha.')


if __name__ == '__main__':
    main()
