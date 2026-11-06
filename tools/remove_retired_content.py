"""Remove cancelled gameplay resources after the historical art installers have finished."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
RETIRED = ('seed_storage_tier1', 'seed_storage_tier2', 'cell_cultivator',
           'biological_altar', 'culture_capsule', 'mature_culture',
           'bio_extract', 'condensed_bio_essence')
RETIRED_LABELS = {'status.appliedagristics.extracting',
                  'entity.appliedagristics.thrown_culture',
                  'jei.appliedagristics.process_cost'}


def install():
    resources = ROOT / 'src/main/resources'
    assets = resources / 'assets/appliedagristics'
    for name in RETIRED:
        for folder in ('blockstates', 'models/item', 'models/block'):
            (assets / folder / (name + '.json')).unlink(missing_ok=True)
        for folder in ('item', 'block'):
            for path in (assets / 'textures' / folder).glob(name + '*.png*'):
                path.unlink()
        for prefix in ('', 'legacy_'):
            (resources / 'data/appliedagristics/recipe' / (prefix + name + '.json')).unlink(missing_ok=True)
    (resources / 'data/appliedagristics/recipe/legacy_bio_seed.json').unlink(missing_ok=True)
    (assets / 'textures/mob_effect/nourishment.png').unlink(missing_ok=True)
    guide = assets / 'ae2guide/index.md'
    text = guide.read_text(encoding='utf-8')
    text = text.replace('旧精华存储器直接升级为作物仓，原有库存、优先级和随方块保存的数据保留。', '')
    text = text.replace('旧四态设置保留原有输出面并开放各面输入。', '')
    guide.write_text(text, encoding='utf-8')

    for language in ('zh_cn', 'en_us'):
        path = assets / 'lang' / (language + '.json')
        data = json.loads(path.read_text(encoding='utf-8'))
        prefixes = tuple(prefix + name for name in RETIRED
                         for prefix in ('block.appliedagristics.', 'item.appliedagristics.',
                                        'tooltip.appliedagristics.'))
        data = {key: value for key, value in data.items()
                if key not in RETIRED_LABELS
                and not key.startswith((*prefixes, 'effect.appliedagristics.nourishment'))}
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    retired_ids = {'appliedagristics:' + name for name in RETIRED}
    for path in (resources / 'data').rglob('tags/**/*.json'):
        data = json.loads(path.read_text(encoding='utf-8'))
        old = data.get('values', [])
        values = [value for value in old
                  if not (isinstance(value, str) and value in retired_ids)]
        if values != old:
            data['values'] = values
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    install()
