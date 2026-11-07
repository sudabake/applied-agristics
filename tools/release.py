"""Validate and package manually requested releases. Uploads belong to mc-publish."""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tomllib
import urllib.error
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.compile(r'(\d+\.\d+\.\d+)(?:-(alpha|beta|rc)\.\d+)?\Z')
PLATFORMS = ('github', 'curseforge', 'modrinth')


def config(root):
    return json.loads((root / '.github/release.json').read_text(encoding='utf-8'))


def validated_version(value, release_type):
    version = value.removeprefix('v')
    match = VERSION.fullmatch(version)
    if not match or release_type not in ('release', 'beta', 'alpha'):
        raise ValueError('Use a version such as 1.3.1 or 1.3.2-beta.1 and a valid release type')
    expected_type = {'rc': 'beta', None: 'release'}.get(match[2], match[2])
    if release_type != expected_type:
        raise ValueError('Version suffix and release type must agree')
    return version


def prepare(root, version, release_type, target, dry_run):
    version = validated_version(version, release_type)
    properties = (root / 'gradle.properties').read_text(encoding='utf-8')
    source = re.search(r'^mod_version=(\d+\.\d+\.\d+)(?:[^\r\n]*)$', properties, re.MULTILINE)
    if not source or source[1] != VERSION.fullmatch(version)[1]:
        raise ValueError('The selected source branch has a different mod_version. Upload the intended source first')
    notes = root / 'docs/RELEASE_NOTES.md'
    if not notes.is_file() or len(notes.read_text(encoding='utf-8').strip()) < 30:
        raise ValueError('Write this version\'s release notes in docs/RELEASE_NOTES.md first')
    heading = notes.read_text(encoding='utf-8').splitlines()[0]
    if source[1] not in heading:
        raise ValueError('The release notes heading must contain this source version')
    if target not in (*PLATFORMS, 'all'):
        raise ValueError('Unknown publication target')
    platforms = list(PLATFORMS) if target == 'all' else [target]
    settings = config(root)
    for platform in platforms:
        if platform != 'github' and not settings.get(platform + '_id'):
            raise ValueError('Configure the ' + platform + ' project in .github/release.json')
    if not dry_run:
        tags = subprocess.run(['git', 'ls-remote', '--tags', 'origin', 'refs/tags/v' + version],
                              cwd=root, capture_output=True, text=True, check=True)
        if tags.stdout.strip():
            raise ValueError('This version tag already exists. For a failed publication, rerun failed jobs instead')
    return {'version': version, 'release_type': release_type, 'platforms': json.dumps(platforms),
            'curseforge_id': str(settings['curseforge_id']), 'modrinth_id': settings['modrinth_id'],
            'dependencies': json.dumps(settings['dependencies']), 'minecraft': settings['minecraft'],
            'loader': settings['loader'], 'java': settings['java']}


def api_json(url, token, header='Authorization'):
    value = token if header != 'Authorization' else 'Bearer ' + token
    request = urllib.request.Request(url, headers={header: value, 'User-Agent': 'Applied-Agristics-release'})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise ValueError('Project or token check failed with HTTP ' + str(error.code)) from None


def credentials(root, platforms):
    settings = config(root)
    output = {}
    for platform in platforms:
        if platform == 'github':
            continue
        name = platform.upper() + '_TOKEN'
        token = os.environ.get(name, '')
        if not token:
            raise ValueError('Add the GitHub Actions secret ' + name)
        if platform == 'modrinth':
            project = api_json('https://api.modrinth.com/v2/project/' + settings['modrinth_id'], token)
            output['modrinth_id'] = project['id']
        else:
            api_json('https://minecraft.curseforge.com/api/game/versions', token, 'X-Api-Token')
    return output


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify(root, version):
    version = validated_version(version, 'alpha' if '-alpha.' in version else 'beta' if '-' in version else 'release')
    folder = root / 'build/publication'
    expected = {'applied-agristics-' + version + '.jar', 'applied-agristics-' + version + '-source.zip'}
    manifest = json.loads((folder / 'manifest.json').read_text(encoding='utf-8'))
    if manifest['version'] != version or set(manifest['sha256']) != expected:
        raise ValueError('Publication manifest does not describe this version\'s exact files')
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
    if manifest['commit'] != commit:
        raise ValueError('Publication files belong to a different source commit')
    for name, checksum in manifest['sha256'].items():
        if digest(folder / name) != checksum:
            raise ValueError('Checksum mismatch for ' + name)
    if digest(folder / 'release-notes.md') != manifest['notes_sha256']:
        raise ValueError('Release notes changed after packaging')
    sums = ''.join(checksum + '  ' + name + '\n' for name, checksum in manifest['sha256'].items())
    if (folder / 'SHA256SUMS.txt').read_text(encoding='utf-8') != sums:
        raise ValueError('Checksum file changed after packaging')
    with zipfile.ZipFile(folder / ('applied-agristics-' + version + '.jar')) as archive:
        metadata = tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode())
        mod = next(mod for mod in metadata['mods'] if mod['modId'] == 'appliedagristics')
        if mod['version'] != version:
            raise ValueError('The JAR internal version differs from the requested version')


def bundle(root, version, release_type):
    version = validated_version(version, release_type)
    folder = root / 'build/publication'
    folder.mkdir(parents=True, exist_ok=False)
    jar_name = 'applied-agristics-' + version + '.jar'
    source_name = 'applied-agristics-' + version + '-source.zip'
    shutil.copyfile(root / 'build/libs' / jar_name, folder / jar_name)
    subprocess.run(['git', 'archive', '--format=zip', '--prefix=applied-agristics-' + version + '/',
                    '--output=' + str((folder / source_name).resolve()), 'HEAD'], cwd=root, check=True)
    shutil.copyfile(root / 'docs/RELEASE_NOTES.md', folder / 'release-notes.md')
    checksums = {name: digest(folder / name) for name in (jar_name, source_name)}
    (folder / 'SHA256SUMS.txt').write_text(''.join(value + '  ' + name + '\n' for name, value in checksums.items()), encoding='utf-8')
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
    manifest = {'version': version, 'type': release_type, 'commit': commit, 'sha256': checksums,
                'notes_sha256': digest(folder / 'release-notes.md')}
    (folder / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    verify(root, version)


def write_outputs(values):
    destination = os.environ.get('GITHUB_OUTPUT')
    if destination:
        with open(destination, 'a', encoding='utf-8') as output:
            for key, value in values.items():
                if '\n' in str(value) or '\r' in str(value):
                    raise ValueError('Unexpected newline in an action output')
                output.write(key + '=' + str(value) + '\n')


def main():
    command = sys.argv[1]
    version = os.environ.get('RELEASE_VERSION', '')
    release_type = os.environ.get('RELEASE_TYPE', 'release')
    if command == 'prepare':
        output = prepare(ROOT, version, release_type, os.environ.get('RELEASE_TARGET', 'all'),
                         os.environ.get('RELEASE_DRY_RUN', 'true') == 'true')
        write_outputs(output)
        print('Prepared ' + output['version'] + ' for ' + output['platforms'])
    elif command == 'credentials':
        write_outputs(credentials(ROOT, json.loads(os.environ['RELEASE_PLATFORMS'])))
        print('Selected platform credentials checked')
    elif command == 'bundle':
        bundle(ROOT, version, release_type)
        print('Publication files packaged and checksummed')
    elif command == 'verify':
        verify(ROOT, version)
        print('Publication files verified')
    else:
        raise ValueError('Unknown release command')


if __name__ == '__main__':
    main()
