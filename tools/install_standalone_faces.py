"""Native full-face models on the user-selected 16px frame, including formed orientations."""
from pathlib import Path
import json
import shutil
from install_me_biology import A, ROOT, write
from install_shared_casing import FACE_ORDER, FUNCTIONAL, MODULES, PARTS, NS


def rotation(facing):
    if facing in ('up', 'down'):
        return {'x': 90 if facing == 'up' else 270}
    angle = {'south': 0, 'west': 90, 'north': 180, 'east': 270}[facing]
    return {'y': angle} if angle else {}


def install(assets=None):
    assets = Path(assets) if assets else A
    textures, models = assets / 'textures/block', assets / 'models/block'
    source = ROOT / 'art/approved/machine_frame_16_20261105/runtime'
    for path in source.glob('*.png'):
        shutil.copyfile(path, textures / path.name)
    write(textures / 'me_bio_cultivator_front.png.mcmeta',
          {'animation': {'frametime': 3, 'interpolate': False}})
    for front in ('south', 'north'):
        suffix = '' if front == 'south' else '_north'
        write(models / f'templates/casing_front{suffix}.json', {
            'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'textures': {'casing': NS + 'planter_frame', 'particle': '#casing'},
            'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16],
                          'faces': {face: {'texture': '#front' if face == front else '#casing',
                                           'cullface': face} for face in FACE_ORDER}}],
        })
    write(models / 'planter_frame.json', {'parent': 'minecraft:block/cube_all',
                                        'textures': {'all': NS + 'planter_frame'}})
    for name in FUNCTIONAL:
        write(models / (name + '.json'), {'parent': NS + 'templates/casing_front',
                                        'textures': {'front': NS + name + '_front'}})
    for name in MODULES:
        write(models / (name + '.json'), {'parent': 'minecraft:block/cube_all',
                                         'textures': {'all': NS + name}})
    write(models / 'me_bio_cultivator.json', {'parent': NS + 'templates/casing_front_north',
                                           'textures': {'front': NS + 'me_bio_cultivator_front'}})
    # Placement and wrench rotation own facing, independently of structure membership.
    for name in PARTS:
        directions = ('north', 'east', 'south', 'west', 'up', 'down') if name in (
            'me_network_access', 'planter_input_port', 'planter_output_port') else ('north', 'east', 'south', 'west')
        variants = {}
        for facing in directions:
            angles = rotation(facing) if name in FUNCTIONAL else {}
            variants[f'facing={facing}'] = {'model': NS + name, **angles}
        write(assets / 'blockstates' / (name + '.json'), {'variants': variants})
    altar = models / 'me_infusion_altar.json'
    data = json.loads(altar.read_text(encoding='utf-8'))
    data['textures']['3'] = NS + 'me_infusion_altar_top'
    write(altar, data)
    # State variants now directly reuse the ordinary models, including the identical glass cells.
    for path in (models / 'formed').glob('*.json'):
        path.unlink()
    for path in (models / 'templates').glob('formed_casing_*.json'):
        path.unlink()
    (models / 'templates/casing_module.json').unlink(missing_ok=True)
    for name in (*[name + '_joined_icon' for name in MODULES],
                 'network_planter_off', 'network_planter_idle', 'network_planter_working',
                 'me_infusion_pedestal_top', 'bottom', 'utils/transparent'):
        for suffix in ('.png', '.png.mcmeta'):
            (textures / (name + suffix)).unlink(missing_ok=True)


if __name__ == '__main__':
    install()
