"""Preserve licensed JDTE animation; split dragon art into tintable RGBA layers."""
import struct
import zlib
from pathlib import Path

def read_rgba(path):
    data = path.read_bytes(); offset = 8; compressed = bytearray()
    while offset < len(data):
        size = struct.unpack('>I', data[offset:offset+4])[0]
        kind, payload = data[offset+4:offset+8], data[offset+8:offset+8+size]
        if kind == b'IHDR':
            width, height, bits, mode, _, _, interlace = struct.unpack('>IIBBBBB', payload)
            assert (bits, mode, interlace) == (8, 6, 0)
        if kind == b'IDAT': compressed.extend(payload)
        offset += size + 12
    raw = zlib.decompress(compressed); previous = bytearray(width*4); rows = []
    for y in range(height):
        start = y*(width*4+1); filt = raw[start]; row = bytearray(raw[start+1:start+1+width*4])
        for i in range(len(row)):
            a = row[i-4] if i >= 4 else 0; b = previous[i]; c = previous[i-4] if i >= 4 else 0
            if filt == 1: predictor = a
            elif filt == 2: predictor = b
            elif filt == 3: predictor = (a+b)//2
            elif filt == 4:
                p = a+b-c; distances = [abs(p-a), abs(p-b), abs(p-c)]
                predictor = [a,b,c][distances.index(min(distances))]
            else:
                assert filt == 0; predictor = 0
            row[i] = (row[i]+predictor) & 255
        rows.append(row); previous = row
    return width, height, rows

def write_rgba(path, width, height, rows):
    def chunk(kind, data):
        return struct.pack('>I', len(data))+kind+data+struct.pack('>I', zlib.crc32(kind+data)&0xffffffff)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR', struct.pack('>IIBBBBB', width,height,8,6,0,0,0))
        +chunk(b'IDAT', zlib.compress(b''.join(b'\0'+row for row in rows)))+chunk(b'IEND', b''))

def generate(root, write):
    source = Path(__file__).resolve().parents[1]/'third_party/jdte'
    destination = root/'assets/appliedagristics/textures/item'
    for name in ['wither_essence', 'ender_dragon_essence']:
        (destination/f'jdte_{name}.png').write_bytes((source/f'{name}.png').read_bytes())
        write(f'assets/appliedagristics/textures/item/jdte_{name}.png.mcmeta', {'animation': {'frametime': 2, 'interpolate': False}})
        write(f'assets/appliedagristics/models/item/bio_{name}.json', {'parent': 'minecraft:item/generated',
            'textures': {'layer0': f'appliedagristics:item/jdte_{name}'}})
    width, height, pixels = read_rgba(source/'ender_dragon_essence.png')
    for layer in range(3):
        rows = []
        for source_row in pixels:
            row = bytearray()
            for x in range(width):
                r,g,b,a = source_row[x*4:x*4+4]
                colored = max(r,g,b)-min(r,g,b) > 20
                body = not colored and max(r,g,b) > 30
                target = 1 if colored else 0 if body else 2
                if a == 0 or target != layer: row.extend((0,0,0,0))
                elif layer == 2: row.extend((r,g,b,a))
                else:
                    shade = max(r,g,b) if colored else min(180,max(r,g,b)*3)
                    row.extend((shade,shade,shade,a))
            rows.append(row)
        write_rgba(destination/f'bio_essence_{layer}.png', width,height,rows)
        write(f'assets/appliedagristics/textures/item/bio_essence_{layer}.png.mcmeta', {'animation': {'frametime': 2, 'interpolate': False}})
    write('assets/appliedagristics/models/item/bio_essence.json', {'parent':'minecraft:item/generated',
        'textures': {f'layer{i}': f'appliedagristics:item/bio_essence_{i}' for i in range(3)},
        'overrides': [
            {'predicate': {'appliedagristics:boss': 1}, 'model': 'appliedagristics:item/bio_wither_essence'},
            {'predicate': {'appliedagristics:boss': 2}, 'model': 'appliedagristics:item/bio_ender_dragon_essence'}]})
