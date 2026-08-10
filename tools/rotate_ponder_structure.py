"""Rotate a saved structure NBT about the Y axis, in place.

Ponder views every scene from a fixed camera, so a structure built facing the wrong way
shows the back of the machines. The camera cannot be re-aimed without an animated swing
(rotateCameraY eases toward its target), so the fix is to turn the structure instead.

This does a full typed round-trip of the NBT, so block entity data on the machines
survives untouched. Positions are mirrored and directional block properties are remapped.

    python tools/rotate_ponder_structure.py <file.nbt> [--degrees 180]

Rotations are clockwise looking down, and only 90/180/270 are accepted.
"""
import argparse
import gzip
import os
import struct

# --- typed NBT round-trip -------------------------------------------------
# Every value is carried as (tag_id, payload) so re-serialising is exact.

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 7, 8, 9, 10, 11, 12


class Reader:
    def __init__(self, data):
        self.d = data
        self.i = 0

    def raw(self, n):
        v = self.d[self.i:self.i + n]
        self.i += n
        return v

    def u1(self):
        return self.raw(1)[0]

    def string(self):
        n = struct.unpack(">H", self.raw(2))[0]
        return self.raw(n).decode("utf-8")

    def value(self, t):
        if t == TAG_BYTE:
            return struct.unpack(">b", self.raw(1))[0]
        if t == TAG_SHORT:
            return struct.unpack(">h", self.raw(2))[0]
        if t == TAG_INT:
            return struct.unpack(">i", self.raw(4))[0]
        if t == TAG_LONG:
            return struct.unpack(">q", self.raw(8))[0]
        if t == TAG_FLOAT:
            return struct.unpack(">f", self.raw(4))[0]
        if t == TAG_DOUBLE:
            return struct.unpack(">d", self.raw(8))[0]
        if t == TAG_BYTE_ARRAY:
            n = struct.unpack(">i", self.raw(4))[0]
            return list(self.raw(n))
        if t == TAG_STRING:
            return self.string()
        if t == TAG_LIST:
            et = self.u1()
            n = struct.unpack(">i", self.raw(4))[0]
            return (et, [self.value(et) for _ in range(n)])
        if t == TAG_COMPOUND:
            out = []
            while True:
                tt = self.u1()
                if tt == TAG_END:
                    return out
                out.append((tt, self.string(), self.value(tt)))
        if t == TAG_INT_ARRAY:
            n = struct.unpack(">i", self.raw(4))[0]
            return [struct.unpack(">i", self.raw(4))[0] for _ in range(n)]
        if t == TAG_LONG_ARRAY:
            n = struct.unpack(">i", self.raw(4))[0]
            return [struct.unpack(">q", self.raw(8))[0] for _ in range(n)]
        raise ValueError("unknown tag %d" % t)


def write_value(t, v, out):
    if t == TAG_BYTE:
        out.append(struct.pack(">b", v))
    elif t == TAG_SHORT:
        out.append(struct.pack(">h", v))
    elif t == TAG_INT:
        out.append(struct.pack(">i", v))
    elif t == TAG_LONG:
        out.append(struct.pack(">q", v))
    elif t == TAG_FLOAT:
        out.append(struct.pack(">f", v))
    elif t == TAG_DOUBLE:
        out.append(struct.pack(">d", v))
    elif t == TAG_BYTE_ARRAY:
        out.append(struct.pack(">i", len(v)))
        out.append(bytes(v))
    elif t == TAG_STRING:
        b = v.encode("utf-8")
        out.append(struct.pack(">H", len(b)))
        out.append(b)
    elif t == TAG_LIST:
        et, items = v
        out.append(bytes([et]))
        out.append(struct.pack(">i", len(items)))
        for it in items:
            write_value(et, it, out)
    elif t == TAG_COMPOUND:
        for tt, name, val in v:
            out.append(bytes([tt]))
            b = name.encode("utf-8")
            out.append(struct.pack(">H", len(b)))
            out.append(b)
            write_value(tt, val, out)
        out.append(b"\x00")
    elif t == TAG_INT_ARRAY:
        out.append(struct.pack(">i", len(v)))
        for x in v:
            out.append(struct.pack(">i", x))
    elif t == TAG_LONG_ARRAY:
        out.append(struct.pack(">i", len(v)))
        for x in v:
            out.append(struct.pack(">q", x))
    else:
        raise ValueError("unknown tag %d" % t)


def get(compound, key):
    for t, name, v in compound:
        if name == key:
            return t, v
    return None, None


def put(compound, key, value):
    for idx, (t, name, v) in enumerate(compound):
        if name == key:
            compound[idx] = (t, name, value)
            return


# --- rotation -------------------------------------------------------------

CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
AXIS_CW = {"x": "z", "z": "x", "y": "y"}


def turn(name, quarters):
    for _ in range(quarters):
        name = CW[name]
    return name


def rotate_props(props, quarters):
    """Remap directional blockstate properties: facing, axis, and the boolean
    connection flags multipart blocks like the Data Cable use."""
    if props is None:
        return None

    existing = {name: (t, v) for t, name, v in props}
    # A flag on side d ends up on side turn(d): collect first, then reassign.
    rotated_conn = {}
    for d in CW:
        if d in existing:
            rotated_conn[turn(d, quarters)] = existing[d]

    out = []
    for t, name, v in props:
        if name == "facing" and v in CW:
            out.append((t, name, turn(v, quarters)))
        elif name == "axis" and v in AXIS_CW:
            a = v
            for _ in range(quarters):
                a = AXIS_CW[a]
            out.append((t, name, a))
        elif name in rotated_conn:
            nt, nv = rotated_conn[name]
            out.append((nt, name, nv))
        else:
            out.append((t, name, v))
    return out


def rotate_pos(x, z, sx, sz, quarters):
    """Clockwise looking down. Returns the new (x, z)."""
    if quarters == 1:
        return sz - 1 - z, x
    if quarters == 2:
        return sx - 1 - x, sz - 1 - z
    if quarters == 3:
        return z, sx - 1 - x
    return x, z


def rotate_file(path, degrees):
    if degrees % 90 != 0:
        raise SystemExit("degrees must be a multiple of 90")
    quarters = (degrees // 90) % 4
    if quarters == 0:
        print("nothing to do")
        return

    raw = gzip.open(path, "rb").read()
    r = Reader(raw)
    assert r.u1() == TAG_COMPOUND
    root_name = r.string()
    root = r.value(TAG_COMPOUND)

    _, size = get(root, "size")
    sx, sy, sz = size[1]

    _, palette = get(root, "palette")
    for entry in palette[1]:
        _, props = get(entry, "Properties")
        if props is not None:
            put(entry, "Properties", rotate_props(props, quarters))

    _, blocks = get(root, "blocks")
    for b in blocks[1]:
        _, pos = get(b, "pos")
        x, y, z = pos[1]
        nx, nz = rotate_pos(x, z, sx, sz, quarters)
        put(b, "pos", (TAG_INT, [nx, y, nz]))

    if quarters % 2 == 1:
        put(root, "size", (TAG_INT, [sz, sy, sx]))

    out = [bytes([TAG_COMPOUND])]
    nb = root_name.encode("utf-8")
    out.append(struct.pack(">H", len(nb)))
    out.append(nb)
    write_value(TAG_COMPOUND, root, out)
    with gzip.open(path, "wb") as f:
        f.write(b"".join(out))
    print("rotated %s by %d degrees" % (os.path.basename(path), degrees))


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("files", nargs="+")
    ap.add_argument("--degrees", type=int, default=180)
    args = ap.parse_args()
    for f in args.files:
        rotate_file(f, args.degrees)
