"""Validate inherited F4 artwork plus both A4/F5 contracts and palette B."""
import colorsys
import json
import re
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]


def main():
    spec=(REPO/'docs/specs/F4-hud.md').read_text()
    table=spec.split('## Contrato de sprites',1)[1].split('### Fonte',1)[0]
    required=set()
    fixed={}
    for line in table.splitlines():
        if not line.startswith('| `'):continue
        columns=line.split('|')
        names=re.findall(r'`([^`]+)`',columns[1])
        required.update(names)
        size=re.search(r'(\d+)×(\d+)',columns[2])
        if size:
            for name in names:fixed[name]=tuple(map(int,size.groups()))
    required.add('cast_vignette');fixed['cast_vignette']=(256,256)
    fixed.update(host_frame=(128,24),host_health_background=(118,5),
                 host_health_progress=(118,5),host_floor_marker=(3,8),
                 target_frame=(150,26),cast_bar_background=(132,4),cast_bar_progress=(132,4),
                 consume_badge=(84,16))
    f5=(REPO/'docs/specs/F5-hud-b-grimorio.md').read_text()
    contract=f5.split('## Contrato de sprites',1)[1].split('## Regras',1)[0]
    hud_table,grimoire_table=contract.split('Grimório, atlas de GUI',1)
    def parse_table(table):
        result={}
        for line in table.splitlines():
            if not line.startswith('| `'):continue
            columns=line.split('|')
            size=re.search(r'(\d+)×(\d+)',columns[2])
            assert size,line
            border=re.search(r'borda (\d+)',columns[2])
            for name in re.findall(r'`([^`]+)`',columns[1]):
                result[name]=(tuple(map(int,size.groups())),int(border[1]) if border else None)
        return result
    overrides=parse_table(hud_table)
    required.update(overrides)
    fixed.update({name:size for name,(size,_) in overrides.items()})
    def check_palette(image,name):
        for r,g,b,a in set(image.getdata()):
            if not a:continue
            hue,saturation,_=colorsys.rgb_to_hsv(r/255,g/255,b/255)
            assert not (155<=hue*360<=195 and saturation>.2),f'Cyan/teal in {name}: {(r,g,b)}'
    assert {p.stem for p in (ROOT/'sprites').glob('*.png')}==required
    manifest=json.loads((ROOT/'manifest.json').read_text())
    assert set(manifest)==required
    for name in required:
        image=Image.open(ROOT/'sprites'/f'{name}.png')
        expected=fixed[name]
        animated={'mana_bar_progress':(64,1),'dna_bar_progress':(64,1),'consume_badge':(16,2)}
        if name in animated:
            count,ticks=animated[name]
            meta=json.loads((ROOT/'sprites'/f'{name}.png.mcmeta').read_text())['animation']
            assert (meta['width'],meta['height'])==expected
            assert meta['frametime']==ticks and meta['interpolate'] is False
            assert image.size==(expected[0],expected[1]*count)
            frames=[image.crop((0,i*expected[1],expected[0],(i+1)*expected[1])).tobytes() for i in range(count)]
            assert len(set(frames))>=count//2, name
        else:
            assert image.size==expected,name
        assert image.mode=='RGBA',name
        check_palette(image,name)
        assert list(expected)==manifest[name]
        if name not in ('host_frame','target_frame','cast_vignette'):
            assert set(image.getchannel('A').getdata()) <= {0,255},name
    vignette=Image.open(ROOT/'sprites/cast_vignette.png')
    assert vignette.getpixel((128,128))[3]==0
    assert 0<vignette.getpixel((0,128))[3]<255
    for name in ('host_frame','target_frame'):
        scale=json.loads((ROOT/'sprites'/f'{name}.png.mcmeta').read_text())['gui']['scaling']
        assert scale=={'type':'nine_slice','width':fixed[name][0],'height':fixed[name][1],'border':4}
    grimoire=parse_table(grimoire_table)
    assert {p.stem for p in (ROOT/'grimoire').glob('*.png')}==set(grimoire)
    gm=json.loads((ROOT/'grimoire_manifest.json').read_text())
    assert set(gm)==set(grimoire)
    for name,(size,border) in grimoire.items():
        image=Image.open(ROOT/'grimoire'/f'{name}.png')
        assert image.mode=='RGBA' and image.size==size,name
        assert gm[name]==list(size)
        check_palette(image,name)
        if name not in ('panel','portrait_frame','points_box_active','points_box_empty'):
            assert set(image.getchannel('A').getdata())<={0,255},name
        if border:
            scale=json.loads((ROOT/'grimoire'/f'{name}.png.mcmeta').read_text())['gui']['scaling']
            assert scale=={'type':'nine_slice','width':size[0],'height':size[1],'border':border}
            assert border*2<min(size),name
    for name in ('sex_male_badge','sex_female_badge'):
        image=Image.open(ROOT/'font'/f'{name}.png')
        assert image.mode=='RGBA' and image.size==(9,9),name
        assert set(image.getchannel('A').getdata())<={0,255},name
    for name,width in [('sex_male',7),('sex_female',5),('dna',5)]:
        image=Image.open(ROOT/'font'/f'{name}.png')
        assert image.mode=='RGBA' and image.size==(width,8)
        color={'sex_male':(100,181,246,255),'sex_female':(244,143,177,255),'dna':(255,255,255,255)}[name]
        assert all(p in ((0,0,0,0),color) for p in image.getdata())
        assert image.getchannel('A').crop((0,7,width,8)).getbbox() is None
    providers=json.loads((ROOT/'font/icons.json').read_text())['providers']
    assert [p['chars'] for p in providers]==[[chr(0xE000+i)] for i in range(5)]
    assert all(p['height']==8 and p['ascent']==7 for p in providers[:3])
    assert all(p['height']==9 and p['ascent']==8 for p in providers[3:])
    gif=Image.open(ROOT/'bars_animated.gif')
    assert gif.n_frames==64 and gif.info['loop']==0 and gif.info['duration']==50
    for index in range(gif.n_frames):
        gif.seek(index)
        assert gif.info['duration']==50
    for name in ('mockup','mockup_host','mockup_grimoire'):
        image=Image.open(ROOT/(name+'.png'))
        assert image.mode=='RGBA' and image.size==(854,480)
        # Every 2x2 output block must be identical: GUI scale 2, no smoothing.
        reduced=image.resize((427,240),Image.Resampling.NEAREST)
        assert reduced.resize(image.size,Image.Resampling.NEAREST).tobytes()==image.tobytes()
    print(f'PASS: {len(required)} HUD sprites + {len(grimoire)} grimoire sprites from F4/F5,')
    print('palette B (no cyan/teal), RGBA/alpha, animations, nine-slice metadata,')
    print('3 colored glyphs + 2 sex badges, font codepoints and nearest-neighbor mockups.')


if __name__=='__main__':main()
