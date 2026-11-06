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
    write(RES/'data/c/tags/item/seeds.json', {'replace': False, 'values': ['appliedagristics:bio_seed']})
    source = ROOT / 'art/approved/user_items_16_20261028'
    for name in ('bio_sampler.png', 'culture_nutrient.png',
                 'bio_essence_0.png', 'bio_essence_0.png.mcmeta',
                 'bio_essence_1.png', 'bio_essence_1.png.mcmeta',
                 'bio_essence_2.png', 'bio_essence_2.png.mcmeta'):
        if (source / name).exists():
            shutil.copyfile(source / name, ASSETS / 'textures/item' / name)
    for name in ('bio_sampler','bio_sampler_sampled'):
        shutil.copyfile(ROOT / f'art/approved/user_sampler_16_20261031/{name}.png', ASSETS / f'textures/item/{name}.png')
    write(ASSETS / 'models/item/bio_sampler.json', {'parent': 'minecraft:item/generated',
        'textures': {'layer0': 'appliedagristics:item/bio_sampler'},
        'overrides': [{'predicate': {'appliedagristics:sampled': 1}, 'model': 'appliedagristics:item/bio_sampler_sampled'}]})
    write(ASSETS / 'models/item/bio_sampler_sampled.json', {'parent': 'minecraft:item/generated',
        'textures': {'layer0': 'appliedagristics:item/bio_sampler_sampled'}})
    # Functional faces point south in the unformed models; show that face in inventory.
    for name in ('network_planter','seed_storage','me_network_access','planter_input_port','planter_output_port','essence_vault'):
        path=ASSETS / f'models/item/{name}.json'
        model=json.loads(path.read_text(encoding='utf-8'))
        model.setdefault('display',{})['gui']={'rotation':[30,45,0], 'scale':[.625,.625,.625]}
        write(path,model)
    recipes = RES / 'data/appliedagristics/recipe'
    def shaped(name, pattern, key):
        write(recipes / f'{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc',
            'pattern': pattern, 'key': key, 'result': {'id': f'appliedagristics:{name}', 'count': 1}})
    shaped('bio_sampler', [' W', 'S '], {'W': {'tag': 'minecraft:wool'}, 'S': {'item': 'minecraft:stick'}})
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
        'gui.appliedagristics.collect': ('收容种子', 'Collect Seeds'),
        'gui.appliedagristics.show_ordinary': ('显示普通作物', 'Show ordinary'),
        'gui.appliedagristics.hide_ordinary': ('隐藏普通作物', 'Hide ordinary'),
        'gui.appliedagristics.culture_progress': ('%s / %s 秒', '%s / %s seconds'),
        'jei.appliedagristics.planting_help': ('可种在耕地上，也可使用种植机', 'Plant on farmland or grow in a planter'),
        'status.appliedagristics.missing_input_port': ('缺少种子来源：种子仓或 ME 种子仓', 'Missing seed storage or ME seed storage'),
        'jei.appliedagristics.optional_storages': ('ME 种子仓、作物仓可用种子仓／输出口替代', 'Seed storage/output ports replace optional ME storages'),
    }
    for index, lang in enumerate(('zh_cn', 'en_us')):
        path = ASSETS / f'lang/{lang}.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        data.update({key: values[index] for key, values in labels.items()})
        write(path, data)
    from install_native_block_art import install as install_native_blocks
    install_native_blocks()
    from install_item_tooltips import install as install_item_tooltips
    install_item_tooltips()
    from install_bio_crop_art import install as install_bio_crop_art
    install_bio_crop_art()
    labels={
        'status.appliedagristics.waiting':('等待生产','Waiting to grow'),
        'gui.appliedagristics.crop_species':('%s植株','%s Crop'),
        'config.jade.plugin_appliedagristics.bio_crop_identity':('生物作物身份','Biological crop identity'),
    }
    for index,lang in enumerate(('zh_cn','en_us')):
        path=ASSETS/f'lang/{lang}.json';data=json.loads(path.read_text(encoding='utf-8'))
        data.update({key:values[index] for key,values in labels.items()});write(path,data)

    from install_me_biology import install as install_me_biology
    install_me_biology()
    from install_ui_unification import install as install_ui_unification
    install_ui_unification()
    from install_standalone_faces import install as install_standalone_faces
    install_standalone_faces()
    from install_machine_refresh import install as install_machine_refresh
    install_machine_refresh()
    from remove_retired_content import install as remove_retired_content
    remove_retired_content()

if __name__ == '__main__':
    install()
