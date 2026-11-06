"""Runtime resource reachability and release-JAR checks, using only the standard library."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import struct
import zipfile

ROOT=Path(__file__).resolve().parents[1]
NS='appliedagristics'
RETIRED_TEXTURES=tuple('block/'+name for name in (
    'cultivation_chamber','early_planter','essence_vault','me_array_palette',
    'me_infusion_altar','me_network_access','network_planter',
    'planter_input_port','planter_output_port','seed_storage'))
RETIRED_TEXTURES += tuple('block/' + name + '_' + face
    for name in ('network_planter','seed_storage','me_network_access','planter_input_port','planter_output_port','essence_vault')
    for face in ('side','top','back','joined_icon'))
RETIRED_TEXTURES += tuple('block/joined_' + face for face in ('front','side','top'))

def usage(assets):
    models=set();textures=set();missing=[]
    def model(name):
        if name in models:return
        if name.endswith('.obj'):
            models.add(name)
            path=assets/name
            if not path.is_file():missing.append(name);return
            for material in re.findall(r'^mtllib\s+(.+)$',path.read_text(encoding='utf-8'),re.M):
                library=path.parent/material.strip()
                if not library.resolve().is_relative_to(assets.resolve()) or not library.is_file():
                    missing.append(str(library.relative_to(assets)));continue
                textures.update(value.split(':',1)[1] for value in re.findall(
                    r'^map_Kd\s+(appliedagristics:\S+)',library.read_text(encoding='utf-8'),re.M))
            return
        models.add(name);path=assets/'models'/(name+'.json')
        if not path.exists():missing.append('models/'+name+'.json');return
        visit(json.loads(path.read_text(encoding='utf-8')))
    def visit(value):
        if isinstance(value,dict):
            for key,item in value.items():
                if key in ('parent','model') and isinstance(item,str) and item.startswith(NS+':'):model(item.split(':',1)[1])
                elif key=='textures' and isinstance(item,dict):
                    textures.update(t.split(':',1)[1] for t in item.values() if isinstance(t,str) and t.startswith(NS+':'))
                else:visit(item)
        elif isinstance(value,list):
            for item in value:visit(item)
    for path in (assets/'blockstates').glob('*.json'):visit(json.loads(path.read_text(encoding='utf-8')))
    for path in (assets/'models/item').glob('*.json'):model('item/'+path.stem)
    # Explicit runtime roots outside the block and item model graph.
    model('block/bio_plant')
    textures.add('block/network_planter_screen')
    alltex={path.relative_to(assets/'textures').with_suffix('').as_posix() for path in (assets/'textures').rglob('*.png')}
    allmodels={path.relative_to(assets/'models').with_suffix('').as_posix() for path in (assets/'models').rglob('*.json')}
    return {'missing_models':sorted(missing),'missing_textures':sorted(textures-alltex),
            'unreferenced_textures':sorted(alltex-textures),
            'unreferenced_models':sorted(allmodels-models),
            'orphan_animation_metadata':sorted(str(path.relative_to(assets)) for path in (assets/'textures').rglob('*.png.mcmeta') if not path.with_suffix('').exists())}

def prune_retired(assets):
    assets=Path(assets);report=usage(assets)
    for name in RETIRED_TEXTURES:
        path=assets/'textures'/(name+'.png')
        if path.exists() and name not in report['unreferenced_textures']:
            raise RuntimeError('Refusing to remove a referenced texture: '+name)
        for suffix in ('.png','.png.mcmeta'):
            path=assets/'textures'/(name+suffix)
            if path.exists():path.unlink()

def png_chunks(data):
    assert data[:8]==b'\x89PNG\r\n\x1a\n'
    result=[];offset=8
    while offset+12<=len(data):
        length=struct.unpack('>I',data[offset:offset+4])[0]
        kind=data[offset+4:offset+8].decode('ascii');result.append(kind)
        offset+=length+12
    return result

def audit_jar(path,assets):
    from remove_retired_content import RETIRED, RETIRED_LABELS
    result={'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'problems':[], 'png_metadata':{}}
    with zipfile.ZipFile(path)as jar:
        names=jar.namelist();base='assets/'+NS+'/'
        if len(set(names))!=len(names):result['problems'].append('Duplicate JAR entries')
        expected={base+p.relative_to(assets).as_posix():p.read_bytes() for p in assets.rglob('*') if p.is_file() and p.name!='.gitkeep'}
        actual={name:jar.read(name) for name in names if name.startswith(base) and not name.endswith('/')}
        for name in sorted(set(expected)|set(actual)):
            if expected.get(name)!=actual.get(name):result['problems'].append('Source/JAR resource mismatch: '+name)
        for name in names:
            filename=name.rsplit('/',1)[-1]
            if name.startswith('assets/appliedagristics/') and any(
                    filename in (retired+'.json',retired+'.png',retired+'.png.mcmeta')
                    for retired in (*RETIRED,'nourishment')):
                result['problems'].append('Retired gameplay resource: '+name)
            if name.startswith('data/appliedagristics/recipe/legacy_'):
                result['problems'].append('Retired migration recipe: '+name)
            implementation = filename.split('$',1)[0].removesuffix('.class')
            if filename.endswith('.class') and implementation in (
                    'CultureItem','ThrownCulture','NourishmentEffect','BioSeedRecipe','BioInfusionRecipe',
                    'BiologicalAltar','CellCultivator','CultureScreen','BiologicalAltarRenderer',
                    'NetworkIOScreen','LegacyBiology','LegacyBiologyMachine','ConnectedCasingModel','CasingConnections'):
                result['problems'].append('Retired implementation: '+name)
            if name.rsplit('/',1)[-1]=='.gitkeep':result['problems'].append('Development placeholder: '+name)
            if '/testing/' in name or '/JeiSmoke' in name or name=='data/appliedagristics/structure/empty.nbt' or name.startswith(('art/','docs/','run/','build/')):
                result['problems'].append('Development-only JAR entry: '+name)
            data=jar.read(name)
            if name in (base+'lang/zh_cn.json',base+'lang/en_us.json'):
                for key in RETIRED_LABELS.intersection(json.loads(data)):
                    result['problems'].append('Retired translation: '+key)
            if name.startswith('io/github/sudabake/appliedagristics/') and name.endswith('.class'):
                compiled=ROOT/'build/classes/java/main'/name
                if not compiled.exists() or compiled.read_bytes()!=data:
                    result['problems'].append('Compiled-class/JAR mismatch: '+name)
            if re.search(rb'[A-Za-z]:[\\/]+Users[\\/]+[^\\/\s]+',data,re.I) or re.search(rb'[A-Za-z]:[\\/]+Minecraft[\\/]+',data,re.I):
                result['problems'].append('Local machine path: '+name)
            if name.endswith('.png'):
                metadata=[kind for kind in png_chunks(data) if kind in ('tEXt','zTXt','iTXt','eXIf')]
                if metadata:result['png_metadata'][name]=metadata
        dims=struct.unpack('>II',actual[base+'textures/block/network_planter_screen.png'][16:24])
        if dims!=(10,260):result['problems'].append('Wrong controller screen atlas dimensions')
        for retired in ('network_planter_idle','network_planter_off','network_planter_working','me_infusion_pedestal_top'):
            if base+'textures/block/'+retired+'.png' in names:
                result['problems'].append('Retired machine artwork: '+retired)
    return result

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--jar',type=Path);parser.add_argument('--report',type=Path);args=parser.parse_args()
    assets=ROOT/'src/main/resources/assets'/NS;result={'resources':usage(assets)}
    if args.jar:result['jar']=audit_jar(args.jar,assets)
    if args.report:args.report.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    resource=result['resources']
    problems=resource['missing_models']+resource['missing_textures']+resource['unreferenced_textures']+resource['orphan_animation_metadata']
    if args.jar:problems+=result['jar']['problems']
    print(json.dumps(result,ensure_ascii=False,indent=2))
    if problems:raise SystemExit(1)

if __name__=='__main__':main()
