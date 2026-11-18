from pathlib import Path
import json
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]


class StatusTranslationsTest(unittest.TestCase):
    def test_every_infusion_status_has_both_translations(self):
        source = ROOT / 'src/main/java/io/github/sudabake/appliedagristics/block/MEInfusionAltar.java'
        statuses = set(re.findall(r'status\s*=\s*"([a-z_]+)"', source.read_text(encoding='utf-8')))
        self.assertIn('infusing', statuses)
        for language in ('zh_cn', 'en_us'):
            path = ROOT / 'src/main/resources/assets/appliedagristics/lang' / (language + '.json')
            labels = json.loads(path.read_text(encoding='utf-8'))
            for status in statuses:
                self.assertTrue(labels.get('status.appliedagristics.' + status), (language, status))


if __name__ == '__main__':
    unittest.main()
