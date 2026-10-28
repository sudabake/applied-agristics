"""Approved 16px block faces on the existing geometry; keep the formed shell intact."""
from pathlib import Path
import json
import shutil

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/appliedagristics'
DRAFT=ROOT/'art/drafts/block_faces_16_v01'
FUNCTIONAL=('network_planter','seed_storage','me_network_access','planter_input_port','planter_output_port','essence_vault')
SELECTED_FUNCTIONAL={
    'network_planter':('controller_16_v01','controller_a'),
    'me_network_access':('me_network_access_user_16_v01','access'),
    'planter_input_port':('seed_storage_16_v01','seed_b'),
    'seed_storage':('me_seed_storage_16_v01','me_a'),
    'essence_vault':('essence_vault_16_v01','vault'),
    'planter_output_port':('output_port_user_16_v01','outlet'),
}
TEXTURES=tuple(f'{name}_{face}' for name in FUNCTIONAL if name not in SELECTED_FUNCTIONAL for face in ('front','side','top','back'))+(
    'cell_cultivator',
    'me_infusion_altar_side','me_infusion_altar_bottom','me_infusion_pedestal_top',
    'biological_altar_side','biological_altar_bottom','biological_altar_top',
    )

def install():
    for name in TEXTURES:
        shutil.copyfile(DRAFT/(name+'.png'),ASSETS/'textures/block'/(name+'.png'))
    for name,(folder,prefix) in SELECTED_FUNCTIONAL.items():
        for face in ('front','side','top','back'):
            shutil.copyfile(ROOT/'art/drafts'/folder/f'{prefix}_{face}.png',ASSETS/'textures/block'/f'{name}_{face}.png')
    shutil.copyfile(ROOT/'art/drafts/planter_frame_16_v02/planter_frame.png',ASSETS/'textures/block/planter_frame.png')
    shutil.copyfile(ROOT/'art/drafts/speed_module_16_v01/speed_a.png',ASSETS/'textures/block/planter_speed_module.png')
    shutil.copyfile(ROOT/'art/drafts/parallel_module_16_v01/parallel_a.png',ASSETS/'textures/block/planter_parallel_module.png')
    shutil.copyfile(ROOT/'art/drafts/conversion_module_16_v01/conversion_b.png',ASSETS/'textures/block/planter_conversion_module.png')
    controller=ROOT/'art/drafts/controller_16_v03'
    for state in ('off','idle','working'):
        shutil.copyfile(controller/f'controller_{state}.png',ASSETS/f'textures/block/network_planter_{state}.png')
    shutil.copyfile(controller/'controller_joined_icon.png',ASSETS/'textures/block/network_planter_joined_icon.png')
    for face in ('window','top'):
        shutil.copyfile(ROOT/f'art/drafts/cultivation_chamber_16_v01/chamber_a_{face}.png',ASSETS/f'textures/block/cultivation_chamber_{face}.png')
    # Restore the previously selected planter, rather than installing the batch draft.
    shutil.copyfile(ROOT/'art/drafts/basic_planter_v05/basic_planter.png',ASSETS/'textures/block/basic_planter.png')
    # Alpha in the PNG is ignored by the default solid render layer.
    paths=[ASSETS/'models/block/cultivation_chamber.json',*(ASSETS/'models/block/formed').glob('cultivation_chamber_*.json')]
    for path in paths:
        model=json.loads(path.read_text(encoding='utf-8'))
        model['render_type']='minecraft:translucent'
        path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    for name in ('wither_essence','ender_dragon_essence'):
        for suffix in ('.png','.png.mcmeta'):
            shutil.copyfile(ROOT/'art/drafts/boss_essences_uniform_16_v01'/(name+suffix),ASSETS/'textures/item'/(name+suffix))
    path=ASSETS/'models/block/biological_altar.json'
    model=json.loads(path.read_text(encoding='utf-8'))
    model['textures']['3']='appliedagristics:block/biological_altar_top'
    path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    from install_connected_array_art import install as install_connected
    install_connected(ASSETS)
    from resource_audit import prune_retired
    prune_retired(ASSETS)

if __name__=='__main__':
    install()
