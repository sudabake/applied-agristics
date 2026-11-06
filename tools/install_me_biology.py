"""Current ME biological processor resources."""
from pathlib import Path
import json,re

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
A=RES/'assets/appliedagristics'

def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def install():
    model={'parent':'minecraft:block/block','render_type':'minecraft:cutout',
        'textures':{'particle':'appliedagristics:block/planter_frame','casing':'appliedagristics:block/planter_frame','culture':'appliedagristics:block/me_bio_cultivator_front'},
        'elements':[{'from':[0,0,0],'to':[16,16,16], 'faces':{side:{'texture':'#casing','cullface':side} for side in ('north','south','east','west','up','down')}},
            {'from':[4,4,-.01],'to':[12,12,0], 'faces':{'north':{'texture':'#culture','uv':[0,0,16,16]}}}]}
    write(A/'models/block/me_bio_cultivator.json',model)
    states={'variants':{f'facing={f}':{'model':'appliedagristics:block/me_bio_cultivator','y':angle} for f,angle in [('north',0),('east',90),('south',180),('west',270)]}}
    item={'parent':'appliedagristics:block/me_bio_cultivator','display':{'gui':{'rotation':[30,225,0],'scale':[.625,.625,.625]}}}
    write(A/'blockstates/me_bio_cultivator.json',states);write(A/'models/item/me_bio_cultivator.json',item)
    recipe=RES/'data/appliedagristics/recipe'
    for name in ('cell_cultivator','biological_altar','bio_infusion','culture_capsule','condensed_bio_essence'):(recipe/f'{name}.json').unlink(missing_ok=True)
    write(recipe/'me_bio_cultivator.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['GIG','ICI','RPR'],
        'key':{'G':{'item':'minecraft:glass'},'I':{'item':'minecraft:iron_ingot'},'C':{'item':'mysticalagriculture:inferium_essence'},'R':{'item':'minecraft:redstone'},'P':{'item':'ae2:engineering_processor'}},
        'result':{'id':'appliedagristics:me_bio_cultivator','count':1}})
    write(recipe/'culture_nutrient.json',{'type':'minecraft:crafting_shapeless','category':'misc',
        'ingredients':[{'item':'minecraft:water_bucket'},{'item':'minecraft:bone_meal'},{'item':'mysticalagriculture:inferium_essence'}],
        'result':{'id':'appliedagristics:culture_nutrient','count':1}})
    style=json.loads((RES/'assets/ae2/screens/appliedagristics_me_infusion.json').read_text(encoding='utf-8'))
    style['slots']['MACHINE_INPUT']={'left':18,'top':40,'grid':'HORIZONTAL'}
    style['text']['dialog_title']['text']={'translate':'block.appliedagristics.me_bio_cultivator'}
    write(RES/'assets/ae2/screens/appliedagristics_me_bio.json',style)
    write(RES/'assets/ae2/screens/appliedagristics_seeds.json',{
        'includes':['common/common.json','common/player_inventory.json'],
        'generatedBackground':{'width':320,'height':240},
        'slots':{'MACHINE_INPUT':{'left':24,'top':29},'PLAYER_INVENTORY':{'left':79,'top':160,'grid':'BREAK_AFTER_9COLS'},'PLAYER_HOTBAR':{'left':79,'top':218,'grid':'HORIZONTAL'}},
        'text':{'dialog_title':{'text':'','position':{'left':8,'top':6}},'state':{'text':'','position':{'left':140,'top':31}},'player_inventory_title':{'text':{'translate':'container.inventory'},'position':{'left':79,'top':150}}},
        'widgets':{'collect':{'left':48,'top':26,'width':80,'height':18}}})
    write(RES/'assets/ae2/screens/appliedagristics_seed_settings.json',{
        'includes':['common/common.json'],'generatedBackground':{'width':176,'height':146},
        'text':{'dialog_title':{'text':{'translate':'gui.appliedagristics.seed_settings'},'position':{'left':8,'top':6}},
            'autoLabel':{'text':{'translate':'gui.appliedagristics.auto_collect_label'},'position':{'left':8,'top':29}},
            'filterLabel':{'text':{'translate':'gui.appliedagristics.collect_filter_label'},'position':{'left':8,'top':47}}},
        'widgets':{'back':{'right':23,'top':-5},'autoCollect':{'left':148,'top':24},
            **{'filter'+str(i):{'left':8+i%2*82,'top':60+i//2*22,'width':76,'height':18}for i in range(4)},
            'eject':{'left':8,'top':116,'width':158,'height':18}}})
    labels={
        'gui.appliedagristics.seed_withdraw_hint':('左键取出一组未占用的种子','Left-click to withdraw one available stack'),
        'jei.appliedagristics.planting_products':('种植产物','Planting Products'),
        'jei.appliedagristics.possible_products':('种子保留，列出可能产物','Seed kept. Possible outputs'),
        'gui.appliedagristics.collect':('收容种子','Collect Seeds'),
        'gui.appliedagristics.seed_settings':('种子仓设置','Seed Storage Settings'),
        'gui.appliedagristics.auto_collect_label':('自动收容种子','Auto collect seeds'),
        'gui.appliedagristics.auto_collect_help':('每秒主动收容网络中的种子，按所选范围过滤','Actively collects network seeds once per second using the selected filter'),
        'gui.appliedagristics.collect_filter_label':('收容范围','Collection filter'),
        'gui.appliedagristics.collect_filter_0':('全部种子','All seeds'),
        'gui.appliedagristics.collect_filter_1':('原版种子','Vanilla'),
        'gui.appliedagristics.collect_filter_2':('神秘种子','Mystical'),
        'gui.appliedagristics.collect_filter_3':('生物种子','Biological'),
        'gui.appliedagristics.eject_seeds':('推出种子','Eject Seeds'),
        'gui.appliedagristics.eject_confirm':('确定？','Confirm?'),
        'gui.appliedagristics.eject_warning':('此操作会将所有种子送入me网络！请确保磁盘容量足够','This sends all seeds into the ME network! Ensure sufficient disk capacity'),
        'block.appliedagristics.me_bio_cultivator':('ME 生物培养器','ME Biological Cultivator'),
        'item.appliedagristics.culture_nutrient':('营养试剂','Nutrient Reagent'),
        'jei.appliedagristics.culture':('ME 生物培养','ME Biological Cultivation'),
        'jei.appliedagristics.culture_duration':('基础培养时间 %s 秒','Base cycle %s s'),
        'jei.appliedagristics.single_use':('每次消耗三种输入各一份','Consumes all three inputs'),
        'gui.appliedagristics.new_seeds_on':('新种默认启用','New seeds enabled'),
        'gui.appliedagristics.new_seeds_off':('新种默认关闭','New seeds disabled'),
        'gui.appliedagristics.auto_collect_on':('自动收容 开','Auto collect on'),
        'gui.appliedagristics.auto_collect_off':('自动收容 关','Auto collect off'),
    }
    for i,lang in enumerate(('zh_cn','en_us')):
        path=A/f'lang/{lang}.json';data=json.loads(path.read_text(encoding='utf-8'))
        data.update({key:value[i] for key,value in labels.items()})
        for key in list(data):
            if key.startswith(('gui.appliedagristics.culture_capsule','gui.appliedagristics.culture_cells','jei.appliedagristics.extraction','jei.appliedagristics.bio_infusion','jei.appliedagristics.ritual_help','jei.appliedagristics.throw_help','jei.appliedagristics.nourishment_help','status.appliedagristics.bio_ritual','status.appliedagristics.add_seed','status.appliedagristics.missing_ground_materials')):data.pop(key)
        write(path,data)
    tag=RES/'data/minecraft/tags/block/mineable/pickaxe.json';data=json.loads(tag.read_text(encoding='utf-8'));data['values']=list(dict.fromkeys([*data['values'],'appliedagristics:me_bio_cultivator']));write(tag,data)
    path=A/'ae2guide/index.md';text=path.read_text(encoding='utf-8')
    text=text.replace('  - appliedagristics:cell_cultivator\n','  - appliedagristics:me_bio_cultivator\n').replace('  - appliedagristics:biological_altar\n','')
    text=re.sub(r'基础种植机和细胞培养器可在前期制作[^\n]*','基础种植机可在前期制作。ME 生物培养器连接 AE 网络，消耗已采样棉签、营养试剂和空白生物种子，直接产出对应物种的种子。基础加工 10 秒，可装四张独立 AE 加速卡。',text)
    text=re.sub(r'生物棉签右键生物采样[^\n]*','生物棉签右键生物采样，不伤害生物。营养试剂由水桶、骨粉和下级精华合成，合成返还空桶。ME 生物培养器的三个输入各消耗一份；机器使用 AE 电力。可从 JEI 将对应配方填入 AE 加工样板，产物自动输出到网络或配置的相邻容器。',text)
    extra='ME 种子仓的自动收容开关控制每秒从网络主动拉取已有种子。控制器的“新种默认启用”开关控制首次进入本阵列的物种，手动暂停的已有任务继续保持设置。两项默认关闭，可按需开启，实现生物采样到种植的自动衔接。'
    if 'ME 种子仓的自动收容开关' not in text:text+='\n'+extra+'\n'
    path.write_text(text,encoding='utf-8')

if __name__=='__main__':install()
