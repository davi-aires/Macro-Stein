"""Prepare a Stein Client index and release assets without publishing anything."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def prepare(artifact, output, repository, notes):
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Repositório deve ter o formato dono/nome")
    with zipfile.ZipFile(artifact) as archive:
        manifest = json.loads(archive.read("steinmod.json"))
        if manifest.get("id") != "macros" or manifest.get("entry") != "com.litemod.MacroBridge":
            raise ValueError("O pacote não é o Macros; não publique o MigrationProbe")
        version = manifest.get("version", "")
        loader = manifest.get("loader", "")
        if not re.fullmatch(r"\d+\.\d+\.\d+", version):
            raise ValueError("A release estável exige versão numérica x.y.z")
        if not re.fullmatch(r"\d+\.\d+\.\d+", loader) or tuple(map(int, loader.split("."))) < (0, 1, 27):
            raise ValueError("O manifest deve exigir Loader 0.1.27 ou superior")
        if any(name.startswith(("net/minecraft/", "assets/minecraft/")) for name in archive.namelist()):
            raise ValueError("O pacote contém arquivos do jogo")
        if archive.testzip() is not None:
            raise ValueError("O pacote está corrompido")
    if not notes.strip():
        raise ValueError("A release precisa de changelog")
    digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
    homepage = "https://github.com/" + repository
    index = dict(format=1, name="Macros para Stein GFX", author="davi-aires", homepage=homepage,
                 source=homepage + "/releases/latest/download/steinmods.json", mods=[dict(
                     id="macros", modId=manifest["id"], name=manifest["name"], version=version,
                     file="Macros.steinmod", url=homepage + "/releases/download/v" + version + "/Macros.steinmod",
                     sha256=digest, loader=loader, description=manifest.get("description", ""))])
    output.mkdir(parents=True, exist_ok=True)
    if artifact.resolve() != (output / "Macros.steinmod").resolve():
        shutil.copy2(artifact, output / "Macros.steinmod")
    (output / "steinmods.json").write_text(json.dumps(index, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (output / "SHA256SUMS.txt").write_text(digest + "  Macros.steinmod\n", encoding="utf-8")
    (output / "notes.md").write_text(notes.rstrip() + "\n\nÍndice de atualização do Stein Client:\n\n" +
                                   index["source"] + "\n\nSHA-256: `" + digest + "`\n", encoding="utf-8")
    return index


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact", type=Path, default=ROOT / "build/libs/Macros.steinmod")
    parser.add_argument("--output", type=Path, default=ROOT / "dist")
    parser.add_argument("--repository", default="davi-aires/Macro-Stein")
    args = parser.parse_args()
    with zipfile.ZipFile(args.artifact) as archive:
        version = json.loads(archive.read("steinmod.json"))["version"]
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("Versão de release inválida")
    notes = (ROOT / "changelog" / (version + ".pt_BR.md")).read_text(encoding="utf-8")
    index = prepare(args.artifact, args.output, args.repository, notes)
    print("Release preparada em:", args.output)
    print("Tag: v" + index["mods"][0]["version"])
    print("Índice após publicar:", index["source"])


if __name__ == "__main__":
    main()
