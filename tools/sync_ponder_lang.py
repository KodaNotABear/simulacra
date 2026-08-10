"""Regenerate the ponder.* lang keys in en_us.json from the scene storyboards.

Ponder numbers a scene's text keys (text_1, text_2, ...) by the order the showText calls
execute, so inserting a line in the middle silently renumbers every key after it. Rather
than hand-maintaining that, this reads SimulacraScenes.java and rewrites the block of
simulacra.ponder.* keys to match the source exactly.

Run from the repo root after editing scenes:
    python tools/sync_ponder_lang.py
"""
import collections
import json
import os
import re

ROOT = os.path.join(os.path.dirname(__file__), "..")
SCENES = os.path.join(ROOT, "src", "main", "java", "studio", "akuro", "simulacra",
                      "client", "ponder", "SimulacraScenes.java")
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "simulacra", "lang", "en_us.json")
PREFIX = "simulacra.ponder."

METHOD = re.compile(r"public static void \w+\(SceneBuilder")
TITLE = re.compile(r'\.title\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)')
TEXT = re.compile(r'\.text\(\s*"([^"]+)"\s*\)')


def scenes_from_source():
    source = open(SCENES, encoding="utf-8").read()
    # Slice the file into one chunk per storyboard method so text calls stay attributed
    # to the scene they belong to.
    starts = [m.start() for m in METHOD.finditer(source)]
    if not starts:
        raise SystemExit("no storyboard methods found - did the signature change?")
    bounds = list(zip(starts, starts[1:] + [len(source)]))

    found = []
    for start, end in bounds:
        chunk = source[start:end]
        title = TITLE.search(chunk)
        if not title:
            raise SystemExit("a storyboard has no title(id, header) call")
        scene_id, header = title.groups()
        texts = [m.group(1) for m in TEXT.finditer(chunk)]
        found.append((scene_id, header, texts))
    return found


def main():
    found = scenes_from_source()

    generated = collections.OrderedDict()
    for scene_id, header, texts in found:
        generated[PREFIX + scene_id + ".header"] = header
        for i, text in enumerate(texts, start=1):
            generated[PREFIX + scene_id + ".text_%d" % i] = text

    lang = json.load(open(LANG, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
    kept = collections.OrderedDict((k, v) for k, v in lang.items() if not k.startswith(PREFIX))
    kept.update(generated)

    with open(LANG, "w", encoding="utf-8") as f:
        json.dump(kept, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print("synced %d ponder keys across %d scenes:" % (len(generated), len(found)))
    for scene_id, _, texts in found:
        print("   %-22s header + %d text" % (scene_id, len(texts)))


if __name__ == "__main__":
    main()
