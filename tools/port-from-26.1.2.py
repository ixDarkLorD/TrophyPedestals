"""
Brings the sources over from the 26.1.2 project (the source of truth) and ports them to Minecraft 26.3.

    python tools/port-from-26.1.2.py [path to TrophyPedestals-26.1.2]

Copies the common/, neoforge/ and fabric/ sources and resources, the example data pack (testpack/) and the changelog,
applies the renames between the two versions, then the edits that need more than a rename (to any file, metadata too),
and rewrites the loot tables in 26.3's format.
An edit whose original text isn't found stops the script, so a change upstream is noticed instead of half ported. Files
in KEEP are written by hand for this version and are never overwritten: port changes to them by hand. The build files
(gradle.properties, the build.gradle files) and docs/ are this version's own and aren't touched.
"""
import json
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

# KubeJS (and Rhino) have no 26.3 build yet: its plugin isn't copied (and nothing else loads it).
SKIP = [
    "neoforge/src/main/java/net/ixdarklord/trophypedestals/neoforge/compat/kubejs",
    "neoforge/src/main/resources/kubejs.plugins.txt",
    # FTB Quests (and FTB Library) have no build for this version: FTB's Maven has only 26.1.2 ones.
    "common/src/main/java/net/ixdarklord/trophypedestals/compat/ftb",
]

RENAMES = [
    # (Also when it's called on the game itself.)
    (r"\bMinecraft\.getInstance\(\)\.setScreen\(", "Minecraft.getInstance().gui.setScreen("),
    # Dyed items come in a collection by color.
    (r"\bItems\.([A-Z]+)_STAINED_GLASS\b", r"Items.STAINED_GLASS.pick(DyeColor.\1)"),
    # Blocks have no codecs (this one's typed as a plain block's).
    (r"\n    public static final MapCodec<(\w+)> CODEC = simpleCodec\(\1::new\);", ""),
    (r"\n    @Override\n    protected MapCodec<\? extends Block> codec\(\) \{\n        return CODEC;\n    \}\n", ""),
    # An advancement's display is a record.
    (r"(\bdisplay|\.display\(\)\.orElseThrow\(\))\.getTitle\(\)", r"\1.title()"),
    (r"(\bdisplay|\.display\(\)\.orElseThrow\(\))\.getIcon\(\)", r"\1.icon()"),
    (r"(\bdisplay|\.display\(\)\.orElseThrow\(\))\.getType\(\)", r"\1.type()"),
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
    # 26.3 only from here on.
    # A pose stack is turned with rotate(); mulPose() takes only matrices.
    (r"\.mulPose\(", ".rotate("),
    # A dropped item says whether its client predicted it (server-side code here, so it didn't).
    (r"\b(player)\.drop\((\w+), false\)", r"\1.drop(\2, false, Prediction.SERVER_ONLY)"),
    (r"\.getOptionalParameter\(", ".getOptional("),
    (r"\bLootItemFunctions\.ROOT_CODEC\b", "LootItemFunctions.DIRECT_CODEC"),
    # How far through its swing an armed mob is.
    (r"\.attackTime\b", ".swingAnimation"),
    # Axes are the items tagged as axes (there's no axe item class).
    (r"stack\.getItem\(\) instanceof AxeItem", "stack.is(ItemTags.AXES)"),
    (r"import net\.minecraft\.world\.item\.AxeItem;\n", ""),
    (r"\bSoundEvents\.AXE_STRIP\b(?!\.)", "SoundEvents.AXE_STRIP.value()"),
    # Blocks have no codecs.
    (r"\n    public static final MapCodec<(\w+)> CODEC = simpleCodec\(\1::new\);", ""),
    (r"\n    @Override\n    protected MapCodec<\? extends BaseEntityBlock> codec\(\) \{\n        return CODEC;\n    \}\n", ""),
    # A loot function has one condition (all_of for more), not a list of them.
    (r"List<LootItemCondition> predicates", "Optional<Holder<LootItemCondition>> condition"),
    (r"super\(predicates\);", "super(condition);"),
    # Loot functions in JSON name their type with "type", as everything else does.
    (r'\\"function\\": ', r'\\"type\\": '),
]

# Classes the renames may start using, imported where they do: (what the text then has, the class).
IMPORTS = [
    ("DyeColor.", "net.minecraft.world.item.DyeColor"),
    ("EntityTypes.", "net.minecraft.world.entity.EntityTypes"),
    ("Vec3.atCenterOf(", "net.minecraft.world.phys.Vec3"),
    ("Prediction.SERVER_ONLY", "net.minecraft.util.Prediction"),
    ("ItemTags.AXES", "net.minecraft.tags.ItemTags"),
    ("Optional<Holder<LootItemCondition>>", "net.minecraft.core.Holder"),
]

# Imports a rename may leave unused: dropped where nothing else names the class.
UNUSED = [
    "net.minecraft.world.entity.EntityType",
    "com.mojang.serialization.MapCodec",
    "net.minecraft.world.level.block.BaseEntityBlock",
    "java.util.List",
]

EDITS: dict[str, list[tuple[str, str]]] = {
    # FTB Quests' reward isn't here (no FTB build for this version).
    "common/src/main/java/net/ixdarklord/trophypedestals/TrophyPedestalsMod.java": [
        ("import net.ixdarklord.trophypedestals.compat.ftb.TrophyReward;\n", ""),
        ("        if (Platform.isModLoaded(\"ftbquests\")) TrophyReward.register();\n", ""),
    ],
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


def loot(value):
    """A loot table (or any part of one) in 26.3's format: one "condition" and one "modifier" (a list is a sequence), each
    naming its kind with "type"; a block state check is match_block."""
    if isinstance(value, list):
        return [loot(item) for item in value]
    if not isinstance(value, dict):
        return value
    out = {}
    for key, item in value.items():
        if key == "functions":
            item = loot(item)
            out["modifier"] = item[0] if len(item) == 1 else item
        elif key == "conditions":
            item = loot(item)
            out["condition"] = item[0] if len(item) == 1 else {"type": "minecraft:all_of", "terms": item}
        elif key in ("function", "condition") and isinstance(item, str):
            out["type"] = item
        else:
            out[key] = loot(item)
    if out.get("type") == "minecraft:block_state_property":
        out = {"type": "minecraft:match_block", "blocks": out["block"], **({"state": out["properties"]} if "properties" in out else {})}
    return out


def port_loot_tables():
    for folder in [ROOT / "common/src/main/resources/data", ROOT / "testpack/data"]:
        for file in folder.glob("*/loot_table/**/*.json"):
            table = json.loads(file.read_text(encoding="utf-8"))
            file.write_text(json.dumps(loot(table), indent=2) + "\n", encoding="utf-8", newline="\n")


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
    for name in UNUSED:
        line = "import " + name + ";\n"
        simple = name.rsplit(".", 1)[1]
        if line in text and re.search(r"\b" + simple + r"\b", text.replace(line, "")) is None:
            text = text.replace(line, "")
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
    port_loot_tables()
    missing = [path for path in KEEP if not (ROOT / path).exists()]
    if missing:
        sys.exit(f"Hand-ported files missing (write them for this version): {missing}")
    print(f"Ported from {SOURCE}")


if __name__ == "__main__":
    port()
