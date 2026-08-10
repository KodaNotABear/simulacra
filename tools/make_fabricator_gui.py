"""Generate the Loot Fabricator's screen background.

The panel carries three groups: the stack of Predictions being worked through, the subject's
possible drops laid out as a grid to pick from, and the finished items below. Fill colour keeps
them distinct so the layout reads without on-screen labels. The current choice is marked in the
grid at runtime rather than mirrored into a slot of its own, following how Hostile Neural Networks
presents the same decision.

    python tools/make_fabricator_gui.py
"""
import os
import sys
import struct
import zlib

W, H = 176, 187
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "simulacra", "textures", "gui")

PANEL = (198, 190, 170)
PANEL_D = (150, 142, 124)
PANEL_L = (228, 222, 204)
BRASS = (158, 105, 71)
BRASS_L = (206, 160, 90)
# Slot fills, one per role, so the three groups never read as one undifferentiated grid.
GHOST = (150, 132, 108)
INPUT = (126, 124, 108)
OUTPUT = (112, 104, 90)
PLAIN = (122, 114, 98)
EDGE_D = (92, 86, 74)
EDGE_L = (216, 210, 192)
HINT = (168, 158, 138)
ARROW = (140, 120, 92)

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


def reticle(x, y):
    """Corner brackets, the same motif as the machine's face, marking the target slot."""
    for dx, dy in ((0, 0), (1, 0), (0, 1)):
        rect(x + 2 + dx, y + 2 + dy, x + 3 + dx, y + 3 + dy, HINT)
        rect(x + 13 - dx, y + 2 + dy, x + 14 - dx, y + 3 + dy, HINT)
        rect(x + 2 + dx, y + 13 - dy, x + 3 + dx, y + 14 - dy, HINT)
        rect(x + 13 - dx, y + 13 - dy, x + 14 - dx, y + 14 - dy, HINT)


def token(x, y):
    """A small diamond, echoing the Prediction item, marking the input slots."""
    for dy in range(-3, 4):
        span = 3 - abs(dy)
        rect(x + 8 - span, y + 8 + dy, x + 8 + span + 1, y + 9 + dy, HINT)


bevel(0, 0, W, H, PANEL_L, PANEL_D, PANEL)
rect(4, 4, W - 4, 17, BRASS)
rect(4, 4, W - 4, 5, BRASS_L)

# Preview of the current choice, top left, wearing the machine's corner brackets.
slot(8, 20, GHOST)
reticle(8, 20)

# Predictions in, below it.
slot(8, 68, INPUT)
token(8, 68)

# The palette: one page of the subject's drops, three by three.
for r in range(3):
    for c in range(3):
        slot(30 + c * 18, 20 + r * 18, PLAIN)

# Vertical progress track between palette and output, filled at runtime. Everything on this
# row is spaced so its outer bevels land on 7 and 169, the same edges the inventory rows
# below use - the output block used to run to 175 and left a one-pixel margin on the right.
rect(87, 20, 93, 74, PANEL_D)
rect(87, 20, 88, 74, EDGE_D)

# Finished items, four by four.
for r in range(4):
    for c in range(4):
        slot(98 + c * 18, 20 + r * 18, OUTPUT)

for r in range(3):
    for c in range(9):
        slot(8 + c * 18, 104 + r * 18)
for c in range(9):
    slot(8 + c * 18, 162)

raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in px)


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


path = os.path.join(OUT, "loot_fabricator.png")
# This panel is meant to be repainted by hand in an image editor once its layout settles, so
# the generator refuses to overwrite work it did not write. Pass --force to lay it out again.
if os.path.exists(path) and "--force" not in sys.argv:
    print("loot_fabricator.png already exists; left alone. Pass --force to regenerate it.")
    sys.exit(0)

os.makedirs(OUT, exist_ok=True)
with open(os.path.join(OUT, "loot_fabricator.png"), "wb") as f:
    f.write(b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw))
            + chunk(b"IEND", b""))
print("wrote loot_fabricator.png %dx%d" % (W, H))
