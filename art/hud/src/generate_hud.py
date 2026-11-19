"""Original A2 pixel art. Run with Python 3 + Pillow; no external image inputs."""
from pathlib import Path
import json
import math

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
EDGE = '#101d29'
PALETTE = {'.':(0,0,0,0), '#':EDGE, 'd':'#294b59', 'm':'#328f9d',
           'c':'#64e5e4', 'l':'#b7fff0', 'w':'#f0fff9', 'g':'#299a79',
           'e':'#55dca0', 'b':'#202d44', 'v':'#a6a0ff', 'r':'#ed6d81'}


def icon(rows, palette=None):
    colors = PALETTE | (palette or {})
    image = Image.new('RGBA',(len(rows[0]),len(rows)))
    for y,row in enumerate(rows):
        assert len(row)==image.width
        for x,char in enumerate(row):
            if char != '.':
                ImageDraw.Draw(image).point((x,y),fill=colors[char])
    return image


CORE = ['...###...','..#clc#..','.#clwlc#.','#clwwwlc#','#clwwwlc#','#cclwlc##','.#cclcc#.','..#ccc#..','...###...']
FLAME = ['.....#...','....#l#..','...#ll#..','.#.#le#..','#l##leg#.','#llegeg##','#legegg##','.#gegg##.','..#####..']


def save(image, folder, name):
    dest = ROOT/folder/(name+'.png')
    dest.parent.mkdir(parents=True,exist_ok=True)
    image.save(dest)
    return image


def empty_shape(rows, blink=False):
    return icon(rows, {k:('#8cbdbb' if blink else '#233741') for k in 'dmclwgebvr'})


def half(full, empty):
    image=empty.copy()
    image.paste(full.crop((0,0,5,9)),(0,0))
    return image


def bar(size, palette, progress=False, helix=False):
    w,h=size
    image=Image.new('RGBA',size)
    d=ImageDraw.Draw(image)
    d.rectangle((0,0,w-1,h-1),fill=EDGE)
    d.rectangle((1,1,w-2,h-2),fill=palette[0] if progress else '#263347')
    if progress:
        d.line((1,1,w-2,1),fill=palette[1])
        d.line((1,h-2,w-2,h-2),fill=palette[2])
        if helix:
            for x in range(2,w-2):
                y=1+(0,1,2,1)[(x//2)%4]
                d.point((x,y),fill=palette[1])
    else:
        d.line((1,1,w-2,1),fill='#384b61')
        for x in range(9,w-2,10):
            d.point((x,h-2),fill='#47536b')
    return image


def frame(size):
    w,h=size
    image=Image.new('RGBA',size)
    d=ImageDraw.Draw(image)
    d.rectangle((1,1,w-2,h-2),fill=(12,23,32,210))
    d.line([(3,0),(w-4,0),(w-1,3),(w-1,h-4),(w-4,h-1),(3,h-1),(0,h-4),(0,3),(3,0)],fill='#397c80')
    for x,y in [(2,2),(w-3,2),(2,h-3),(w-3,h-3)]:
        d.point((x,y),fill='#91e3cc')
    return image


FONT = {
 'A':['01110','10001','10001','11111','10001','10001','10001'],
 'B':['11110','10001','10001','11110','10001','10001','11110'],
 'C':['01111','10000','10000','10000','10000','10000','01111'],
 'D':['11110','10001','10001','10001','10001','10001','11110'],
 'E':['11111','10000','10000','11110','10000','10000','11111'],
 'F':['11111','10000','10000','11110','10000','10000','10000'],
 'G':['01111','10000','10000','10111','10001','10001','01111'],
 'H':['10001','10001','10001','11111','10001','10001','10001'],
 'I':['111','010','010','010','010','010','111'],
 'J':['00111','00010','00010','00010','10010','10010','01100'],
 'K':['10001','10010','10100','11000','10100','10010','10001'],
 'L':['10000','10000','10000','10000','10000','10000','11111'],
 'M':['10001','11011','10101','10101','10001','10001','10001'],
 'N':['10001','11001','11001','10101','10011','10011','10001'],
 'O':['01110','10001','10001','10001','10001','10001','01110'],
 'P':['11110','10001','10001','11110','10000','10000','10000'],
 'Q':['01110','10001','10001','10001','10101','10010','01101'],
 'R':['11110','10001','10001','11110','10100','10010','10001'],
 'S':['01111','10000','10000','01110','00001','00001','11110'],
 'T':['11111','00100','00100','00100','00100','00100','00100'],
 'U':['10001','10001','10001','10001','10001','10001','01110'],
 'V':['10001','10001','10001','10001','10001','01010','00100'],
 'W':['10001','10001','10001','10101','10101','11011','10001'],
 'X':['10001','10001','01010','00100','01010','10001','10001'],
 'Y':['10001','10001','01010','00100','00100','00100','00100'],
 'Z':['11111','00001','00010','00100','01000','10000','11111'],
 '0':['01110','10001','10011','10101','11001','10001','01110'],
 '1':['010','110','010','010','010','010','111'],
 '2':['01110','10001','00001','00010','00100','01000','11111'],
 '3':['11110','00001','00001','01110','00001','00001','11110'],
 '4':['10010','10010','10010','11111','00010','00010','00010'],
 '5':['11111','10000','10000','11110','00001','00001','11110'],
 '6':['01110','10000','10000','11110','10001','10001','01110'],
 '7':['11111','00001','00010','00100','01000','01000','01000'],
 '8':['01110','10001','10001','01110','10001','10001','01110'],
 '9':['01110','10001','10001','01111','00001','00001','01110'],
 '/':['00001','00001','00010','00100','01000','10000','10000'],
 '%':['11001','11010','00010','00100','01000','01011','10011'],
 ':':['0','1','1','0','1','1','0'],
 '-':['000','000','000','111','000','000','000'],
 '+':['000','010','010','111','010','010','000'],
 '.':['0','0','0','0','0','1','1'],
 ' ':['000']*7,
}


def text(image, message, x, y, color='#e4f4ed', center=False):
    message=message.upper()
    width=sum(len(FONT[c][0])+1 for c in message)-1
    if center: x-=width//2
    draw=ImageDraw.Draw(image)
    for c in message:
        for j,row in enumerate(FONT[c]):
            for i,v in enumerate(row):
                if v=='1': draw.point((x+i+1,y+j+1),fill=EDGE)
        for j,row in enumerate(FONT[c]):
            for i,v in enumerate(row):
                if v=='1': draw.point((x+i,y+j),fill=color)
        x+=len(FONT[c][0])+1


def make_sprites():
    sprites={}
    def put(name,image): sprites[name]=save(image,'sprites',name)
    core,flame=icon(CORE),icon(FLAME)
    container=empty_shape(CORE)
    put('core_container',container)
    put('core_container_blinking',empty_shape(CORE,True))
    put('core_full',core)
    # Health foreground halves are transparent on the right; the container is separate.
    core_half=Image.new('RGBA',(9,9));core_half.paste(core.crop((0,0,5,9)),(0,0))
    put('core_half',core_half)
    hunger_empty=empty_shape(FLAME)
    put('hunger_empty',hunger_empty);put('hunger_full',flame);put('hunger_half',half(flame,hunger_empty))
    for kind,colors,size in [('mana',('#6252c9','#b5adff','#35459b'),(182,5)),
                             ('dna',('#299a79','#c7ffe8','#196359'),(182,5)),
                             ('host_health',('#42b994','#a1f1d0','#23735d'),(164,6)),
                             ('cast',('#409ba9','#cbfff8','#286177'),(132,4))]:
        prefix=kind+'_bar' if kind in ('mana','dna','cast') else kind
        put(prefix+'_background',bar(size,colors))
        put(prefix+'_progress',bar(size,colors,True,kind=='dna'))
    put('dna_icon',icon(['.##...##.','#le#.#wl#','.#el#wl#.','..#ele#..','...#e#...','..#wle#..','.#wl#el#.','#wl#.#el#','.##...##.']))
    put('host_frame',frame((182,44)));put('target_frame',frame((150,32)))
    marker=Image.new('RGBA',(3,8));d=ImageDraw.Draw(marker)
    d.rectangle((0,0,2,7),fill=EDGE);d.line((1,0,1,7),fill='#ffd694')
    put('host_floor_marker',marker)
    put('consume_ready',icon(['...###...','..#wlw#..','.#wlelw#.','#wle.elw#','#le...el#','#wle.elw#','.#wlelw#.','..#wlw#..','...###...']))
    put('no_ep',icon(['..#####..','.#ddddd#.','#drdddrd#','#ddrdrdd#','#dddrddd#','#ddrdrdd#','#drdddrd#','.#ddddd#.','..#####..']))
    put('target_ok',icon(['.........','......##.','.....#el#','....#el#.','.###el#..','#elel#...','.#el#....','..##.....','.........']))
    put('target_blocked',icon(['.........','.##...##.','#rr#.#rr#','.#rr#rr#.','..#rrr#..','.#rr#rr#.','#rr#.#rr#','.##...##.','.........']))
    vignette=Image.new('RGBA',(256,256))
    for y in range(256):
        for x in range(256):
            # Quantized edge fade preserves the pixel-art direction.
            distance=min(x,y,255-x,255-y)
            alpha=0 if distance>=52 else round(150*((52-distance)/52)**2/6)*6
            vignette.putpixel((x,y),(63,220,183,alpha))
    put('cast_vignette',vignette)
    return sprites


def make_glyphs():
    patterns={
      'sex_male':['....###','.....##','.###.#.','#...#..','#...#..','#...#..','.###...','.......'],
      'sex_female':['.###.','#...#','#...#','.###.','..#..','.###.','..#..','.....'],
      'dna':['#...#','.###.','..#..','.###.','#...#','.###.','..#..','.....']}
    glyphs={name:save(icon(rows,{'#':'#ffffff'}),'font',name) for name,rows in patterns.items()}
    providers=[{'type':'bitmap','file':f'wispborn:font/{name}.png','ascent':7,'height':8,'chars':[chr(0xE000+i)]}
               for i,name in enumerate(patterns)]
    (ROOT/'font/icons.json').write_text(json.dumps({'providers':providers},indent=2)+'\n')
    return glyphs


def mockup(sprites,glyphs,host):
    canvas=Image.new('RGBA',(427,240),'#75888e');d=ImageDraw.Draw(canvas)
    # Neutral original voxel landscape, not a game screenshot.
    d.rectangle((0,90,426,239),fill='#50655f')
    for x,y,w,h,c in [(0,72,90,27,'#687c77'),(69,62,64,38,'#647772'),(126,80,92,24,'#6c7f78'),
                        (292,68,75,34,'#60756e'),(355,80,72,24,'#687c77'),(0,147,91,93,'#42564f'),
                        (319,134,108,106,'#465951')]:
        d.rectangle((x,y,x+w,y+h),fill=c)
    for x in range(0,427,32):
        d.line((x,159,x+80,239),fill='#586b60')
    text(canvas,'WISPBORN / A2',12,12)
    text(canvas,'HOST' if host else 'WISP',12,24,color='#a7dac9')
    # Crosshair stays unobstructed.
    d.line((210,120,216,120),fill='#e5eeee');d.line((213,117,213,123),fill='#e5eeee')
    def paste(name,x,y):canvas.alpha_composite(sprites[name],(x,y))
    cx=213
    for i in range(3):
        paste('core_container',cx-91+i*8,201)
        paste('core_full' if i<2 else 'core_half',cx-91+i*8,201)
    for i in range(10):paste('hunger_full' if i<7 else 'hunger_empty',cx+11+i*8,201)
    paste('dna_icon',cx-27,191);text(canvas,'NV 7',cx-14,193)
    text(canvas,'+2',cx+38,193,color='#8be8bd')
    for kind,y,value in [('dna',215,.6),('mana',230,42/70)]:
        paste(kind+'_bar_background',cx-91,y)
        canvas.alpha_composite(sprites[kind+'_bar_progress'].crop((0,0,math.floor(182*value),5)),(cx-91,y))
    text(canvas,'MANA 42/70',cx,222,color='#d0caff',center=True)
    if host:
        paste('host_frame',cx-91,146)
        text(canvas,'ZUMBI',cx-82,152)
        canvas.alpha_composite(glyphs['sex_female'],(cx-47,152))
        text(canvas,'NV 6',cx-36,152)
        text(canvas,'62%',cx+56,152)
        paste('host_health_background',cx-82,165)
        canvas.alpha_composite(sprites['host_health_progress'].crop((0,0,101,6)),(cx-82,165))
        paste('host_floor_marker',cx-82+32-1,164)
        text(canvas,'CONSUMIR: ATE 20%',cx-82,177,color='#e4cba6')
    else:
        paste('target_frame',cx-75,67)
        text(canvas,'ZUMBI',cx-66,73)
        canvas.alpha_composite(glyphs['sex_female'],(cx-31,73))
        text(canvas,'NV 6',cx-20,73)
        paste('target_ok',cx+55,71)
        text(canvas,'INFESTAR',cx-66,83,color='#a1efd6')
        paste('cast_bar_background',cx-66,91)
        canvas.alpha_composite(sprites['cast_bar_progress'].crop((0,0,79,4)),(cx-66,91))
    return canvas.resize((854,480),Image.Resampling.NEAREST)


def variants():
    canvas=Image.new('RGBA',(300,100),'#111e2a')
    candidates=[('CORE A',icon(CORE)),('CORE B',icon(['....#....','...#w#...','..#wlw#..','.#wlclw#.','#wlccclw#','.#wlclw#.','..#wlw#..','...#w#...','....#....'])),
                ('FOGO A',icon(FLAME)),('FOGO B',icon(['....#....','...#l#...','..#ll#...','.#llel#..','.#legel#.','#lleggg##','#legegg##','.#gegg##.','..#####..']))]
    for i,(name,sprite) in enumerate(candidates):
        x=10+i*74
        text(canvas,name,x,12)
        canvas.alpha_composite(sprite.resize((36,36),Image.Resampling.NEAREST),(x+4,32))
        text(canvas,'PADRAO' if i in (0,2) else 'OPCAO',x,80,color='#a7c9c4')
    canvas.resize((900,300),Image.Resampling.NEAREST).save(ROOT/'variants.png')


def main():
    sprites=make_sprites();glyphs=make_glyphs()
    for host,name in [(False,'mockup'),(True,'mockup_host')]:
        mockup(sprites,glyphs,host).save(ROOT/(name+'.png'))
    review=Image.new('RGBA',(1708,1920))
    for index,name in enumerate(('mockup','mockup_host')):
        source=Image.open(ROOT/(name+'.png'))
        review.paste(source.resize((1708,960),Image.Resampling.NEAREST),(0,index*960))
    review.save(ROOT/'review_200.png')
    overlay=sprites['cast_vignette'].resize((854,480),Image.Resampling.NEAREST)
    overlay.putalpha(overlay.getchannel('A').point(lambda value: round(value*.6)))
    Image.alpha_composite(Image.open(ROOT/'mockup.png'),overlay).save(ROOT/'vignette_preview.png')
    variants()
    (ROOT/'manifest.json').write_text(json.dumps({name:list(image.size) for name,image in sprites.items()},indent=2)+'\n')
    print(f'Generated {len(sprites)} sprites, {len(glyphs)} glyphs, font provider, two mockups and variants.')


if __name__=='__main__':main()
