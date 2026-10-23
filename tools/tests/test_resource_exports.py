"""Portable export regressions; no game, external packages or working resources are modified."""
from pathlib import Path
import gzip
import json
import sys
import tempfile
import unittest
import struct
import zlib

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
from resource_bytes import gzip_bytes, png_comparison_bytes
import install_art_resources


class ResourceExportTests(unittest.TestCase):
    @staticmethod
    def png(pixels, level, metadata=b''):
        def chunk(kind, payload):
            return struct.pack('>I', len(payload)) + kind + payload + struct.pack('>I', zlib.crc32(kind + payload) & 0xffffffff)
        return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 2, 1, 8, 6, 0, 0, 0))
                + (chunk(b'gAMA', metadata) if metadata else b'')
                + chunk(b'IDAT', zlib.compress(b'\0' + pixels, level)) + chunk(b'IEND', b''))

    def test_png_different_compression_preserves_pixels(self):
        pixels = bytes([10, 20, 30, 255, 50, 60, 70, 128])
        with tempfile.TemporaryDirectory() as folder:
            a, b = Path(folder) / 'a.png', Path(folder) / 'b.png'
            a.write_bytes(self.png(pixels, 0)); b.write_bytes(self.png(pixels, 9))
            self.assertNotEqual(a.read_bytes(), b.read_bytes())
            self.assertEqual(png_comparison_bytes(a), png_comparison_bytes(b))

    def test_png_pixel_alpha_and_color_metadata_changes_are_detected(self):
        pixels = bytes([10, 20, 30, 255, 50, 60, 70, 128])
        with tempfile.TemporaryDirectory() as folder:
            a, b = Path(folder) / 'a.png', Path(folder) / 'b.png'
            a.write_bytes(self.png(pixels, 9))
            expected = png_comparison_bytes(a)
            for changed, metadata in ((bytes([11]) + pixels[1:], b''),
                                      (pixels[:-1] + bytes([127]), b''),
                                      (pixels, struct.pack('>I', 45455))):
                b.write_bytes(self.png(changed, 9, metadata))
                self.assertNotEqual(expected, png_comparison_bytes(b))

    def test_png_corrupt_crc_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'bad.png'
            data = bytearray(self.png(bytes(range(8)), 9)); data[-1] ^= 1
            path.write_bytes(data)
            with self.assertRaisesRegex(ValueError, 'CRC'):
                png_comparison_bytes(path)

    def test_gzip_matches_approved_nbt_without_host_metadata(self):
        approved = TOOLS.parent / 'src/main/resources/data/appliedagristics/structure/empty.nbt'
        encoded = approved.read_bytes()
        payload = gzip.decompress(encoded)
        generated = gzip_bytes(payload)
        self.assertEqual(generated, encoded)
        self.assertEqual(generated[3], 0)  # No filename or optional header fields.
        self.assertEqual(generated[4:8], b'\0' * 4)
        self.assertEqual(generated[9], 255)
        self.assertEqual(gzip.decompress(generated), payload)

    def test_gzip_repeated_exports_are_byte_identical(self):
        payload = bytes(range(256)) * 20
        self.assertEqual(gzip_bytes(payload), gzip_bytes(payload))
        self.assertEqual(gzip.decompress(gzip_bytes(payload)), payload)

    def test_collision_export_uses_lf_without_changing_geometry(self):
        original_root, original_assets = install_art_resources.ROOT, install_art_resources.ASSETS
        try:
            with tempfile.TemporaryDirectory() as folder:
                root = Path(folder)
                assets = root / 'assets'
                model = assets / 'models/block/probe.json'
                model.parent.mkdir(parents=True)
                model.write_text(json.dumps({'elements': [
                    {'from': [1, 2, 3], 'to': [14, 15, 16]}
                ]}), encoding='utf-8')
                target = root / 'src/main/java/io/github/sudabake/appliedagristics/block/MachineShapes.java'
                target.parent.mkdir(parents=True)
                install_art_resources.ROOT, install_art_resources.ASSETS = root, assets
                install_art_resources.collision_shapes(['probe'])
                first = target.read_bytes()
                self.assertNotIn(b'\r', first)
                self.assertIn(b'add("probe",new double[][]{{1,2,3,14,15,16}})', first)
                install_art_resources.collision_shapes(['probe'])
                self.assertEqual(target.read_bytes(), first)
        finally:
            install_art_resources.ROOT, install_art_resources.ASSETS = original_root, original_assets


if __name__ == '__main__':
    unittest.main()
