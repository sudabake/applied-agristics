"""Install bilingual hover descriptions only; never regenerate artwork or recipes."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PREFIX='tooltip.appliedagristics.'

def install():
    descriptions=json.loads((ROOT/'tools/item_descriptions.json').read_text(encoding='utf-8'))
    for lang in ('zh_cn','en_us'):
        path=ROOT/f'src/main/resources/assets/appliedagristics/lang/{lang}.json'
        data=json.loads(path.read_text(encoding='utf-8'))
        data={k:v for k,v in data.items() if not k.startswith(PREFIX)}
        data[PREFIX+'hold_shift']='按住 %s 查看详情' if lang=='zh_cn' else 'Hold %s for details'
        for name,values in descriptions.items():
            lines=values[lang]
            data[PREFIX+name+'.summary']=lines[0]
            for i,line in enumerate(lines[1:]):data[f'{PREFIX}{name}.detail.{i}']=line
        path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':install()
