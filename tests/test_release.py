import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("prepare_release", ROOT / "scripts/prepare_release.py")
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        (ROOT / "build").mkdir(exist_ok=True)

    def package(self, directory, extra=None, **overrides):
        manifest = dict(id="macros", name="Macro / Keybind Mod", version="1.0.0",
                        author="Mumfrey", entry="com.litemod.MacroBridge", loader="0.1.27")
        manifest.update(overrides)
        artifact = directory / "Macros.steinmod"
        with zipfile.ZipFile(artifact, "w") as archive:
            archive.writestr("steinmod.json", json.dumps(manifest))
            archive.writestr("com/litemod/MacroBridge.class", b"test fixture")
            if extra:
                archive.writestr(extra, b"unexpected")
        return artifact

    def test_index_uses_actual_archive_id_version_and_hash(self):
        with tempfile.TemporaryDirectory(dir=ROOT / "build") as temporary:
            work = Path(temporary)
            artifact = self.package(work)
            release.prepare(artifact, work / "dist", "davi-aires/Macro-Stein", "Release notes")
            index = json.loads((work / "dist/steinmods.json").read_text())
            mod = index["mods"][0]
            self.assertEqual(index["format"], 1)
            self.assertEqual(index["source"],
                             "https://github.com/davi-aires/Macro-Stein/releases/latest/download/steinmods.json")
            self.assertEqual((mod["modId"], mod["version"], mod["loader"]), ("macros", "1.0.0", "0.1.27"))
            self.assertEqual(mod["url"],
                             "https://github.com/davi-aires/Macro-Stein/releases/download/v1.0.0/Macros.steinmod")
            self.assertEqual(mod["sha256"], hashlib.sha256(artifact.read_bytes()).hexdigest())
            self.assertEqual((work / "dist/Macros.steinmod").read_bytes(), artifact.read_bytes())
            self.assertEqual((work / "dist/SHA256SUMS.txt").read_text(),
                             mod["sha256"] + "  Macros.steinmod\n")

    def test_rejects_game_files_and_migration_probe(self):
        for extra, overrides in [("net/minecraft/Test.class", {}),
                                 ("assets/minecraft/test.png", {}),
                                 (None, {"id": "zz_migration_probe"}),
                                 (None, {"version": "../1.0"}),
                                 (None, {"loader": "0.1.0"})]:
            with self.subTest(extra=extra, overrides=overrides), tempfile.TemporaryDirectory(dir=ROOT / "build") as temporary:
                work = Path(temporary)
                artifact = self.package(work, extra, **overrides)
                with self.assertRaises(ValueError):
                    release.prepare(artifact, work / "dist", "davi-aires/Macro-Stein", "Notes")
                self.assertFalse((work / "dist").exists())


if __name__ == "__main__":
    unittest.main()
