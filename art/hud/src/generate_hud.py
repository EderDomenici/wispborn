"""Original A2/A4 pixel art, palette B. Run with Python 3 + Pillow; no external image inputs."""
from pathlib import Path
import json
import math

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
EDGE = '#120F0C'
BAR_FRAMES = 64
BAR_FRAME_TICKS = 1
PALETTE = {'.':(0,0,0,0), '#':EDGE, 'd':'#4A1B16', 'm':'#7D2A21',
           'c':'#B5402F', 'l':'#E2775F', 'w':'#F7CBB8', 'g':'#A8976A',
           'e':'#A8C46A', 'b':'#26211C', 'v':'#9FB4EE', 'r':'#D0563C'}


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
    return icon(rows, {k:('#B5A88F' if blink else '#2B2621') for k in 'dmclwgebvr'})


def half(full, empty):
    image=empty.copy()
    image.paste(full.crop((4,0,9,9)),(4,0))
    return image


def bar(size, palette, progress=False, helix=False):
    w,h=size
    image=Image.new('RGBA',size)
    d=ImageDraw.Draw(image)
    d.rectangle((0,0,w-1,h-1),fill=EDGE)
    d.rectangle((1,1,w-2,h-2),fill=palette[0] if progress else '#26211C')
    if progress:
        d.line((1,1,w-2,1),fill=palette[1])
        d.line((1,h-2,w-2,h-2),fill=palette[2])
        if helix:
            for x in range(2,w-2):
                y=1+(0,1,2,1)[(x//2)%4]
                d.point((x,y),fill=palette[1])
    else:
        d.line((1,1,w-2,1),fill='#3A332B')
        for x in range(9,w-2,10):
            d.point((x,h-2),fill='#4A4137')
    return image


def frame(size):
    w,h=size
    image=Image.new('RGBA',size)
    d=ImageDraw.Draw(image)
    d.rectangle((1,1,w-2,h-2),fill=(29,25,21,225))
    d.line([(3,0),(w-4,0),(w-1,3),(w-1,h-4),(w-4,h-1),(3,h-1),(0,h-4),(0,3),(3,0)],fill='#CDBB94')
    for x,y in [(2,2),(w-3,2),(2,h-3),(w-3,h-3)]:
        d.point((x,y),fill='#FFF3D6')
    return image


BADGE_FRAMES = 16
BADGE_FRAME_TICKS = 2


def mix(a, b, t):
    ca=[int(a[i:i+2],16) for i in (1,3,5)];cb=[int(b[i:i+2],16) for i in (1,3,5)]
    return '#'+''.join(f'{round(x+(y-x)*t):02x}' for x,y in zip(ca,cb))


def consume_badge(phase):
    """Selo 'Consumir' (20/11/2026): corpo chanfrado com presas nas pontas; a borda pulsa em 16 quadros."""
    w,h=84,16
    t=(1-math.cos(2*math.pi*phase/BADGE_FRAMES))/2
    glow=mix('#C4573A','#FFF3D6',t)
    image=Image.new('RGBA',(w,h));d=ImageDraw.Draw(image)
    body=[(7,1),(w-8,1),(w-6,3),(w-6,h-4),(w-8,h-2),(7,h-2),(5,h-4),(5,3)]
    d.polygon(body,fill='#1D1915')
    d.line(body+[body[0]],fill=glow)
    d.line((8,2,w-9,2),fill='#3A332B')
    # presas: tres pixels apontando para fora em cada lado
    for x0,dx in ((4,-1),(w-5,1)):
        for i,(yy,ln) in enumerate(((5,2),(7,3),(9,3),(11,2))):
            for k in range(ln):
                d.point((x0+dx*k,yy if i<2 else yy),fill=glow if k<ln-1 else mix('#C4573A','#FFF3D6',t))
    # brilho que percorre a borda de cima
    run=round((w-20)*phase/BADGE_FRAMES)+9
    for k in range(3):
        if 8<=run+k<=w-9:d.point((run+k,1),fill='#FFF3D6')
    return image


def animated_bar(kind, phase):
    """Sixty-four looping pixel frames with half-pixel phase steps at 20 fps."""
    width=80 if kind=='mana' else 200
    colors=('#3F5AA8','#9FB4EE','#24346A') if kind=='mana' else ('#A8976A','#FFF6D8','#6F644A')
    image=bar((width,6),colors,True)
    draw=ImageDraw.Draw(image)
    for x in range(2,width-2):
        angle=2*math.pi*(x/32-phase/BAR_FRAMES)
        if kind=='mana':
            y=2+round(math.sin(angle))
            draw.point((x,y),fill='#8AA0E6')
            draw.point((x,y+1),fill='#6A80CC')
            if (x-32*phase/BAR_FRAMES)%32 < 2:
                draw.point((x,1),fill='#D9E2FF')
        else:
            offset=round(math.sin(angle))
            if (x-32*phase/BAR_FRAMES)%4 < 1:
                draw.line((x,2-abs(offset),x,3+abs(offset)),fill='#6F644A')
            draw.point((x,2+offset),fill='#FFF6D8')
            draw.point((x,3-offset),fill='#D9C99C')
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


def text(image, message, x, y, color='#F3EAD6', center=False):
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


def dna_orb(dna):
    image=Image.new('RGBA',(19,19));d=ImageDraw.Draw(image)
    shape=[(6,0),(12,0),(16,3),(18,6),(18,12),(15,16),(12,18),(6,18),(2,15),(0,12),(0,6),(3,2)]
    d.polygon(shape,fill=EDGE)
    d.line([(6,1),(12,1),(15,3),(17,6),(17,12),(15,15),(12,17),(6,17),(3,15),(1,12),(1,6),(3,3),(6,1)],fill='#CDBB94',width=1)
    d.line((6,2,12,2),fill='#FFF3D6')
    d.polygon([(6,3),(12,3),(15,6),(15,12),(12,15),(6,15),(3,12),(3,6)],fill='#26211C')
    image.alpha_composite(dna,(5,5))
    return image


def nine_slice_meta(folder,name,size,border):
    (ROOT/folder/(name+'.png.mcmeta')).write_text(json.dumps({
        'gui':{'scaling':{'type':'nine_slice','width':size[0],'height':size[1],'border':border}}
    },indent=2)+'\n')


def make_grimoire():
    kit={}
    def put(name,image,border=None):
        kit[name]=save(image,'grimoire',name)
        if border is not None:nine_slice_meta('grimoire',name,image.size,border)
    put('panel',frame((64,64)),6)
    put('portrait_frame',frame((32,32)),4)
    underline=Image.new('RGBA',(16,3));d=ImageDraw.Draw(underline)
    d.line((0,1,15,1),fill='#C4573A');d.line((1,0,14,0),fill='#E2775F')
    put('tab_underline',underline,1)
    for state in ('empty','filled','pending'):
        image=Image.new('RGBA',(7,7));d=ImageDraw.Draw(image)
        d.polygon([(1,0),(5,0),(6,1),(6,5),(5,6),(1,6),(0,5),(0,1)],fill=EDGE)
        d.rectangle((1,1,5,5),fill={'empty':'#2B2621','filled':'#A8976A','pending':'#C4573A'}[state])
        if state!='empty':d.line((1,1,5,1),fill='#FFF3D6' if state=='pending' else '#D9C99C')
        if state=='pending':d.rectangle((1,1,5,5),outline='#FFF3D6')
        put('pip_'+state,image)
    for primary in (False,True):
        for state in ('','_hover','_disabled'):
            name='button'+('_primary' if primary else '')+state
            image=frame((20,20));d=ImageDraw.Draw(image)
            fill=('#E2775F' if state=='_hover' else '#C4573A') if primary else '#26211C'
            if state=='_disabled':fill='#2B2621'
            d.rectangle((2,3,17,16),fill=fill)
            d.line((3,2,16,2),fill='#FFF3D6' if state=='_hover' else '#4A4137' if state=='_disabled' else '#CDBB94')
            image.putdata([(*p[:3],255) if p[3] else p for p in image.getdata()])
            if state=='_disabled':
                image.putdata([((111,101,90,255) if p[:3] in ((205,187,148),(255,243,214)) else p) for p in image.getdata()])
            put(name,image,3)
    for active in (True,False):
        image=frame((24,24));d=ImageDraw.Draw(image)
        d.rectangle((3,3,20,20),fill='#26211C' if active else '#2B2621')
        d.line((4,20,19,20),fill='#C4573A' if active else '#4A4137')
        put('points_box_'+('active' if active else 'empty'),image,4)
    # EP keeps the full 120 px; life, mana and hunger are 90 px so their values fit to the right.
    put('bar_background',bar((120,5),('#26211C',)*3))
    put('bar_background_short',bar((90,5),('#26211C',)*3))
    for name,colors,width in [('life',('#B5402F','#F7CBB8','#7D2A21'),90),
                              ('mana',('#3F5AA8','#9FB4EE','#24346A'),90),
                              ('hunger',('#C9A55A','#F2E2B0','#6E5A2A'),90),
                              ('ep',('#A8976A','#FFF6D8','#6F644A'),120)]:
        put('bar_'+name,bar((width,5),colors,True))
    (ROOT/'grimoire_manifest.json').write_text(json.dumps({k:list(v.size) for k,v in kit.items()},indent=2)+'\n')
    return kit


def slice_image(image,size,border):
    """Preview the same nine regions that the GUI atlas scales independently."""
    result=Image.new('RGBA',size)
    sx=[0,border,image.width-border,image.width];sy=[0,border,image.height-border,image.height]
    dx=[0,border,size[0]-border,size[0]];dy=[0,border,size[1]-border,size[1]]
    for y in range(3):
        for x in range(3):
            tile=image.crop((sx[x],sy[y],sx[x+1],sy[y+1]))
            result.paste(tile.resize((dx[x+1]-dx[x],dy[y+1]-dy[y]),Image.Resampling.NEAREST),(dx[x],dy[y]))
    return result


def make_sprites():
    sprites={}
    def put(name,image): sprites[name]=save(image,'sprites',name)
    core=icon(CORE)
    flame=icon(FLAME,{'l':'#F2E2B0','e':'#C9A55A','g':'#6E5A2A'})
    container=empty_shape(CORE)
    put('core_container',container)
    put('core_container_blinking',empty_shape(CORE,True))
    put('core_full',core)
    # Health foreground halves are transparent on the right; the container is separate.
    core_half=Image.new('RGBA',(9,9));core_half.paste(core.crop((0,0,5,9)),(0,0))
    put('core_half',core_half)
    hunger_empty=empty_shape(FLAME)
    put('hunger_empty',hunger_empty);put('hunger_full',flame);put('hunger_half',half(flame,hunger_empty))
    for kind,colors,size in [('mana',('#3F5AA8','#9FB4EE','#24346A'),(80,6)),
                             ('dna',('#A8976A','#FFF6D8','#6F644A'),(200,6)),
                             ('host_health',('#B5402F','#F7CBB8','#7D2A21'),(118,5)),
                             ('cast',('#C4573A','#E2775F','#7D2A21'),(132,4))]:
        prefix=kind+'_bar' if kind in ('mana','dna','cast') else kind
        put(prefix+'_background',bar(size,colors))
        put(prefix+'_progress',bar(size,colors,True,kind=='dna'))
        if kind in ('mana','dna'):
            sheet=Image.new('RGBA',(size[0],size[1]*BAR_FRAMES))
            for phase in range(BAR_FRAMES):sheet.paste(animated_bar(kind,phase),(0,phase*size[1]))
            save(sheet,'sprites',prefix+'_progress')
            sprites[prefix+'_progress']=animated_bar(kind,0)
            (ROOT/'sprites'/(prefix+'_progress.png.mcmeta')).write_text(json.dumps({
                'animation':{'width':size[0],'height':size[1],'frametime':BAR_FRAME_TICKS,'interpolate':False}
            },indent=2)+'\n')
    put('dna_icon',icon(['.##...##.','#le#.#wl#','.#el#wl#.','..#ele#..','...#e#...','..#wle#..','.#wl#el#.','#wl#.#el#','.##...##.'],{'l':'#D9C99C','e':'#A8976A','w':'#FFF6D8'}))
    put('dna_orb',dna_orb(sprites['dna_icon']))
    put('host_frame',frame((128,24)));put('target_frame',frame((150,26)))
    # Molduras em nine-slice: o jogo desenha em qualquer tamanho sem esticar o chanfro (20/11/2026).
    for name,size in (('host_frame',(128,24)),('target_frame',(150,26))):
        (ROOT/'sprites'/(name+'.png.mcmeta')).write_text(json.dumps({
            'gui':{'scaling':{'type':'nine_slice','width':size[0],'height':size[1],'border':4}}
        },indent=2)+'\n')
    marker=Image.new('RGBA',(3,8));d=ImageDraw.Draw(marker)
    d.rectangle((0,0,2,7),fill=EDGE);d.line((1,0,1,7),fill='#FFF3D6')
    put('host_floor_marker',marker)
    put('consume_ready',icon(['...###...','..#wlw#..','.#wlelw#.','#wle.elw#','#le...el#','#wle.elw#','.#wlelw#.','..#wlw#..','...###...']))
    put('no_ep',icon(['..#####..','.#ddddd#.','#drdddrd#','#ddrdrdd#','#dddrddd#','#ddrdrdd#','#drdddrd#','.#ddddd#.','..#####..']))
    put('target_ok',icon(['.........','......##.','.....#el#','....#el#.','.###el#..','#elel#...','.#el#....','..##.....','.........'],{'e':'#A8C46A','l':'#FFF3D6'}))
    put('target_blocked',icon(['.........','.##...##.','#rr#.#rr#','.#rr#rr#.','..#rrr#..','.#rr#rr#.','#rr#.#rr#','.##...##.','.........']))
    sheet=Image.new('RGBA',(84,16*BADGE_FRAMES))
    for phase in range(BADGE_FRAMES):sheet.paste(consume_badge(phase),(0,phase*16))
    save(sheet,'sprites','consume_badge')
    sprites['consume_badge']=consume_badge(0)
    (ROOT/'sprites'/'consume_badge.png.mcmeta').write_text(json.dumps({
        'animation':{'width':84,'height':16,'frametime':BADGE_FRAME_TICKS,'interpolate':False}
    },indent=2)+'\n')
    vignette=Image.new('RGBA',(256,256))
    for y in range(256):
        for x in range(256):
            # Quantized edge fade preserves the pixel-art direction.
            distance=min(x,y,255-x,255-y)
            alpha=0 if distance>=52 else round(150*((52-distance)/52)**2/6)*6
            vignette.putpixel((x,y),(196,87,58,alpha))
    put('cast_vignette',vignette)
    return sprites


def make_glyphs():
    patterns={
      'sex_male':['....###','.....##','.###.#.','#...#..','#...#..','#...#..','.###...','.......'],
      'sex_female':['.###.','#...#','#...#','.###.','..#..','.###.','..#..','.....'],
      'dna':['#...#','.###.','..#..','.###.','#...#','.###.','..#..','.....'],
      'sex_male_badge':['..#####..','.#bbbbb#.','#bbbbwww#','#bbbbbww#','#bwwwbwb#','#wbbbwbb#','#wbbbwbb#','.#wwwbb#.','..#####..'],
      'sex_female_badge':['..#####..','.#ppppp#.','#ppwwwpp#','#pwpppwp#','#ppwwwpp#','#pppwppp#','#ppwwwpp#','.#ppwpp#.','..#####..']}
    colors={'sex_male':'#64b5f6','sex_female':'#f48fb1','dna':'#ffffff'}
    badge={'b':'#3d8fd6','p':'#d9628f','w':'#ffffff','#':'#101d29'}
    glyphs={name:save(icon(rows,badge if name.endswith('_badge') else {'#':colors[name]}),'font',name)
            for name,rows in patterns.items()}
    preview=Image.new('RGBA',(180,64),'#1D1915')
    for name,label,x in [('sex_female','FEMEA',12),('sex_male','MACHO',100)]:
        preview.alpha_composite(glyphs[name].resize((glyphs[name].width*4,32),Image.Resampling.NEAREST),(x+4,8))
        text(preview,label,x,48,color=colors[name])
    preview.resize((720,256),Image.Resampling.NEAREST).save(ROOT/'gender_symbols.png')
    providers=[{'type':'bitmap','file':f'wispborn:font/{name}.png',
                'ascent':8 if name.endswith('_badge') else 7,'height':9 if name.endswith('_badge') else 8,
                'chars':[chr(0xE000+i)]}
               for i,name in enumerate(patterns)]
    (ROOT/'font/icons.json').write_text(json.dumps({'providers':providers},indent=2)+'\n')
    return glyphs


def mockup(sprites,glyphs,host):
    canvas=Image.new('RGBA',(427,240),'#625D53');d=ImageDraw.Draw(canvas)
    # Neutral original voxel landscape, not a game screenshot.
    d.rectangle((0,90,426,239),fill='#46453B')
    for x,y,w,h,c in [(0,72,90,27,'#57594C'),(69,62,64,38,'#535547'),(126,80,92,24,'#5B5C50'),
                        (292,68,75,34,'#505244'),(355,80,72,24,'#57594C'),(0,147,91,93,'#35372F'),
                        (319,134,108,106,'#3C3E33')]:
        d.rectangle((x,y,x+w,y+h),fill=c)
    for x in range(0,427,32):
        d.line((x,159,x+80,239),fill='#4B4C3E')
    text(canvas,'WISPBORN / A4',12,12)
    text(canvas,'HOST' if host else 'WISP',12,24,color='#B5A88F')
    # Crosshair stays unobstructed.
    d.line((210,120,216,120),fill='#F3EAD6');d.line((213,117,213,123),fill='#F3EAD6')
    def paste(name,x,y):canvas.alpha_composite(sprites[name],(x,y))
    cx=213
    for i in range(3):
        paste('core_container',cx-100+i*8,212)
        paste('core_full' if i<2 else 'core_half',cx-100+i*8,212)
    for i in range(10):paste('hunger_empty' if i<3 else 'hunger_full',cx+20+i*8,204)
    paste('dna_orb',cx-9,206)
    text(canvas,'8920',cx,197,color='#FFF6D8',center=True)
    for kind,x,y,value in [('dna',cx-100,226,.6),('mana',cx+20,214,42/70)]:
        paste(kind+'_bar_background',x,y)
        progress=sprites[kind+'_bar_progress']
        canvas.alpha_composite(progress.crop((0,0,math.floor(progress.width*value),progress.height)),(x,y))
    if host:
        # Painel compacto no topo central (20/11/2026).
        paste('host_frame',cx-64,4)
        text(canvas,'ZUMBI',cx-59,8)
        canvas.alpha_composite(glyphs['sex_female_badge'],(cx-30,7))
        text(canvas,'NV 6',cx-19,8)
        text(canvas,'18%',cx+46,8)
        paste('host_health_background',cx-59,19)
        canvas.alpha_composite(sprites['host_health_progress'].crop((0,0,21,5)),(cx-59,19))
        paste('host_floor_marker',cx-59+23-1,17)
        # Selo de consumo no centro inferior, acima da linha do nivel.
        paste('consume_badge',cx-42,179)
        paste('consume_ready',cx-37,183)
        text(canvas,'CONSUMIR C',cx-24,184,color='#FFF6D8')
    else:
        # Cartao do alvo no topo central (opcao B, 20/11/2026): nome, sexo e nivel, estado.
        canvas.alpha_composite(frame((96,36)),(cx-48,2))
        text(canvas,'ZUMBI',cx-42,7)
        canvas.alpha_composite(glyphs['sex_female_badge'],(cx-42,16))
        text(canvas,'NV 6',cx-30,17,color='#B5A88F')
        paste('target_ok',cx-42,26)
        text(canvas,'INFESTAR',cx-30,27,color='#A8C46A')
        # Barra de conjuracao so durante o cast, no centro inferior.
        paste('cast_bar_background',cx-66,191)
        canvas.alpha_composite(sprites['cast_bar_progress'].crop((0,0,79,4)),(cx-66,191))
    return canvas.resize((854,480),Image.Resampling.NEAREST)


def variants():
    canvas=Image.new('RGBA',(300,100),'#1D1915')
    candidates=[('CORE A',icon(CORE)),('CORE B',icon(['....#....','...#w#...','..#wlw#..','.#wlclw#.','#wlccclw#','.#wlclw#.','..#wlw#..','...#w#...','....#....'])),
                ('FOGO A',icon(FLAME,{'l':'#F2E2B0','e':'#C9A55A','g':'#6E5A2A'})),('FOGO B',icon(['....#....','...#l#...','..#ll#...','.#llel#..','.#legel#.','#lleggg##','#legegg##','.#gegg##.','..#####..'],{'l':'#F2E2B0','e':'#C9A55A','g':'#6E5A2A'}))]
    for i,(name,sprite) in enumerate(candidates):
        x=10+i*74
        text(canvas,name,x,12)
        canvas.alpha_composite(sprite.resize((36,36),Image.Resampling.NEAREST),(x+4,32))
        text(canvas,'PADRAO' if i in (0,2) else 'OPCAO',x,80,color='#B5A88F')
    canvas.resize((900,300),Image.Resampling.NEAREST).save(ROOT/'variants.png')


def grimoire_mockup(kit):
    canvas=Image.new('RGBA',(427,240),'#171511')
    d=ImageDraw.Draw(canvas)
    d.rectangle((0,114,426,239),fill='#202019')
    for x in range(0,427,42):d.line((x,144,x+65,239),fill='#28271F')
    ox,oy=55,20
    def paste(name,x,y,size=None,border=None):
        image=kit[name]
        if size:image=slice_image(image,size,border)
        canvas.alpha_composite(image,(ox+x,oy+y))
    def label(message,x,y,color='#F3EAD6',center=False):
        text(canvas,message,ox+x,oy+y,color,center)
    paste('panel',0,0,(316,200),6)
    label('GRIMORIO',8,7)
    # A 6px gap is within the permitted 2px adjustment and fits every tab.
    x=74
    for title in ('WISP','TRACOS','ASPECTOS','HABILIDADES','BESTIARIO'):
        width=sum(len(FONT[c][0])+1 for c in title)-1
        label(title,x,7,'#F3EAD6' if title=='WISP' else '#6F655A')
        if title=='WISP':paste('tab_underline',x,17,(width,3),1)
        x+=width+6
    paste('portrait_frame',8,24,(100,168),4)
    # Schematic portrait only: live 3D remains the responsibility of GrimoireScreen.
    # Wisp color is independent from the interface palette, as the approved design specifies.
    portrait=Image.new('RGBA',(37,37));pd=ImageDraw.Draw(portrait)
    pd.polygon([(14,5),(22,5),(29,12),(31,18),(28,26),(22,31),(13,29),(6,23),(5,14)],outline='#307F80')
    pd.polygon([(14,9),(22,9),(27,14),(27,22),(22,27),(14,27),(9,22),(9,14)],fill='#64E5E4')
    pd.polygon([(15,11),(21,11),(25,15),(25,21),(21,25),(15,25),(11,21),(11,15)],fill='#B7FFF0')
    pd.rectangle((15,15,21,21),fill='#F0FFF9')
    for x,y in [(3,6),(31,8),(2,27),(29,29)]:
        pd.rectangle((x,y,x+3,y+3),fill='#64E5E4');pd.point((x+1,y+1),fill='#F0FFF9')
    canvas.alpha_composite(portrait.resize((74,74),Image.Resampling.NEAREST),(ox+21,oy+63))
    label('WISP',58,153,center=True)
    label('RETRATO 3D',58,167,'#B5A88F',True)
    label('LVL 7',116,26)
    ep='8920 / 9800'
    label(ep,236-(sum(len(FONT[c][0])+1 for c in ep)-1),26,'#B5A88F')
    paste('bar_background',116,38)
    canvas.alpha_composite(kit['bar_ep'].crop((0,0,85,5)),(ox+116,oy+38))
    # Bars end at 236, aligned with the EP bar; values start at 240, clear of the bars.
    for title,name,y,value,numbers in [('VIDA','life',48,1,'8/8'),('MANA','mana',58,92/125,'92/125'),('FOME','hunger',68,.7,'14/20')]:
        label(title,116,y,'#B5A88F')
        paste('bar_background_short',146,y+1)
        canvas.alpha_composite(kit['bar_'+name].crop((0,0,math.floor(90*value),5)),(ox+146,oy+y+1))
        label(numbers,240,y)
    # The +N counter needs no caption; a caption would collide with the mana value.
    paste('points_box_active',270,24,(38,28),4)
    label('+2',289,33,'#FFB9A3',True)
    for i,(name,effect,spent,pending) in enumerate([
            ('ABSORCAO','3.0%/PULSO',2,1),('EFICIENCIA','EP X1.3',3,0),
            ('RESERVA','70 MANA',4,0),('VITALIDADE','3 NUCLEOS',1,0)]):
        y=84+22*i
        label(name,116,y)
        label(effect,116,y+10,'#E2775F' if pending else '#B5A88F')
        paste('button' if pending else 'button_disabled',202,y,(11,11),3)
        label('-',206,y+2,'#F3EAD6' if pending else '#6F655A')
        for j in range(10):
            state='filled' if j<spent else 'pending' if j<spent+pending else 'empty'
            paste('pip_'+state,215+8*j,y+2)
        paste('button',297,y,(11,11),3);label('+',301,y+2)
    paste('button',190,176,(56,18),3)
    paste('button_primary',252,176,(56,18),3)
    label('CANCELAR',218,181,center=True);label('CONFIRMAR',280,181,center=True)
    label('1 PENDENTE',116,181,'#E2775F')
    return canvas.resize((854,480),Image.Resampling.NEAREST)


def main():
    sprites=make_sprites();glyphs=make_glyphs()
    grimoire=make_grimoire()
    grimoire_mockup(grimoire).save(ROOT/'mockup_grimoire.png')
    Image.open(ROOT/'mockup_grimoire.png').resize((1708,960),Image.Resampling.NEAREST).save(ROOT/'grimoire_200.png')
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
    frames=[]
    for phase in range(BAR_FRAMES):
        animated=sprites | {kind+'_bar_progress':animated_bar(kind,phase) for kind in ('mana','dna')}
        frames.append(mockup(animated,glyphs,True))
    frames[0].save(ROOT/'bars_animated.gif',save_all=True,append_images=frames[1:],duration=BAR_FRAME_TICKS*50,loop=0,disposal=2)
    (ROOT/'manifest.json').write_text(json.dumps({name:list(image.size) for name,image in sprites.items()},indent=2)+'\n')
    print(f'Generated {len(sprites)} sprites, {len(glyphs)} glyphs, font provider, two mockups and variants.')


if __name__=='__main__':main()
