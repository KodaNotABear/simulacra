# Create: Simulacra — Art & Audio Direction

What the mod is *about*, and how that decides what every item looks like and every machine sounds
like. [DESIGN.md](DESIGN.md) covers the systems; this covers the feel. When an art or sound decision
is ambiguous, the thesis below is the tiebreaker.

---

## 1. The thesis

**Create is about making work visible. Cognition is the first machine in your factory whose work you
have to take on faith.**

Everything in Create is legible. You watch the cog turn, the belt move, the press come down. The
fantasy is honest mechanical labour you can follow with your eyes.

Cognition breaks that on purpose. A Neural Node spins, and *something happens*. There is no belt to
watch, no item to follow. Compute is invisible by definition. At the far end, a box hands you a
zombie's guts and there was never a zombie.

Two things follow, and they drive nearly every art decision:

**a) The front panel is the machine's only honest surface.** A Cognition machine cannot show you its
work the way a Mechanical Press can, so it has to *tell* you — with lights, bars, readouts. That is
why every machine is built as a casing frame around a lit panel, and it is why those panels deserve
the bulk of the texturing effort. Everything the player learns about a machine's inner life comes
through that window. A machine whose panel is dark while it is working is lying to the player.

**b) The unease lives in the gap.** A lot of brass is spinning, and meat comes out. The mod should
never be *scary* — it is a Create add-on, not a horror mod — but it should be quietly unsettling that
this works so well. The mod already names its top model grade **Self-Aware** and calls the bound mob
the **Subject**. Those are promises the art should cash.

**Tone: warm outside, cold inside.** The chassis is pure Create — brass, andesite, proud industrial
equipment, entirely comfortable, never sinister. All the disquiet is *behind the glass*: the panels
and the items get less mechanical and more organic as you climb the tiers, and by Self-Aware the
thing in the box is clearly no longer just a spreadsheet. This keeps the mod visually native to
Create, which matters — it has to sit next to a Mechanical Mixer without looking like a mod from a
different game.

---

## 2. The split that the art has to sell

The loop has two halves, and they run on different resources. This is the single most important thing
for a player to understand, and the art carries most of that job.

| | **Thinking half** | **Physical half** |
|---|---|---|
| Machines | Neural Node, Mainframe Controller, Data Cable, Simulation Chamber | Loot Fabricator |
| Runs on | compute (CU/t), pooled by the array | rotation, straight off a shaft |
| Decides | *what* you are able to simulate | *how fast* you stamp the results out |
| Reads as | cold, still, humming | warm, mechanical, striking |

A **Prediction** is the token that crosses between them: fungible, worth one roll of its subject's
loot table, meaningless until spent. The Chamber prints them; the Fabricator spends them at a price
derived from how often that mob really drops the thing you asked for.

So: **the Chamber should never look like it is stamping, and the Fabricator should never look like it
is thinking.** If the two machines read as the same kind of device, the split that makes the mod
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

Two rules worth holding strictly:

- **Rose only ever means "something that was alive is in here."** A blank matrix has none. A bound one
  does. A Prediction is rose by definition — it is condensed mob. This makes binding state and
  Prediction subjects readable from the icon alone, and it makes the Resonant Catalyst's rose *mean*
  something rather than just being a pretty colour.
- **Red is reserved for trouble.** The machines currently lean amber-and-red for healthy states, which
  spends the one colour that should mean "go look at that one". Moving healthy panels to cyan frees it.

---

## 4. The item chain, as a story

Two strands that meet at the Chamber and part again at the Fabricator: **a mind being filled** and **a
medium being printed on**. In order the chain should narrate *empty → filled → understood → aware*,
and alongside it *blank → printed → spent*.

### Data Cable — *the nerve*
Fine fibre bundled in a brass sleeve, visible at the cut ends. It carries pattern, not power.
**Avoid:** redstone red, and the chunky power-cable silhouette.

### Blank Data Matrix — *the empty mind*
A brass frame around a dark void holding a regular, **unlit** lattice. The emptiness is the point of
the item. **Avoid:** any rose, any suggestion of contents.

### Incomplete Data Matrix — *under construction*
The frame exists, the lattice is half-strung, wire ends loose. Must read as *being built*, distinctly
from "finished but empty" — not merely a dimmer blank matrix.

### Trained model grades — *Coarse → Tuned → Deep → Self-Aware*
The mod's best piece of storytelling if the icon is ever tiered:
- **Coarse** — scattered rose noise, points with no relationship.
- **Tuned** — the points cluster; structure emerging.
- **Deep** — organised, symmetric, dense. It clearly describes *something*.
- **Self-Aware** — one coherent shape with a focus at its centre. **The only tier that looks back at
  you.** Nothing before it should have anything eye-like; that is what makes the last step land.

### Prediction — *condensed mob, not yet a thing*
The newest and most abstract item in the mod, and the one carrying the most explanatory weight. It
should read as **potential rather than substance**: rose, luminous, weightless, obviously *not* an
object you could hold the way you hold an ingot. Its subject should be legible at a glance, because
players will hold stacks of several kinds at once.
**Avoid:** looking like a finished material. The moment a Prediction reads as loot, the reason the
Fabricator exists stops being obvious.

### Crude / Refined / Pristine Imprint Blank — *the medium*
Rough pressed pulp, then machine-finished ceramic, then pearl with a rose inlay. Each tier needs a
**hue shift and a structural mark**, not just a brightness change — measured, not eyeballed; the
`minecraft-pixel-art` skill has a script that scores how distinguishable a set is, and the current
crude/refined pair fails it.

### Resonant Catalyst — *left over, not made*
Rose crystal with a dark core, still faintly resonating. The **one item that should not look
manufactured** — everything else in the mod is built; this is residue.

### Corrupted Imprint — *a failed photocopy*
The substrate silhouette, ruined: scan-line tearing, colour separation, the ghost of the intended
shape. **Keep silhouette continuity with the blanks** — it must read as *this process failed*, not as
a cursed artefact from somewhere else. The horror is that it is the same object, wrong.

---

## 5. What each process sounds like

The goal: **stand in your base with your eyes closed and know it is healthy.** Three layers, and each
machine occupies exactly one.

| Layer | Sound | Means |
|---|---|---|
| **Bed** | the array hum | power is flowing, compute exists |
| **Pulse** | chamber jobs and fabricator strikes | work is completing |
| **Exception** | stall | something needs you |

**Neural Node — labour.** Create's own kinetic whir, which it gets free as a kinetic block. The
*only* mechanical sound in the thinking half, and deliberately so: the nodes audibly strain while
everything downstream is silent thought. Never add a second loop here — players build these in racks.

**Mainframe Controller — attention.** A held tone with no rhythm, felt more than heard, at low volume
because it runs for hours. Spin-up and wind-down are announced, because an array that stopped because
a shaft broke should not be indistinguishable from one that is merely idle.

**Simulation Chamber — effort, then a soft impression.** Training is iterative work converging on a
result and should sound like it. A finished job is the blank being impressed and cured: the press
sound pitched well down, so it reads as a heavier, slower relative of the Fabricator's strike. That
relationship is deliberate — the two machines are cousins, and pitch is what separates them.

**Loot Fabricator — fabrication.** The sharpest, most satisfying sound in the mod, because it is the
moment the player finally gets the thing they asked for. Charge, snap, release. It should be the
sound you build a base around hearing.

**Stall — absence.** The real signal is the working sound stopping; the cue is one soft note, quiet
and never looped. An alarm is welcome once and resented by the twentieth chamber.

> **Where this stands:** the milestone one-shots go through Create's `AllSoundEvents` directly, because
> Create's API handles its layered playback and its subtitles honestly describe a press-like machine
> pressing. The continuous and diagnostic cues are registered as `simulacra:*` events aliased onto
> Create and vanilla sounds, so they caption correctly and a resource pack — or a later bespoke
> `.ogg` — can retarget them without touching code.

---

## 6. The texture slots waiting for art

Every machine follows the same construction: a Create casing shell with a lit panel on the working
face, plus a separate `*_glow.png` emissive layer so the lit pixels stay bright at night. Drop art in
by replacing a PNG.

| Machine | Panel | Glow | Wants |
|---|---|---|---|
| Neural Node | `neural_node_front[_off].png` | — | driven vs stopped |
| Mainframe Controller | `mainframe_controller_front[_off].png` | `_glow` | array producing vs idle |
| Simulation Chamber | `simulation_chamber_front[_off].png` | `_glow` | fed vs idle |
| Loot Fabricator | `loot_fabricator_front[_off].png` | `_glow` | able to stamp vs idle |

Animated panels are 16×128 (8 frames of 16×16) with a `.mcmeta`; the glow layer's `.mcmeta` must match
its base or the two drift apart frame by frame. `tools/make_emissive_glow.py` derives the glow layers
from the fronts, so repaint the front and re-run it rather than painting the glow twice.

Per §3, healthy states want cyan for the thinking half and rose for anything holding a life-pattern,
leaving red to mean only trouble.

**Still unsplit:** the Chamber signals training and simulating with one lit/unlit boolean, so its two
most different processes look identical. Splitting that is a blockstate change, not an art change, and
it would double the Chamber's panel count — worth deciding before painting it.

---

## 7. The Loot Fabricator's Ponder scene

The one machine still without a scene, and the one that most needs one: it is the only block with a
screen, the only one with two modes, and the only place Predictions are spent.

### What it has to teach, in priority order

1. **Predictions arrive from a Simulation Chamber and drops leave.** A funnel in, a funnel out. This
   is the build the machine exists for and the thing a player is most likely to get wrong.
2. **It runs on rotation, not compute.** The single most confusing thing about this machine is that
   everything upstream of it needs an array and it does not. The shaft has to be visibly the thing
   making it go.
3. **Two modes.** Left alone it rolls the subject's table — one Prediction, one roll, whatever comes
   out. Pick a drop on its screen and it makes only that, priced by how rarely the mob drops it.

Everything else — the press curve, the one-stack buffer — is discoverable and does not earn a beat.

### Suggested structure

Add a `loot_fabricator()` to `tools/make_ponder_structures.py` alongside the others. A 9×4×9 plate,
everything on one row so the camera reads it left to right:

- **Shaft** running in from the left along X into the Fabricator's back face.
- **Fabricator** centre, facing west so its screen faces the camera.
- **Inbound belt** feeding a funnel on the back/side, carrying Predictions.
- **Outbound belt** leaving the far side under a second funnel, carrying finished drops.
- Optionally a **Simulation Chamber** at the far left of the inbound belt, so the Prediction visibly
  comes from somewhere rather than appearing on a belt for no reason.

Belt direction is the trap the chamber scene already hit: insertion is refused when the side argument
equals the belt's direction of travel, so items have to be handed in from behind.

### Suggested beats

Two scenes, matching how the chamber splits into training and jobs. Keep to three texts each — that
is the shape of every other scene in the mod, and the lang sync numbers keys by execution order.

**Scene 1 — "Spending Predictions"**
1. Show the machine and spin the shaft. *"The Loot Fabricator turns Predictions into real drops, and
   runs on rotation rather than compute."*
2. Run the inbound belt, flap the funnel, let Predictions land. *"Feed it Predictions from a
   Simulation Chamber — a funnel in, a funnel out."*
3. Run the outbound belt with assorted zombie drops. *"Left alone it rolls the subject's loot table:
   one Prediction per roll, and you keep whatever comes out."*

**Scene 2 — "Choosing a drop"**
1. Point at the machine's face. *"Open it to see everything its subject can drop."*
2. Show only one item type leaving on the outbound belt. *"Pick one and it makes only that."*
3. Point at the outbound belt. *"Rarer drops cost more Predictions, so choosing never beats simply
   rolling — it only makes the result predictable."*

Beat 3 is the one worth getting right. The pricing is the mechanic that keeps the machine honest, and
without it a player reasonably assumes picking a rare drop is strictly better.

### A limitation to plan around

Ponder cannot open a GUI, so scene 2 has to teach a screen without showing it. The workable trick is
contrast: run the same setup twice and let the output change from mixed drops to a single item. Show
the difference, not the interface.
