# Create: Simulacra

A [Create](https://www.curseforge.com/minecraft/mc-mods/create) add-on for **NeoForge 1.21.1**.
(Mod id: `simulacra`.)

Create: Simulacra turns surplus rotation into a new resource, **compute**. Gather data from mobs,
spend compute to train models of them, then run those models to **get the mob's loot with nothing
spawned**. No AI, no pathfinding, no spawn caps, and bosses are on the menu.

It gives Create's late game something to spend SU on.

See [DESIGN.md](DESIGN.md) for the concept document and [CHANGELOG.md](CHANGELOG.md) for what is
actually in the mod.

## Status

Beta (`1.0.0-beta.1`). The full loop is playable: bind a mob, train its model, simulate it for loot,
grade the model up, climb the substrate tiers, close the boss catalyst economy. Balance values live
in `simulacra-common.toml` and are the focus of the beta.

## Building

Requires JDK 21.

```bash
./gradlew build              # produces build/libs/simulacra-<version>.jar
./gradlew runClient          # launch a dev client with Create and JEI present
./gradlew runServer          # launch a dev dedicated server
./gradlew runGameTestServer  # run the GameTests headlessly; fails the build on a regression
```

Assets and data are hand-authored under `src/main/resources`. A `runData` config exists but no
`GatherDataEvent` providers are registered, so it generates nothing. See [ROADMAP.md](ROADMAP.md).

The first build downloads NeoForge, Create, Flywheel, Ponder and Registrate, so it takes a while and
needs network access.

### Dependency versions

Pinned in `gradle.properties`, mirrored from a known-good Create 6.x / NeoForge 1.21.1 build:

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.233 |
| Create | 6.0.10-281 |
| Flywheel | 1.0.6 |
| Ponder | 1.0.82 |
| Registrate | MC1.21-1.3.0+67 |

If a build fails to resolve Create, check [the Create "depend on Create" wiki page](https://wiki.createmod.net/developers/depend-on-create/neoforge-1.21.1)
for the current `create_version` and bump the pin.

## The loop

Two resources, and the split is the point: **compute decides what you can simulate, rotation decides
how fast you turn the results into items.**

1. **Build a Cognition Array.** Neural Nodes are kinetic blocks with a shaft on the back. They draw
   stress and turn rotation into compute. A lone node makes nothing. A **Mainframe Controller** pools
   every driven node it can reach, and bigger arrays are more efficient. **Data Cables** extend the
   array to remote machines. One network, one controller. Extras go dormant.
2. **Bind a model.** Hold a **Blank Data Matrix** in your off hand and kill a mob. It binds to that
   mob and gathers data from further kills. Bosses are worth far more per kill.
3. **Train it.** Drop the matrix into a **Simulation Chamber** and feed it compute.
4. **Simulate.** Each job spends compute and an Imprint Blank to print **Predictions**. One
   Prediction is one roll of that mob's loot table, rolled live, so drops other mods inject are
   included.
5. **Spend them.** Feed Predictions to a **Loot Fabricator**. A funnel in, a funnel out. Left alone
   it rolls the loot table and keeps whatever comes. Open it to pick a drop and it makes only that,
   priced by how rarely the mob drops it, so rarity survives being able to choose. It runs on
   rotation, on the Mechanical Press's speed curve, so a line can be sized on paper.
6. **Scale.** Models grade up through use (Coarse to Tuned to Deep to Self-Aware), running cheaper
   and more accurately. Better substrate raises yield per job, only **Pristine** blanks can hold a
   boss, and boss simulations can print a **Resonant Catalyst** that makes Pristine renewable.

Hold **W** over any machine for its Ponder scene.

## License

MIT. See [LICENSE](LICENSE).

## Credits

Create team for the mod this builds on. The "train a data model, simulate the mob for loot" loop is
inspired by **Deep Mob Learning** and its successor **Hostile Neural Networks**, and the matrix,
chamber, Prediction and Fabricator chain follows theirs deliberately. What is new is running it on
rotation: a Create array makes the compute that decides what you can simulate, and a shaft decides
how fast you stamp the results out.
