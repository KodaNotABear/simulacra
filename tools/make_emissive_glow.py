"""Derive emissive glow layers from the machine front textures.

The red LEDs are painted into the same texture as the rest of the machine front, so they
are lit by the world like any other pixel and go dull in the dark. Splitting them into a
separate layer lets the model draw them at full brightness.

For each source texture this writes a companion `*_glow.png` holding only the LED pixels,
with everything else fully transparent, plus a matching `.mcmeta` so an animated front and
its glow layer stay in step frame for frame.

This never modifies the source textures. Re-run it after repainting a front:

    python tools/make_emissive_glow.py
"""
import json
import os
import struct
import sys
import zlib

BLOCKS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                      "assets", "simulacra", "textures", "block")

# Fronts whose LEDs should glow. The _off variants are deliberately absent: an unpowered
# machine has no lit LEDs, and mainframe_controller_side.png is not referenced by any model.
SOURCES = [
    "neural_node_front.png",
    "mainframe_controller_front.png",
    "simulation_chamber_front.png",
    "loot_fabricator_front.png",
]


def read_png(path):
    d = open(path, "rb").read()
    if d[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("%s is not a PNG" % path)
    pos, idat, plte, trns = 8, b"", None, None
    w = h = ct = None
    while pos < len(d):
        ln = struct.unpack(">I", d[pos:pos + 4])[0]
        tag = d[pos + 4:pos + 8]
        chunk = d[pos + 8:pos + 8 + ln]
        if tag == b"IHDR":
            w, h, bd, ct = struct.unpack(">IIBB", chunk[:10])
            if bd != 8:
                raise ValueError("%s must be 8 bit" % path)
        elif tag == b"IDAT":
            idat += chunk
        elif tag == b"PLTE":
            plte = chunk
        elif tag == b"tRNS":
            trns = chunk
        pos += 12 + ln

    raw = zlib.decompress(idat)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    stride = w * channels
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        filt = raw[i]
        i += 1
        line = bytearray(raw[i:i + stride])
        i += stride
        if filt == 1:
            for x in range(channels, stride):
                line[x] = (line[x] + line[x - channels]) & 255
        elif filt == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif filt == 3:
            for x in range(stride):
                left = line[x - channels] if x >= channels else 0
                line[x] = (line[x] + (left + prev[x]) // 2) & 255
        elif filt == 4:
            for x in range(stride):
                a = line[x - channels] if x >= channels else 0
                b = prev[x]
                c = prev[x - channels] if x >= channels else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if pa <= pb and pa <= pc else b if pb <= pc else c
                line[x] = (line[x] + pred) & 255
        prev = line
        row = []
        for x in range(w):
            if ct == 6:
                r, g, b, a = line[x * 4:x * 4 + 4]
            elif ct == 2:
                r, g, b = line[x * 3:x * 3 + 3]
                a = 255
            elif ct == 3:
                idx = line[x]
                r, g, b = plte[idx * 3:idx * 3 + 3]
                a = trns[idx] if trns and idx < len(trns) else 255
            else:
                r = g = b = line[x]
                a = 255
            row.append((r, g, b, a))
        rows.append(row)
    return w, h, rows


def write_rgba(path, px):
    h, w = len(px), len(px[0])
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in px)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n"
                + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw))
                + chunk(b"IEND", b""))


def saturation(pixel):
    hi, lo = max(pixel[:3]), min(pixel[:3])
    return 0.0 if hi == 0 else (hi - lo) / hi


def is_lit(pixel, threshold=0.62):
    """A warm, saturated pixel: red through amber.

    The threshold matters. Create's brass sits around 0.55 saturation and the lamps start
    at roughly 0.62, so this is the line between 'part of the casing' and 'a light'. Do not
    loosen it without re-running the tool and checking the printed pixel counts, or large
    patches of casing will start glowing.
    """
    r, g, b, a = pixel
    return a > 16 and r >= g and r > b and saturation(pixel) >= threshold


def is_lamp(pixel):
    """Warm and saturated, and not the dark brown of shadowed brass.

    Saturation alone cannot split these: a dim red lamp sits at 0.74 and shadowed casing at
    0.71, which is too close to threshold safely. Hue separates them cleanly. A red lamp has
    green and blue near each other (#5c1e18 is 30 and 24); brown casing has green well above
    blue (#3a2411 is 36 and 17). Amber lamps break that rule, but they are far brighter than
    any shadow, so luminance rescues them.
    """
    if not is_lit(pixel):
        return False
    r, g, b, _ = pixel
    luminance = 0.299 * r + 0.587 * g + 0.114 * b
    return g / max(b, 1) < 1.5 or luminance >= 90


def find_light_pixels(w, frames, px):
    """Locate the lamps in a texture, returned as coordinates within a single frame.

    Two signals. Anything that changes colour between animation frames is a lamp doing
    something (pulsing, scanning, blinking), which catches dim states a colour test alone
    would throw away. Anything static that reads as lamp-coloured is an always-on lamp,
    which is how the constantly lit halves of the controller's bars get found. Casing is
    neither animated nor lamp-coloured.
    """
    animated = set()
    for y in range(w):
        for x in range(w):
            if len({px[y + f * w][x][:3] for f in range(frames)}) > 1:
                animated.add((x, y))

    lights = {(x, y) for (x, y) in animated
              if any(is_lamp(px[y + f * w][x]) for f in range(frames))}
    lights |= {(x, y) for y in range(w) for x in range(w)
               if (x, y) not in animated and is_lamp(px[y][x])}
    return lights


def main():
    total = 0
    for name in SOURCES:
        src = os.path.join(BLOCKS, name)
        if not os.path.exists(src):
            print("   %-38s missing, skipped" % name)
            continue
        w, h, px = read_png(src)
        frames = h // w if w and h % w == 0 else 1
        lights = find_light_pixels(w, frames, px)

        out = [[(0, 0, 0, 0)] * w for _ in range(h)]
        count = 0
        for f in range(frames):
            for (x, y) in lights:
                pixel = px[y + f * w][x]
                # A lamp mid-blink drops below the threshold; leave those frames clear so it
                # goes properly dark instead of glowing black over the front.
                if is_lamp(pixel):
                    out[y + f * w][x] = pixel
                    count += 1

        glow_name = name.replace(".png", "_glow.png")
        write_rgba(os.path.join(BLOCKS, glow_name), out)

        # Carry the source's animation across so the layers never drift apart.
        meta_src = src + ".mcmeta"
        note = ""
        if os.path.exists(meta_src):
            with open(meta_src) as f:
                meta = json.load(f)
            with open(os.path.join(BLOCKS, glow_name + ".mcmeta"), "w") as f:
                json.dump(meta, f, indent=4)
                f.write("\n")
            note = " (%d frames)" % frames
        print("   %-38s -> %-38s %d lamps, %d lit px%s" % (
            name, glow_name, len(lights), count, note))
        total += count
    if total == 0:
        print("\nNo lamp pixels matched. Check is_lit() against the palette before shipping.")
        return 1
    return 0


if __name__ == "__main__":
    print("emissive glow layers in %s" % os.path.normpath(BLOCKS))
    sys.exit(main())
