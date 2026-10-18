"""Build only the compatibility patch; original mod and game stay external."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def game_classpath(game):
    version_dir = game / "versions/1.8.9-SteinLoader"
    version = json.loads((version_dir / "1.8.9-SteinLoader.json").read_text(encoding="utf-8"))
    libraries = []
    for library in version["libraries"]:
        artifact = library.get("downloads", {}).get("artifact")
        if artifact:
            libraries.append(game / "libraries" / artifact["path"])
        elif "natives" not in library:
            group, name, release = library["name"].split(":")
            libraries.append(game / "libraries" / group.replace(".", "/") / name / release
                             / (name + "-" + release + ".jar"))
    libraries.append(version_dir / "1.8.9-SteinLoader.jar")
    for library in libraries:
        if not library.is_file():
            raise ValueError("Biblioteca ausente: " + str(library))
    return libraries


def dependencies(directory, download):
    result = {}
    for entry in json.loads((ROOT / "dependencies.lock.json").read_text()):
        path = directory / entry["file"]
        if not path.is_file():
            if not download:
                raise ValueError("Dependência ausente: " + str(path) + "; use --download-deps")
            data = urllib.request.urlopen(entry["url"], timeout=60).read()
            if hashlib.sha256(data).hexdigest() != entry["sha256"]:
                raise ValueError("SHA256 inesperado: " + entry["file"])
            directory.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
        if hashlib.sha256(path.read_bytes()).hexdigest() != entry["sha256"]:
            raise ValueError("SHA256 inesperado: " + str(path))
        result[entry["file"]] = path
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True, help="Macros 0.11.4 original para Stein")
    parser.add_argument("--game-dir", type=Path, required=True, help="Diretório .steinclient da SVG 7.0")
    parser.add_argument("--java", type=Path, help="Java 25+; padrão: runtime da instalação")
    parser.add_argument("--deps-dir", type=Path, default=ROOT / "deps")
    parser.add_argument("--download-deps", action="store_true")
    args = parser.parse_args()
    original, game = args.input.resolve(), args.game_dir.resolve()
    java = args.java or game / "stein-client/runtime/bin" / ("java.exe" if os.name == "nt" else "java")
    cp = game_classpath(game)
    with zipfile.ZipFile(original) as archive:
        metadata = json.loads(archive.read("steinmod.json"))
        if metadata.get("id") != "macros" or metadata.get("version") != "0.11.4":
            raise ValueError("É necessário o Macros 0.11.4 já portado para Stein, não o .litemod")
        if "Svg7Graphics.class" in archive.namelist():
            raise ValueError("Este pacote já tem a adaptação GFX; use a base original")
        input_patched = "com/litemod/Svg7Input.class" in archive.namelist()
    deps = dependencies(args.deps_dir.resolve(), args.download_deps)
    build = ROOT / "build"
    build.mkdir(exist_ok=True)
    libraries = build / "libs"
    libraries.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="compile-", dir=build) as temporary:
        work = Path(temporary)

        def run(*command):
            subprocess.run(list(map(str, command)), check=True)

        def compile_java(sources, destination, classpath):
            destination.mkdir()
            run(java, "-jar", deps["ecj-3.43.0.jar"], "-encoding", "UTF-8", "-source", "8", "-target", "8",
                "-d", destination, "-cp", os.pathsep.join(map(str, classpath)), *sources)

        asm = [deps["asm-9.8.jar"], deps["asm-tree-9.8.jar"]]
        patchers = work / "patchers"
        compile_java(sorted((ROOT / "src/patcher/java").glob("*.java")), patchers, asm)
        patch_cp = os.pathsep.join(map(str, [patchers] + asm))
        intermediate = original
        if not input_patched:
            inputs = work / "input"
            compile_java([ROOT / "src/main/java/com/litemod/Svg7Input.java"], inputs, cp)
            intermediate = work / "Macros-input.steinmod"
            run(java, "-cp", patch_cp, "PatchMacros", original, intermediate,
                inputs / "com/litemod/Svg7Input.class")
        gfx = work / "gfx"
        compile_java([ROOT / "src/main/java" / name for name in
                      ["Svg7Graphics.java", "Svg7Logic.java", "Svg7Reflect.java"]], gfx, cp + [intermediate])
        artifact = work / "Macros.steinmod"
        run(java, "-cp", patch_cp, "PatchMacrosGfx", intermediate, artifact, gfx)
        # The distribution manifest describes this adaptation, not the older base.
        packaged = work / "Macros-release.steinmod"
        with zipfile.ZipFile(artifact) as source, zipfile.ZipFile(packaged, "w", zipfile.ZIP_DEFLATED) as output:
            for entry in source.infolist():
                data = ((ROOT / "src/main/resources/steinmod.json").read_bytes()
                        if entry.filename == "steinmod.json" else source.read(entry))
                output.writestr(entry, data)
        packaged.replace(artifact)
        tests = work / "tests"
        compile_java(sorted((ROOT / "tests/java").glob("*.java")), tests, cp + [artifact])
        test_cp = os.pathsep.join(map(str, [tests, artifact] + cp))
        for test in ["TestSvg7Input", "TestGfxCompatibility"]:
            run(java, "-cp", test_cp, test, artifact)
        with zipfile.ZipFile(work / "MigrationProbe.steinmod", "w", zipfile.ZIP_DEFLATED) as archive:
            archive.write(tests / "com/litemod/test/MigrationProbe.class", "com/litemod/test/MigrationProbe.class")
            archive.writestr("steinmod.json", json.dumps(dict(id="zz_migration_probe", name="Migration Test",
                              version="1.0", entry="com.litemod.test.MigrationProbe", loader="0.1.27")))
        for name in ["Macros.steinmod", "MigrationProbe.steinmod"]:
            shutil.copy2(work / name, libraries / name)
        report = dict(input_sha256=hashlib.sha256(original.read_bytes()).hexdigest(),
                      output_sha256=hashlib.sha256(artifact.read_bytes()).hexdigest(),
                      input_already_adapted=input_patched,
                      tested=dict(minecraft="1.8.9", loader="0.1.27", bridge="0.1.21"))
        (build / "build-report.json").write_text(json.dumps(report, indent=2) + "\n")
    print("Build e testes concluídos:", libraries / "Macros.steinmod")
    print("Probe somente para testes:", libraries / "MigrationProbe.steinmod")
    print("A instalação do jogo não foi alterada.")


if __name__ == "__main__":
    main()
