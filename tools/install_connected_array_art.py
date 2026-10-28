"""Install the accepted connected shell using the existing formed-position models."""
from pathlib import Path
import json
import shutil

ROOT=Path(__file__).resolve().parents[1]

def install(assets=None):
    assets=Path(assets) if assets else ROOT/'src/main/resources/assets/appliedagristics'
    source=ROOT/'art/drafts/array_connected_16_v02/runtime'
    for path in source.glob('*.png'):
        shutil.copyfile(path,assets/'textures/block'/path.name)
    for path in (assets/'models/block/formed').glob('*.json'):
        model=json.loads(path.read_text(encoding='utf-8'))
        model['render_type']='minecraft:translucent'
        name=path.stem.rsplit('_',1)[0]
        if name not in ('planter_frame','cultivation_chamber') and 'front' in model.get('textures',{}):
            # All exposed faces of a six-sided module carry its functional art.
            shell=model['elements'][0];model['elements']=[shell]
            visible=list(shell['faces'])
            multi=name in ('planter_speed_module','planter_parallel_module','planter_conversion_module')
            selected=visible if multi else next(([face] for face in ('south','north','east','west','down','up') if face in visible),[])
            model['textures']['icon']=f'appliedagristics:block/{name}_joined_icon'
            for face in selected:
                lo=[0,0,0];hi=[16,16,16]
                axis={'south':2,'north':2,'east':0,'west':0,'up':1,'down':1}[face]
                if face in ('south','east','up'):lo[axis]=16.005;hi[axis]=16.01
                else:lo[axis]=-.01;hi[axis]=-.005
                model['elements'].append({'from':lo,'to':hi,'faces':{face:{'uv':[0,0,16,16],'texture':'#icon'}}})
        path.write_text(json.dumps(model,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')

if __name__=='__main__':
    install()
