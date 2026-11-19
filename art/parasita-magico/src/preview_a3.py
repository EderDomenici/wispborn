"""Software previews of exported geometry, textures and linear animation tracks.

These are orthographic inspection renders, NOT Blockbench/Minecraft captures.
No bloom, postprocessing, particles or GeckoLib shader emulation is applied.
"""
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]


def add(a, b):
    return [x+y for x, y in zip(a, b)]


def sub(a, b):
    return [x-y for x, y in zip(a, b)]


def dot(a, b):
    return sum(x*y for x, y in zip(a, b))


def cross(a, b):
    return [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]


def rotate(point, angles):
    x, y, z = point
    rx, ry, rz = [math.radians(a) for a in angles]
    y, z = y*math.cos(rx)-z*math.sin(rx), y*math.sin(rx)+z*math.cos(rx)
    x, z = x*math.cos(ry)+z*math.sin(ry), -x*math.sin(ry)+z*math.cos(ry)
    x, y = x*math.cos(rz)-y*math.sin(rz), x*math.sin(rz)+y*math.cos(rz)
    return [x, y, z]


def sample(track, time, default):
    if track is None:
        return default
    if isinstance(track, (int, float)):
        return [track]*3
    if isinstance(track, list):
        return track
    keys = sorted((float(k), v) for k, v in track.items())
    if time <= keys[0][0]:
        return keys[0][1]
    for (start, a), (end, b) in zip(keys, keys[1:]):
        if time <= end:
            t = (time-start)/(end-start)
            return [x+(y-x)*t for x, y in zip(a, b)]
    return keys[-1][1]


def render(clip_name, time, background, name):
    geo = json.loads((ROOT/'wisp.geo.json').read_text())['minecraft:geometry'][0]
    bones = {b['name']: b for b in geo['bones']}
    anim = json.loads((ROOT/'wisp.animation.json').read_text())['animations']['animation.wisp.'+clip_name]['bones']
    texture = Image.open(ROOT/'wisp.png').convert('RGBA')
    emission = Image.open(ROOT/'wisp_glowmask.png').convert('RGBA')

    def transform(point, bone_name):
        bone = bones[bone_name]
        track = anim.get(bone_name, {})
        pivot = bone['pivot']
        scale = sample(track.get('scale'), time, [1, 1, 1])
        point = [(p-o)*s for p, o, s in zip(point, pivot, scale)]
        rotation = add(bone.get('rotation', [0, 0, 0]), sample(track.get('rotation'), time, [0, 0, 0]))
        point = add(add(rotate(point, rotation), pivot), sample(track.get('position'), time, [0, 0, 0]))
        return transform(point, bone['parent']) if 'parent' in bone else point

    yaw, pitch = math.radians(-35), math.radians(20)
    view = [math.sin(yaw)*math.cos(pitch), math.sin(pitch), -math.cos(yaw)*math.cos(pitch)]
    right = [math.cos(yaw), 0, math.sin(yaw)]
    up = cross(view, right)
    up = [-v for v in up]

    def project(point):
        point = sub(point, [0, 12, 0])
        return (320+dot(point, right)*18, 260-dot(point, up)*18)

    faces = []
    for bone in geo['bones']:
        for cube in bone.get('cubes', []):
            x, y, z = cube['origin']
            X, Y, Z = add(cube['origin'], cube['size'])
            corners = {
                'north': [(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],
                'south': [(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],
                'east': [(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],
                'west': [(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],
                'up': [(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)],
                'down': [(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)]}
            for face, points in corners.items():
                vertices = [transform(p, bone['name']) for p in points]
                normal = cross(sub(vertices[1],vertices[0]), sub(vertices[2],vertices[0]))
                if dot(normal, view) <= 1e-8:
                    continue
                depth = sum(dot(v, view) for v in vertices)/4
                faces.append((depth, vertices, cube['uv'][face], face))

    canvas = Image.new('RGBA', (640,480), background)
    for _, vertices, uv, face in sorted(faces, key=lambda f: f[0]):
        layer = Image.new('RGBA', canvas.size)
        draw = ImageDraw.Draw(layer)
        width, height = map(lambda x: int(abs(x)), uv['uv_size'])
        def point(u, v):
            return project([vertices[0][i] + (vertices[1][i]-vertices[0][i])*u
                            + (vertices[3][i]-vertices[0][i])*v for i in range(3)])
        for iy in range(height):
            for ix in range(width):
                tx = int(uv['uv'][0] + (ix+.5)*uv['uv_size'][0]/width)
                ty = int(uv['uv'][1] + (iy+.5)*uv['uv_size'][1]/height)
                color = texture.getpixel((tx,ty))
                if not color[3]:
                    continue
                glow = emission.getpixel((tx,ty))[3]/255
                shade = {'up':1,'north':.85,'east':.75,'west':.75,'south':.85,'down':.65}[face]
                shade += (1-shade)*glow
                color = tuple(round(c*shade) for c in color[:3])+(color[3],)
                draw.polygon([point(ix/width,iy/height),point((ix+1)/width,iy/height),
                              point((ix+1)/width,(iy+1)/height),point(ix/width,(iy+1)/height)], fill=color)
        canvas = Image.alpha_composite(canvas, layer)
    draw = ImageDraw.Draw(canvas)
    dark = background[0] < 100
    ink = '#d6f6ef' if dark else '#193d40'
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 16)
    small = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 12)
    draw.text((24,20),f'A3 / {clip_name.upper()} / {time:.2f} s',font=font,fill=ink)
    draw.text((24,445),'Technical preview - orthographic - no bloom - not an in-game capture',font=small,fill=ink)
    canvas.save(ROOT/name)
    return canvas


def main():
    sheet = Image.new('RGBA',(1920,960))
    for column, (clip, time) in enumerate((('idle',0),('walking',.37),('possess',.7))):
        for row, (suffix, background) in enumerate((('',(12,23,31,255)),('_light',(215,226,224,255)))):
            result = render(clip,time,background,clip+suffix+'.png')
            sheet.paste(result,(column*640,row*480))
    sheet.save(ROOT/'a3-preview.png')
    print('Six technical previews and a3-preview.png generated.')


if __name__ == '__main__':
    main()
