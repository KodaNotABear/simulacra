# Create: Simulacra Roadmap & Feature List

A living checklist. Milestones are ordered so each one is playable on its own. Do not pull features
forward from a later milestone until the current loop is proven fun. [DESIGN.md](DESIGN.md) has the
reasoning.

## Design pillars (the rules every feature must respect)

- **Zero entities.** Simulation spawns nothing. The only exception is a deliberate aberration leak.
- **Compute is an allocatable rate**, not a stored battery. The player triages jobs.
- **Crafting is Create-native.** Lean on Create's processing verbs and Sequenced Assembly.
- **Farm the un-farmable.** Bosses and non-spawnable mobs are the high-value targets; common mobs
  are a convenience tier.
- **Resource generation is fine.** Balance with compute, substrate and training cost, not artificial
  restrictions.

---

## 0.1 — Scaffold & first kinetic block ✅ (done)

- [x] NeoForge 1.21.1 + Create 6.x Gradle project that builds
- [x] Registration plumbing (blocks, items, block entities, creative tab, stress)
- [x] **Neural Node**: kinetic consumer, rotation → compute rate
- [x] Spinning input shaft rendered Create-style (SHAFT_HALF half-shaft on the back face)
- [x] Legible placeholder textures (andesite + brass)
- [x] DESIGN.md, README.md, this roadmap

## 0.2 — The compute network & the core machine loop ✅ (core done; polish items remain)

Compute stops being a number on a tooltip and becomes a pooled, spendable resource.

- [x] **Compute network model** implemented (cabled flood-fill: cables conduct, nodes are leaves)
- [x] **Data Cable** block (passive conductor), connection-aware pipe model
- [x] **Mainframe Controller**: flood-fills the network, sums connected nodes' compute into a pool
- [x] Pooled readout: right-click status message + goggle tooltip (total cu/t, node count)
- [x] **Simulation Chamber**: the controller pushes pooled compute to it. First produce → pool →
      consume loop
- [x] Machine fronts follow the casing convention (brass casing base + proud detail panel)
- [x] **Compute generation as a multiblock (Cognition Array)** — a lone node produces nothing; the
      controller pools every driven node in its array, and stress is drawn per node. See below
- [x] Fair-share allocation: compute splits proportionally to demand, so scan order cannot starve
      the chambers at the back of the list
- [x] Proper Chamber output inventory: 9-slot buffer with a pull-only item capability face
- [x] Multi-controller ownership: the lowest-positioned controller owns a network, the rest go
      dormant
- [ ] Job **priorities** on top of fair-share (fair-share alone is in)
- [ ] **Cluster compute API**: extract allocation into a reusable object
- [ ] Controller **GUI/menu**: total compute, running jobs, priorities
- [ ] Event-driven rescan instead of the 1s periodic walk (perf for large networks)

### Generation rework (Create convention) — DONE
Modelled on Create's connectivity multiblocks. The Controller flood-fills its connected Neural Nodes
(nodes conduct, so a contiguous rack is one array; cables extend reach and link remote consumers).
Output is the sum of each driven node's `speed * COMPUTE_PER_RPM`, strictly proportional, so the
ratios hold at every RPM: four nodes per chamber per Fabricator. A node outside any array produces
nothing, and each node draws stress.

The Steam-Engine-style size bonus and a per-node idle RPM were built, then defaulted off. The bonus
multiplied the total differently at every size, so no node count landed on a whole chamber; the idle
cost made output non-proportional to speed, so a ratio true at 32 RPM was wrong at 64. Both stay in
config. Still to refine: a "capped by weakest dimension" rule once cooling and heat exist.

## 0.3 — Data models & training (the "learn a mob" half) ✅ (done bar the Synaptic Recorder)

- [x] **Blank Data Matrix**: binds to the first mob killed while carried, accumulates data in
      custom_data; non-stacking; tooltip shows bound mob and data count
- [x] Per-mob data target and trained flag
- [x] **Training job**: insert a matrix into the Simulation Chamber; cluster compute trains it, then
      the chamber switches to simulating
- [x] Chamber holds a model (right-click insert, sneak-extract, right-click status) and gates output
      on a trained matrix
- [x] Chamber output rolls the bound mob's **actual loot table** live, so other mods' injected drops
      are included
- [x] Multi-tier models, shipped as **grades**: Coarse → Tuned → Deep → Self-Aware, earned by data
- [x] Model grade affects simulation: cheaper jobs, higher accuracy, bonus rolls at Self-Aware
- [x] First real end-to-end loop: data → train → simulate, **no entities**
- [ ] **Synaptic Recorder** block: records data from matching mob deaths in range

## 0.4 — The substrate & Create crafting chains ✅ (done; Matrix Gel dropped for now)

- [x] **Crude Imprint Blank**: consumed per simulation job
- [x] Substrate has a Create production route: every tier compacts in the basin
- [x] A **Sequenced Assembly** centrepiece: the Blank Data Matrix (brass sheet, polished rose
      quartz, electron tube, 2 loops, failure chance). The Cognition Core was dropped as a middle-man
- [x] All machine blocks crafted from Create components
- [x] Blank **grade caps yield**: substrate tier sets rolls per job, and only pristine holds a boss
- [ ] **Matrix Gel** fluid (mixing + spouting) — no fluids are registered and the solid line works
      without it, so this only earns its place if the chain needs more depth

## 0.45 — Predictions & the Loot Fabricator ✅ (done)

The chamber used to print drops directly, so you either took what the loot table gave you or
filtered at the chamber and wrecked the rarity curve. Splitting the loop in two fixed both.

- [x] **Prediction** item: fungible, one per roll of its subject's loot table, printed by the
      chamber instead of loot
- [x] **Loot Fabricator**: spends Predictions on a target the player picks, driven by **rotation**,
      so the array decides *what* you can simulate and the shaft decides *how fast* you stamp it out
- [x] **Rarity-preserving pricing**: an item costs `1 / average yield per roll`, sampled live from
      the mob's loot table and cached
- [x] Fabricator screen: subject, paged drop palette with prices, preview, progress, output slots
- [x] Two modes: no selection rolls the table, a selection makes one drop at its price
- [x] Speed proportional to rotation (`RPM / 8`), so chambers per Fabricator is RPM/32
- [x] Fabricator lights its front while it can stamp
- [x] Ponder scenes: "Turning Predictions into drops" and "Choosing what to make"
- [ ] Fabricator cue for a target the held Predictions can never make (the screen and goggles say
      so; the machine is silent)

A Create-filter slot for setting the target was built and removed. Funnels are one-directional, so
no build could swap a filter once it was in, and a filter is a set while the machine wants one
target. Create's `FilteringBehaviour` puts the setting on a face slot, which cannot show a price,
and the price is what keeps choosing honest.

## 0.5 — Heat & cooling

- [ ] Per-node/per-cluster **heat** that scales with utilisation, not just being on
- [ ] **Throttling**: compute output falls as heat passes a threshold; damage at the extreme
- [ ] Cooling tiers: passive heat sink → water adjacency → active liquid cooler
- [ ] Heat readout on goggles/Jade

## 0.6 — Fidelity & aberrations

- [ ] Fidelity dial wired into simulation yield/quality
- [ ] Under-funded or overclocked sims produce reduced or **corrupted** output
- [ ] **Aberration leak**: rare hostile glitched entity spawn as the failure state (the one
      exception)

## 0.7 — Bosses & the circular endgame ✅ (economy closed; aberrations still pending)

- [x] **Boss data capture** gated behind defeating the boss (boss kills grant 16 data)
- [x] **Refined** and **Pristine** Imprint Blank tiers; only pristine holds a `c:bosses` subject
- [x] **Resonant Catalyst** (the plan's "Pristine Catalyst"), a chance print from boss sims,
      compacted with refined blanks into pristine substrate. The economy loop closes
- [ ] Steep compute cost specifically for boss simulations (the boss gate is substrate tier)
- [ ] Bosses tied into the aberration system (a destabilised Wither sim leaks a real Wither)

## 0.8 — Model fusion

- [ ] Combine trained models into hybrids
- [ ] Fusion-only outputs / rare results
- [ ] Fusion UI

## 0.9+ — Platform expansion (turns it from a feature into a mod)

- [ ] **Loot-table models**: sample a structure's chest, simulate its loot table
- [ ] **Process models**: a model outputs *behaviour* (defense, trades, logistics), not items
- [ ] Optimization jobs that modestly buff existing Create setups (idle-compute sink)

---

## Cross-cutting / infrastructure (do alongside, not as a phase)

- [x] Tuning constants in a real NeoForge config spec (`simulacra-common.toml`)
- [x] Advancements for progression (9, first machine → closed catalyst economy)
- [x] Goggle info on the Controller, Chamber and Fabricator, plus right-click status readouts. The
      Node uses Create's own kinetic readout
- [x] **Ponder** scenes — nine across seven structures, covering every block and the matrix
- [x] Sounds — milestone one-shots through Create's `AllSoundEvents`, plus `simulacra:*` events for
      the array hum, the training loop, stalls and slot interactions. No audio files ship; it all
      aliases Create or vanilla, so packs can retarget it
- [x] GameTests — 11, on the chamber's stall rules and compute demand and the Fabricator's lit
      state, pricing and output, driven by a real creative motor. `./gradlew runGameTestServer`
- [ ] **Data generation** (`runData`) for recipes, models, blockstates, loot tables, lang, tags
- [ ] **JEI / EMI** integration for recipes and the simulation "recipe"
- [ ] **Jade** integration specifically (goggles are covered; Jade is not)
- [ ] CI build + publish to Modrinth/CurseForge (`runGameTestServer` already fails the build on a
      regression, so it can gate the pipeline as-is)
- [ ] Final art pass to replace placeholder textures and add block models

## Open design questions (resolve before the milestone that needs them)

- ~~Network model: cable graph vs adjacency vs wireless cluster radius?~~ Resolved: cable graph,
  nodes and controllers conducting. One network, one owning controller (lowest position wins), so
  redundant controllers cannot double-count the same nodes.
- ~~Does training run in the Controller or a dedicated Trainer block?~~ Resolved: the Chamber trains
  and then simulates.
- Recorder: hook vanilla death events, or require a Create-style interaction? (blocks the Synaptic
  Recorder in 0.3)
- ~~Final mod name~~ Resolved: **Create: Simulacra**, mod id `simulacra`.
