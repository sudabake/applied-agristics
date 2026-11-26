"""Native planting data, with the approved Botany Trees yield baseline for trees."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def install():
    trees = {
        'oak_sapling': 'oak_log', 'spruce_sapling': 'spruce_log',
        'birch_sapling': 'birch_log', 'jungle_sapling': 'jungle_log',
        'acacia_sapling': 'acacia_log', 'dark_oak_sapling': 'dark_oak_log',
        'cherry_sapling': 'cherry_log', 'mangrove_propagule': 'mangrove_log',
        'azalea': 'oak_log', 'flowering_azalea': 'oak_log',
        'crimson_fungus': 'crimson_stem', 'warped_fungus': 'warped_stem',
    }
    for seed, log in trees.items():
        def item_pool(item, chance=None, count=None):
            entry = {'type': 'minecraft:item', 'name': 'minecraft:' + item}
            if chance is not None:
                entry['conditions'] = [{'condition': 'minecraft:random_chance', 'chance': chance}]
            if count is not None:
                entry['functions'] = [{'function': 'minecraft:set_count', 'count': count}]
            return {'rolls': 1, 'entries': [entry]}

        pools = [item_pool(log, count={'type': 'minecraft:uniform', 'min': 1, 'max': 4}),
                 item_pool(seed, chance=0.05)]
        if seed in ('oak_sapling', 'dark_oak_sapling'):
            pools.append(item_pool('apple', chance=0.05))
        name = 'native/' + seed
        write(ROOT / f'data/appliedagristics/loot_table/planting/{seed}.json',
              {'type': 'minecraft:block', 'pools': pools})
        write(ROOT / f'data/appliedagristics/recipe/{name}.json', {
            'type': 'appliedagristics:planting', 'seed': {'item': 'minecraft:' + seed},
            'outputs': [{'id': 'minecraft:' + log, 'count': 1}],
            'display': {'Name': 'minecraft:' + seed}, 'ticks': 2400,
            'reproduces': True, 'loot_table': 'appliedagristics:planting/' + seed,
        })

    # These plants lack a mature fruit state usable by the generic crop-loot path.
    for plant in ('sugar_cane', 'cactus', 'bamboo', 'kelp', 'glow_berries', 'brown_mushroom', 'red_mushroom'):
        write(ROOT / f'data/appliedagristics/recipe/native/{plant}.json', {
            'type': 'appliedagristics:planting', 'seed': {'item': 'minecraft:' + plant},
            'outputs': [{'id': 'minecraft:' + plant, 'count': 1}],
            'display': {'Name': 'minecraft:' + ('cave_vines' if plant == 'glow_berries' else plant)},
            'ticks': 200, 'reproduces': True,
        })
    write(ROOT / 'data/appliedagristics/recipe/native/chorus_flower.json', {
        'type': 'appliedagristics:planting', 'seed': {'item': 'minecraft:chorus_flower'},
        'outputs': [{'id': 'minecraft:chorus_fruit', 'count': 1}],
        'display': {'Name': 'minecraft:chorus_flower'}, 'ticks': 200, 'reproduces': False,
    })

    translations = {
        'gui.appliedagristics.required_power': ('当前运行需求：%s AE/t（含批次限制）', 'Current demand: %s AE/t (batch limits included)'),
        'gui.appliedagristics.full_load_power': ('理论满负荷：%s AE/t（忽略批次限制）', 'Theoretical full load: %s AE/t (before batch limits)'),
        'gui.appliedagristics.next_speed_power': ('再加一个速度模块：%s AE/t（含批次限制）', 'One more speed module: %s AE/t (batch limits included)'),
        'gui.appliedagristics.next_parallel_power': ('再加一个并行模块：%s AE/t（已有种子／批次限制）', 'One more parallel module: %s AE/t (available seeds and batch limits)'),
        'jei.appliedagristics.possible_product': ('可能产物／种植材料，图标数量不代表每轮产量或保底掉落', 'Possible output or planting material. Icon counts do not guarantee a yield per cycle'),
    }
    for index, locale in enumerate(('zh_cn', 'en_us')):
        path = ROOT / f'assets/appliedagristics/lang/{locale}.json'
        values = json.loads(path.read_text(encoding='utf-8'))
        values.update({key: pair[index] for key, pair in translations.items()})
        write(path, values)

if __name__ == '__main__':
    install()
