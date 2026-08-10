"""Generate the structure templates Ponder scenes are staged on, as vanilla structure NBT.

Ponder scenes need a small world to play out in. These are deliberately plain: a casing
floor with the machines laid out in the arrangement each scene talks about. They are
scaffolding to be replaced - build the scene in-game, save it with a structure block, and
drop it in over the generated file. Existing files are never overwritten without --force.

The Neural Node scenes are already hand-authored and so are not generated here.

Run from the repo root:
    python tools/make_ponder_structures.py
"""
import gzip
import os
import struct
import sys

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "simulacra", "ponder")
DATA_VERSION = 3955  # 1.21.1

BASE = "create:andesite_casing"


# --- minimal NBT writer (same shape as the one in the Noclip repo) ---------

def tag_string(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def named(tagtype, name, payload):
    return bytes([tagtype]) + tag_string(name) + payload


def t_int(v):
    return struct.pack(">i", v)


def t_compound(items):
    return b"".join(named(t, n, p) for t, n, p in items) + b"\x00"


def t_list(tagtype, payloads):
    return bytes([tagtype]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def palette_entry(name, props=None):
    items = [(8, "Name", tag_string(name))]
    if props:
        items.append((10, "Properties", t_compound([(8, k, tag_string(v)) for k, v in props.items()])))
    return t_compound(items)


def block(pos, state):
    return t_compound([
        (9, "pos", t_list(3, [t_int(p) for p in pos])),
        (3, "state", t_int(state)),
    ])


class Scene:
    """Collects blocks and de-duplicates palette entries as they are placed."""

    def __init__(self, size):
        self.size = size
        self._palette = []
        self._index = {}
        self.blocks = []

    def state(self, name, props=None):
        key = (name, tuple(sorted((props or {}).items())))
        if key not in self._index:
            self._index[key] = len(self._palette)
            self._palette.append(palette_entry(name, props))
        return self._index[key]

    def put(self, pos, name, props=None):
        self.blocks.append(block(pos, self.state(name, props)))

    def floor(self, name=BASE):
        w, _, d = self.size
        for x in range(w):
            for z in range(d):
                self.put((x, 0, z), name)

    def write(self, filename):
        root = t_compound([
            (9, "size", t_list(3, [t_int(v) for v in self.size])),
            (9, "entities", t_list(0, [])),
            (9, "blocks", t_list(10, self.blocks)),
            (9, "palette", t_list(10, self._palette)),
            (3, "DataVersion", t_int(DATA_VERSION)),
        ])
        os.makedirs(OUT, exist_ok=True)
        path = os.path.join(OUT, filename)
        # These are scaffolding. Once a scene has been built in-game and saved over, that file is
        # authored work and this script must not silently destroy it.
        if os.path.exists(path) and "--force" not in sys.argv:
            return (filename, "kept existing")
        with gzip.open(path, "wb") as f:
            f.write(named(10, "", root))
        return (filename, "written")


def node(scene, pos, facing="north"):
    scene.put(pos, "simulacra:neural_node", {"facing": facing, "lit": "false"})


def controller(scene, pos, facing="north"):
    scene.put(pos, "simulacra:mainframe_controller", {"facing": facing, "lit": "false"})


def chamber(scene, pos, facing="north"):
    scene.put(pos, "simulacra:simulation_chamber", {"facing": facing, "lit": "false"})


def cable(scene, pos, **connections):
    props = {d: "false" for d in ("north", "east", "south", "west", "up", "down")}
    props.update({k: "true" for k in connections})
    scene.put(pos, "simulacra:data_cable", props)


# --- scene 2: the Cognition Array and its controller ----------------------

def mainframe_controller():
    s = Scene((7, 4, 7))
    s.floor()
    z = 3
    # A 2x2 rack of nodes, contiguous so they form one array, with the
    # controller cabled onto the side of it.
    for x in (2, 3):
        for y in (1, 2):
            node(s, (x, y, z), "east")
    s.put((0, 1, z), "create:shaft", {"axis": "x"})
    s.put((1, 1, z), "create:shaft", {"axis": "x"})
    cable(s, (4, 1, z), west=True, east=True)
    controller(s, (5, 1, z), "west")
    return s.write("mainframe_controller.nbt")


# --- scene 3: Data Cable reaching a remote consumer -----------------------

def data_cable():
    s = Scene((9, 4, 9))
    s.floor()
    z = 4
    node(s, (1, 1, z), "east")
    node(s, (2, 1, z), "east")
    controller(s, (3, 1, z), "west")
    for x in range(4, 7):
        cable(s, (x, 1, z), west=True, east=True)
    chamber(s, (7, 1, z), "west")
    return s.write("data_cable.nbt")


# --- scene 4: the Simulation Chamber doing the work -----------------------

def simulation_chamber():
    s = Scene((7, 4, 7))
    s.floor()
    z = 3
    node(s, (1, 1, z), "east")
    node(s, (1, 2, z), "east")
    controller(s, (2, 1, z), "west")
    cable(s, (3, 1, z), west=True, east=True)
    chamber(s, (4, 1, z), "west")
    return s.write("simulation_chamber.nbt")


if __name__ == "__main__":
    results = [mainframe_controller(), data_cable(), simulation_chamber()]
    print("ponder structures in %s" % os.path.normpath(OUT))
    for name, status in results:
        print("   %-28s %s" % (name, status))
    if any(status == "kept existing" for _, status in results):
        print("\nSome files already existed and were left alone. Pass --force to overwrite them,")
        print("but note that anything saved from a structure block will be lost if you do.")
