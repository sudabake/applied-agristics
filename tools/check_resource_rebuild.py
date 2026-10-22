"""Verify the complete resource generator in an isolated, retained build directory (Python 3.10+)."""
from pathlib import Path
import datetime
import hashlib
import json
import shutil
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
DRAFTS=('biology_items_v05','functional_faces_v01','cultivation_chamber_v02','array_faces_32_v06',
        'basic_planter_v05','cell_cultivator_v06','me_infusion_altar_v05','controller_animation_v01')

def snapshot(folder):
    result={}
    for path in folder.rglob('*'):
        if not path.is_file():
            continue
        if path.suffix in ('.json','.mcmeta'):
            value=json.dumps(json.loads(path.read_text(encoding='utf-8')),sort_keys=True,separators=(',',':')).encode()
        else:
            value=path.read_bytes()
        result[path.relative_to(folder).as_posix()]=hashlib.sha256(value).hexdigest()
    return result

def main():
    workspace=ROOT/'build/resource-rebuild-check'/datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    workspace.mkdir(parents=True)
    for name in ('tools','third_party','src/main/resources','src/main/java/io/github/sudabake/appliedagristics/block'):
        shutil.copytree(ROOT/name,workspace/name,ignore=shutil.ignore_patterns('__pycache__','*.pyc'))
    for draft in DRAFTS:
        relative=Path('art/drafts')/draft
        shutil.copytree(ROOT/relative,workspace/relative)
    resources=workspace/'src/main/resources'
    before=snapshot(resources)
    shape=workspace/'src/main/java/io/github/sudabake/appliedagristics/block/MachineShapes.java'
    original_shape=shape.read_bytes()
    results=[]
    for run in range(2):
        process=subprocess.run([sys.executable,str(workspace/'tools/generate_resources.py')],cwd=workspace,capture_output=True,text=True,encoding='utf-8')
        (workspace/f'run-{run+1}.log').write_text(process.stdout+process.stderr,encoding='utf-8')
        after=snapshot(resources)
        changed=sorted(name for name in before if after.get(name)!=before[name])
        extra=sorted(set(after)-set(before))
        results.append({'run':run+1,'exit_code':process.returncode,'changed':changed,'new':extra,'same_collision_shapes':shape.read_bytes()==original_shape})
        if process.returncode or changed or extra or shape.read_bytes()!=original_shape:
            (workspace/'result.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
            raise SystemExit(f'Resource rebuild differs from approved resources; inspect {workspace}')
    (workspace/'result.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
    print(f'RESOURCE_REBUILD_PASSED twoRuns=true approvedResources={len(before)} collisionShapes=true report={workspace}')

if __name__=='__main__':
    main()
