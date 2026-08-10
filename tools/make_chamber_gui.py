"""Generate the Simulation Chamber's screen background.

Reads left to right the way the machine works: the subject being simulated, the substrate it prints
onto, a progress track, and the loot that comes out. Palette and slot conventions are shared with the
Loot Fabricator so the two screens read as the same machine family.

The subject viewport is a recessed well rather than a slot, because what sits in it is a rendered mob
rather than an item you can pick up.

    python tools/make_chamber_gui.py
"""
import os
import sys
import struct
import zlib

W, H = 176, 222
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "simulacra", "textures", "gui")

PANEL = (198, 190, 170)
PANEL_D = (150, 142, 124)
PANEL_L = (228, 222, 204)
BRASS = (158, 105, 71)
BRASS_L = (206, 160, 90)
INPUT = (126, 124, 108)
OUTPUT = (112, 104, 90)
PLAIN = (122, 114, 98)
EDGE_D = (92, 86, 74)
EDGE_L = (216, 210, 192)
HINT = (168, 158, 138)
WELL = (86, 92, 96)
WELL_D = (64, 70, 74)

px = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]


def rect(x1, y1, x2, y2, c):
    for y in range(y1, y2):
        for x in range(x1, x2):
            if 0 <= x < W and 0 <= y < H:
                px[y][x] = (*c, 255)


def bevel(x1, y1, x2, y2, light, dark, fill=None):
    if fill:
        rect(x1, y1, x2, y2, fill)
    rect(x1, y1, x2, y1 + 1, light)
    rect(x1, y1, x1 + 1, y2, light)
    rect(x1, y2 - 1, x2, y2, dark)
    rect(x2 - 1, y1, x2, y2, dark)


def slot(x, y, fill=PLAIN):
    bevel(x - 1, y - 1, x + 17, y + 17, EDGE_D, EDGE_L, fill)


def sheet(x, y):
    """A flat rectangle, echoing an imprint blank, marking the substrate slot."""
    rect(x + 4, y + 3, x + 13, y + 14, HINT)
    rect(x + 5, y + 4, x + 12, y + 13, INPUT)


bevel(0, 0, W, H, PANEL_L, PANEL_D, PANEL)
rect(4, 4, W - 4, 17, BRASS)
rect(4, 4, W - 4, 5, BRASS_L)

# The subject viewport. Everything on this panel aligns to bevels 7 and 169, the edges a vanilla
# nine-wide inventory row uses.
bevel(7, 20, 53, 66, EDGE_D, EDGE_L, WELL)
rect(8, 21, 52, 24, WELL_D)
for cx, cy in ((9, 22), (47, 22), (9, 60), (47, 60)):
    rect(cx, cy, cx + 4, cy + 1, HINT)
    rect(cx, cy, cx + 1, cy + 4, HINT)

# Substrate in. The two lines of text beside it run to the full width of the panel, which is what
# the readout column beside the viewport cannot do.
slot(8, 72, INPUT)
sheet(8, 72)

# Progress, full width so the bar is legible at a glance rather than a sliver.
bevel(7, 97, 169, 105, EDGE_D, EDGE_L, PANEL_D)

# Finished loot, one row of nine.
for c in range(9):
    slot(8 + c * 18, 112, OUTPUT)

for r in range(3):
    for c in range(9):
        slot(8 + c * 18, 146 + r * 18)
for c in range(9):
    slot(8 + c * 18, 204)

raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in px)


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


path = os.path.join(OUT, "simulation_chamber.png")
# This panel is meant to be repainted by hand in an image editor once its layout settles, so
# the generator refuses to overwrite work it did not write. Pass --force to lay it out again.
if os.path.exists(path) and "--force" not in sys.argv:
    print("simulation_chamber.png already exists; left alone. Pass --force to regenerate it.")
    sys.exit(0)

os.makedirs(OUT, exist_ok=True)
with open(os.path.join(OUT, "simulation_chamber.png"), "wb") as f:
    f.write(b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw))
            + chunk(b"IEND", b""))
print("wrote simulation_chamber.png %dx%d" % (W, H))
