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
        'gui.appliedagristics.all_crops':('全部作物','All crops'),
        'gui.appliedagristics.ordinary_crops':('普通作物','Ordinary crops'),
        'gui.appliedagristics.direct_output':('普通产物直入 ME','Produce to ME'),
        'gui.appliedagristics.direct_output_on':('开启：非精华产物直接进入 ME 网络；网络满时暂停','On: non-essence output enters ME; pauses when full'),
        'gui.appliedagristics.direct_output_off':('关闭：产物进入本结构仓储或输出口','Off: output stays in local vaults or output ports'),
        'gui.appliedagristics.unknown_spawner':('无法识别刷怪笼中的生物','Cannot identify the mob configured in this spawner'),
        'gui.appliedagristics.network_power':('实际耗电：%s AE/t','Actual power: %s AE/t'),
        'status.appliedagristics.network_output_full':('ME 网络无法接收产物','ME network cannot accept output'),
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
(ROOT/'assets/ae2/screens/appliedagristics_seed_storage.json').unlink(missing_ok=True)
vault_style=json.loads(json.dumps(port_style)); vault_style['text']['dialog_title']['text']={'translate':'block.appliedagristics.essence_vault'}
vault_style['text']['state']['position']['top']=94
write(ROOT/'assets/ae2/screens/appliedagristics_vault.json',vault_style)
(ROOT/'assets/ae2/screens/appliedagristics_port_io.json').unlink(missing_ok=True)
from install_art_resources import install
install()
write(ROOT/'data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':[
    'appliedagristics:'+name for name in ('early_planter','seed_storage','network_planter','cell_cultivator','me_infusion_altar','biological_altar','seed_storage_tier1','seed_storage_tier2',*PARTS)]})
for index,lang in enumerate(('zh_cn','en_us')):
    path=ROOT/f'assets/appliedagristics/lang/{lang}.json';values=json.loads(path.read_text(encoding='utf-8'))
    for key,pair in {
      'block.appliedagristics.biological_altar':('生物祭坛','Biological Altar'),
      'item.appliedagristics.empty_bio_seed':('空白生物种子','Empty Biological Seed'),
      'item.appliedagristics.condensed_bio_essence':('凝聚生物精华','Condensed Biological Essence'),
      'effect.appliedagristics.nourishment':('生物滋养','Biological Nourishment'),
      'entity.appliedagristics.thrown_culture':('投掷的培养物','Thrown Culture'),
      'gui.appliedagristics.show_ordinary':('显示普通作物','Show ordinary crops'),
      'gui.appliedagristics.hide_ordinary':('隐藏普通作物','Hide ordinary crops'),
      'gui.appliedagristics.bio_altar_pedestals':('周围需要八个注魔基座','Requires eight infusion pedestals'),
      'status.appliedagristics.missing_pedestals':('缺少八个注魔基座','Eight infusion pedestals required'),
      'status.appliedagristics.bio_ritual':('生物凝聚中','Biological infusion'),
      'jei.appliedagristics.bio_infusion':('生物祭坛','Biological Infusion'),
      'jei.appliedagristics.extraction':('投掷培养物','Culture Splash'),
      'jei.appliedagristics.throw_help':('右键投掷，或使用发射器','Throw or use a dispenser'),
      'jei.appliedagristics.nourishment_help':('碰撞后掉落萃取物，附近玩家获得滋养','Impact: extract + nearby nourishment'),
      'jei.appliedagristics.ritual_help':('八个基座；四份同种萃取物','8 pedestals; 4 matching extracts'),
    }.items():values[key]=pair[index]
    for key in ('culture_toggle_on','culture_toggle_off','culture_toggle_help'):
        values.pop('gui.appliedagristics.'+key,None)
    write(path,values)
for index,lang in enumerate(('zh_cn','en_us')):
    path=ASSETS/f'lang/{lang}.json';values=json.loads(path.read_text(encoding='utf-8'))
    for key,pair in {
      'block.appliedagristics.seed_storage':('ME 种子仓','ME Seed Storage'),
      'block.appliedagristics.seed_storage_tier1':('ME 种子仓','ME Seed Storage'),
      'block.appliedagristics.seed_storage_tier2':('ME 种子仓','ME Seed Storage'),
      'effect.appliedagristics.nourishment.desc':('缓慢恢复饥饿值，不恢复饱和度','Slowly restores hunger without saturation'),
      'gui.appliedagristics.seed_cell_capacity':('16 格；每格 %s 颗','16 cells; %s seeds per cell'),
      'gui.appliedagristics.seed_cell_usage':('占用 %s / 共 %s 颗','In use %s / total %s seeds'),
      'gui.appliedagristics.seed_overflow':('旧库存超额，请从 ME 取出','Overflow: withdraw through ME'),
      'gui.appliedagristics.bio_altar_ground':('先投材料，再右键放空白种子','Drop materials, then use a blank seed'),
      'status.appliedagristics.missing_ground_materials':('缺少附近的萃取物或凝聚精华，或祭坛忙碌','Missing nearby extract or condensed essence, or altar busy'),
      'status.appliedagristics.add_seed':('投入材料后放入空白种子','Drop ingredients, then insert a blank seed'),
      'jei.appliedagristics.ritual_help':('先投萃取物和精华，再放空白种子','Drop extract + essence; use a blank seed'),
    }.items():values[key]=pair[index]
    for key in ('seed_cell_capacity','seed_cell_usage','seed_overflow','bio_altar_ground'):values.pop('gui.appliedagristics.'+key,None)
    values.pop('gui.appliedagristics.bio_altar_pedestals',None);values.pop('status.appliedagristics.missing_pedestals',None)
    write(path,values)
for name in ('seed_storage_tier1','seed_storage_tier2'):(ROOT/f'data/appliedagristics/recipe/{name}.json').unlink(missing_ok=True)
print('Multiblock resources and native AE screens generated.')

from install_word_update import install as install_word_update
install_word_update()
