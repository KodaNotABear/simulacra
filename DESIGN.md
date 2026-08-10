# Create: Simulacra (Design)

This is the living design doc. It captures the concept, the rules that keep it coherent, and the
planned build order. This doc is a guideline, not gospel: the shipped mod deliberately diverges where playtesting led elsewhere. CHANGELOG.md describes what actually exists.

## 1. The pitch

A Create add-on where the new resource is **compute**, produced by a physical "neural network"
datacenter that consumes surplus rotational power (SU). You spend compute to **train models of mobs**
(from data you gather) and to **run simulations** of those models that output the mob's loot.

The headline promise: **simulation spawns no entities.** You pay the entity cost once, while gathering
training data; after that the loot comes from pure computation with no mob ticking, no pathfinding, and
no spawn cap consumed. It is the server-friendly mob/loot farm.

## 2. Why it fits Create (and doesn't overshadow it)

Create's progression is one axis: *more power* (waterwheel → windmill → steam → addon reactors). Once
you have a surplus of SU, Create gives you little to spend it on. Simulacra adds a second axis: turn
surplus SU into **intelligence**. The datacenter is a large, hungry SU sink, which finally gives an
over-built power setup a purpose.

Resource generation is not treated as a problem to avoid — "many ways to do the same thing" is part of
why Create packs are fun. The balance levers are the compute cost, the substrate cost, and the training
investment, not an artificial ban on producing matter. The natural high-value target is **the
un-farmable**: bosses, dangerous or non-spawnable mobs, cross-dimension mobs. Common mobs are a
convenience tier; nobody spends a datacenter farming rotten flesh.

## 2b. What actually happens (fiction, and what it means for art)

The premise in one line: **you cannot have the drop without the death, unless you can predict the
death precisely enough that the prediction is as good as the event.** Every machine is one step in
turning a single witnessed death into arbitrarily many predicted ones. Art, sound and animation
should all be readable against that sentence.

**Blank Data Matrix - the instrument.** Brass frame, quartz lattice, tubes. Held in the off hand it
witnesses a kill and records the pattern: what the creature was, how it came apart, what fell. It
locks to one species because the lattice takes a set once and can only be refined afterwards. Three
states worth showing: empty and unwritten, bound but unresolved, trained and ordered.

**Cognition Array - the thinking.** Raw observation is noise; the nodes grind it into a model that
can be run. This is why compute is a rate and not a battery: it is ongoing mechanical reasoning, not
stored charge. Nothing here is electronic.

**Imprint Blank - the receiving medium.** Soft, unfired, fine grained; a pressed tablet, not a lump.
Grade is grain. Crude is coarse and takes one clean impression. Rose quartz makes it finer. Amethyst
makes it resonant enough to hold a boss-grade pattern.

**Simulation Chamber - the impression.** It runs the model against the blank, simulating the death
over and over, and each resolved outcome is pressed into the material and cured. The blank goes in
soft and comes out hard, scored and divided into as many Predictions as its grain could hold. Soft in,
fired out, is the strongest visual beat in the chain.

**Prediction - a die, not an item.** A hardened chip carrying one recorded outcome for one species.
It knows the shape of a possible drop without having collapsed into one.

This is also where the derived pricing stops being a spreadsheet and becomes fiction. A single
impression rarely contains a rare outcome, so stamping a wither skeleton skull means consulting
enough recorded deaths that the pattern is present at all - roughly forty. Rotten flesh needs one.
The price *is* the rarity, expressed as how many deaths you had to look at.

**Loot Fabricator - the collapse.** A press. It reads across the impressions, finds the target
pattern, and stamps matter into that shape. Rotation drives the die; spent Predictions are used-up
impressions.

**Corrupted Imprint - a smeared press.** The model was too coarse and the impression did not take.
It crushes back to pulp because it never became anything.

### Sprite direction

One material in three states, so the chain reads at a glance: soft tablet, fired marked chip,
finished item.

| Sprite | Reads as | Palette | Notes |
| --- | --- | --- | --- |
| Crude Imprint Blank | Unfired pressed tablet, coarse, matte, soft-edged | Grey-brown clay, `#6b5f4e` to `#8a7c66` | Visible grain. Slightly irregular edge; it has not been fired yet |
| Refined Imprint Blank | Same tablet, finer body | Warmer, pink-flecked, `#8a7466` with rose quartz specks | Cleaner edge than crude, faint sheen |
| Pristine Imprint Blank | Same tablet, resonant | Violet sheen over pale clay, `#9a8fa6` | Faint internal glow; the material that can hold a boss pattern |
| Prediction | A fired chip broken from a blank | Vitrified, glassy, sharp-edged, amber core `#f0c450` | Same silhouette family as the blanks but hard and marked. Should look like a fragment, since one blank yields several |
| Corrupted Imprint | The same wafer, ruined | Blank palette, desaturated, cracked | Smeared impression, a crack or two. Obviously scrap |
| Blank Data Matrix | An instrument, not a consumable | Brass frame `#9e6947`, dark lattice, quartz | Unwritten: lattice dark. Bound: tinted, unresolved. Trained: ordered, steady glow |
| Resonant Catalyst | Something that came out of a boss | Deep violet, faceted, internally lit | The only item that should look like it is humming |
| Data Cable | Coil of cable, ends showing | Warm dark brown body, orange connectors | Value range near Create's belt, not black |

### Sound

All Create or vanilla, no custom foley.

| Moment | Sound | Why |
| --- | --- | --- |
| Matrix binds on first kill | `AllSoundEvents.CONFIRM_2`, pitch 1.4 | Permanently commits the matrix to a species; it was silent before, which is how people bound the wrong mob without noticing |
| Training completes | `AllSoundEvents.CONFIRM` | The payoff of a long wait, so it gets a chime rather than another machine noise |
| Simulation job finishes | `MECHANICAL_PRESS_ACTIVATION`, pitch 0.65 | The blank being impressed and cured: a heavier, slower relative of the Fabricator's strike |
| Fabricator stamps an item | `MECHANICAL_PRESS_ACTIVATION`, pitch 1.15 | The collapse into a real object; the sharpest sound in the mod |

Candidates left unused, for later: `COGS` for an array idling, `CRAFTER_CLICK` or `CONTROLLER_CLICK`
for the controller reallocating, `DENY` for a rejected target, `STEAM` for a chamber venting.

### Animation

The Fabricator drives Create's `MECHANICAL_PRESS_HEAD` down on each fabrication, which is what
visually justifies its shaft. If the machines get custom models later: the node wants an internal reel
turning with the shaft, and the chamber wants the matrix visible behind a window with a sweep during
training and a shutter on each job.

## 3. Core systems

### Compute
- A **rate**, not a stored battery. Measured in compute units per tick (`cu/t`).
- Produced by a **Cognition Array**: Neural Nodes connected to a Mainframe Controller and driven by
  rotation. A lone node produces nothing; output scales with the number of driven nodes and their speed,
  and larger arrays are more efficient (Steam-Engine-style). Stress is drawn per node, so rotation does
  real mechanical work rather than instantly becoming a separate energy number.
- Pooled by the **Mainframe Controller** into one cluster budget.
- **Allocated** across jobs by a scheduler. The player is a datacenter operator triaging jobs. This
  allocation decision is the verb that makes Simulacra more than "FE with a new color."

### Jobs (what compute is spent on)
- **Training**: convert raw data gathered from a mob into a higher-tier, more accurate model.
- **Simulation**: run a trained model continuously to produce loot.
- (Later) **Optimization**: persistent background jobs that modestly buff your existing Create setup, so
  idle compute always has somewhere to go.

### Substrate (the consumable that keeps a factory in the loop)
A running simulation consumes one **Imprint Blank** per output cycle and impresses it into
**Predictions**, which the Loot Fabricator later stamps into a chosen drop. (Before the Fabricator
existed the chamber printed loot directly; it no longer does.) This is deliberate: it makes simulation
an ongoing Create production line, not a fire-and-forget printer.

Two independent bottlenecks gate throughput, and both are factories you build:
- **Compute** sets how many sims run in parallel and how fast they cycle.
- **Blank supply** sets whether they have anything to print on.

Blank **grade caps fidelity**. Effective fidelity = min(compute allocated, blank grade). You cannot run a
top-tier boss model on cheap blanks no matter how much SU you throw at it.

### Fidelity and aberrations (tension, added later)
A sim's fidelity scales with allocated compute vs the target's complexity. Under-fund or overclock a sim
and accuracy drops: reduced/garbage yield, and at the extreme the sim **leaks** — spawning a single
hostile "glitched" entity. This is the *only* time the mod spawns anything, and it is a punishment, not a
feature. Bosses are the aberration hotspot, which ties the boss tier to this system instead of bolting on
a separate mechanic.

## 4. The substrate and crafting chains (Create-native)

Crafting the mod's items leans hard on Create's processing verbs, and especially on **Sequenced
Assembly** for the key component. Step-by-step production is a feature, not a tax.

### Imprint Blank (tier 1 substrate)
As shipped, all three grades are basin compacting recipes so the whole chain automates off one press
with nothing passing through a crafting grid:

1. **Compact** 2x Create pulp + a clay ball → 4x **Crude Imprint Blank**.
2. **Compact** 2x crude + polished rose quartz → 2x **Refined**.
3. **Compact** 2x refined + an amethyst cluster (renewable), an echo shard, or a Resonant Catalyst →
   **Pristine**, at 1, 2 and 4 respectively.

The longer silica-and-gel chain originally sketched here was never built; the clay reading matches
what ships and what the sprites should look like.

### Cognition Core (key component, via Sequenced Assembly)
Start with a pressed metal Core Frame on a depot, then loop it through stages — deploy wiring, spout
Matrix Gel, deploy a quartz crystal, press — for N cycles, finishing as a **Cognition Core**. Neural
Nodes, the Controller, and the Simulation Chamber are all built from Cognition Cores.

### Tiers and the circular endgame
- **Crude Blank** → common-mob sims.
- **Refined Blank** (a couple more steps) → uncommon mobs, better yield.
- **Pristine Blank** → needs a **Pristine Catalyst**, whose only source is running high-fidelity *boss*
  sims. So farming bosses well needs pristine blanks, and pristine blanks need boss output. You bootstrap
  up through it.

## 5. Block list

### Tier 1 (the first playable slice)
- **Neural Node** — kinetic consumer; rotation → compute. *(built in 0.1.0)*
- **Mainframe Controller** — pools compute from connected nodes; hosts the job UI; runs training jobs.
- **Simulation Chamber** — holds a trained model, pulls compute, consumes Imprint Blanks, outputs loot.
- **Synaptic Recorder** — slot for a Data Matrix; records data from mob deaths in range (point it at any
  farm). Optional for the very first loop; a matrix can be filled by hand.
- **Data Cable** — connects nodes ↔ controller ↔ chamber. Fallback: require adjacency.

### Items
- **Blank Data Matrix** — empty model; binds to a mob, fills a raw-data bar. *(placeholder in 0.1.0)*
- **Imprint Blank** (Crude / Refined / Pristine) — the substrate. *(crude placeholder in 0.1.0)*

### Deferred (do NOT build until the core loop is proven fun)
Heat & cooling · fidelity dial & aberration leaks · boss capture & Pristine catalyst · model fusion ·
loot-table and process model domains · multiblock mainframe · higher node tiers.

## 6. The first prototype loop (the "rotten flesh" slice)

1. Build the Create blank line → a stack of Crude Imprint Blanks.
2. Craft a Blank Data Matrix.
3. Kill zombies (matrix in hand, or via a Synaptic Recorder) until the raw-data bar hits tier 1.
4. Drive a Neural Node with Create rotation; cable it to the Mainframe Controller → a compute rate.
5. Slot the matrix into the Controller; run a Training job (spends compute over time) → trained model.
6. Move the model to the Simulation Chamber; feed it Imprint Blanks; connect compute.
7. Each cycle burns one blank + compute → rotten flesh into the chamber's output. No entity spawns.
8. Scale: more nodes for more compute; automate the blank line so the chamber never starves.

Prove steps 1–8 are fun with one mob before building anything on the deferred list.

## 7. Starter numbers (tune in-game)

- Neural Node: draws stress; outputs `|speed| * COMPUTE_PER_RPM` cu/t (0.25 placeholder).
- Train a tier-1 model: ~100,000 compute total.
- Sim cycle: 1 blank + ~20 compute, one cycle ≈ every 5 s, yields 1–3 rotten flesh.
- Raw data to reach tier 1: ~64 kills.

## 8. Open questions

- Compute network model: cabled graph vs adjacency vs chunk-wide wireless cluster.
- Does training happen in the Controller, or in a dedicated Trainer block?
- Whether the Synaptic Recorder reads vanilla death events or requires a Create-style interaction.
- Naming: working title is **Simulacra** (contains "cog", means computation). Cheap to change now.
