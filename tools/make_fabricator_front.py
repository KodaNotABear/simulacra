"""Generate the Loot Fabricator's front texture (animated) and its unpowered variant.

The Fabricator needed to stop looking like a Simulation Chamber. The chamber is a black box
you feed a mind into; this machine stamps out one specific chosen object, so the face is built
around a targeting reticle closing on a die in the middle rather than a screen. Same brass
frame and lamp palette as the rest of the set, different silhouette at a glance.

    python tools/make_fabricator_front.py
"""
import os
import struct
import zlib

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "simulacra", "textures", "block")
FRAMES = 8

# Palette lifted from the existing machine fronts so the family holds together.
FRAME_LIGHT = (206, 160, 90)
FRAME_MID = (158, 105, 71)
FRAME_DARK = (92, 62, 42)
RECESS = (45, 39, 34)
RECESS_DEEP = (40, 35, 30)
DIE = (120, 98, 42)
DIE_LIT = (240, 196, 80)
RETICLE = (240, 196, 80)
RETICLE_DIM = (120, 98, 42)
LED_ON = (226, 52, 42)
LED_OFF = (92, 30, 24)


def hash_noise(x, y, salt=0):
    v = (x * 374761393 + y * 668265263 + salt * 69069) & 0xFFFFFFFF
    v = ((v ^ (v >> 13)) * 1274126177) & 0xFFFFFFFF
    return (v ^ (v >> 16)) & 0xFF


def jitter(c, x, y, amount, salt=3):
    d = (hash_noise(x, y, salt) % (2 * amount + 1)) - amount
    return tuple(max(0, min(255, v + d)) for v in c)


def frame(f, powered=True):
    px = [[(0, 0, 0, 255)] * 16 for _ in range(16)]
    # Built on mirrored coordinates so the whole face is symmetric about both centre lines;
    # noise keyed on raw x/y would break that and read as grime rather than machining.
    for y in range(16):
        for x in range(16):
            mx, my = min(x, 15 - x), min(y, 15 - y)
            edge = min(mx, my)
            if edge == 0:
                c = jitter(FRAME_DARK, mx, my, 4)
            elif edge == 1:
                c = jitter(FRAME_MID if (mx + my) % 3 else FRAME_LIGHT, mx, my, 5)
            else:
                c = jitter(RECESS if (mx + my) % 2 else RECESS_DEEP, mx, my, 3)
            px[y][x] = (*c, 255)

    # The die in the middle: the thing being stamped out. Four wide, so it straddles the
    # 7.5 centre line and mirrors cleanly instead of sitting one pixel off.
    for y in range(6, 10):
        for x in range(6, 10):
            base = DIE_LIT if powered and (f % 4) < 2 else DIE
            px[y][x] = (*jitter(base, min(x, 15 - x), min(y, 15 - y), 6), 255)

    # Reticle: four corner brackets closing in on the die over the first half of the loop,
    # then springing back out, so the machine reads as repeatedly locking onto a target.
    reach = [4, 3, 3, 2, 2, 3, 3, 4][f % FRAMES] if powered else 4
    colour = RETICLE if powered else RETICLE_DIM
    for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        cx = 8 + dx * reach - (1 if dx < 0 else 0)
        cy = 8 + dy * reach - (1 if dy < 0 else 0)
        if 2 <= cx <= 13 and 2 <= cy <= 13:
            px[cy][cx] = (*colour, 255)
            if 2 <= cx + dx <= 13:
                px[cy][cx + dx] = (*colour, 255)
            if 2 <= cy + dy <= 13:
                px[cy + dy][cx] = (*colour, 255)

    # Status lamps along the bottom lip, lit in mirrored pairs rather than marching across, so
    # the face stays symmetric about its centre line while still reading as active.
    for pair, (left, right) in enumerate(((5, 10), (7, 8))):
        on = powered and (f % FRAMES) // 2 % 2 == pair
        colour = LED_ON if on else LED_OFF
        px[13][left] = (*colour, 255)
        px[13][right] = (*colour, 255)
    return px


def write_png(path, rows):
    h, w = len(rows), len(rows[0])
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in rows)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n"
                + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw))
                + chunk(b"IEND", b""))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    strip = []
    for f in range(FRAMES):
        strip.extend(frame(f, powered=True))
    write_png(os.path.join(OUT, "loot_fabricator_front.png"), strip)
    with open(os.path.join(OUT, "loot_fabricator_front.png.mcmeta"), "w") as fh:
        fh.write('{\n    "animation": {\n        "frametime": 4\n    }\n}\n')
    write_png(os.path.join(OUT, "loot_fabricator_front_off.png"), frame(0, powered=False))
    print("wrote loot_fabricator_front.png (%d frames) and _off" % FRAMES)
