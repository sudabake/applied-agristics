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
        (root / 'gradle.properties').write_text('mod_version=1.3.1-dev\n', encoding='utf-8')
        settings = {'curseforge_id': '1772431', 'modrinth_id': 'applied-agristics',
                    'dependencies': {'curseforge': '223794(required)', 'modrinth': 'XxWD5pD3(required)'},
                    'minecraft': '1.21.1', 'loader': 'neoforge', 'java': '21'}
        (root / '.github/release.json').write_text(json.dumps(settings), encoding='utf-8')
        (root / 'docs/RELEASE_NOTES.md').write_text('# Applied Agristics 1.3.1\nA release note long enough to describe the tested update.\n', encoding='utf-8')

    def test_versions_are_normalized_and_shell_text_is_rejected(self):
        self.assertEqual(release.validated_version('v1.3.1', 'release'), '1.3.1')
        self.assertEqual(release.validated_version('1.3.1-beta.1', 'beta'), '1.3.1-beta.1')
        self.assertEqual(release.validated_version('1.3.1-rc.1', 'beta'), '1.3.1-rc.1')
        for version in ('../1.3.1', '1.3.1;echo token', '1.3.1\nsecret', '1.3.1-dev'):
            with self.assertRaises(ValueError):
                release.validated_version(version, 'release')
        with self.assertRaises(ValueError):
            release.validated_version('1.3.1-beta.1', 'release')

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
                    patch.object(release, 'api_json', return_value={'id': 'project123'}):
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
            (packed / jar.name).write_bytes(b'tampered')
            with self.assertRaisesRegex(ValueError, 'Checksum mismatch'):
                release.verify(root, '1.3.1')


if __name__ == '__main__':
    unittest.main()
