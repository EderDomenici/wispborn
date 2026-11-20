"""Check A2 against the sprite names in F4, PNG formats, alpha and glyph contracts."""
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
        assert list(expected)==manifest[name]
        if name not in ('host_frame','target_frame','cast_vignette'):
            assert set(image.getchannel('A').getdata()) <= {0,255},name
    vignette=Image.open(ROOT/'sprites/cast_vignette.png')
    assert vignette.getpixel((128,128))[3]==0
    assert 0<vignette.getpixel((0,128))[3]<255
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
    for name in ('mockup','mockup_host'):
        image=Image.open(ROOT/(name+'.png'))
        assert image.mode=='RGBA' and image.size==(854,480)
        # Every 2x2 output block must be identical: GUI scale 2, no smoothing.
        reduced=image.resize((427,240),Image.Resampling.NEAREST)
        assert reduced.resize(image.size,Image.Resampling.NEAREST).tobytes()==image.tobytes()
    print(f'PASS: {len(required)} sprite names/dimensions from F4, RGBA/alpha, 3 colored glyphs + 2 sex badges,')
    print('font codepoints, transparent vignette center and nearest-neighbor mockups.')


if __name__=='__main__':main()
