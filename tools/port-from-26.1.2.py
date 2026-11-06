"""
Brings the sources over from the 26.1.2 project (the source of truth) and ports them to Minecraft 26.2.

    python tools/port-from-26.1.2.py [path to TrophyPedestals-26.1.2]

Copies the common/, neoforge/ and fabric/ sources and resources, the example data pack (testpack/) and the changelog,
applies the renames between the two versions, then the edits that need more than a rename (to any file, metadata too).
An edit whose original text isn't found stops the script, so a change upstream is noticed instead of half ported. Files
in KEEP are written by hand for this version and are never overwritten: port changes to them by hand. The build files
(gradle.properties, the build.gradle files) and docs/ are this version's own and aren't touched.
"""
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT.parent / "TrophyPedestals-26.1.2"
MODULES = ["common", "neoforge", "fabric"]
COPIED = ["testpack", "CHANGELOG.md"]
JAVA = "src/main/java/net/ixdarklord/trophypedestals"

KEEP: set[str] = set()

# KubeJS (and Rhino) have no 26.2 build yet: its plugin isn't copied (and nothing else loads it).
SKIP = [
    "neoforge/src/main/java/net/ixdarklord/trophypedestals/neoforge/compat",
    "neoforge/src/main/resources/kubejs.plugins.txt",
]

RENAMES = [
    # The entity type constants moved from EntityType to EntityTypes (EntityType is still the class of a type).
    (r"\bEntityType\.(?=[A-Z][A-Z0-9_]*\b)", "EntityTypes."),
    # The hide-the-HUD toggle (F1) moved from the options to the HUD.
    (r"\b(\w+)\.options\.hideGui\b", r"\1.gui.hud.isHidden()"),
    # Screens are opened through the GUI.
    (r"\bminecraft\.setScreen\(", "minecraft.gui.setScreen("),
    # A block position has no getCenter() of its own.
    (r"\b(\w+)\.getCenter\(\)", r"Vec3.atCenterOf(\1)"),
    # Copper blocks come in sets by weathering: the plain one is the unaffected one.
    (r"\b(Items|Blocks)\.COPPER_BLOCK\b", r"\1.COPPER_BLOCK.weathering().unaffected()"),
]

# Classes the renames may start using, imported where they do: (what the text then has, the class).
IMPORTS = [
    ("EntityTypes.", "net.minecraft.world.entity.EntityTypes"),
    ("Vec3.atCenterOf(", "net.minecraft.world.phys.Vec3"),
]

EDITS: dict[str, list[tuple[str, str]]] = {
    # The quartz and purpur pillars' side textures are named for their side now.
    # The mod list's square icon (logoFile is deprecated: it's the banner now, and dev runs warn about it).
    "neoforge/src/main/resources/META-INF/neoforge.mods.toml": [
        ('logoFile = "mod_logo.png"', 'iconFile = "mod_logo.png"'),
    ],
    "common/src/main/resources/assets/trophypedestals/models/block/quartz_pedestal.json": [
        ('"column": "minecraft:block/quartz_pillar"', '"column": "minecraft:block/quartz_pillar_side"'),
    ],
    "common/src/main/resources/assets/trophypedestals/models/block/purpur_pedestal.json": [
        ('"column": "minecraft:block/purpur_pillar"', '"column": "minecraft:block/purpur_pillar_side"'),
    ],
}


def add_import(text: str, used: str, name: str) -> str:
    """Imports this class, in order among the other imports, if the text uses it and doesn't import it yet."""
    line = "import " + name + ";\n"
    if used not in text or line in text:
        return text
    imports = list(re.finditer(r"^import (?!static )([\w.]+);\n", text, flags=re.M))
    after = next((m for m in imports if m.group(1) > name), None)
    at = after.start() if after is not None else imports[-1].end()
    return text[:at] + line + text[at:]


def fix_imports(text: str) -> str:
    for used, name in IMPORTS:
        text = add_import(text, used, name)
    # An EntityType import nothing but the import names any more.
    entity_type = "import net.minecraft.world.entity.EntityType;\n"
    if re.search(r"\bEntityType\b", text.replace(entity_type, "")) is None:
        text = text.replace(entity_type, "")
    return text


def port():
    for module in MODULES:
        source = SOURCE / module / "src"
        target = ROOT / module / "src"
        kept = {path: (ROOT / path).read_bytes() for path in KEEP if path.startswith(module + "/") and (ROOT / path).exists()}
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(source, target)
        for path in SKIP:
            skipped = ROOT / path
            if skipped.is_dir():
                shutil.rmtree(skipped)
            elif skipped.exists():
                skipped.unlink()
        for path, content in kept.items():
            (ROOT / path).parent.mkdir(parents=True, exist_ok=True)
            (ROOT / path).write_bytes(content)
        for file in target.rglob("*"):
            relative = file.relative_to(ROOT).as_posix()
            is_java = file.suffix == ".java"
            if relative in KEEP or not (is_java or relative in EDITS):
                continue
            text = file.read_text(encoding="utf-8")
            if is_java:
                for pattern, replacement in RENAMES:
                    text = re.sub(pattern, replacement, text)
                text = fix_imports(text)
            for original, replacement in EDITS.get(relative, []):
                if original not in text:
                    sys.exit(f"{relative}: expected text not found, port it by hand:\n{original}")
                text = text.replace(original, replacement)
            file.write_text(text, encoding="utf-8", newline="\n")
    for name in COPIED:
        source, target = SOURCE / name, ROOT / name
        if source.is_dir():
            if target.exists():
                shutil.rmtree(target)
            shutil.copytree(source, target)
        else:
            shutil.copy2(source, target)
    missing = [path for path in KEEP if not (ROOT / path).exists()]
    if missing:
        sys.exit(f"Hand-ported files missing (write them for this version): {missing}")
    print(f"Ported from {SOURCE}")


if __name__ == "__main__":
    port()
