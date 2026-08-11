# Create: Simulacra (Design)

The living design doc: the concept, the rules that keep it coherent, and the planned build order.
A guideline, not gospel. The shipped mod diverges where playtesting led elsewhere, and CHANGELOG.md
describes what actually exists.

## 1. The pitch

A Create add-on where the new resource is **compute**, made by a physical "neural network" datacenter
that eats surplus rotational power. You spend compute to **train models of mobs** and to **run
simulations** of those models for the mob's loot.

The headline promise: **simulation spawns no entities.** You pay the entity cost once, gathering
training data. After that it is computation: no mob ticking, no pathfinding, no spawn cap. The
server-friendly mob farm.

## 2. Why it fits Create (and doesn't overshadow it)

Create's progression is one axis: *more power* (waterwheel → windmill → steam → addon reactors).
Once you have surplus SU there is little to spend it on. Simulacra adds a second axis: turn surplus
SU into **intelligence**. The datacenter is a large, hungry SU sink.

Resource generation is not a problem to avoid. The levers are compute cost, substrate cost and
training investment, not a ban on producing matter. The high-value target is **the un-farmable**:
bosses, dangerous or non-spawnable mobs, cross-dimension mobs. Common mobs are a convenience tier.

## 2b. What actually happens (fiction, and what it means for art)

The premise in one line: **you cannot have the drop without the death, unless you can predict the
death precisely enough that the prediction is as good as the event.** Every machine is one step in
turning a single witnessed death into arbitrarily many predicted ones.

**Blank Data Matrix - the instrument.** Brass frame, quartz lattice, tubes. Held in the off hand it
witnesses a kill and records the pattern. It locks to one species because the lattice takes a set
once and can only be refined after. Three states worth showing: empty, bound, trained.

**Cognition Array - the thinking.** Raw observation is noise; the nodes grind it into a runnable
model. This is why compute is a rate and not a battery. Nothing here is electronic.

**Imprint Blank - the receiving medium.** Soft, unfired, fine grained; a pressed tablet, not a lump.
Grade is grain. Crude takes one clean impression. Rose quartz makes it finer. Amethyst makes it
resonant enough to hold a boss-grade pattern.

**Simulation Chamber - the impression.** It runs the model against the blank and presses each
resolved outcome into the material. The blank goes in soft and comes out hard, scored into as many
Predictions as its grain could hold. Soft in, fired out, is the strongest visual beat in the chain.

**Prediction - a die, not an item.** A hardened chip carrying one recorded outcome for one species.
It knows the shape of a possible drop without having collapsed into one. The pricing is fiction too:
stamping a wither skeleton skull means consulting enough recorded deaths that the pattern is there
at all, roughly forty. Rotten flesh needs one. The price *is* the rarity.

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
| Crude Imprint Blank | Unfired pressed tablet, coarse, matte, soft-edged | Grey-brown clay, `#6b5f4e` to `#8a7c66` | Visible grain, slightly irregular edge |
| Refined Imprint Blank | Same tablet, finer body | Warmer, pink-flecked, `#8a7466` with rose quartz specks | Cleaner edge than crude, faint sheen |
| Pristine Imprint Blank | Same tablet, resonant | Violet sheen over pale clay, `#9a8fa6` | Faint internal glow; the material that holds a boss pattern |
| Prediction | A fired chip broken from a blank | Vitrified, glassy, sharp-edged, amber core `#f0c450` | The blanks' silhouette family, but hard and marked. A fragment: one blank yields several |
| Corrupted Imprint | The same wafer, ruined | Blank palette, desaturated, cracked | Smeared impression, a crack or two. Obviously scrap |
| Blank Data Matrix | An instrument, not a consumable | Brass frame `#9e6947`, dark lattice, quartz | Unwritten: lattice dark. Bound: tinted. Trained: ordered, steady glow |
| Resonant Catalyst | Something that came out of a boss | Deep violet, faceted, internally lit | The only item that should look like it is humming |
| Data Cable | Coil of cable, ends showing | Warm dark brown body, orange connectors | Value range near Create's belt, not black |

### Sound

All Create or vanilla, no custom foley. Milestone one-shots call `AllSoundEvents` directly;
continuous and diagnostic cues are `simulacra:*` events aliased onto Create and vanilla sounds, so a
pack can retarget them.

| Moment | Sound | Why |
| --- | --- | --- |
| Matrix binds on first kill | `AllSoundEvents.CONFIRM_2`, pitch 1.4 | It commits the matrix to a species permanently. Silence is how people bound the wrong mob |
| Training completes | `AllSoundEvents.CONFIRM` | The payoff of a long wait, so it gets a chime rather than another machine noise |
| Simulation job finishes | `MECHANICAL_PRESS_ACTIVATION`, pitch 0.65 | The blank being impressed and cured: a heavier, slower relative of the Fabricator's strike |
| Fabricator stamps an item | `MECHANICAL_PRESS_ACTIVATION`, pitch 1.15 | The collapse into a real object; the sharpest sound in the mod |
| Array producing | looped hum | The bed. A stopped array should not sound like an idle one |
| Chamber training | looped work | The longest wait in the mod, and it used to be silent |
| Chamber stalls | one quiet note | An alarm is welcome once and resented by the twentieth chamber |

### Animation

The Fabricator has no press head. It had one, driving Create's `MECHANICAL_PRESS_HEAD`, and it read
as a Mechanical Press floating inside a block that is not shaped like one. Its speed justifies its
shaft instead. If the machines get custom models later: the node wants an internal reel turning with
the shaft, and the chamber wants the matrix behind a window, sweeping while training.

## 3. Core systems

### Compute
- A **rate**, not a stored battery. Measured in compute units per tick (`cu/t`).
- Produced by a **Cognition Array**: Neural Nodes on a Mainframe Controller, driven by rotation. A
  lone node produces nothing. Output is the sum of each driven node's speed, strictly proportional,
  so the ratios hold at every RPM. A per-node size bonus sits in config, defaulted off, because it
  made whole-number ratios impossible. Stress is drawn per node.
- Pooled by the **Mainframe Controller** into one cluster budget.
- **Allocated** across jobs by a scheduler. The player is a datacenter operator triaging jobs. That
  decision is what makes Simulacra more than "FE with a new colour."

### Jobs (what compute is spent on)
- **Training**: convert raw data gathered from a mob into a higher-grade, more accurate model.
- **Simulation**: run a trained model to produce Predictions.
- (Later) **Optimization**: background jobs that modestly buff an existing Create setup, so idle
  compute always has somewhere to go.

### Substrate (the consumable that keeps a factory in the loop)
A running simulation eats one **Imprint Blank** per cycle and impresses it into **Predictions**,
which the Loot Fabricator stamps into a chosen drop. The chamber printed loot directly before the
Fabricator existed; it no longer does. Simulation is a production line, not a fire-and-forget
printer.

Two bottlenecks gate throughput, and both are factories you build:
- **Compute** sets how many sims run in parallel and how fast they cycle.
- **Blank supply** sets whether they have anything to print on.

Blank **grade caps fidelity**. Effective fidelity = min(compute allocated, blank grade). No amount
of SU runs a boss model on cheap blanks.

### Fidelity and aberrations (tension, added later)
Fidelity scales with allocated compute against the target's complexity. Under-fund or overclock a
sim and accuracy drops: reduced or garbage yield, and at the extreme the sim **leaks**, spawning one
hostile "glitched" entity. The only time the mod spawns anything, and a punishment. Bosses are the
hotspot, which ties the boss tier to this system instead of a bolted-on mechanic.

## 4. The substrate and crafting chains (Create-native)

Crafting leans on Create's processing verbs, and on **Sequenced Assembly** for the centrepiece.
Step-by-step production is a feature, not a tax.

### Imprint Blank (tier 1 substrate)
All three grades are basin compacting recipes, so the chain automates off one press with nothing
passing through a crafting grid:

1. **Compact** 2x Create pulp + a clay ball → 4x **Crude Imprint Blank**.
2. **Compact** 2x crude + polished rose quartz → 2x **Refined**.
3. **Compact** 2x refined + an amethyst cluster (renewable), an echo shard, or a Resonant Catalyst →
   **Pristine**, at 1, 2 and 4 respectively.

The longer silica-and-gel chain sketched here originally was never built.

### Cognition Core (key component, via Sequenced Assembly)
Not built, and not planned. The plan was a pressed Core Frame looped through deploy, spout and press
stages into a **Cognition Core** that every machine was made from. It was a middle-man item, so the
Sequenced Assembly centrepiece became the **Blank Data Matrix** instead and the machines craft
straight from Create components.

### Tiers and the circular endgame
- **Crude Blank** → common-mob sims.
- **Refined Blank** → uncommon mobs, better yield.
- **Pristine Blank** → needs a **Resonant Catalyst**, which only boss sims print. Farming bosses
  well needs pristine blanks, and pristine blanks need boss output. You bootstrap up through it.

## 5. Block list

### Tier 1 (the first playable slice)
- **Neural Node** — kinetic consumer; rotation → compute. *(built)*
- **Mainframe Controller** — pools compute from connected nodes. *(built)*
- **Simulation Chamber** — holds a model, pulls compute, consumes Imprint Blanks, outputs
  Predictions. *(built)*
- **Loot Fabricator** — spends Predictions on drops, driven by rotation. *(built; not in the
  original plan)*
- **Data Cable** — connects nodes, controller and consumers. *(built)*
- **Synaptic Recorder** — slot for a Data Matrix; records data from mob deaths in range. *(not
  built; a matrix can be filled by hand)*

### Items
- **Blank Data Matrix** — empty model; binds to a mob, fills a raw-data bar. *(built)*
- **Imprint Blank** (Crude / Refined / Pristine) — the substrate. *(built)*
- **Prediction** — one roll of a subject's loot table, spent at the Fabricator. *(built)*

### Deferred (do NOT build until the core loop is proven fun)
Heat & cooling · fidelity dial & aberration leaks · model fusion · loot-table and process model
domains · multiblock mainframe · higher node tiers.

## 6. The first prototype loop (the "rotten flesh" slice)

Blank line → Crude Imprint Blanks. Matrix in the off hand, kill zombies. Node on a shaft, cabled to
a Controller → compute. Matrix in the Chamber → a trained model. Blanks plus compute → Predictions.
Predictions plus a shaft → rotten flesh, no entity spawned. Scale with more nodes, more chambers, a
faster shaft.

Prove that is fun with one mob before building anything on the deferred list.

## 7. Starter numbers (tune in-game)

Everything here lives in `simulacra-common.toml`; the shipped values are the ones that make the
ratios whole, not these.

- Neural Node: draws stress; outputs `|speed| * COMPUTE_PER_RPM` cu/t.
- Four nodes drive one chamber, which feeds one Fabricator, at any speed.
- Fabricator work per tick is `RPM / 8`, so chambers between array and Fabricator are RPM/32.
- Raw data to train: dozens of kills, or a handful for a boss.

## 8. Open questions

- Compute network model. **Resolved:** cabled graph, nodes and controllers conducting, one
  controller per network.
- Does training happen in the Controller, or a dedicated Trainer block? **Resolved:** the Simulation
  Chamber trains and then simulates.
- Whether the Synaptic Recorder reads vanilla death events or requires a Create-style interaction.
  Still open, and it blocks the Recorder.
- Naming. **Resolved:** Create: Simulacra, mod id `simulacra`.
