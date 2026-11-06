from pathlib import Path
import json
import os
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from export_version_resources import export, changes


class VersionResourcesTests(unittest.TestCase):
    def test_pack_contains_exact_version_assets_and_detects_preserved_timestamp_edits(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            jar = root / 'test.jar'
            with zipfile.ZipFile(jar, 'w') as archive:
                archive.writestr('pack.mcmeta', '{"pack":{"pack_format":34,"description":"test"}}')
                archive.writestr('assets/appliedagristics/example.json', '{}')
                archive.writestr('assets/ae2/screens/test.json', '{}')
                archive.writestr('data/appliedagristics/recipe/test.json', '{}')
                archive.writestr('io/Test.class', 'test')
            manifest, baseline = export(jar, root / 'packs', 'demo', root / 'manifests')
            directory = Path(baseline['directory'])
            self.assertEqual(len(baseline['files']), 3)
            self.assertFalse((directory / 'data').exists())
            self.assertEqual(changes(manifest), {'modified': [], 'added': [], 'deleted': [], 'timestamp_only': []})
            target = directory / 'assets/appliedagristics/example.json'
            timestamp = target.stat().st_mtime_ns
            target.write_text('[]', encoding='utf-8')
            os.utime(target, ns=(timestamp, timestamp))
            (directory / 'assets/ae2/screens/test.json').unlink()
            (directory / 'extra.txt').write_text('user', encoding='utf-8')
            result = changes(manifest)
            self.assertEqual(result['modified'], ['assets/appliedagristics/example.json'])
            self.assertEqual(result['deleted'], ['assets/ae2/screens/test.json'])
            self.assertEqual(result['added'], ['extra.txt'])
            second, other = export(jar, root / 'packs', 'demo', root / 'manifests')
            self.assertNotEqual(baseline['directory'], other['directory'])
            self.assertEqual(target.read_text(encoding='utf-8'), '[]')

    def test_unsafe_version_and_archive_paths_are_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            jar = root / 'test.jar'
            with zipfile.ZipFile(jar, 'w') as archive:
                archive.writestr('pack.mcmeta', '{}')
                archive.writestr('assets/../../outside.json', '{}')
            with self.assertRaises(ValueError):
                export(jar, root / 'packs', '../bad', root / 'manifests')
            with self.assertRaises(ValueError):
                export(jar, root / 'packs', 'demo', root / 'manifests')


if __name__ == '__main__':
    unittest.main()
