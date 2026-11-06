"""Export an editable pack from a verified local JAR and compare later user edits."""
from pathlib import Path
from datetime import datetime, timezone, timedelta
import argparse
import hashlib
import json
import zipfile


def file_record(path):
    stat = path.stat()
    return {'mtime_ns': stat.st_mtime_ns, 'size': stat.st_size,
            'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}


def export(jar, resourcepacks, label, manifests):
    jar, resourcepacks, manifests = Path(jar), Path(resourcepacks), Path(manifests)
    if not label or any(char in label for char in '/\\:*?"<>|') or label in ('.', '..'):
        raise ValueError('Version label must be a safe directory name')
    now = datetime.now(timezone(timedelta(hours=8)))
    name = 'Applied Agristics-' + label + '-' + now.strftime('%Y%m%d-%H%M%S-%f')
    destination = resourcepacks / name
    with zipfile.ZipFile(jar) as archive:
        entries = [entry for entry in archive.infolist()
                   if not entry.is_dir() and (entry.filename.startswith('assets/') or entry.filename == 'pack.mcmeta')]
        for entry in entries:
            target = destination / entry.filename
            if not target.resolve().is_relative_to(destination.resolve()) or '\\' in entry.filename:
                raise ValueError('Unsafe archive path')
        if not any(entry.filename == 'pack.mcmeta' for entry in entries):
            raise ValueError('JAR must contain pack.mcmeta')
        destination.mkdir(parents=True, exist_ok=False)
        for entry in entries:
            target = destination / entry.filename
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(entry))
    records = {path.relative_to(destination).as_posix(): file_record(path)
               for path in destination.rglob('*') if path.is_file()}
    manifest = {'exported_at': now.isoformat(), 'version': label,
                'directory': str(destination.resolve()), 'jar_sha256': file_record(jar)['sha256'],
                'files': records}
    manifests.mkdir(parents=True, exist_ok=True)
    path = manifests / (name + '.json')
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    return path, manifest


def changes(manifest_path):
    baseline = json.loads(Path(manifest_path).read_text(encoding='utf-8'))
    directory = Path(baseline['directory'])
    if not directory.is_dir():
        raise FileNotFoundError(directory)
    current = {path.relative_to(directory).as_posix(): file_record(path)
               for path in directory.rglob('*') if path.is_file()}
    result = {'modified': [], 'added': [], 'deleted': [], 'timestamp_only': []}
    for name in sorted(set(baseline['files']) | set(current)):
        old, new = baseline['files'].get(name), current.get(name)
        if old is None:
            result['added'].append(name)
        elif new is None:
            result['deleted'].append(name)
        elif old['sha256'] != new['sha256']:
            result['modified'].append(name)
        elif old['mtime_ns'] != new['mtime_ns']:
            result['timestamp_only'].append(name)
    return result


def main():
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest='command', required=True)
    out = commands.add_parser('export')
    out.add_argument('--jar', type=Path, required=True)
    out.add_argument('--resourcepacks', type=Path, required=True)
    out.add_argument('--label', required=True)
    out.add_argument('--manifests', type=Path, default=Path(__file__).resolve().parents[1] / 'build/resource-exports')
    compare = commands.add_parser('changes')
    compare.add_argument('--manifest', type=Path, required=True)
    args = parser.parse_args()
    if args.command == 'changes':
        print(json.dumps(changes(args.manifest), ensure_ascii=False, indent=2))
    else:
        path, data = export(args.jar, args.resourcepacks, args.label, args.manifests)
        print(json.dumps({'manifest': str(path), 'directory': data['directory'],
                          'files': len(data['files']), 'jar_sha256': data['jar_sha256']}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
