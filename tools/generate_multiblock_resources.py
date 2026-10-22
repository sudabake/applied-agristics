"""Registration assets and labels, followed by the current editable art exports.

Run after generate_resources.py. Does not build a JAR or install anything.
"""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
ASSETS=ROOT/'assets/appliedagristics'
PARTS={
    'me_network_access':('ME 网络接入模块','ME Network Access',(79,175,195)),
    'planter_frame':('农业阵列框架','Agricultural Array Frame',(148,151,170)),
    'cultivation_chamber':('农业培养舱','Cultivation Chamber',(106,169,115)),
    'planter_speed_module':('农业速度模块','Agricultural Speed Module',(93,165,205)),
    'planter_parallel_module':('农业并行模块','Agricultural Parallel Module',(203,175,91)),
    'planter_conversion_module':('种子转换模块','Seed Conversion Module',(137,186,115)),
    'planter_input_port':('农业输入口','Agricultural Input Port',(104,176,102)),
    'planter_output_port':('农业输出口','Agricultural Output Port',(204,166,93)),
    'essence_vault':('精华存储器','Essence Storage',(161,121,198))}
LEGACY=('compact_altar','awakening_upgrade','speed_upgrade','parallel_upgrade','seed_conversion_upgrade','essence_cell')

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Current geometry and state variants are installed once, after labels and screens.
for lang,index in (('zh_cn',0),('en_us',1)):
    path=ASSETS/f'lang/{lang}.json'; values=json.loads(path.read_text(encoding='utf-8'))
    for name,labels in PARTS.items(): values['block.appliedagristics.'+name]=labels[index]
    for key,pair in {
        'block.appliedagristics.network_planter':('ME 种植控制器','ME Planting Controller'),
        'block.appliedagristics.seed_storage':('种子仓','Seed Storage Bay'),
        'block.appliedagristics.me_infusion_altar':('ME 注魔祭坛','ME Infusion Altar'),
        'status.appliedagristics.unformed':('多方块未成型','Multiblock not formed'),
        'status.appliedagristics.formed':('结构已成型','Structure formed'),
        'status.appliedagristics.invalid_controller':('控制器位置错误','Invalid controller position'),
        'status.appliedagristics.missing_chamber':('内部需要两个培养舱','Two internal cultivation chambers required'),
        'status.appliedagristics.invalid_shell':('外壳不完整或部件不符','Invalid or incomplete shell'),
        'status.appliedagristics.missing_seed_storage':('缺少种子仓','Missing seed storage bay'),
        'status.appliedagristics.missing_input_port':('缺少种子来源：输入口或种子仓','Missing seed source: input port or seed bay'),
        'status.appliedagristics.missing_output_port':('缺少产物去向：输出口或精华存储器','Missing output: output port or essence storage'),
        'status.appliedagristics.requires_output_port':('非精华产物需要输出口','Non-essence produce needs an output port'),
        'status.appliedagristics.missing_network_access':('缺少 ME 网络接入模块','Missing ME network access'),
        'status.appliedagristics.multiple_network_access':('每座结构需要且仅允许一个 ME 接入模块','Exactly one ME network access required'),
        'status.appliedagristics.structure_unloaded':('部分结构区块未加载','Part of structure not loaded'),
        'status.appliedagristics.structure_overlap':('与另一座结构重叠','Overlaps another structure'),
        'status.appliedagristics.missing_infusion_pedestals':('缺少周围的注魔基座','Missing surrounding infusion pedestals'),
        'gui.appliedagristics.module_summary':('速度模块 %s；并行模块 %s；种子转换 %s','Speed modules %s; parallel modules %s; seed conversion %s'),
        'gui.appliedagristics.auto_input_label':('自动输入','Auto input'),
        'gui.appliedagristics.auto_input_on':('自动输入：开','Auto input: on'),
        'gui.appliedagristics.auto_input_off':('自动输入：关','Auto input: off'),
        'gui.appliedagristics.vault_readonly':('%s 种精华；AE 仅可提取','%s types; AE extraction only'),
        'gui.appliedagristics.next_speed_power':('再加一个速度模块：%s AE/t','One more speed module: %s AE/t'),
        'gui.appliedagristics.next_parallel_power':('再加一个并行模块：%s AE/t（按已有种子）','One more parallel module: %s AE/t (available seeds)'),
    }.items(): values[key]=pair[index]
    write(path,values)

# Superseded crafting entries are kept under build/backups by the transition step, not regenerated.
for name in LEGACY:
    path=ROOT/f'data/appliedagristics/recipe/{name}.json'
    if path.exists(): path.unlink()

style={'includes':['common/common.json','common/player_inventory.json'],
    'generatedBackground':{'width':176,'height':184},
    'slots':{'MACHINE_INPUT':{'left':48,'top':40},'MACHINE_OUTPUT':{'left':116,'top':40}},
    'text':{'dialog_title':{'text':{'translate':'block.appliedagristics.me_infusion_altar'},'position':{'left':8,'top':6}},
            'state':{'text':'','position':{'left':8,'top':76}},'speed':{'text':'','position':{'left':130,'top':64}}},
    'images':{'progressBar':{'texture':'guis/molecular_assembler.png','srcRect':[176,0,6,18]}},
    'widgets':{'progressBar':{'left':152,'top':39}}}
write(ROOT/'assets/ae2/screens/appliedagristics_me_infusion.json',style)
port_style={'includes':['common/common.json','common/player_inventory.json'],
    'generatedBackground':{'width':176,'height':200},
    'text':{'dialog_title':{'text':'','position':{'left':8,'top':6}},'state':{'text':'','position':{'left':8,'top':22}}}}
write(ROOT/'assets/ae2/screens/appliedagristics_port.json',port_style)
vault_style=json.loads(json.dumps(port_style)); vault_style['text']['dialog_title']['text']={'translate':'block.appliedagristics.essence_vault'}
vault_style['text']['state']['position']['top']=94
write(ROOT/'assets/ae2/screens/appliedagristics_vault.json',vault_style)
io_style=json.loads((ROOT/'assets/ae2/screens/appliedagristics_machine_io.json').read_text(encoding='utf-8'))
io_style['generatedBackground']['height']=160
io_style['text']['autoInputLabel']={'text':{'translate':'gui.appliedagristics.auto_input_label'},'position':{'left':8,'top':139}}
io_style['widgets']['autoInput']={'left':142,'top':132}
write(ROOT/'assets/ae2/screens/appliedagristics_port_io.json',io_style)
from install_art_resources import install
install()
print('Multiblock resources and native AE screens generated.')
