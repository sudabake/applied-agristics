"""Install the approved ME machine geometry and palette-only altar recolor."""
from pathlib import Path
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/approved/me_machines_16_20261106/runtime'


def install(assets=None):
    assets = Path(assets) if assets else ROOT / 'src/main/resources/assets/appliedagristics'
    for source in SOURCE.rglob('*'):
        if source.is_file():
            target = assets / source.relative_to(SOURCE)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
    for suffix in ('.png', '.png.mcmeta'):
        (assets / ('textures/block/me_bio_cultivator_front' + suffix)).unlink(missing_ok=True)
    (assets / 'models/block/templates/casing_front_north.json').unlink(missing_ok=True)
    from install_art_resources import collision_shapes
    from install_shared_casing import PARTS
    selection=json.loads((SOURCE.parent / 'selection_boxes.json').read_text(encoding='utf-8'))
    collision_shapes([*PARTS, 'early_planter', 'me_infusion_altar', 'me_bio_cultivator'],selection)


if __name__ == '__main__':
    install()
