from pathlib import Path
import json
import os
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import release


class ReleaseTests(unittest.TestCase):
    def fixture(self, root):
        (root / '.github').mkdir()
        (root / 'docs').mkdir()
        (root / 'gradle.properties').write_text('mod_version=1.3.1\n', encoding='utf-8')
        settings = {'curseforge_id': '1772431', 'modrinth_id': 'applied-agristics',
                    'dependencies': dict(release.DEPENDENCIES),
                    'minecraft': '1.21.1', 'loader': 'neoforge', 'java': '21'}
        (root / '.github/release.json').write_text(json.dumps(settings), encoding='utf-8')
        (root / 'docs/RELEASE_NOTES.md').write_text('# Applied Agristics 1.3.1\nA release note long enough to describe the tested update.\n', encoding='utf-8')
        (root / 'README.md').write_text('当前版本为 **1.3.1**。\n', encoding='utf-8')

    def test_dependency_identity_rejects_the_crafttweaker_id(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.fixture(root)
            path = root / '.github/release.json'
            path.write_text(path.read_text().replace('mystical-agriculture(required)', '239197(required)'), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'Dependency IDs'):
                release.prepare(root, '1.3.1', 'release', 'all', True)

    def test_exact_source_notes_and_readme_versions_are_required(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.fixture(root)
            for source in ('1.3.1-dev', '1.3.1-beta.1'):
                (root / 'gradle.properties').write_text('mod_version=' + source + '\n')
                with self.assertRaisesRegex(ValueError, 'different mod_version'):
                    release.prepare(root, '1.3.1', 'release', 'all', True)
            (root / 'gradle.properties').write_text('mod_version=1.3.1\n')
            (root / 'docs/RELEASE_NOTES.md').write_text('# Applied Agristics 1.3.10\nDetailed update notes for a different release.\n')
            with self.assertRaisesRegex(ValueError, 'exact requested version'):
                release.prepare(root, '1.3.1', 'release', 'all', True)
            (root / 'docs/RELEASE_NOTES.md').write_text('# Applied Agristics 1.3.1\nDetailed release notes for the current version.\n')
            (root / 'README.md').write_text('当前版本为 **1.3.0**。\n', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'README'):
                release.prepare(root, '1.3.1', 'release', 'all', True)

    def test_versions_are_normalized_and_shell_text_is_rejected(self):
        self.assertEqual(release.validated_version('v1.3.1', 'release'), '1.3.1')
        self.assertEqual(release.validated_version('1.3.1-beta.1', 'beta'), '1.3.1-beta.1')
        self.assertEqual(release.validated_version('1.3.1-rc.1', 'beta'), '1.3.1-rc.1')
        for version in ('../1.3.1', '1.3.1;echo token', '1.3.1\nsecret', '1.3.1-dev'):
            with self.assertRaises(ValueError):
                release.validated_version(version, 'release')
        with self.assertRaises(ValueError):
            release.validated_version('1.3.1-beta.1', 'release')

    def test_modrinth_readback_rejects_wrong_dependencies(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.fixture(root)
            target = root / 'build/publication'
            target.mkdir(parents=True)
            jar = 'applied-agristics-1.3.1.jar'
            (target / 'manifest.json').write_text(json.dumps({'sha256': {jar: 'tested-sha256'}}))
            files = [{'name': jar}]
            remote = {'project_id': 'project123', 'version_number': '1.3.1', 'name': jar,
                      'game_versions': ['1.21.1'], 'loaders': ['neoforge'],
                      'dependencies': [{'project_id': value, 'dependency_type': 'required'}
                                       for value in ('XxWD5pD3', 'C95ReXie')],
                      'files': [{'filename': jar, 'hashes': {'sha256': 'tested-sha256'}}]}
            with patch.dict(os.environ, {'MODRINTH_TOKEN': 'test-value', 'GITHUB_STEP_SUMMARY': ''}), \
                    patch.object(release, 'api_json', return_value=remote):
                release.receipt(root, '1.3.1', 'modrinth', 'project123', 'version123',
                                'https://modrinth.com/mod/applied-agristics/version/version123', files)
                remote['dependencies'] = []
                with self.assertRaisesRegex(ValueError, 'metadata or file hash'):
                    release.receipt(root, '1.3.1', 'modrinth', 'project123', 'version123',
                                    'https://modrinth.com/mod/applied-agristics/version/version123', files)

    def test_receipts_require_expected_files_and_platform_identity(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.fixture(root)
            (root / 'build/publication').mkdir(parents=True)
            files = [{'name': 'applied-agristics-1.3.1.jar', 'id': 123, 'url': 'https://example.invalid/file'}]
            with patch.dict(os.environ, {'GITHUB_STEP_SUMMARY': ''}):
                result = release.receipt(root, '1.3.1', 'curseforge', '1772431', '9487306',
                                         'https://www.curseforge.com/minecraft/mc-mods/applied-agristics/files/9487306', files)
                self.assertEqual(result['status'], 'upload accepted')
                self.assertTrue((root / 'build/publication/curseforge-receipt.json').is_file())
                with self.assertRaisesRegex(ValueError, 'identity mismatch'):
                    release.receipt(root, '1.3.1', 'curseforge', '239197', '9487306',
                                    result['url'], files)
                with self.assertRaisesRegex(ValueError, 'unexpected files'):
                    release.receipt(root, '1.3.1', 'curseforge', '1772431', '9487306',
                                    result['url'], [])
                with self.assertRaisesRegex(ValueError, 'receipt URL'):
                    release.receipt(root, '1.3.1', 'curseforge', '1772431', '9487306',
                                    'https://example.invalid/wrong-platform', files)

    def test_prepare_blocks_wrong_source_and_missing_notes(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.fixture(root)
            result = release.prepare(root, '1.3.1', 'release', 'all', True)
            self.assertEqual(json.loads(result['platforms']), list(release.PLATFORMS))
            with self.assertRaisesRegex(ValueError, 'different mod_version'):
                release.prepare(root, '1.3.2', 'release', 'all', True)
            (root / 'docs/RELEASE_NOTES.md').write_text('', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'release notes'):
                release.prepare(root, '1.3.1', 'release', 'all', True)

    def test_real_publication_rejects_an_existing_tag(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.fixture(root)
            with patch.object(release.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, 'sha refs/tags/v1.3.1')):
                with self.assertRaisesRegex(ValueError, 'already exists'):
                    release.prepare(root, '1.3.1', 'release', 'github', False)

    def test_credentials_fail_before_upload_and_never_echo_values(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.fixture(root)
            with patch.dict(os.environ, {'MODRINTH_TOKEN': ''}):
                with self.assertRaisesRegex(ValueError, 'MODRINTH_TOKEN'):
                    release.credentials(root, ['modrinth'])
            with patch.dict(os.environ, {'MODRINTH_TOKEN': 'private-test-value'}), \
                    patch.object(release, 'api_json', side_effect=[
                        {'id': 'project123', 'slug': 'applied-agristics'},
                        {'id': 'XxWD5pD3', 'slug': 'ae2'},
                        {'id': 'C95ReXie', 'slug': 'mystical-agriculture'}]):
                self.assertEqual(release.credentials(root, ['modrinth']), {'modrinth_id': 'project123'})

    def test_bundle_matches_source_and_detects_file_tampering(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.fixture(root)
            subprocess.run(['git', 'init', '-q'], cwd=root, check=True)
            subprocess.run(['git', 'add', '.'], cwd=root, check=True)
            subprocess.run(['git', '-c', 'user.name=Release Test', '-c', 'user.email=release@example.invalid',
                            'commit', '-qm', 'fixture'], cwd=root, check=True)
            libs = root / 'build/libs'
            libs.mkdir(parents=True)
            jar = libs / 'applied-agristics-1.3.1.jar'
            with zipfile.ZipFile(jar, 'w') as archive:
                archive.writestr('META-INF/neoforge.mods.toml', '[[mods]]\nmodId="appliedagristics"\nversion="1.3.1"\n')
            release.bundle(root, '1.3.1', 'release')
            release.verify(root, '1.3.1')
            packed = root / 'build/publication'
            with zipfile.ZipFile(packed / 'applied-agristics-1.3.1-source.zip') as archive:
                self.assertTrue(all(name.startswith('applied-agristics-1.3.1/') for name in archive.namelist()))
                self.assertFalse(any('/build/' in name for name in archive.namelist()))
            config_path = root / '.github/release.json'
            original_config = config_path.read_bytes()
            config_path.write_bytes(original_config + b' ')
            with self.assertRaisesRegex(ValueError, 'configuration changed'):
                release.verify(root, '1.3.1')
            config_path.write_bytes(original_config)
            (packed / jar.name).write_bytes(b'tampered')
            with self.assertRaisesRegex(ValueError, 'Checksum mismatch'):
                release.verify(root, '1.3.1')


if __name__ == '__main__':
    unittest.main()
