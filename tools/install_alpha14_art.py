"""Install approved drafts only; no source artwork or third-party mods are altered."""
import copy
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
    for name in ('bio_sampler','culture_capsule','mature_culture','bio_extract','culture_nutrient'):
        src=biology/(name+('-animated'if name=='mature_culture'else'')+'.png')
        cp(src,A/f'textures/item/{name}.png')
    cp(biology/'mature_culture-animated.png.mcmeta',A/'textures/item/mature_culture.png.mcmeta')
    for name in PARTS:
        if name in FUNCTIONAL:
            src=D/'functional_faces_v01';model=json.loads((src/f'models/block/{name}.json').read_text())
            # Draft fronts point north; existing blockstates rotate a south front.
            for e in model['elements']:
                e['faces']['north'],e['faces']['south']=e['faces']['south'],e['faces']['north']
                for direction,face in e['faces'].items():face['cullface']=direction
            for part in ('front','side','top','back'):texture(src/f'{name}_{part}.png',f'{name}_{part}')
        elif name=='cultivation_chamber':
            src=D/'cultivation_chamber_v02';model=json.loads((src/f'{name}.json').read_text())
            for part in ('window','top'):texture(src/f'{name}_{part}.png',f'{name}_{part}')
        else:
            src=D/'array_faces_32_v06';model={'parent':'minecraft:block/cube_all','textures':{'all':f'appliedagristics:block/{name}'}}
            texture(src/f'{name}.png',name)
        write(A/f'models/block/{name}.json',model)
        write(A/f'models/item/{name}.json',{'parent':f'appliedagristics:block/{name}'})
    for block,folder,stem in [('early_planter','basic_planter_v05','basic_planter'),('cell_cultivator','cell_cultivator_v06','cell_cultivator'),('me_infusion_altar','me_infusion_altar_v05','me_infusion_altar')]:
        source=D/folder;model=json.loads((source/f'{stem}.json').read_text(encoding='utf-8'))
        if block=='me_infusion_altar':
            for path in source.glob('me_*.png'):
                if path.name.startswith(('me_infusion_altar_','me_infusion_pedestal_')):texture(path,path.stem)
        else:texture(source/f'{stem}.png',stem)
        write(A/f'models/block/{block}.json',model)
        write(A/f'models/item/{block}.json',{'parent':f'appliedagristics:block/{block}'})
        write(A/f'blockstates/{block}.json',{'variants':{f'facing={f}':{'model':f'appliedagristics:block/{block}','y':angle} for f,angle in ANGLES.items()}})
    # Controller state graphics are rendered only on its existing inset display.
    src=D/'controller_animation_v01'
    for source,name in [('controller_idle','network_planter_idle'),('controller_off','network_planter_off'),('network_planter_working','network_planter_working')]:texture(src/f'{source}.png',name)
    skins=D/'array_faces_32_v06'
    for name in ('joined_front','joined_side','joined_top'):texture(skins/f'{name}.png',name)
    for name in FUNCTIONAL+PARTS[8:]:texture(skins/f'{name}_joined_icon.png',f'{name}_joined_icon')
    for name in PARTS:
        variants={f'facing={f},formed=false':{'model':f'appliedagristics:block/{name}','y':angle}for f,angle in ANGLES.items()}
        for index in range(36):
            x,y,z=index%3,index//9,(index//3)%3
            px,pz=2-x,2-z
            outer={'south':z==0,'north':z==2,'east':x==0,'west':x==2,'up':y==3,'down':y==0}
            faces={}
            for face,visible in outer.items():
                if not visible:continue
                if face in ('up','down'):uv=[px/3*16,pz/3*16,(px+1)/3*16,(pz+1)/3*16];tex='#top'
                else:
                    column={'south':x,'north':2-x,'east':z,'west':2-z}[face]
                    uv=[column/3*16,(3-y)/4*16,(column+1)/3*16,(4-y)/4*16]
                    tex='#front'if face=='south'else'#side'
                faces[face]={'uv':uv,'texture':tex,'cullface':face}
            # Interior chambers keep their distinct observation art.
            if x==1 and z==1 and y in (1,2):
                formed=json.loads((A/'models/block/cultivation_chamber.json').read_text())
            else:
                textures={'front':'appliedagristics:block/joined_front','side':'appliedagristics:block/joined_side','top':'appliedagristics:block/joined_top','particle':'appliedagristics:block/planter_frame'}
                elements=[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]
                if y<2 and name not in ('planter_frame','cultivation_chamber'):
                    side='south'if z==0 else'north'if z==2 else'east'if x==0 else'west'if x==2 else'down'
                    lo=[0,0,0];hi=[16,16,16]
                    axis={'south':2,'north':2,'east':0,'west':0,'down':1}[side]
                    if side in ('south','east'):lo[axis]=16.005;hi[axis]=16.01
                    else:lo[axis]=-.01;hi[axis]=-.005
                    textures['icon']=f'appliedagristics:block/{name}_joined_icon'
                    elements.append({'from':lo,'to':hi,'faces':{side:{'uv':[0,0,16,16],'texture':'#icon'}}})
                formed={'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':textures,'elements':elements}
            write(A/f'models/block/formed/{name}_{index}.json',formed)
            for facing,angle in ANGLES.items():variants[f'cell={index},facing={facing},formed=true']={'model':f'appliedagristics:block/formed/{name}_{index}','y':angle}
        write(A/f'blockstates/{name}.json',{'variants':variants})
    from install_art_resources import collision_shapes
    # cube_all parent models need an explicit full cube for collision extraction.
    collision_shapes(PARTS+['early_planter','cell_cultivator','me_infusion_altar'])
    for lang in ('zh_cn','en_us'):
        path=A/f'lang/{lang}.json';data=json.loads(path.read_text(encoding='utf-8'))
        data.update({'status.appliedagristics.fixed_upper_shell':'上两层只允许固定框架与培养舱'if lang=='zh_cn'else'Upper two layers require fixed casing/chambers',
          'status.appliedagristics.too_many_speed_modules':'速度模块最多四块'if lang=='zh_cn'else'At most four speed modules',
          'status.appliedagristics.too_many_parallel_modules':'并行模块最多四块'if lang=='zh_cn'else'At most four parallel modules',
          'key.appliedagristics.structure':'阵列结构用途（世界中）'if lang=='zh_cn'else'Array structure (in world)',
          'key.appliedagristics.guide':'阵列说明（世界中）'if lang=='zh_cn'else'Array guide (in world)',
          'key.categories.appliedagristics':'Applied Agristics','jei.appliedagristics.structure':'农业阵列结构'if lang=='zh_cn'else'Agricultural Array',
          'jei.appliedagristics.rotate':'旋转'if lang=='zh_cn'else'Rotate','jei.appliedagristics.all_layers':'全部层'if lang=='zh_cn'else'All layers',
          'jei.appliedagristics.layer':'第 %s 层'if lang=='zh_cn'else'Layer %s','jei.appliedagristics.guide':'打开说明'if lang=='zh_cn'else'Open guide',
          'jei.appliedagristics.example_materials':'满升级搭建示例用量'if lang=='zh_cn'else'Fully upgraded example materials',
          'jei.appliedagristics.optional_storages':'种子仓、精华仓可用输入／输出口替代'if lang=='zh_cn'else'Input/output ports can replace the optional storages'})
        lines=['3×3×4 结构','上半部固定','下半部可替换','速度 ≤ 4 块','并行 ≤ 4 块','中轴两个培养舱']if lang=='zh_cn'else['3 x 3 x 4','Fixed upper half','Lower half modular','Speed: up to 4','Parallel: up to 4','2 inner chambers']
        data.update({f'jei.appliedagristics.array_line{i}':text for i,text in enumerate(lines)});write(path,data)
    print('Installed approved Alpha.14 art: 14 blocks, 396 joined variants, five biological items and state graphics.')

if __name__=='__main__':install()
