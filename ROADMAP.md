# Create: Simulacra Roadmap & Feature List

A living checklist. Milestones are deliberately ordered so each one is playable on its own and builds on
the last. Do not pull features forward from a later milestone until the current loop is proven fun. See
[DESIGN.md](DESIGN.md) for the reasoning behind any of this.

## Design pillars (the rules every feature must respect)

- **Zero entities.** Simulation spawns nothing. The only exception is a deliberate aberration leak.
- **Compute is an allocatable rate**, not a stored battery. The player triages jobs.
- **Crafting is Create-native.** Lean on Create's processing verbs and Sequenced Assembly.
- **Farm the un-farmable.** High value targets are bosses / dangerous / non-spawnable mobs; common mobs
  are a convenience tier.
- **Resource generation is fine.** Balance with compute cost, substrate cost, and training investment,
  not artificial restrictions.

---

## 0.1 — Scaffold & first kinetic block ✅ (done)

- [x] NeoForge 1.21.1 + Create 6.x Gradle project that builds
- [x] Registration plumbing (blocks, items, block entities, creative tab, stress)
- [x] **Neural Node**: kinetic consumer, rotation → compute rate, goggle readout
- [x] Spinning input shaft rendered Create-style (SHAFT_HALF half-shaft on the back face)
- [x] Legible placeholder textures (andesite + brass), generator script
- [x] DESIGN.md, README.md, this roadmap

## 0.2 — The compute network & the core machine loop ✅ (core done; polish items remain)

The goal: compute stops being a number on a tooltip and becomes a pooled, spendable resource.

- [x] **Compute network model** decided and implemented (cabled flood-fill: cables conduct, nodes are leaves)
- [x] **Data Cable** block (passive conductor)
- [x] **Mainframe Controller** block: flood-fills the cable network, sums connected nodes' compute into a pool
- [x] Pooled readout: right-click status message + goggle tooltip (total cu/t, node count)
- [x] Connection-aware Data Cable model (pipe-style, arms to neighbours)
- [x] **Simulation Chamber** (stub): controller pushes pooled compute to it; runs a hardcoded job that
      pops a placeholder drop (rotten flesh). First produce→pool→consume loop is live.
- [x] Basic in-order compute allocation (controller → consumers)
- [x] Machine fronts follow the casing convention (Create brass casing base + proud detail panel)
- [x] **Compute generation as a multiblock (Cognition Array)** — a lone node produces nothing; the
      controller pools every driven node in its connected array, larger arrays are more efficient, and
      stress is drawn per node (real mechanical work). See "Generation rework" below.
- [x] Fair-share allocation: compute splits proportionally to demand, so scan order can't starve the
      chambers at the back of the list
- [x] Proper Chamber output inventory: 9-slot buffer with a pull-only item capability face
- [x] Multi-controller ownership: one network is owned by the lowest-positioned controller; the rest
      go dormant instead of each pooling the same nodes
- [ ] Job **priorities** on top of fair-share (fair-share alone is in)
- [ ] **Cluster compute API**: extract allocation into a reusable object
- [ ] Controller **GUI/menu**: total compute, running jobs, priorities
- [ ] Event-driven rescan instead of the 1s periodic walk (perf for large networks)

### Generation rework (Create convention) — DONE
Modelled on Create's Steam Engine + connectivity-multiblock conventions. The Mainframe Controller
flood-fills its connected Neural Nodes (nodes conduct, so a contiguous rack forms one array; cables
extend reach and link remote consumers). Output = sum of each driven node's `speed * COMPUTE_PER_RPM`,
times a size bonus that grows with the count of driven nodes. A node not in any controller's array
produces nothing, and each node draws stress, so rotation does real work. Still to refine: a hard
"capped by weakest dimension" rule once cooling/heat is added; strict-cuboid validation if we ever want
it (currently forgiving connected-array).

## 0.3 — Data models & training (the "learn a mob" half) ✅ (done bar the Synaptic Recorder)

- [x] **Blank Data Matrix** functional: binds to the first mob killed while carried, accumulates data
      (stored in custom_data; non-stacking; tooltip shows bound mob + data count)
- [x] Manual data gathering: carried matrix gains data on matching kills (LivingDeathEvent)
- [x] Per-mob data target + trained flag (need DATA_TO_TRAIN data before a model can be trained)
- [x] **Training job**: insert a matrix into the Simulation Chamber; cluster compute trains it
      (raw data → trained), then the chamber switches to simulating
- [x] Chamber holds a model (right-click insert / sneak-extract / right-click status) and gates output
      on a trained matrix
- [x] Simulation Chamber emits the bound mob's **actual loot**, rolled from the live loot table at
      runtime (so other mods' injected drops are simulated too)
- [x] Multi-tier models, shipped as **grades**: Coarse → Tuned → Deep → Self-Aware, earned by data
- [x] Model grade affects simulation: cheaper jobs per grade, higher accuracy, bonus rolls at Self-Aware
- [x] First real end-to-end loop: data → train → simulate → loot, **no entities**
- [ ] **Synaptic Recorder** block: records data from matching mob deaths in range (point it at a farm)

## 0.4 — The substrate & Create crafting chains ✅ (done; Matrix Gel dropped for now)

- [x] **Crude Imprint Blank** functional: consumed per simulation job
- [x] Substrate has a Create production route: crude is pressed from pulp + clay, refined and pristine
      are compacted in the basin
- [x] A **Sequenced Assembly** centrepiece recipe — it ended up being the Blank Data Matrix (brass
      sheet, polished rose quartz, electron tube, 2 loops, failure chance) rather than a separate
      Cognition Core, which was dropped as a redundant middle-man item
- [x] All machine blocks crafted from Create components
- [x] Blank **grade caps yield**: substrate tier sets loot rolls per job, and only pristine holds a boss
- [ ] **Matrix Gel** fluid (mixing + spouting) — no fluids are registered yet; the current substrate
      line is solid without it, so this is only worth doing if the chain needs more depth

## 0.45 — Predictions & the Loot Fabricator ✅ (done)

The chamber used to print a mob's drops directly, which meant either taking whatever the loot table
gave you or filtering at the chamber and wrecking the rarity curve. Splitting the loop in two fixed
both.

- [x] **Prediction** item: fungible, one per roll of its subject's loot table, printed by the chamber
      instead of loot
- [x] **Loot Fabricator**: spends Predictions on a target the player picks, driven by **rotation**
      rather than compute, so the array decides *what* you can simulate and the shaft decides *how
      fast* you stamp it out
- [x] **Rarity-preserving pricing**: an item costs `1 / average yield per roll`, sampled from the
      mob's real loot table and cached, so choosing a rare drop costs proportionally more
- [x] Fabricator screen showing the target, its price, and how many matching Predictions are held
- [x] Fabricator lights its front while it can actually stamp, and its press head strokes on impact
- [ ] **Ponder scene for the Fabricator** — the one machine still without one
- [ ] Fabricator cue for a target the held Predictions can never make (the screen says so; the
      machine is silent)

## 0.5 — Heat & cooling

- [ ] Per-node/per-cluster **heat** that scales with utilisation, not just being on
- [ ] **Throttling**: compute output falls as heat passes a threshold; damage at the extreme
- [ ] Cooling tiers: passive heat sink → water adjacency → active liquid cooler (power + water)
- [ ] Heat readout on goggles/Jade

## 0.6 — Fidelity & aberrations

- [ ] Fidelity dial wired into simulation yield/quality
- [ ] Under-funded / overclocked sims produce reduced or **corrupted** output
- [ ] **Aberration leak**: rare hostile glitched entity spawn as the failure state (the one exception)

## 0.7 — Bosses & the circular endgame ✅ (economy closed; aberrations still pending)

- [x] **Boss data capture** gated behind actually defeating the boss (boss kills grant 16 data, so a
      boss model trains in a handful of kills rather than dozens)
- [x] **Refined** and **Pristine** Imprint Blank tiers; only pristine can hold a `c:bosses` subject
- [x] **Resonant Catalyst** (the "Pristine Catalyst" of the original plan), a chance print from boss
      sims, compacted with refined blanks into pristine substrate — the economy loop closes
- [ ] Steep compute cost specifically for boss simulations (jobs currently cost the same regardless
      of subject; the boss gate is substrate tier, not compute)
- [ ] Bosses tied into the aberration system (a destabilised Wither sim can leak a real Wither)

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

- [x] Move tuning constants into a real NeoForge config spec (`simulacra-common.toml`)
- [x] Advancements for progression (9 advancements covering first machine → closed catalyst economy)
- [x] Goggle info providers on all four machines, plus right-click status readouts
- [x] **Ponder** scenes — seven across six structures, covering the node, matrix, controller, cable,
      and chamber. **The Loot Fabricator still has none**, and it is the machine that most needs one.
- [x] Sounds — milestone one-shots through Create's `AllSoundEvents`, plus `simulacra:*` events for
      the array hum, the training loop, stalls, and slot interactions. No audio files are shipped;
      everything aliases Create or vanilla, so packs can retarget it.
- [x] GameTests — 11, covering the chamber's stall rules and compute demand, and the Fabricator's
      lit state, pricing, and output, driven by a real creative motor. `./gradlew runGameTestServer`.
- [ ] **Data generation** (`runData`) for recipes, models, blockstates, loot tables, lang, tags
- [ ] **JEI / EMI** integration for recipes and the simulation "recipe"
- [ ] **Jade** integration specifically (goggles are covered; Jade is not)
- [ ] CI build + publish to Modrinth/CurseForge (`runGameTestServer` already fails the build on a
      regression, so it can gate the pipeline as-is)
- [ ] Final art pass to replace placeholder textures and add block models

## Open design questions (resolve before the milestone that needs them)

- ~~Network model: cable graph vs adjacency vs wireless cluster radius?~~ Resolved: cable graph, with
  nodes and controllers conducting. One network is owned by exactly one controller (lowest position
  wins) so redundant controllers cannot double-count the same nodes.
- ~~Does training run in the Controller or a dedicated Trainer block?~~ Resolved: the Simulation
  Chamber trains and then simulates, so one machine covers both halves of the loop.
- Recorder: hook vanilla death events, or require a Create-style interaction? (blocks the Synaptic
  Recorder in 0.3)
- ~~Final mod name~~ Resolved: **Create: Simulacra**, mod id `simulacra`.
