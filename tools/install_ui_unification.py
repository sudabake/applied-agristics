"""Install shared native AE layouts and current six-face module inventory models."""
import json
from install_me_biology import A, RES, write


def install():
    screens = RES / 'assets/ae2/screens'
    for name, width, height, inventory_x, inventory_y, title in (
        ('planter', 320, 240, 79, 160, 'block.appliedagristics.network_planter'),
        ('early', 176, 218, 8, 136, 'block.appliedagristics.early_planter'),
    ):
        write(screens / f'appliedagristics_{name}.json', {
            'includes': ['common/common.json'],
            'generatedBackground': {'width': width, 'height': height},
            'slots': {
                'PLAYER_INVENTORY': {'left': inventory_x, 'top': inventory_y, 'grid': 'BREAK_AFTER_9COLS'},
                'PLAYER_HOTBAR': {'left': inventory_x, 'top': inventory_y + 58, 'grid': 'HORIZONTAL'},
            },
            'text': {
                'dialog_title': {'text': {'translate': title}, 'position': {'left': 8, 'top': 6}},
                'player_inventory_title': {'text': {'translate': 'container.inventory'},
                                           'position': {'left': inventory_x, 'top': inventory_y - 10}},
            },
        })

    path = screens / 'appliedagristics_seeds.json'
    style = json.loads(path.read_text(encoding='utf-8'))
    style['slots']['MACHINE_INPUT']['top'] = 27
    style['widgets']['collect']['top'] = 26
    style['text']['state']['position']['top'] = 31
    for i in range(12):
        style['widgets']['withdraw' + str(i)] = {
            'left': 86 + i % 3 * 100, 'top': 61 + i // 3 * 22, 'width': 16, 'height': 18,
        }
    write(path, style)
    path = screens / 'appliedagristics_seed_settings.json'
    style = json.loads(path.read_text(encoding='utf-8'))
    style['text'].pop('autoLabel', None)
    style['widgets']['autoCollect'] = {'left': 8, 'top': 24, 'width': 160, 'height': 16}
    write(path, style)

    labels = {
        'gui.appliedagristics.auto_new_seeds': ('自动启用新作物', 'Enable new crops'),
        'gui.appliedagristics.hide_vanilla': ('隐藏原版作物', 'Hide vanilla crops'),
        'gui.appliedagristics.io_config': ('输出方向', 'Output sides'),
        'gui.appliedagristics.output_side_on': ('启用', 'Enabled'),
        'gui.appliedagristics.output_side_off': ('禁用', 'Disabled'),
        'gui.appliedagristics.output_enabled': ('启用输出', 'Output enabled'),
        'gui.appliedagristics.output_disabled': ('禁用输出', 'Output disabled'),
        'gui.appliedagristics.auto_collect_help': (
            '开启后接收网络新存入的种子，并每秒收容已有种子；按所选范围过滤',
            'Accepts incoming network seeds and collects stored seeds each second using the selected filter'),
        'gui.appliedagristics.seed_withdraw_hint': ('取出一组未占用的种子', 'Withdraw one available stack'),
        'jei.appliedagristics.planting_products': ('种植机', 'Planter'),
    }
    for index, lang in enumerate(('zh_cn', 'en_us')):
        path = A / f'lang/{lang}.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        data.update({key: values[index] for key, values in labels.items()})
        data.pop('jei.appliedagristics.possible_products', None)
        for suffix in ('altar_acceleration', 'auto_output_on', 'auto_output_off', 'culture_sample', 'culture_nutrient', 'culture_progress', 'io_0', 'io_1', 'io_2', 'io_3', 'ordinary_crops', 'show_ordinary', 'hide_ordinary', 'new_seeds_on', 'new_seeds_off', 'auto_collect_on', 'auto_collect_off'):
            data.pop('gui.appliedagristics.' + suffix, None)
        write(path, data)
    path = screens / 'appliedagristics_machine_io.json'
    style = json.loads(path.read_text(encoding='utf-8'))
    style['text']['dialog_title']['text'] = {'translate': 'gui.appliedagristics.io_config'}
    write(path, style)
    path = A / 'ae2guide/index.md'
    text = path.read_text(encoding='utf-8')
    text = text.replace('ME 种子仓的自动收容开关控制每秒从网络主动拉取已有种子。',
                        'ME 种子仓的自动收容开关同时控制网络新存入种子的接收与每秒主动收容。关闭后仍可手动收容，阵列直产种子沿现有规则分流。')
    text = text.replace('“新种默认启用”', '“自动启用新作物”')
    text = text.replace('只有成型后的 ME 网络接入模块可以连接 AE，整座结构使用一个通道。',
                        '只有成型后的 ME 网络接入模块可以连接 AE，整座结构自身不占频道。')
    text = text.replace('并配置各面的输入／输出模式。', '并开关各面的输出；所有面始终允许输入合法材料。')
    text = text.replace('“拉取种子”按钮用于手动导入网络已有种子，不持续扫描全网。',
                        '“收容种子”按钮手动导入网络已有种子；自动收容开启后每秒按筛选范围收容。')
    extra = '所有 AE 联网机器自身不占频道。ME 加工机六面均可输入，输出方向页只设置各面是否启用输出；启用输出的面仍可输入。'
    if extra not in text:
        text += '\n' + extra + '\n'
    path.write_text(text, encoding='utf-8')


if __name__ == '__main__':
    install()
