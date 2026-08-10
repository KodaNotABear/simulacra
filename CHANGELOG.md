# Changelog

All notable changes to Create: Simulacra (mod id `simulacra`) are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versioning is `MODVERSION` for now and will move to
`MODVERSION+MCVERSION` once published.

## [Unreleased]

### Added
- Sound for the parts of the loop that had none. The Cognition Array now hums while producing and
  announces spinning up and winding down, so an array stopped by a broken shaft is distinguishable
  from an idle one. Training — the longest wait in the mod — has a working loop instead of silence,
  and a chamber that becomes blocked plays one quiet cue rather than going silently inert. Slot
  interactions (matrix in/out, blanks loaded/unloaded) are audible. These are registered as
  `simulacra:*` events aliased onto Create and vanilla sounds, so they caption correctly and a
  resource pack can retarget any of them; the existing milestone one-shots still go through Create's
  `AllSoundEvents` where its layered playback and subtitles are already right. No audio ships.
- GameTests: 11 covering the chamber's stall rules and compute demand, and the Fabricator's lit
  state, rarity pricing, and output — the Fabricator ones driven by a real Create creative motor, so
  the kinetic hookup is exercised rather than stubbed. `./gradlew runGameTestServer` runs them
  headlessly in ~15s and fails the build on a regression.
- `DIRECTION.md`: the art and audio bible — the thinking/physical split the art has to sell, the
  four-channel colour language, what each item should read as, and what each machine should sound
  like.
- Summary tooltip on the Loot Fabricator, which was the only machine without one.

### Changed
- The Loot Fabricator has two modes, and the default is the useful one. With no drop chosen it rolls
  the subject's loot table and keeps whatever comes out, spending one Prediction per roll — which is
  exactly what a Prediction is defined to be, so a freshly placed machine fed Predictions simply works
  with nothing to configure. Picking a drop in its screen switches to the priced mode: pay the
  rarity-derived cost and get that item every time. Previously an empty selection meant the machine sat
  idle, which read as broken, and there was no way to ask for unfiltered drops at all.
- The Loot Fabricator runs on Create's own Mechanical Press curve —
  `tickSpeed = lerp(clamp(rpm / 512), 1, 60)` — instead of a plain multiple of rotation speed. It now
  speeds up the way every other Create machine does and, like them, stops improving past 512 RPM,
  rather than accelerating without limit. Its cycle is several times a press's so it sits behind the
  Simulation Chamber feeding it rather than outrunning it: about 9s per item at 32 RPM, 2.4s at 128.
- It holds a single stack of Predictions, the way Create's processing blocks hold one stack of work.
  A multi-slot buffer made it a chest that happened to stamp things; one stack makes a Chamber feeding
  it the obvious build. One stack also means one subject at a time, so the picker always shows exactly
  one mob's drops.
- The Fabricator's screen is laid out after Hostile Neural Networks' loot fabricator, which solves the
  same problem well: the subject named in the header, a paged three-by-three palette of its drops to
  choose from, a preview of the current choice, a vertical progress bar, and a four-by-four block of
  output slots. Each drop carries its price and unaffordable ones are dimmed, so choosing is done by
  looking rather than reading — six on-screen strings became numbers and tooltips. The arrangement is
  borrowed; the art is this mod's own.
- The press head is gone from the Fabricator, along with the stroke machinery that only existed to
  drive it. It was a Mechanical Press's geometry on a block that is not shaped like one, and it read as
  an object floating in the machine rather than part of it.
- Block and item summaries hide behind Shift with a "Hold [Shift]" prompt, using Create's own
  `TooltipHelper`, instead of printing in full on every stack.

- In-game and player-facing text caught up with the Prediction rework. The Simulation Chamber's
  Ponder scene was still titled "Printing drops without the mob" and showed iron and rotten flesh
  leaving the chamber, which has not been true since the Fabricator landed; its summary tooltip said
  the same. README, the Modrinth description, and the roadmap had no mention of the Loot Fabricator
  or Predictions at all, and the Modrinth "known issues" still listed the double-counting controller
  bug and the absence of Ponder scenes, both long since fixed.

### Fixed
- **Every machine was unharvestable.** All four call `requiresCorrectToolForDrops()`, and the mod
  shipped no block tags at all — so no tool was ever the correct one and mining any of them dropped
  nothing, whatever you used. They are now in `minecraft:mineable/pickaxe`, with a test asserting an
  iron pickaxe is a valid tool for each.
- **The Loot Fabricator's item capability was never registered.** It had a working `FabricatorItemHandler`
  and a `getItemHandler()` that nothing exposed, so no funnel, hopper, belt or pipe could put a
  Prediction in or take a drop out — the entire intended build, a Simulation Chamber feeding a
  Fabricator, could not be plumbed at all. Every test passed throughout because they all reached into
  the block entity's handlers directly rather than through the capability automation actually uses;
  there is now a test that goes through the capability.
- The Loot Fabricator never lit up. It declared a `lit` property, shipped four lit blockstate
  variants and an emissive lit model, and nothing ever set it — so the machine gave no feedback while
  working and its glow texture was unreachable. It now lights whenever it can actually stamp.
- The Fabricator's press head never animated. `pressTicks` was synced to the client but only counted
  down on the server, so the client held the value between strikes and the head sat at rest
  permanently.
- The Neural Node changed its lit state with a raw `setBlock`. It is a `KineticBlockEntity`, and
  Create provides `switchToBlockState` precisely because a plain `setBlock` can cost a kinetic block
  its network association — which would surface as a driven node intermittently reporting no speed.

## [1.0.0-beta.1] - 2026-07-17

First beta. The full loop is playable: bind and hunt a mob, train its model, simulate it for loot,
grade the model up, climb the substrate tiers, and close the boss catalyst economy.

### Added
- Project scaffold targeting NeoForge 1.21.1 and Create 6.x.
- `Neural Node`: a kinetic consumer with a back shaft input that converts rotation into a compute
  rate. Chain-drive style model with an animated drive front that only lights while driven
  (LIT blockstate). Registered with a Create stress impact.
- `Mainframe Controller`: the brain of a Cognition Array. Flood-fills through Data Cables and
  contiguous Neural Nodes, pools their compute with a size bonus, and splits it across consumers
  proportionally to demand when oversubscribed. Animated dashboard lights only while the array
  produces compute. Goggles and right-click report CU/t and driven node counts.
- `Data Cable`: connection-aware pipe that extends an array and links remote consumers. Belt-inspired
  dark grey texture.
- `Simulation Chamber`: holds a Data Matrix, trains it with compute, then runs simulation jobs that
  print the subject's loot with zero spawned entities. Loot is rolled from the mob's live loot table
  at runtime, so drops injected by other mods are simulated too. Internal 9-slot output buffer with a
  pull-only item capability face plus a substrate input slot (push-in only). Stalls instead of
  spilling when out of substrate, blocked by grade, or full. Animated viewport lights only while fed
  compute, with a watchdog that goes dark if the controller disappears.
- `Blank Data Matrix`: binds to the first mob defeated while held in the offhand, gathers data from
  kills, and trains in the chamber. Crafted via Sequenced Assembly (brass sheet, polished rose
  quartz, electron tube, press, 2 loops) with `Incomplete Data Matrix` as the transitional item and a
  small failure chance.
- Model grades: trained matrices keep learning from completed jobs and further kills, climbing
  Coarse, Tuned, Deep, and Self-Aware. Higher grades run jobs cheaper; Self-Aware adds a bonus loot
  roll.
- Simulation accuracy by grade: a botched job spends its compute and substrate but prints a
  `Corrupted Imprint` instead of loot, which crushes back into pulp. Models still learn from
  failures.
- Substrate tiers: `Crude`, `Refined`, and `Pristine Imprint Blanks`. Higher grades print more loot
  rolls per job; only pristine can hold a boss-grade subject (the `c:bosses` entity tag). Every tier
  is a basin compacting recipe, so the whole substrate chain automates off one press with nothing
  passing through a crafting grid. Crude presses from Create pulp and clay; refined adds polished
  rose quartz; pristine takes an amethyst cluster, an echo shard, or a Resonant Catalyst, in
  ascending order of yield. The amethyst route keeps the boss tier renewable without ancient city
  loot.
- `Resonant Catalyst`: a chance print from boss simulations. Compacting it with refined blanks is
  the highest-yield route to pristine substrate, closing the endgame loop.
- Compute demand scales with the work: finer substrate and boss-grade subjects draw proportionally
  more per tick, and cost proportionally more in total. A simulated Wither runs at the same pace as
  a zombie but needs roughly ten times the array to do it, so the size of a Cognition Array now
  decides what it is capable of simulating rather than only how many chambers it can feed.
- Chambers overclock. Surplus array output above a chamber's rated draw is spent speeding it up
  instead of being discarded, at diminishing returns, so scaling an array up is worthwhile without
  ever beating the throughput of building another chamber. The controller covers every chamber's
  rated demand first and only then distributes what is left, so nothing runs hot while a neighbour
  starves.
- Neural Nodes spend their first few RPM running themselves. Compute and stress were both linear in
  speed, which made the compute-per-stress rate speed-invariant and left the array bonus to decide
  everything: many slow nodes were strictly better than a few fast ones, so there was exactly one
  correct array. Charging each node an overhead gives width a real cost and puts the best build
  somewhere in the middle.
- Training and job costs raised substantially now that surplus compute has somewhere to go: training
  is a real gate rather than ten seconds, and a job is no longer a full loot table every 1.65s.
- A Data Matrix alone in a crafting grid comes back blank, so a matrix bound to the wrong mob by a
  stray kill is recoverable instead of ruined.
- Training progress lives on the matrix rather than the chamber, so pulling one out mid-train no
  longer silently bins the compute.
- Sneak-clicking a chamber with an empty hand takes the substrate back out once the matrix is
  removed, so tiers can be swapped without breaking the block, and trying to load a tier it will not
  accept now says so instead of doing nothing.
- Simulations are credited to a fake player, so `killed_by_player` drops behave the way players
  expect: a blaze model prints blaze rods, a wither skeleton model prints skulls.
- Simulation loot overrides at `simulacra:simulation/<mob>`. The Wither and the Ender Dragon hand
  out their rewards in death logic rather than a loot table, so simulating them yielded nothing;
  they now print nether stars and dragon's breath. Any datapack can override or add a mob.
- Advancement tree (9 advancements) guiding the full loop from first machine to the closed catalyst
  economy, including detection of a trained matrix via its NBT.
- NeoForge config (`simulacra-common.toml`) for every balance value: compute rates, array bonus,
  stress, job costs, substrate rolls, model grade thresholds, accuracy, and catalyst chance.
- Create-native recipes for all machines and items; JEI on the dev runtime for recipe checks.
- Consistent glossary: the node network is the Cognition Array, compute rate is CU/t, and a matrix's
  mob is its Subject.
- Boss kills grant extra data (configurable, default 16), so a boss model trains in a handful of
  kills instead of dozens.
- Comparator output on the Simulation Chamber reads the loot buffer fill level.
- Summary tooltips on all four machine blocks.

### Known gaps
- Simulation Chamber has no GUI; interaction is right-click and goggle based.
- No JEI category for simulation yields; no Ponder scenes yet.
- Boss data collection still requires defeating the boss with a matrix in the offhand.
