# Changelog

Notable changes to Create: Simulacra (mod id `simulacra`). Format follows
[Keep a Changelog](https://keepachangelog.com/). Versioning is `MODVERSION` for now and moves to
`MODVERSION+MCVERSION` once published.

## [1.0.0] - 2026-08-11

First release. The full loop ships: bind a mob, train its model, simulate it for Predictions, stamp
Predictions into drops, climb the substrate tiers, close the boss catalyst economy.

### Added
- Sound for the parts of the loop that had none: the array hum, spin-up and wind-down, the training
  loop, a stall cue, and slot interactions. Registered as `simulacra:*` events aliased onto Create
  and vanilla sounds, so they caption correctly and a resource pack can retarget any of them.
  Milestone one-shots still go through Create's `AllSoundEvents`. No audio files ship.
- GameTests: 11, covering the chamber's stall rules and compute demand and the Fabricator's lit
  state, pricing and output. The Fabricator ones run off a real Create creative motor.
  `./gradlew runGameTestServer` runs them headlessly in ~15s and fails the build on a regression.
- `DIRECTION.md`: the art and audio bible.
- Summary tooltip on the Loot Fabricator, the only machine without one.

### Changed
- Whole-number ratios across the chain. Four Neural Nodes drive one Simulation Chamber, which feeds
  one Loot Fabricator, at any speed. The array bonus and the node idle RPM both default to off; each
  broke the arithmetic, and a pack that wants them back can set them. Width still costs: stress
  scales with nodes times RPM, so eight nodes at 16 RPM cost what four at 32 do.
- Fabricator speed is proportional to rotation: work per tick is `RPM / 8`. It no longer follows
  Create's Mechanical Press curve, which rounded to whole ticks and made throughput lumpy. Chambers
  between an array and a Fabricator are RPM/32: one at 32, two at 64, four at 128.
- The Loot Fabricator has two modes, and the default is the useful one. With no drop chosen it rolls
  the subject's loot table and keeps what comes out, one Prediction per roll. Pick a drop in its
  screen and it switches to the priced mode: pay the rarity-derived cost, get that item every time.
  An empty selection used to leave the machine idle, which read as broken.
- It holds one stack of Predictions, the way Create's processing blocks hold one stack of work. One
  stack means one subject at a time, so the picker always shows exactly one mob's drops.
- The Fabricator's screen follows Hostile Neural Networks' loot fabricator: subject in the header, a
  paged three-by-three palette of its drops, a preview, a vertical progress bar, and a four-by-four
  block of output slots. Prices sit on the drops and unaffordable ones are dimmed. The panel states
  which mode it is in, and the palette clicks on select and clear and fails audibly out of reach.
  Six on-screen strings became numbers and tooltips.
- The press head is gone from the Fabricator, along with the stroke machinery that drove it. It was
  a Mechanical Press's geometry on a block that is not shaped like one.
- Block and item summaries hide behind Shift with a "Hold [Shift]" prompt, via Create's
  `TooltipHelper`.
- Player-facing text is much shorter. Lang values are down 26% overall, goggle summaries 44%, JEI
  pages 47%, ponder captions 35%. The Chamber's Ponder scene and tooltip no longer claim it prints
  drops, which stopped being true when the Fabricator landed.
- The advancement tree tiles polished deepslate instead of the Data Cable texture, which is a pipe
  and seamed in a grid across the screen.

### Fixed
- **Every machine was unharvestable.** All four call `requiresCorrectToolForDrops()` and the mod
  shipped no block tags, so no tool was ever correct and mining any of them dropped nothing. They
  are now in `minecraft:mineable/pickaxe`, with a test per machine.
- **The Loot Fabricator's item capability was never registered.** No funnel, hopper, belt or pipe
  could put a Prediction in or take a drop out, so the intended build could not be plumbed. Tests
  all passed because they reached into the block entity's handlers directly; one now goes through
  the capability.
- An outbound funnel jammed on the Fabricator's Prediction slot.
- The Fabricator's goggle tooltip told every player that every subject never drops what the machine
  was set to make. Pricing ran server-side only, so the client always fell into the no-match branch.
  The price is now synced, and a real price, an impossible target and nothing loaded read as three
  different things.
- The Loot Fabricator never lit up. It declared a `lit` property, shipped lit blockstate variants
  and an emissive model, and nothing set it. It now lights whenever it can stamp.
- The Fabricator's paging arrows flashed for a frame or two on every screen open.
- The Neural Node changed its lit state with a raw `setBlock`. It is a `KineticBlockEntity`, and a
  plain `setBlock` can cost a kinetic block its network association, which surfaces as a driven node
  intermittently reporting no speed. It uses Create's `switchToBlockState`.

## [1.0.0-beta.1] - 2026-07-17

First beta. The full loop is playable: bind and hunt a mob, train its model, simulate it for loot,
grade the model up, climb the substrate tiers, and close the boss catalyst economy.

### Added
- Project scaffold targeting NeoForge 1.21.1 and Create 6.x.
- `Neural Node`: a kinetic consumer with a back shaft input that turns rotation into a compute rate.
  Animated drive front that only lights while driven. Registered with a Create stress impact.
- `Mainframe Controller`: the brain of a Cognition Array. Flood-fills through Data Cables and
  contiguous Neural Nodes, pools their compute, and splits it across consumers proportionally to
  demand when oversubscribed. Goggles and right-click report CU/t and driven node counts.
- `Data Cable`: connection-aware pipe that extends an array and links remote consumers.
- `Simulation Chamber`: holds a Data Matrix, trains it with compute, then runs simulation jobs that
  print the subject's loot with zero spawned entities. Loot rolls from the mob's live loot table at
  runtime, so drops injected by other mods are simulated too. 9-slot output buffer with a pull-only
  item face plus a push-only substrate slot. Stalls instead of spilling when out of substrate,
  blocked by grade, or full.
- `Blank Data Matrix`: binds to the first mob defeated while held in the offhand, gathers data from
  kills, and trains in the chamber. Sequenced Assembly recipe (brass sheet, polished rose quartz,
  electron tube, press, 2 loops) with `Incomplete Data Matrix` as the transitional item.
- Model grades: trained matrices keep learning from jobs and kills, climbing Coarse, Tuned, Deep and
  Self-Aware. Higher grades run jobs cheaper; Self-Aware adds a bonus loot roll.
- Simulation accuracy by grade: a botched job spends its compute and substrate and prints a
  `Corrupted Imprint`, which crushes back into pulp. Models still learn from failures.
- Substrate tiers: `Crude`, `Refined` and `Pristine Imprint Blanks`. Higher grades print more rolls
  per job; only pristine holds a boss-grade subject (the `c:bosses` entity tag). Every tier is a
  basin compacting recipe, so the chain automates off one press with nothing passing through a
  crafting grid. Crude presses from Create pulp and clay; refined adds polished rose quartz;
  pristine takes an amethyst cluster, an echo shard, or a Resonant Catalyst, in ascending yield. The
  amethyst route keeps the boss tier renewable without ancient city loot.
- `Resonant Catalyst`: a chance print from boss simulations. Compacting it with refined blanks is
  the highest-yield route to pristine substrate.
- Compute demand scales with the work. Finer substrate and boss-grade subjects draw proportionally
  more per tick and cost more in total, so array size decides what you can simulate, not just how
  many chambers you can feed.
- Chambers overclock. Surplus array output above a chamber's rated draw speeds it up at diminishing
  returns, so scaling an array up is worthwhile without beating a second chamber. The controller
  covers every rated demand first, so nothing runs hot while a neighbour starves.
- Training and job costs raised now that surplus compute has somewhere to go.
- A Data Matrix alone in a crafting grid comes back blank, so one bound by a stray kill is
  recoverable.
- Training progress lives on the matrix, so pulling one out mid-train no longer bins the compute.
- Sneak-clicking a chamber with an empty hand takes the substrate back out once the matrix is gone,
  and a tier it will not accept says so instead of doing nothing.
- Simulations are credited to a fake player, so `killed_by_player` drops behave: a blaze model
  prints blaze rods, a wither skeleton model prints skulls.
- Simulation loot overrides at `simulacra:simulation/<mob>`. The Wither and the Ender Dragon hand
  out rewards in death logic rather than a loot table, so they now print nether stars and dragon's
  breath. Any datapack can override or add a mob.
- Advancement tree: 9 advancements from first machine to the closed catalyst economy.
- NeoForge config (`simulacra-common.toml`) for every balance value.
- Create-native recipes for all machines and items; JEI on the dev runtime.
- Consistent glossary: the node network is the Cognition Array, compute rate is CU/t, and a matrix's
  mob is its Subject.
- Boss kills grant extra data (default 16), so a boss model trains in a handful of kills.
- Comparator output on the Simulation Chamber reads the loot buffer fill level.
- Summary tooltips on all four machine blocks.

### Known gaps
- Simulation Chamber has no GUI; interaction is right-click and goggle based.
- No JEI category for simulation yields; no Ponder scenes yet.
- Boss data collection requires defeating the boss with a matrix in the offhand.
