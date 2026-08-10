# Create: Simulacra

**Farm any mob's drops without spawning a single one — powered by rotation, not electricity.**

Bind a Data Matrix to a mob by killing it. Feed the matrix to a Simulation Chamber and let a
Cognition Array think about it until the model is trained. From then on the chamber prints
Predictions of that mob, and a Loot Fabricator turns Predictions into real drops — endlessly, quietly,
and without a mob farm anywhere in your base.

Every machine runs on **rotational force**. No electricity, no cables, no other tech mod required.

---

## The loop

**1. Record.** Hold a **Blank Data Matrix** in your off hand and kill a mob. The first kill binds the
matrix to that species permanently; a few more give it enough raw data to be worth training.

**2. Think.** Build a **Cognition Array** — a rack of **Neural Nodes** on a shaft — and pool it with a
**Mainframe Controller**. Nodes turn RPM into Cognition Units. Bigger arrays run more efficiently, and
**Data Cables** carry the pool to machines that aren't next to it.

**3. Train.** A **Simulation Chamber** spends compute turning your raw data into a trained model.
Models keep learning after that, climbing from Coarse to Self-Aware — and better grades botch fewer
prints and simulate faster.

**4. Simulate.** The trained chamber prints **Predictions** onto **Imprint Blanks**. Finer substrate
yields more Predictions per job, and only Pristine substrate can hold a boss.

**5. Fabricate.** A **Loot Fabricator** — the one machine that runs on rotation instead of compute —
spends Predictions on drops. Leave it alone and it rolls the mob's real loot table, one Prediction per
roll. Pick a drop on its screen and it makes only that, priced by how rarely the mob drops it. Choosing
never beats rolling on value; it just makes the result predictable.

---

## Built for Create, not ported to it

- **Rotation all the way down.** Neural Nodes take a shaft. The Fabricator follows the Mechanical
  Press's speed curve. Stress values scale the way Create's own machines do.
- **Display Link support.** Wire a chamber or fabricator to a Display Board or Nixie tubes and read
  your subject, grade, progress and substrate from across the base.
- **Engineer's Goggles** report every machine's state, and comparators read the buffers.
- **Ponder scenes** for all five blocks, so nothing needs a wiki.
- **Sequenced Assembly** for the Data Matrix, and crushing recipes to recover failed prints.
- **Funnels, belts, hoppers and Mechanical Arms** all work with every machine's inventory.

## Works with any mob in your pack

Drops come from **live loot tables**, not a hand-written list. Any mob any mod adds is a valid subject
the moment it's installed — no datapack, no config, no compatibility patch. Boss-tagged mobs need
Pristine substrate and pay out accordingly.

## Balance you can actually change

Every number — compute rates, stress, training thresholds, model accuracy, substrate yields, job costs
— lives in the config. Pack authors can retune the whole progression without touching a recipe.

---

## Requirements

- Minecraft **1.21.1**
- **NeoForge**
- **Create 6.0.10** or later

**Optional:** JEI (item info and per-subject search).

## FAQ

**Does this need power or another tech mod?** No. Rotational force only.

**Does it spawn mobs?** Never. Nothing is summoned, killed, or simulated as an entity — the drops come
straight from the loot table.

**Will it work with modded mobs?** Yes, automatically.

**Can I automate it?** That's the point. Funnel Imprint Blanks into a chamber, funnel Predictions into
a fabricator, funnel drops into your storage. Gate the chamber with redstone when your barrels fill up.

---

*Licensed under the terms in the repository. Issues and suggestions welcome on the issue tracker.*
