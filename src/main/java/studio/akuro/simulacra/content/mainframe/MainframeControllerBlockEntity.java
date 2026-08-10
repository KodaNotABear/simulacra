package studio.akuro.simulacra.content.mainframe;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import studio.akuro.simulacra.SimulacraConfig;
import studio.akuro.simulacra.content.network.DataCableBlock;
import studio.akuro.simulacra.content.neuralnode.NeuralNodeBlock;
import studio.akuro.simulacra.content.neuralnode.NeuralNodeBlockEntity;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlock;
import studio.akuro.simulacra.content.simulation.SimulationChamberBlockEntity;
import studio.akuro.simulacra.index.ModBlockEntities;
import studio.akuro.simulacra.index.ModSounds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Discovers the Cognition Array by flood-fill through Data Cables, then each tick sums the compute of
 * every connected Neural Node into the pool consumers draw from.
 */
public class MainframeControllerBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    /** Safety cap so a pathological cable maze can't make the flood-fill expensive. */
    private static final int MAX_NETWORK = 4096;
    /** How often to re-walk the cable network (ticks). Topology changes rarely; the sum is per-tick. */
    private static final int RESCAN_INTERVAL = 20;
    /** How often a running array repeats its hum, matching vanilla's pacing for the beacon drone. */
    private static final int AMBIENT_INTERVAL = 80;

    private final Set<BlockPos> connectedNodes = new HashSet<>();
    private final Set<BlockPos> connectedChambers = new HashSet<>();
    private final Set<BlockPos> peerControllers = new HashSet<>();
    private int rescanCooldown = 0;

    // Synced to client for the goggle tooltip.
    private float totalCompute = 0f;
    private int nodeCount = 0;
    private int activeNodeCount = 0;
    private boolean primary = true;

    public MainframeControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAINFRAME_CONTROLLER.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (rescanCooldown-- <= 0) {
            rescanCooldown = RESCAN_INTERVAL;
            rescanNetwork();
        }

        // One network, one owner. Every controller sees the same peer set, so "lowest position wins"
        // needs no messaging between them. Without it each controller pools the same nodes and the
        // network's compute is multiplied by the number of controllers on it.
        boolean isPrimary = peerControllers.stream()
                .noneMatch(peer -> Long.compare(peer.asLong(), worldPosition.asLong()) < 0);
        if (!isPrimary) {
            goDormant();
            return;
        }

        // Each driven node contributes compute from its speed, and larger arrays are more efficient.
        // A node spinning outside any array contributes nothing: it is in no controller's node set.
        float raw = 0f;
        int active = 0;
        float computePerRpm = SimulacraConfig.COMPUTE_PER_RPM.get().floatValue();
        float idleRpm = SimulacraConfig.NODE_IDLE_RPM.get().floatValue();
        for (BlockPos nodePos : connectedNodes) {
            if (level.getBlockEntity(nodePos) instanceof NeuralNodeBlockEntity node) {
                float speed = Math.abs(node.getSpeed());
                if (speed > 0f) {
                    // Each node burns the first few RPM on itself. Stress is linear in speed, so
                    // without an overhead the compute-per-SU rate is speed-invariant and the array
                    // bonus makes many slow nodes strictly better than a few fast ones.
                    raw += Math.max(0f, speed - idleRpm) * computePerRpm;
                    active++;
                }
            }
        }
        float bonus = 1f + Math.min(Math.max(active - 1, 0), SimulacraConfig.ARRAY_BONUS_MAX_NODES.get())
                * SimulacraConfig.ARRAY_BONUS_PER_NODE.get().floatValue();
        float newTotal = raw * bonus;
        int newCount = connectedNodes.size();

        if (Math.abs(newTotal - totalCompute) > 0.01f || newCount != nodeCount || active != activeNodeCount
                || !primary) {
            totalCompute = newTotal;
            nodeCount = newCount;
            activeNodeCount = active;
            primary = true;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        boolean lit = newTotal > 0f;
        setLit(lit);
        // Offset by position so a wall of controllers does not thud in unison, and paced to match
        // how vanilla spaces the beacon drone this borrows.
        if (lit && Math.floorMod(level.getGameTime() + worldPosition.hashCode(), AMBIENT_INTERVAL) == 0) {
            playSound(ModSounds.ARRAY_AMBIENT.get(), 0.2f, 1.6f);
        }

        float totalDemand = 0f;
        List<SimulationChamberBlockEntity> consumers = new ArrayList<>();
        for (BlockPos chamberPos : connectedChambers) {
            if (level.getBlockEntity(chamberPos) instanceof SimulationChamberBlockEntity chamber) {
                consumers.add(chamber);
                totalDemand += chamber.getComputeDemand();
            }
        }

        // Two passes, so surplus is spent rather than discarded. Rated demand is covered first, split
        // proportionally when the array cannot cover it (in-order allocation would let scan order
        // starve the chambers at the back). Only leftovers go into overclock headroom, so no chamber
        // runs hot while another is starving.
        float ratio = totalDemand > newTotal && totalDemand > 0f ? newTotal / totalDemand : 1f;
        float[] granted = new float[consumers.size()];
        float spent = 0f;
        for (int i = 0; i < consumers.size(); i++) {
            granted[i] = consumers.get(i).getComputeDemand() * ratio;
            spent += granted[i];
        }

        float surplus = newTotal - spent;
        if (surplus > 0.01f) {
            float headroom = 0f;
            for (SimulationChamberBlockEntity chamber : consumers) {
                headroom += chamber.getOverclockHeadroom();
            }
            if (headroom > 0f) {
                float share = Math.min(1f, surplus / headroom);
                for (int i = 0; i < consumers.size(); i++) {
                    granted[i] += consumers.get(i).getOverclockHeadroom() * share;
                }
            }
        }

        for (int i = 0; i < consumers.size(); i++) {
            consumers.get(i).supplyCompute(granted[i]);
        }
    }

    /**
     * Stand down because another controller owns this network: produce nothing, drive nothing, go
     * dark, so the redundant controller is visibly inert rather than silently duplicating output.
     */
    private void goDormant() {
        if (totalCompute != 0f || nodeCount != 0 || activeNodeCount != 0 || primary) {
            totalCompute = 0f;
            nodeCount = 0;
            activeNodeCount = 0;
            primary = false;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setLit(false);
    }

    /**
     * Drives the dashboard light, with a cue on spin-up and wind-down: an array that stopped because
     * a shaft broke is otherwise indistinguishable from one that is merely idle.
     */
    private void setLit(boolean lit) {
        BlockState state = getBlockState();
        if (!state.hasProperty(MainframeControllerBlock.LIT)
                || state.getValue(MainframeControllerBlock.LIT) == lit) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(MainframeControllerBlock.LIT, lit), Block.UPDATE_CLIENTS);
        playSound(lit ? ModSounds.ARRAY_ENGAGE.get() : ModSounds.ARRAY_DISENGAGE.get(),
                0.3f, lit ? 1.5f : 1.3f);
    }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
    }

    /** Flood-fill outward from the controller; see the per-block cases for what conducts. */
    private void rescanNetwork() {
        connectedNodes.clear();
        connectedChambers.clear();
        peerControllers.clear();
        if (level == null) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        visited.add(worldPosition);
        frontier.add(worldPosition);

        while (!frontier.isEmpty() && visited.size() < MAX_NETWORK) {
            BlockPos current = frontier.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                Block block = level.getBlockState(next).getBlock();
                if (block instanceof DataCableBlock) {
                    visited.add(next);
                    frontier.add(next); // cables conduct
                } else if (block instanceof NeuralNodeBlock) {
                    visited.add(next);
                    connectedNodes.add(next);
                    frontier.add(next); // nodes conduct, so a contiguous rack forms one array
                } else if (block instanceof SimulationChamberBlock) {
                    visited.add(next);
                    connectedChambers.add(next); // consumers are leaves
                } else if (block instanceof MainframeControllerBlock) {
                    visited.add(next);
                    peerControllers.add(next);
                    frontier.add(next); // controllers conduct, so every one on a network sees the rest
                }
            }
        }
    }

    public float getTotalCompute() {
        return totalCompute;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getActiveNodeCount() {
        return activeNodeCount;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ")
                .append(Component.translatable("tooltip.simulacra.array"))
                .withStyle(ChatFormatting.GRAY));
        if (!primary) {
            tooltip.add(Component.literal("    ")
                    .append(Component.translatable("tooltip.simulacra.array_dormant"))
                    .withStyle(ChatFormatting.RED));
            return true;
        }
        tooltip.add(Component.literal("    ")
                .append(Component.translatable("tooltip.simulacra.array_compute", String.format("%.1f", totalCompute)))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("    ")
                .append(Component.translatable("tooltip.simulacra.array_nodes", activeNodeCount, nodeCount))
                .withStyle(ChatFormatting.AQUA));
        return true;
    }

    // --- client sync (transient pool values; the topology is recomputed on load) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putFloat("TotalCompute", totalCompute);
        tag.putInt("NodeCount", nodeCount);
        tag.putInt("ActiveNodes", activeNodeCount);
        tag.putBoolean("Primary", primary);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        totalCompute = tag.getFloat("TotalCompute");
        nodeCount = tag.getInt("NodeCount");
        activeNodeCount = tag.getInt("ActiveNodes");
        primary = !tag.contains("Primary") || tag.getBoolean("Primary");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("TotalCompute", totalCompute);
        tag.putInt("NodeCount", nodeCount);
        tag.putInt("ActiveNodes", activeNodeCount);
        tag.putBoolean("Primary", primary);
    }
}
