# Create: Simulacra — Art & Audio Direction

What the mod is *about*, and how that decides what every item looks like and every machine sounds
like. [DESIGN.md](DESIGN.md) covers the systems; this covers the feel. When an art or sound decision
is ambiguous, the thesis below is the tiebreaker.

---

## 1. The thesis

**Create is about making work visible. Simulacra is the first machine in your factory whose work you
have to take on faith.**

Everything in Create is legible. You watch the cog turn, the belt move, the press come down. The
fantasy is honest mechanical labour you can follow with your eyes.

Simulacra breaks that on purpose. A Neural Node spins, and *something happens*. There is no belt to
watch, no item to follow. Compute is invisible by definition. At the far end, a box hands you a
zombie's guts and there was never a zombie.

Two things follow, and they drive nearly every art decision:

**a) The front panel is the machine's only honest surface.** These machines cannot show their work
the way a Mechanical Press can, so they have to *tell* you, with lights, bars and readouts. That is
why every machine is a casing frame around a lit panel, and why those panels deserve the bulk of the
texturing effort. A machine whose panel is dark while it is working is lying to the player.

**b) The unease lives in the gap.** A lot of brass is spinning, and meat comes out. The mod should
never be *scary*, but it should be quietly unsettling that this works so well. It already names its
top model grade **Self-Aware** and calls the bound mob the **Subject**. The art should cash those.

**Tone: warm outside, cold inside.** The chassis is pure Create: brass, andesite, proud industrial
equipment. All the disquiet is *behind the glass*. Panels and items get less mechanical and more
organic as you climb the tiers, and by Self-Aware the thing in the box is clearly no longer a
spreadsheet. The mod has to sit next to a Mechanical Mixer without looking like a different game.

---

## 2. The split that the art has to sell

The loop has two halves running on different resources. This is the single most important thing for
a player to understand, and the art carries most of that job.

| | **Thinking half** | **Physical half** |
|---|---|---|
| Machines | Neural Node, Mainframe Controller, Data Cable, Simulation Chamber | Loot Fabricator |
| Runs on | compute (CU/t), pooled by the array | rotation, straight off a shaft |
| Decides | *what* you are able to simulate | *how fast* you stamp the results out |
| Reads as | cold, still, humming | warm, mechanical, striking |

A **Prediction** is the token that crosses between them: fungible, worth one roll of its subject's
loot table, meaningless until spent. The Chamber prints them; the Fabricator spends them at a price
derived from how often that mob really drops the thing you asked for.

So: **the Chamber should never look like it is stamping, and the Fabricator should never look like
it is thinking.** If the two read as the same kind of device, the split that makes the mod
interesting is invisible.

---

## 3. The colour language

Four channels, each with exactly one meaning. Once this holds, a player can diagnose a base at a
glance.

| Channel | Means | Where it appears |
|---|---|---|
| **Brass / warm gold** | honest mechanical work | every casing, frame, shaft — Create's own voice |
| **Cold cyan / white** | compute; the machine thinking | array readouts, live cables, training, healthy panels |
| **Rose / magenta** | a captured life-pattern; the Subject | Predictions, bound matrices, catalyst, pristine inlay |
| **Violet + hot red** | the pattern breaking down | corrupted imprints, stalls, faults |

Two rules to hold strictly:

- **Rose only ever means "something that was alive is in here."** A blank matrix has none. A bound
  one does. A Prediction is rose by definition; it is condensed mob. Binding state and Prediction
  subject then read from the icon alone, and the Resonant Catalyst's rose *means* something.
- **Red is reserved for trouble.** The machines lean amber-and-red for healthy states, which spends
  the one colour that should mean "go look at that one". Moving healthy panels to cyan frees it.

---

## 4. The item chain, as a story

Two strands that meet at the Chamber and part again at the Fabricator: **a mind being filled** and
**a medium being printed on**. The chain narrates *empty → filled → understood → aware*, and
alongside it *blank → printed → spent*.

### Data Cable — *the nerve*
Fine fibre bundled in a brass sleeve, visible at the cut ends. It carries pattern, not power.
**Avoid:** redstone red, and the chunky power-cable silhouette.

### Blank Data Matrix — *the empty mind*
A brass frame around a dark void holding a regular, **unlit** lattice. The emptiness is the point.
**Avoid:** any rose, any suggestion of contents.

### Incomplete Data Matrix — *under construction*
The frame exists, the lattice is half-strung, wire ends loose. It must read as *being built*, not as
a dimmer blank matrix.

### Trained model grades — *Coarse → Tuned → Deep → Self-Aware*
The mod's best storytelling if the icon is ever tiered:
- **Coarse** — scattered rose noise, points with no relationship.
- **Tuned** — the points cluster; structure emerging.
- **Deep** — organised, symmetric, dense. It clearly describes *something*.
- **Self-Aware** — one coherent shape with a focus at its centre. **The only tier that looks back at
  you.** Nothing before it should have anything eye-like.

### Prediction — *condensed mob, not yet a thing*
The most abstract item in the mod, carrying the most explanatory weight. It should read as
**potential rather than substance**: rose, luminous, weightless, obviously *not* an object you could
hold the way you hold an ingot. Its subject should be legible at a glance, because players hold
stacks of several kinds at once.
**Avoid:** looking like a finished material. The moment a Prediction reads as loot, the reason the
Fabricator exists stops being obvious.

### Crude / Refined / Pristine Imprint Blank — *the medium*
Rough pressed pulp, then machine-finished ceramic, then pearl with a rose inlay. Each tier needs a
**hue shift and a structural mark**, not a brightness change. Measure it: the `minecraft-pixel-art`
skill has a script that scores how distinguishable a set is, and the current crude/refined pair
fails it.

### Resonant Catalyst — *left over, not made*
Rose crystal with a dark core, still faintly resonating. The **one item that should not look
manufactured**. Everything else in the mod is built; this is residue.

### Corrupted Imprint — *a failed photocopy*
The substrate silhouette, ruined: scan-line tearing, colour separation, the ghost of the intended
shape. **Keep silhouette continuity with the blanks.** It must read as *this process failed*, not as
a cursed artefact from somewhere else. The horror is that it is the same object, wrong.

---

## 5. What each process sounds like

The goal: **stand in your base with your eyes closed and know it is healthy.** Three layers, and
each machine occupies exactly one.

| Layer | Sound | Means |
|---|---|---|
| **Bed** | the array hum | power is flowing, compute exists |
| **Pulse** | chamber jobs and fabricator strikes | work is completing |
| **Exception** | stall | something needs you |

**Neural Node — labour.** Create's own kinetic whir, free with the kinetic block. The *only*
mechanical sound in the thinking half, deliberately: the nodes audibly strain while everything
downstream is silent thought. Never add a second loop here. Players build these in racks.

**Mainframe Controller — attention.** A held tone with no rhythm, felt more than heard, at low
volume because it runs for hours. Spin-up and wind-down are announced, so an array stopped by a
broken shaft does not sound like one that is merely idle.

**Simulation Chamber — effort, then a soft impression.** Training is iterative work converging on a
result and should sound like it. A finished job is the blank impressed and cured: the press sound
pitched well down, a heavier, slower relative of the Fabricator's strike. The two machines are
cousins, and pitch is what separates them.

**Loot Fabricator — fabrication.** The sharpest, most satisfying sound in the mod, because it is the
moment the player gets the thing they asked for. Charge, snap, release.

**Stall — absence.** The real signal is the working sound stopping; the cue is one soft note, quiet
and never looped. An alarm is welcome once and resented by the twentieth chamber.

> **Where this stands:** the milestone one-shots go through Create's `AllSoundEvents` directly,
> because Create's API handles layered playback and its subtitles honestly describe a press-like
> machine pressing. The continuous and diagnostic cues are `simulacra:*` events aliased onto Create
> and vanilla sounds, so they caption correctly and a pack, or a later bespoke `.ogg`, can retarget
> them without touching code.

---

## 6. The texture slots waiting for art

Every machine is the same construction: a Create casing shell with a lit panel on the working face,
plus a separate `*_glow.png` emissive layer so the lit pixels stay bright at night. Drop art in by
replacing a PNG.

| Machine | Panel | Glow | Wants |
|---|---|---|---|
| Neural Node | `neural_node_front[_off].png` | — | driven vs stopped |
| Mainframe Controller | `mainframe_controller_front[_off].png` | `_glow` | array producing vs idle |
| Simulation Chamber | `simulation_chamber_front[_off].png` | `_glow` | fed vs idle |
| Loot Fabricator | `loot_fabricator_front[_off].png` | `_glow` | able to stamp vs idle |

Animated panels are 16×128 (8 frames of 16×16) with a `.mcmeta`; the glow layer's `.mcmeta` must
match its base or the two drift frame by frame. The glow layers are derived from the fronts by a
local generator script, so repaint the front and re-derive rather than painting the glow twice. The
generators are gitignored, since only their output belongs in the repo.

Per §3, healthy states want cyan for the thinking half and rose for anything holding a life-pattern,
leaving red to mean only trouble.

**Still unsplit:** the Chamber signals training and simulating with one lit/unlit boolean, so its
two most different processes look identical. Splitting that is a blockstate change, not an art
change, and it doubles the Chamber's panel count. Decide before painting it.

---

## 7. The Loot Fabricator's Ponder scene

Built, as two scenes. It is the only block with a screen, the only one with two modes, and the only
place Predictions are spent, so it needed both.

### What it teaches, in priority order

1. **Predictions arrive from a Simulation Chamber and drops leave.** A funnel in, a funnel out. This
   is the build the machine exists for and the thing a player is most likely to get wrong.
2. **It runs on rotation, not compute.** The most confusing thing about the machine is that
   everything upstream needs an array and it does not. The shaft is visibly what makes it go.
3. **Two modes.** Left alone it rolls the subject's table. Pick a drop on its screen and it makes
   only that, priced by how rarely the mob drops it.

The speed rule and the one-stack buffer are discoverable and do not earn a beat.

### Structure

`loot_fabricator()` in `tools/make_ponder_structures.py`. A 9×4×9 plate, everything on one row so
the camera reads left to right: a shaft running in from the left into the Fabricator's back face,
the Fabricator centre facing west, an inbound belt feeding a funnel with Predictions, an outbound
belt under a second funnel carrying drops, and a Simulation Chamber at the far left so the
Prediction comes from somewhere.

Belt direction is the trap the chamber scene already hit: insertion is refused when the side
argument equals the belt's direction of travel, so items have to be handed in from behind.

### Beats

Three texts each, matching every other scene in the mod. The lang sync numbers keys by execution
order, so re-run `tools/sync_ponder_lang.py` after editing a scene.

**Scene 1 — "Turning Predictions into drops"**
1. *"The Loot Fabricator runs on rotation, not compute"*
2. *"Feed it Predictions. A funnel in, a funnel out."*
3. *"Left alone it rolls the loot table, one Prediction per roll"*

**Scene 2 — "Choosing what to make"**
1. *"Open it to see every drop the subject has"*
2. *"Pick one and it makes only that"*
3. *"Rarer drops cost more Predictions. Choosing only makes it predictable."*

Beat 3 of scene 2 is the one that matters. Pricing is what keeps the machine honest, and without it
a player assumes picking a rare drop is strictly better.

### A limitation worked around

Ponder cannot open a GUI, so scene 2 teaches a screen without showing it. The trick is contrast: run
the same setup twice and let the output change from mixed drops to a single item. Show the
difference, not the interface.
