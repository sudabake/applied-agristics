"""Install confirmed user item artwork and the October 28 gameplay resources."""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/appliedagristics'

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def install():
    for source in (ROOT / 'art/approved/user_items_16_20261028').glob('*.png*'):
        shutil.copyfile(source, ASSETS / 'textures/item' / source.name)
    shutil.copyfile(ROOT / 'art/drafts/sampler_sampled_16_v01/bio_sampler_sampled.png', ASSETS / 'textures/item/bio_sampler_sampled.png')
    write(ASSETS / 'models/item/bio_sampler.json', {'parent': 'minecraft:item/generated',
        'textures': {'layer0': 'appliedagristics:item/bio_sampler'},
        'overrides': [{'predicate': {'appliedagristics:sampled': 1}, 'model': 'appliedagristics:item/bio_sampler_sampled'}]})
    write(ASSETS / 'models/item/bio_sampler_sampled.json', {'parent': 'minecraft:item/generated',
        'textures': {'layer0': 'appliedagristics:item/bio_sampler_sampled'}})
    # Functional faces point south in the unformed models; show that face in inventory.
    for name in ('network_planter','seed_storage','me_network_access','planter_input_port','planter_output_port','essence_vault','seed_storage_tier1','seed_storage_tier2'):
        path=ASSETS / f'models/item/{name}.json'
        model=json.loads(path.read_text(encoding='utf-8'))
        model.setdefault('display',{})['gui']={'rotation':[30,45,0], 'scale':[.625,.625,.625]}
        write(path,model)
    variants = {}
    for age in range(8):
        # Keep the existing biological plant sprite until block art is redrawn.
        height = 4 + age * 12 / 7
        model = {'render_type': 'minecraft:cutout',
                 'textures': {'plant': 'appliedagristics:block/bio_plant', 'particle': 'appliedagristics:block/bio_plant'},
                 'elements': []}
        for angle in (-45, 45):
            model['elements'].append({'from': [0, 0, 8], 'to': [16, height, 8], 'shade': False,
                'rotation': {'origin': [8, 0, 8], 'axis': 'y', 'angle': angle, 'rescale': True},
                'faces': {face: {'uv': [0, 0, 16, 16], 'texture': '#plant', 'tintindex': 0} for face in ('north', 'south')}})
        write(ASSETS / f'models/block/bio_crop_{age}.json', model)
        variants[f'age={age}'] = {'model': f'appliedagristics:block/bio_crop_{age}'}
    write(ASSETS / 'blockstates/bio_crop.json', {'variants': variants})
    recipes = RES / 'data/appliedagristics/recipe'
    def shaped(name, pattern, key):
        write(recipes / f'{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc',
            'pattern': pattern, 'key': key, 'result': {'id': f'appliedagristics:{name}', 'count': 1}})
    shaped('bio_sampler', [' W', 'S '], {'W': {'tag': 'minecraft:wool'}, 'S': {'item': 'minecraft:stick'}})
    shaped('culture_capsule', [' N ', 'G G', 'N  '], {'N': {'item': 'minecraft:iron_nugget'}, 'G': {'item': 'minecraft:glass'}})
    shaped('early_planter', ['S S', 'PHP', 'SSS'], {'S': {'item': 'minecraft:stone'},
        'P': {'item': 'mysticalagriculture:prosperity_shard'}, 'H': {'item': 'minecraft:hopper'}})
    shaped('planter_input_port', ['H', 'F', 'C'], {'H': {'item': 'minecraft:hopper'},
        'F': {'item': 'appliedagristics:planter_frame'}, 'C': {'item': 'minecraft:chest'}})
    shaped('planter_output_port', ['C', 'F', 'H'], {'H': {'item': 'minecraft:hopper'},
        'F': {'item': 'appliedagristics:planter_frame'}, 'C': {'item': 'minecraft:chest'}})
    path = recipes / 'seed_storage.json'
    recipe = json.loads(path.read_text(encoding='utf-8'))
    recipe['type'] = 'appliedagristics:empty_machine_upgrade'
    recipe['key']['F'] = {'item': 'appliedagristics:planter_input_port'}
    write(path, recipe)
    labels = {
        'block.appliedagristics.planter_input_port': ('种子仓', 'Seed Storage'),
        'block.appliedagristics.planter_output_port': ('输出口', 'Output Port'),
        'block.appliedagristics.bio_crop': ('生物植株', 'Biological Crop'),
        'gui.appliedagristics.collect': ('拉取种子', 'Pull Seeds'),
        'gui.appliedagristics.culture_progress': ('%s / %s 秒', '%s / %s seconds'),
        'jei.appliedagristics.culture_duration': ('培养时间：%s 秒；无需电力', 'Cultivation: %s s; no power required'),
        'jei.appliedagristics.planting_help': ('可种在耕地上，也可使用种植机', 'Plant on farmland or grow in a planter'),
        'status.appliedagristics.missing_input_port': ('缺少种子来源：种子仓或 ME 种子仓', 'Missing seed storage or ME seed storage'),
        'jei.appliedagristics.optional_storages': ('ME 种子仓、精华仓可用种子仓／输出口替代', 'Seed storage/output ports replace optional ME storages'),
    }
    for index, lang in enumerate(('zh_cn', 'en_us')):
        path = ASSETS / f'lang/{lang}.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        data.update({key: values[index] for key, values in labels.items()})
        write(path, data)
    from install_native_block_art import install as install_native_blocks
    install_native_blocks()

if __name__ == '__main__':
    install()
