"""Generate base resources and development recipes; install editable art exports last (Python 3.10+)."""
from pathlib import Path
import json
import struct
import zlib
import gzip

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
NS = 'appliedagristics'

def write(path, value):
    path = ROOT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def png(path, accent, item=False):
    pixels = []
    for y in range(16):
        row = bytearray()
        for x in range(16):
            if item and (x < 2 or x > 13 or y < 1 or y > 14):
                color = (0, 0, 0, 0)
            elif x in (0, 15) or y in (0, 15):
                color = (39, 54, 54, 255)
            elif x in (2, 13) or y in (2, 13):
                color = (110, 132, 123, 255)
            elif (5 <= x <= 10 and 5 <= y <= 10) or (y == 7 and 2 <= x <= 13):
                color = (*accent, 255)
            else:
                shade = 68 + (x + y) % 3 * 4
                color = (shade, shade + 15, shade + 10, 255)
            row.extend(color)
        pixels.append(b'\0' + bytes(row))
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(b''.join(pixels))) + chunk(b'IEND', b'')
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)

blocks = {'early_planter': (115, 204, 97), 'seed_storage': (233, 199, 92), 'network_planter': (97, 216, 198), 'cell_cultivator': (80, 170, 220)}
items = {'bio_sampler': (225, 225, 220), 'mature_culture': (115, 210, 155), 'bio_extract': (210, 105, 185), 'culture_capsule': (170, 215, 230), 'culture_nutrient': (220, 180, 90), 'bio_seed': (180, 180, 180), 'bio_essence': (200, 200, 200)}
# Machine geometry, textures and directional states belong to the approved art installer.
for name, color in items.items():
    if name not in ('bio_seed','bio_essence'):
        png(f'assets/{NS}/textures/item/{name}.png', color, True)
    write(f'assets/{NS}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})

names = {
    'early_planter': ('基础种植机', 'Basic Planter'), 'seed_storage': ('种子存储驱动器', 'Seed Storage Drive'),
    'network_planter': ('ME 种植机', 'ME Planter'),
    'cell_cultivator': ('细胞培养器', 'Cell Cultivator'), 'bio_sampler': ('生物采样器', 'Biological Sampler'), 'mature_culture': ('成熟培养物', 'Mature Culture'), 'bio_extract': ('生物萃取物', 'Biological Extract'), 'culture_capsule': ('空培养胶囊', 'Empty Culture Capsule'), 'culture_nutrient': ('营养剂', 'Culture Nutrient'), 'bio_seed': ('生物种子', 'Biological Seed'), 'bio_essence': ('生物精华', 'Biological Essence')}
labels = {
    'search_hint': ('英文作物 ID，如 iron', 'Crop ID, e.g. iron'), 'seed_count': ('种子（占用 / 总数）', 'Seeds (used / total)'),
    'stock': ('现有库存', 'Stock'), 'state': ('状态', 'State'),
    'search': ('搜索', 'Search'), 'collect': ('收容种子', 'Collect Seeds'), 'target': ('目标库存', 'Stock Target'),
    'seed': ('种子', 'Seed'), 'farmland': ('耕地', 'Soil'), 'output': ('产出', 'Output'),
    'harvest_rules': ('按成熟作物原生掉落及耕地等级收获；保留一份种子用于重种。', 'Harvest uses native mature drops and soil tier; one seed is reserved for replanting.'),
    'cycle_help': ('基础 %s 秒 / 加水 %s 秒', 'Dry %s seconds / watered %s seconds'),
    'seed_chance': ('额外种子概率：%s%%', 'Extra seed chance: %s%%'),
    'fertilizer_chance': ('肥料精华概率：%s%%（遵循神秘农业配置）', 'Fertilized essence chance: %s%% (Mystical Agriculture config)'),
    'water': ('水：%s / 16000 mB', 'Water: %s / 16000 mB'), 'altar_inputs': ('材料缓存', 'Materials'),
    'remainders': ('返还物', 'Remainders')}
labels['auto_output_label'] = ('自动输出', 'Auto output')
labels.update({
    'crux': ('基座', 'Crux'), 'crux_help': ('需要将 %s 放在机器正下方。', 'Requires %s directly below the machine.'), 'seed_parallel': ('种子（占用 / 并行）', 'Seeds (used / capacity)'),
    'network_speed': ('速度倍率：%s', 'Speed multiplier: %s'), 'network_power': ('当前生产耗电：%s AE/t', 'Current production power: %s AE/t')})
labels.update({'required_power': ('运行所需：%s AE/t', 'Required while running: %s AE/t'),
               'next_speed_power': ('再加一个速度升级：%s AE/t', 'One more speed upgrade: %s AE/t'),
               'next_parallel_power': ('再加一个并行升级：%s AE/t（按已有种子）', 'One more parallel upgrade: %s AE/t (available seeds)')})
labels.update({'enable_all': ('全开', 'All on'), 'disable_all': ('全关', 'All off'),
               'io_config': ('输入／输出配置', 'Input / Output Configuration'),
               'target_help': ('-1：无限种植；0：停止补货。整批收获可能超过目标。', '-1: unlimited; 0: no restocking. Complete batches may exceed the target.'),
               'auto_output_on': ('自动输出：开', 'Auto output: on'), 'auto_output_off': ('自动输出：关', 'Auto output: off'),
               'altar_acceleration': ('AE 加速卡：四个独立槽，每槽一张；每张速度翻倍。', 'AE acceleration cards: four slots, one per slot; each doubles speed.')})
labels.update({'bio_name': ('%s（%s）', '%s (%s)'), 'bio_identity': ('生物：%s', 'Entity: %s'), 'bio_exchange': ('%s → %s', '%s → %s'), 'bio_more_drops': ('另有 %s 种战利品', '%s more loot types'), 'bio_open_guide': ('手持右键查看全部兑换图案', 'Use to view all exchange patterns'), 'bio_guide_help': ('滚动查看；每个有图标的格子放一份精华', 'Scroll; use one essence per marked slot'), 'bio_no_drops': ('此生物尚无可兑换的物品掉落', 'No item loot exchanges for this entity'),
    'culture_toggle_on': ('萃取✓', 'Auto✓'), 'culture_toggle_off': ('萃取×', 'Auto×'),
    'culture_toggle_help': ('开启：培养后自动萃取；关闭：积攒成熟培养物。当前批次会完成。', 'On: automatically extract mature cultures; off: keep cultures. Active batches finish.'),
    'culture_sample': ('样本', 'Sample'), 'culture_nutrient': ('营养', 'Food'), 'culture_capsule': ('胶囊', 'Shell'), 'culture_cells': ('培养物', 'Culture'),
    'culture_progress': ('%s / %s 秒；%s FE/t', '%s / %s seconds; %s FE/t')})
for removed in ('bio_exchange','bio_more_drops','bio_open_guide','bio_guide_help','bio_no_drops'):
    labels.pop(removed,None)
for i, pair in enumerate([('下', 'Down'), ('上', 'Up'), ('北', 'North'), ('南', 'South'), ('西', 'West'), ('东', 'East')]):
    labels[f'side_{i}'] = pair
for i, pair in enumerate([('禁用', 'Disabled'), ('输入', 'Input'), ('输出', 'Output'), ('双向', 'Both')]):
    labels[f'io_{i}'] = pair
statuses = {
    'idle': ('待机', 'Idle'), 'disabled': ('未启用', 'Disabled'), 'missing_inputs': ('缺少材料', 'Missing input'),
    'wrong_farmland': ('耕地或生长条件不符', 'Invalid soil or growing conditions'), 'missing_seed': ('无可用种子', 'No free seed'), 'output_full': ('输出阻塞', 'Blocked'), 'watered': ('水加速', 'Watered'),
    'growing': ('种植中', 'Growing'), 'online': ('已联网', 'Online'), 'offline': ('网络离线', 'Offline'),
    'target_reached': ('已达目标', 'Stocked'), 'no_power': ('电量不足', 'No power'), 'no_recipe': ('无匹配配方', 'No recipe'), 'crafting': ('制作中', 'Crafting'), 'culturing': ('培养中', 'Culturing'), 'extracting': ('萃取中', 'Extracting'), 'no_water': ('缺水', 'No water')}
for locale, index in [('zh_cn', 0), ('en_us', 1)]:
    lang = {'itemGroup.appliedagristics': 'Applied Agristics'}
    for name, pair in names.items():
        lang[f'{"block" if name in blocks else "item"}.{NS}.{name}'] = pair[index]
    lang.update({f'gui.{NS}.{name}': pair[index] for name, pair in labels.items()})
    lang.update({f'status.{NS}.{name}': pair[index] for name, pair in statuses.items()})
    jei = {
        'sampling': ('生物采样', 'Biological Sampling'), 'culture': ('细胞培养', 'Cell Cultivation'),
        'extraction': ('生物萃取', 'Biological Extraction'), 'bio_planting': ('生物精华种植', 'Biological Essence Farming'),
        'mob_reference': ('生物示意；右键活体采样，不消耗刷怪蛋', 'Entity reference; sample a living mob, no egg consumed'),
        'process_cost': ('%s 秒 · %s FE/t', '%s seconds · %s FE/t'),
        'single_use': ('每份输入消耗一次，保留生物标签', 'Input consumed once; entity identity retained'),
        'sampling_help': ('右键活体采样，不伤害生物', 'Use on a living mob; no damage'),
        'planting_help': ('机器种植，保留种子；不能种在地上', 'Machine farming; seed kept; no field planting')}
    lang.update({f'jei.{NS}.{name}': pair[index] for name, pair in jei.items()})
    write(f'assets/{NS}/lang/{locale}.json', lang)

# Equipment and consumable recipes are maintained directly in data/appliedagristics/recipe.
# Never overwrite their survival progression with the old development recipes.
write(f'data/{NS}/recipe/basic_farmland.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
    'ingredients': [{'item': 'minecraft:dirt'}, {'item': 'minecraft:wheat_seeds'}], 'result': {'id': 'minecraft:farmland', 'count': 1}})
write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'{NS}:{name}' for name in blocks]})
write('pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Applied Agristics resources'}})

# AE's own screen loader, generated background, toolbar, slots and upgrade panel.
write('assets/ae2/screens/appliedagristics_machine_io.json', {
    'includes': ['common/common.json'], 'generatedBackground': {'width': 176, 'height': 134},
    'text': {
        'dialog_title': {'text': {'translate': 'gui.appliedagristics.io_config'}, 'position': {'left': 8, 'top': 6}},
        'autoOutputLabel': {'text': {'translate': 'gui.appliedagristics.auto_output_label'}, 'position': {'left': 8, 'top': 111}}},
    'widgets': {'back': {'right': 23, 'top': -5},
        'side0': {'left': 68, 'top': 72}, 'side1': {'left': 68, 'top': 24},
        'side2': {'left': 68, 'top': 48}, 'side3': {'left': 92, 'top': 72},
        'side4': {'left': 44, 'top': 48}, 'side5': {'left': 92, 'top': 48},
        'autoOutput': {'left': 142, 'top': 104}}})

# Empty 8x6x8 GameTest structure; no third-party template assets.
def nbt_name(name):
    data = name.encode('utf-8')
    return struct.pack('>H', len(data)) + data
template = b'\x0a\0\0'
template += b'\x03' + nbt_name('DataVersion') + struct.pack('>i', 3955)
template += b'\x09' + nbt_name('size') + b'\x03' + struct.pack('>i', 3) + struct.pack('>iii', 8, 6, 8)
for name in ['blocks', 'entities']:
    template += b'\x09' + nbt_name(name) + b'\x0a' + struct.pack('>i', 0)
template += b'\x09' + nbt_name('palette') + b'\x0a' + struct.pack('>i', 1)
template += b'\x08' + nbt_name('Name') + nbt_name('minecraft:air') + b'\0\0'
template_path = ROOT / f'data/{NS}/structure/empty.nbt'
template_path.parent.mkdir(parents=True, exist_ok=True)
template_path.write_bytes(gzip.compress(template, mtime=0))

# Shared layered masks. Primary/secondary tint comes from each entity's spawn egg.
def mask_png(name, layer, seed):
    shape = ["  ##  ", " #### ", "######", "######", " #### ", "  ##  ", "   #  "] if seed else ["   ##   ", "  ####  ", " ###### ", "########", "########", " ###### ", "  ####  ", "   ##   "]
    pixels = []
    points = {(x + (5 if seed else 4), y + (4 if seed else 3)) for y, row in enumerate(shape) for x, c in enumerate(row) if c == '#'}
    for y in range(16):
        row = bytearray()
        for x in range(16):
            if seed:
                # Three staggered grains, with soft tinted edges and a bright face.
                # Keep each grain's shading local rather than shading the whole icon.
                color = (0, 0, 0, 0)
                for sx, sy in [(6, 1), (2, 9), (10, 7)]:
                    dx, dy = x - sx, y - sy
                    grain = [" ## ", "####", "####", "####", "####", " ## "]
                    if not (0 <= dx < 4 and 0 <= dy < 6 and grain[dy][dx] == '#'):
                        continue
                    spot = (dx, dy) in [(2, 3), (2, 4)]
                    if layer == (1 if spot else 0):
                        shade = 255 if dx == 1 and 1 <= dy <= 3 else (210 if dx == 2 and dy <= 2 else 135 if dx == 0 else 105 if dy == 5 else 165)
                        color = (shade, shade, shade, 255)
                row.extend(color)
                continue
            edge = (x, y) in points and any((x + dx, y + dy) not in points for dx, dy in [(1,0),(-1,0),(0,1),(0,-1)])
            core = (x, y) in points and not edge and 7 <= x <= 9
            if (x, y) not in points or (layer == 2 and not edge) or (layer < 2 and edge) or (layer == 1 and not core) or (layer == 0 and core): color = (0,0,0,0)
            elif layer == 2: color = (32,35,42,255)
            else:
                shade = max(105, min(255, 255 - (x-5)*14 - (y-4)*9))
                color = (shade,shade,shade,255)
            row.extend(color)
        pixels.append(b'\0'+bytes(row))
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    data=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(pixels)))+chunk(b'IEND',b'')
    (ROOT/f'assets/{NS}/textures/item/{name}_{layer}.png').write_bytes(data)
for name in ['bio_seed','bio_essence']:
    for layer in range(3):mask_png(name,layer,name=='bio_seed')
    write(f'assets/{NS}/models/item/{name}.json', {'parent':'minecraft:item/generated','textures':{f'layer{i}':f'{NS}:item/{name}_{i}' for i in range(3)}})
write(f'data/{NS}/recipe/bio_seed.json', {'type':f'{NS}:bio_seed','category':'misc'})
write(f'data/{NS}/recipe/bio_loot.json', {'type':f'{NS}:bio_loot','category':'misc'})
from essence_layers import generate as generate_essence
generate_essence(ROOT, write)
# Keep the current registration placeholders and retired-recipe policy after the legacy generator.
import runpy
runpy.run_path(str(Path(__file__).with_name('generate_multiblock_resources.py')))
