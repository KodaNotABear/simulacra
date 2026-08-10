package studio.akuro.simulacra.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.akuro.simulacra.Simulacra;

/**
 * Sound events the mod owns. Simulacra ships no audio: each event is declared in
 * {@code assets/simulacra/sounds.json} as an alias ({@code "type": "event"}) onto a Create or vanilla
 * sound. Owning the ids lets a resource pack retarget them and makes swapping in a real {@code .ogg}
 * a one-line change.
 *
 * <p>Only sounds Create has no equivalent for, or where a borrowed subtitle would mislead, live here;
 * milestone one-shots go straight through Create's {@code AllSoundEvents}. Neural Nodes get none:
 * {@code KineticBlockEntity#tickAudio} already whirs, and they are built in racks where a second
 * per-block loop would stack into noise.
 */
public final class ModSounds {
    private ModSounds() {}

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Simulacra.MOD_ID);

    /** Low hum from a Mainframe Controller whose array is producing compute. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ARRAY_AMBIENT = register("array_ambient");
    /** An array comes online: first compute after being idle. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ARRAY_ENGAGE = register("array_engage");
    /** An array goes offline — rotation stopped, or a rival controller took ownership. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ARRAY_DISENGAGE = register("array_disengage");

    /** Iterative churn while a chamber is training a model. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_TRAINING = register("chamber_training");
    /** A running chamber just became blocked. Played once on the transition, never on a loop. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_STALL = register("chamber_stall");
    /** A Data Matrix was slotted into a chamber. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_INSERT = register("chamber_insert");
    /** A Data Matrix was pulled back out. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_EJECT = register("chamber_eject");
    /** Imprint Blanks were loaded into or pulled out of the substrate slot. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_SUBSTRATE = register("chamber_substrate");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Simulacra.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
