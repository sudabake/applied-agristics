"""Install approved drafts only; no source artwork or third-party mods are altered."""
import json
import shutil
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/appliedagristics'
D=ROOT/'art/drafts'
PARTS=['network_planter','seed_storage','me_network_access','planter_input_port','planter_output_port','essence_vault','planter_frame','cultivation_chamber','planter_speed_module','planter_parallel_module','planter_conversion_module']
FUNCTIONAL=PARTS[:6]
ANGLES={'north':180,'east':270,'south':0,'west':90}
def write(path,obj):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
def cp(source,dest):dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dest)
def texture(path,name):cp(path,A/f'textures/block/{name}.png')
def install():
    biology=D/'biology_items_v05'
    for name in ('bio_sampler','culture_nutrient'):
        src=biology/(name+'.png')
        cp(src,A/f'textures/item/{name}.png')
    for name in PARTS:
        if name in FUNCTIONAL:
            src=D/'functional_faces_v01';model=json.loads((src/f'models/block/{name}.json').read_text())
            # Draft fronts point north; existing blockstates rotate a south front.
            for e in model['elements']:
                e['faces']['north'],e['faces']['south']=e['faces']['south'],e['faces']['north']
                for direction,face in e['faces'].items():face['cullface']=direction
            texture(src/f'{name}_front.png',f'{name}_front')
        elif name=='cultivation_chamber':
            src=D/'cultivation_chamber_v02';model=json.loads((src/f'{name}.json').read_text())
            for part in ('window','top'):texture(src/f'{name}_{part}.png',f'{name}_{part}')
        else:
            src=D/'array_faces_32_v06';model={'parent':'minecraft:block/cube_all','textures':{'all':f'appliedagristics:block/{name}'}}
            texture(src/f'{name}.png',name)
        write(A/f'models/block/{name}.json',model)
        write(A/f'models/item/{name}.json',{'parent':f'appliedagristics:block/{name}'})
    for block,folder,stem in [('early_planter','basic_planter_v05','basic_planter'),('me_infusion_altar','me_infusion_altar_v05','me_infusion_altar')]:
        source=D/folder;model=json.loads((source/f'{stem}.json').read_text(encoding='utf-8'))
        if block=='me_infusion_altar':
            for path in source.glob('me_*.png'):
                if path.name.startswith(('me_infusion_altar_','me_infusion_pedestal_')):texture(D/'alpha18_original_v01'/path.name,path.stem)
        else:texture(source/f'{stem}.png',stem)
        write(A/f'models/block/{block}.json',model)
        item={'parent':f'appliedagristics:block/{block}'}
        if block=='me_infusion_altar':
            # This native altar model has no block/block parent to supply GUI transforms.
            item['gui_light']='side'
            item['display']={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.625,.625,.625]},
                'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375]*3},
                'thirdperson_lefthand':{'rotation':[75,225,0],'translation':[0,2.5,0],'scale':[.375]*3},
                'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,0,0],'scale':[.4]*3},
                'firstperson_lefthand':{'rotation':[0,225,0],'translation':[0,0,0],'scale':[.4]*3},
                'ground':{'translation':[0,3,0],'scale':[.25]*3},'fixed':{'scale':[.5]*3}}
        write(A/f'models/item/{block}.json',item)
        write(A/f'blockstates/{block}.json',{'variants':{f'facing={f}':{'model':f'appliedagristics:block/{block}','y':angle} for f,angle in ANGLES.items()}})
    src=D/'alpha19_biology_v01'
    for name in ('empty_bio_seed',):
        cp(src/f'{name}.png',A/f'textures/item/{name}.png')
        write(A/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'appliedagristics:item/{name}'}})
    from install_art_resources import collision_shapes
    # cube_all parent models need an explicit full cube for collision extraction.
    collision_shapes(PARTS+['early_planter','me_infusion_altar'])
    for lang in ('zh_cn','en_us'):
        path=A/f'lang/{lang}.json';data=json.loads(path.read_text(encoding='utf-8'))
        data.pop('status.appliedagristics.fixed_upper_shell',None)
        data.update({'status.appliedagristics.too_many_speed_modules':'速度模块最多四块'if lang=='zh_cn'else'At most four speed modules',
          'status.appliedagristics.too_many_parallel_modules':'并行模块最多四块'if lang=='zh_cn'else'At most four parallel modules',
          'key.appliedagristics.structure':'阵列结构用途（世界中）'if lang=='zh_cn'else'Array structure (in world)',
          'key.appliedagristics.guide':'阵列说明（世界中）'if lang=='zh_cn'else'Array guide (in world)',
          'key.categories.appliedagristics':'Applied Agristics','jei.appliedagristics.structure':'农业阵列结构'if lang=='zh_cn'else'Agricultural Array',
          'jei.appliedagristics.rotate':'旋转'if lang=='zh_cn'else'Rotate','jei.appliedagristics.all_layers':'全部层'if lang=='zh_cn'else'All layers',
          'jei.appliedagristics.drag_rotate':'按住左键拖动旋转；下方可切换分层'if lang=='zh_cn'else'Hold left mouse and drag to rotate; use the layer button below',
          'jei.appliedagristics.layer':'第 %s 层'if lang=='zh_cn'else'Layer %s','jei.appliedagristics.guide':'打开说明'if lang=='zh_cn'else'Open guide',
          'jei.appliedagristics.example_materials':'满升级搭建示例用量'if lang=='zh_cn'else'Fully upgraded example materials',
          'jei.appliedagristics.optional_storages':'ME 种子仓、作物仓可用种子仓／输出口替代'if lang=='zh_cn'else'Seed storage/output ports replace optional ME storages'})
        lines=['3×3×4 结构','外壳各层可替换','控制器位置固定','速度 ≤ 4 块','并行 ≤ 4 块','中轴两个培养舱']if lang=='zh_cn'else['3 x 3 x 4','All shell layers modular','Fixed controller position','Speed: up to 4','Parallel: up to 4','2 inner chambers']
        data.update({f'jei.appliedagristics.array_line{i}':text for i,text in enumerate(lines)});write(path,data)
    from install_word_update import install as install_word_update
    install_word_update()
    print('Installed approved art, including user 16px items and current gameplay resources.')

if __name__=='__main__':install()
