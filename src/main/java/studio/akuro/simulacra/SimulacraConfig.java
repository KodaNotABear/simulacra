package studio.akuro.simulacra;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * End-user tuning, exposed as a NeoForge config spec ({@code simulacra-common.toml}).
 *
 * <p>Any number that affects balance lives here rather than as a constant in game logic. Values are
 * read live via {@code .get()} at the point of use. COMMON, so it loads early enough for stress
 * registration at common setup.
 */
public final class SimulacraConfig {
    private SimulacraConfig() {}

    public static final ModConfigSpec SPEC;

    // --- Compute generation (the Cognition Array) ---
    /** Base compute/tick from one driven Neural Node per unit of |rotation speed| (RPM). */
    public static final ModConfigSpec.DoubleValue COMPUTE_PER_RPM;
    /** Per-node efficiency bonus for a larger array (Steam-Engine style: bigger structure, more output). */
    public static final ModConfigSpec.DoubleValue ARRAY_BONUS_PER_NODE;
    /** Node count past which the array bonus stops growing. */
    public static final ModConfigSpec.IntValue ARRAY_BONUS_MAX_NODES;
    /** Create stress impact of a Neural Node (SU drawn per RPM). Comparable machines sit at 4-8. */
    public static final ModConfigSpec.DoubleValue NEURAL_NODE_STRESS_IMPACT;

    // --- Simulation Chamber ---
    /** Compute/tick a chamber requests from the array while it has work it can do. */
    public static final ModConfigSpec.DoubleValue CHAMBER_COMPUTE_DEMAND;
    /** Total compute to train a raw Data Matrix into a finished model. */
    public static final ModConfigSpec.DoubleValue TRAIN_COST;
    /** Total compute per simulation job (one batch of loot). */
    public static final ModConfigSpec.DoubleValue JOB_COST;
    /** Mob kills (data) a Data Matrix must gather before it can be trained. */
    public static final ModConfigSpec.IntValue DATA_TO_TRAIN;
    /** Imprint blanks consumed per simulation job. */
    public static final ModConfigSpec.IntValue SUBSTRATE_PER_JOB;
    /** Loot-table rolls per job, by substrate grade. */
    public static final ModConfigSpec.IntValue LOOT_ROLLS_CRUDE;
    public static final ModConfigSpec.IntValue LOOT_ROLLS_REFINED;
    public static final ModConfigSpec.IntValue LOOT_ROLLS_PRISTINE;
    /** Chance per boss job to also print a Resonant Catalyst. */
    public static final ModConfigSpec.DoubleValue CATALYST_CHANCE;

    // --- Data model progression ---
    /** Total data thresholds for model grades 2-4 (Tuned / Deep / Self-Aware). */
    public static final ModConfigSpec.IntValue MODEL_DATA_TIER2;
    public static final ModConfigSpec.IntValue MODEL_DATA_TIER3;
    public static final ModConfigSpec.IntValue MODEL_DATA_TIER4;
    /** Job cost reduction per grade above Coarse. */
    public static final ModConfigSpec.DoubleValue MODEL_SPEED_BONUS;
    /** Extra loot rolls at Self-Aware grade. */
    public static final ModConfigSpec.IntValue SELF_AWARE_BONUS_ROLLS;
    /** Data granted per kill of a boss-tagged mob (regular mobs grant 1). */
    public static final ModConfigSpec.IntValue BOSS_KILL_DATA;
    /** Simulation accuracy by model grade; a failed job prints a Corrupted Imprint instead of loot. */
    public static final ModConfigSpec.DoubleValue ACCURACY_COARSE;
    public static final ModConfigSpec.DoubleValue ACCURACY_TUNED;
    public static final ModConfigSpec.DoubleValue NODE_IDLE_RPM;
    public static final ModConfigSpec.DoubleValue SUBSTRATE_DEMAND_REFINED;
    public static final ModConfigSpec.DoubleValue SUBSTRATE_DEMAND_PRISTINE;
    public static final ModConfigSpec.DoubleValue BOSS_DEMAND_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue OVERCLOCK_MAX;
    public static final ModConfigSpec.DoubleValue OVERCLOCK_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue FABRICATOR_STRESS_IMPACT;
    public static final ModConfigSpec.DoubleValue FABRICATOR_WORK;
    public static final ModConfigSpec.DoubleValue FABRICATOR_PRICE_BASE;
    public static final ModConfigSpec.IntValue FABRICATOR_SAMPLES;
    public static final ModConfigSpec.DoubleValue ACCURACY_DEEP;
    public static final ModConfigSpec.DoubleValue ACCURACY_SELF_AWARE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Compute generation: the Cognition Array of Neural Nodes feeding a Mainframe Controller.")
                .push("compute");
        COMPUTE_PER_RPM = builder
                .comment("Compute per tick from one driven Neural Node, per unit of rotation speed (RPM).",
                        "Set so the whole chain lands on whole numbers: a node makes 5 CU/t at 32 RPM, four",
                        "nodes make the 20 CU/t one Simulation Chamber draws, and that chamber feeds exactly",
                        "one Loot Fabricator. Four nodes drive one Fabricator at any speed; the chambers",
                        "between them are RPM over 32.")
                .defineInRange("computePerRpm", 0.15625, 0.0, 1024.0);
        ARRAY_BONUS_PER_NODE = builder
                .comment("Extra array efficiency per additional driven node. Total = sum(node base) * (1 + min(active-1, max) * this).",
                        "Off by default. It multiplied the array's total by a different amount at every size,",
                        "which made whole-number ratios impossible - three nodes came to 0.99 of a chamber and",
                        "no count ever landed exactly. Packs that would rather have the bonus than the",
                        "arithmetic can turn it back on.")
                .defineInRange("arrayBonusPerNode", 0.0, 0.0, 10.0);
        ARRAY_BONUS_MAX_NODES = builder
                .comment("Node count past which the array bonus stops growing.")
                .defineInRange("arrayBonusMaxNodes", 20, 1, 4096);
        NODE_IDLE_RPM = builder
                .comment("Rotation a node spends running itself before it produces anything.",
                        "Off by default, because subtracting a fixed cost makes output non-proportional to",
                        "speed: doubling RPM would more than double a node, and the ratios would only hold at",
                        "one speed instead of all of them. Width is not free regardless - stress scales with",
                        "nodes times RPM, so eight nodes at 16 RPM cost exactly what four at 32 do.")
                .defineInRange("nodeIdleRpm", 0.0, 0.0, 256.0);
        NEURAL_NODE_STRESS_IMPACT = builder
                .comment("Create stress impact of a Neural Node (SU drawn per RPM).")
                .defineInRange("neuralNodeStressImpact", 8.0, 0.0, 4096.0);
        builder.pop();

        builder.comment("Simulation Chamber: training Data Matrices and running simulations for loot.")
                .push("simulation");
        CHAMBER_COMPUTE_DEMAND = builder
                .comment("Compute per tick a chamber draws from the array while it has work to do.")
                .defineInRange("computeDemand", 20.0, 0.0, 100000.0);
        SUBSTRATE_DEMAND_REFINED = builder
                .comment("Compute demand multiplier while printing onto refined substrate.")
                .defineInRange("substrateDemandRefined", 1.5, 0.1, 100.0);
        SUBSTRATE_DEMAND_PRISTINE = builder
                .comment("Compute demand multiplier while printing onto pristine substrate.")
                .defineInRange("substrateDemandPristine", 2.5, 0.1, 100.0);
        BOSS_DEMAND_MULTIPLIER = builder
                .comment("Compute demand multiplier for a boss-grade subject (the c:bosses entity tag).",
                        "Stacks with the substrate multiplier, so a simulated Wither is what an array is for.")
                .defineInRange("bossDemandMultiplier", 4.0, 1.0, 1000.0);
        OVERCLOCK_MAX = builder
                .comment("Most compute a chamber will accept, as a multiple of its rated demand.",
                        "Surplus array output is spent here instead of being discarded, so a bigger array",
                        "makes each chamber faster rather than only letting you run more of them.")
                .defineInRange("overclockMax", 3.0, 1.0, 100.0);
        OVERCLOCK_EFFICIENCY = builder
                .comment("How much of the compute above rated demand actually counts, 0 to 1.",
                        "Below 1 overclocking has diminishing returns, so scaling up is worthwhile but",
                        "never strictly better than building a second chamber.")
                .defineInRange("overclockEfficiency", 0.5, 0.0, 1.0);
        TRAIN_COST = builder
                .comment("Total compute to train a raw Data Matrix into a finished model.")
                .defineInRange("trainCost", 40000.0, 1.0, 1.0E9);
        JOB_COST = builder
                .comment("Total compute per simulation job (one batch of loot).")
                .defineInRange("jobCost", 6000.0, 1.0, 1.0E9);
        DATA_TO_TRAIN = builder
                .comment("Mob kills a Data Matrix must gather (in the offhand) before it can be trained.",
                        "Deliberately short. This is the only part of the loop the player has to do by",
                        "hand, and it sits before they have seen any of the automation the mod is about;",
                        "the long haul belongs to the grade thresholds above, which a chamber grinds out.")
                .defineInRange("dataToTrain", 8, 1, 100000);
        SUBSTRATE_PER_JOB = builder
                .comment("Imprint blanks consumed per simulation job.")
                .defineInRange("substratePerJob", 1, 1, 64);
        LOOT_ROLLS_CRUDE = builder
                .comment("Loot-table rolls per job on crude substrate.")
                .defineInRange("lootRollsCrude", 1, 1, 16);
        LOOT_ROLLS_REFINED = builder
                .comment("Loot-table rolls per job on refined substrate.")
                .defineInRange("lootRollsRefined", 2, 1, 16);
        LOOT_ROLLS_PRISTINE = builder
                .comment("Loot-table rolls per job on pristine substrate (the only grade that can simulate bosses).")
                .defineInRange("lootRollsPristine", 3, 1, 16);
        CATALYST_CHANCE = builder
                .comment("Chance per boss simulation job to also print a Resonant Catalyst (the renewable route to pristine substrate).")
                .defineInRange("catalystChance", 0.25, 0.0, 1.0);
        builder.pop();

        builder.comment("Data model progression: trained matrices keep learning from completed simulation jobs.")
                .push("models");
        MODEL_DATA_TIER2 = builder
                .comment("Total data for a trained model to reach Tuned grade.")
                .defineInRange("dataTier2", 128, 2, 1000000);
        MODEL_DATA_TIER3 = builder
                .comment("Total data for a trained model to reach Deep grade.")
                .defineInRange("dataTier3", 384, 3, 1000000);
        MODEL_DATA_TIER4 = builder
                .comment("Total data for a trained model to reach Self-Aware grade.")
                .defineInRange("dataTier4", 1024, 4, 1000000);
        MODEL_SPEED_BONUS = builder
                .comment("Job cost reduction per model grade above Coarse (0.15 = Tuned jobs cost 15% less, Deep 30%, Self-Aware 45%).")
                .defineInRange("speedBonusPerGrade", 0.15, 0.0, 0.3);
        SELF_AWARE_BONUS_ROLLS = builder
                .comment("Extra loot rolls per job once a model reaches Self-Aware grade.")
                .defineInRange("selfAwareBonusRolls", 1, 0, 8);
        BOSS_KILL_DATA = builder
                .comment("Data granted per kill of a boss-tagged mob (c:bosses); regular mobs grant 1.",
                        "Keeps boss models trainable in a handful of kills instead of dozens.")
                .defineInRange("bossKillData", 16, 1, 10000);
        ACCURACY_COARSE = builder
                .comment("Chance a job succeeds per model grade. A failed job still spends the compute and",
                        "substrate but prints a Corrupted Imprint instead of loot (crushable back to pulp).")
                .defineInRange("accuracyCoarse", 0.70, 0.0, 1.0);
        ACCURACY_TUNED = builder
                .defineInRange("accuracyTuned", 0.85, 0.0, 1.0);
        ACCURACY_DEEP = builder
                .defineInRange("accuracyDeep", 0.94, 0.0, 1.0);
        ACCURACY_SELF_AWARE = builder
                .defineInRange("accuracySelfAware", 0.98, 0.0, 1.0);
        builder.pop();

        builder.comment("Loot Fabricator: spends Predictions on a chosen drop, driven by rotation.")
                .push("fabricator");
        FABRICATOR_STRESS_IMPACT = builder
                .comment("Create stress impact of a Loot Fabricator (SU drawn per RPM).")
                .defineInRange("fabricatorStressImpact", 8.0, 0.0, 4096.0);
        FABRICATOR_WORK = builder
                .comment("Work one fabrication takes, against the Mechanical Press speed curve.",
                        "Chosen so a whole system can be balanced rather than only throttled. A chamber on",
                        "crude substrate prints one Prediction every 300 ticks (20 CU/t against a 6000 CU",
                        "job), and at 32 RPM this value makes a Fabricator consume exactly one every 300",
                        "ticks too - so one chamber feeds one Fabricator with nothing backing up or",
                        "starving. 64 RPM doubles the Fabricator and wants two chambers; refined and",
                        "pristine substrate multiply a chamber's output by their roll count.")
                .defineInRange("fabricatorWork", 1200.0, 1.0, 1.0E9);
        FABRICATOR_PRICE_BASE = builder
                .comment("Predictions an item costs when the mob drops exactly one per kill.",
                        "Every price is this divided by the drop's measured average yield, so rarer drops",
                        "cost proportionally more and choosing what you get stays value-neutral against",
                        "simply rolling the table.")
                .defineInRange("fabricatorPriceBase", 1.0, 0.01, 1000.0);
        FABRICATOR_SAMPLES = builder
                .comment("Loot table rolls sampled per mob when measuring drop rates. Higher is more",
                        "accurate and slower, once per mob per datapack reload.")
                .defineInRange("fabricatorSamples", 2000, 100, 200000);
        builder.pop();

        SPEC = builder.build();
    }
}
