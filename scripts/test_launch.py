"""Smoke test Macros against SVG 7.0 in an isolated game directory."""
import argparse
import json
import pathlib
import shutil
import subprocess
import time
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
BASE = ROOT / "build"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--gfx", choices=["true", "false"], default="true")
    parser.add_argument("--game-dir", type=pathlib.Path, required=True)
    parser.add_argument("--mod", type=pathlib.Path)
    parser.add_argument("--probe", action="store_true")
    parser.add_argument("--gl-debug", action="store_true")
    args = parser.parse_args()
    NEW = args.game_dir.resolve()
    BASE.mkdir(exist_ok=True)
    game = pathlib.Path(tempfile.mkdtemp(prefix="test-gfx-" + args.gfx + "-", dir=BASE))
    (game / "mods").mkdir()
    shutil.copy2(args.mod or BASE / "libs/Macros.steinmod", game / "mods/Macros.steinmod")
    if args.probe:
        shutil.copy2(BASE / "libs/MigrationProbe.steinmod", game / "mods/MigrationProbe.steinmod")
    version_dir = NEW / "versions/1.8.9-SteinLoader"
    version = json.loads((version_dir / "1.8.9-SteinLoader.json").read_text())
    cp = []
    for lib in version["libraries"]:
        artifact = lib.get("downloads", {}).get("artifact")
        if artifact:
            path = NEW / "libraries" / artifact["path"]
            assert path.is_file(), path
            cp.append(str(path))
        elif "natives" not in lib:
            group, name, release = lib["name"].split(":")
            path = NEW / "libraries" / group.replace(".", "/") / name / release / (name + "-" + release + ".jar")
            assert path.is_file(), path
            cp.append(str(path))
    cp.append(str(version_dir / "1.8.9-SteinLoader.jar"))
    command = [
        str(NEW / "stein-client/runtime/bin/java.exe"),
        "-Xmx1024m", "--enable-native-access=ALL-UNNAMED",
        "-Dsteinbridge.gfx=" + args.gfx,
        "-Djava.library.path=" + str(version_dir / "natives-steinclient"),
        "-cp", ";".join(cp), version["mainClass"],
        "--username", "MacroMigration", "--version", "1.8.9",
        "--gameDir", str(game), "--assetsDir", str(NEW / "assets"),
        "--assetIndex", "1.8", "--uuid", "00000000000000000000000000000000",
        "--accessToken", "0", "--userProperties", "{}", "--userType", "legacy",
        "--width", "854", "--height", "480",
    ]
    if args.gl_debug:
        command.insert(1, "-Dsteinbridge.gfx.glDebug=true")
    log = game / "smoke-test.log"
    with log.open("w", encoding="utf-8") as output:
        process = subprocess.Popen(command, cwd=game, stdout=output, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 45
            while process.poll() is None and time.monotonic() < deadline:
                time.sleep(1)
                text = log.read_text(encoding="utf-8", errors="replace")
                if "[migration-test] FAILED" in text:
                    break
                ready = "GFX migration checks complete" if args.probe else "Macro / Keybind Mod iniciado"
                if ready in text:
                    time.sleep(3)
                    break
        finally:
            if process.poll() is None:
                process.terminate()
            process.wait(timeout=10)
    text = log.read_text(encoding="utf-8", errors="replace")
    seen = set()
    for line in text.splitlines():
        if any(word in line.lower() for word in ["macro", "error", "exception", "created:", "gfx", "opengl", "migration-test"]):
            if line not in seen and not line.startswith("\t") and "bnl:" not in line:
                print(line)
                seen.add(line)
    assert "[loader] loaded: macros" in text, "Macros was not loaded; see " + str(log)
    assert "Macro / Keybind Mod iniciado" in text, "Macros did not initialize; see " + str(log)
    assert "textures-atlas" in text, "Game did not finish loading; see " + str(log)
    for failure in ["NoSuchMethodError", "NoSuchFieldException", "NoClassDefFoundError", "VerifyError", "error in com.litemod", "GL ERROR"]:
        assert failure not in text, failure + "; see " + str(log)
    if args.probe:
        assert "[migration-test] FAILED" not in text, "GUI probe failed; see " + str(log)
        assert "GFX migration checks complete" in text, "GUI probe did not finish; see " + str(log)
    print("Logs e capturas:", game)
    print("PASS: Macros initialized and Minecraft reached its main menu (gfx=" + args.gfx + ")")


if __name__ == "__main__":
    main()
