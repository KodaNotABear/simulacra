"""Placeholder icon for the Prediction item.

It was a straight copy of the Crude Imprint Blank, which made two unrelated items look the
same in an inventory. This is still placeholder art, but it has its own silhouette: a
faceted token rather than a flat square, because a Prediction is stored value rather than
a sheet you print onto.

    python tools/make_prediction_item.py
"""
import os
import struct
import zlib

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "simulacra", "textures", "item")

RIM = (120, 84, 52)
RIM_LIGHT = (168, 124, 76)
BODY = (58, 50, 42)
BODY_LIGHT = (78, 68, 56)
CORE = (240, 196, 80)
CORE_HOT = (255, 226, 140)


def build():
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    # A diamond token, mirrored on both axes so it reads as a deliberate object.
    for y in range(16):
        for x in range(16):
            dx = abs(x - 7.5)
            dy = abs(y - 7.5)
            d = dx + dy
            if d > 7.5:
                continue
            if d > 6.0:
                c = RIM_LIGHT if (y < 8) else RIM
            elif d > 5.0:
                c = RIM
            elif d > 2.0:
                c = BODY_LIGHT if (x + y) % 2 == 0 else BODY
            elif d > 1.0:
                c = CORE
            else:
                c = CORE_HOT
            px[y][x] = (*c, 255)
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
    write_png(os.path.join(OUT, "prediction.png"), build())
    print("wrote prediction.png")
