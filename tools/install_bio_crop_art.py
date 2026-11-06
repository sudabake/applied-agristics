"""Install the user-approved native 16px biological crop stages, without resizing."""
from pathlib import Path
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/appliedagristics'
SOURCE = ROOT / 'art/approved/bio_crop_stages_16_20261102/source'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def install():
    variants = {}
    for age in range(8):
        textures = {layer: f'appliedagristics:block/bio_crop_{age}_{layer}' for layer in ('base', 'pods')}
        for layer in textures:
            shutil.copyfile(SOURCE / f'stage_{age}_{layer}.png',
                            ASSETS / f'textures/block/bio_crop_{age}_{layer}.png')
        # Vanilla-style crossed planes. A one-pixel offset meets the 15/16 farmland top.
        elements = []
        for layer in textures:
            for angle in (-45, 45):
                face = {'uv': [0, 0, 16, 16], 'texture': '#' + layer}
                if layer == 'pods':
                    face['tintindex'] = 0
                elements.append({'from': [0, -1, 8], 'to': [16, 15, 8], 'shade': False,
                                 'rotation': {'origin': [8, 0, 8], 'axis': 'y', 'angle': angle, 'rescale': True},
                                 'faces': {side: dict(face) for side in ('north', 'south')}})
        write(ASSETS / f'models/block/bio_crop_{age}.json',
              {'render_type': 'minecraft:cutout', 'textures': {**textures, 'particle': textures['base']},
               'elements': elements})
        variants[f'age={age}'] = {'model': f'appliedagristics:block/bio_crop_{age}'}
    write(ASSETS / 'blockstates/bio_crop.json', {'variants': variants})
    # The basic planter's standalone display uses the exact same mature model.
    write(ASSETS / 'models/block/bio_plant.json', {'parent': 'appliedagristics:block/bio_crop_7'})
    # Superseded stretched-seed sprite: remove only after all models have changed.
    old = ASSETS / 'textures/block/bio_plant.png'
    if old.exists():
        from resource_audit import usage
        if 'block/bio_plant' not in usage(ASSETS)['unreferenced_textures']:
            raise RuntimeError('The old biological plant sprite is still referenced')
        old.unlink()


if __name__ == '__main__':
    install()
