package studio.akuro.simulacra.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.akuro.simulacra.Simulacra;

/**
 * Sound events the mod owns.
 *
 * <p>Simulacra ships no audio of its own. Every event here is declared in
 * {@code assets/simulacra/sounds.json} as an alias ({@code "type": "event"}) onto an existing Create
 * or vanilla sound, chosen for character rather than for the machine it was named after: a beacon's
 * drone reads as a server room, Create's mixing as the churn of a model being trained.
 *
 * <p><b>Why some sounds are here and others are not.</b> The one-shots that mark a milestone — a
 * matrix binding, a model finishing training, a job or a stamp landing — are played directly through
 * Create's {@code AllSoundEvents}, because Create's own API handles its layered playback and its
 * subtitles genuinely describe what is happening ("a press activates" when a press-like machine
 * stamps something). The events below are the ones Create has no equivalent for, or where a borrowed
 * subtitle would actively mislead: a Cognition Array humming should not caption itself as a beacon,
 * least of all on a loop the player hears for hours.
 *
 * <p>Going through our own ids also means a resource pack can retarget any of these, and dropping in
 * a bespoke {@code .ogg} later is a one-line change per entry in sounds.json with no code churn.
 *
 * <p>Neural Nodes deliberately have none: they are kinetic blocks, so Create's
 * {@code KineticBlockEntity#tickAudio} already gives them the whirring every Create machine has, and
 * players build them in racks where a second per-block loop would stack into noise.
 */
public final class ModSounds {
    private ModSounds() {}

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Simulacra.MOD_ID);

    /** Low hum from a Mainframe Controller whose array is producing compute. The bed of a base. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ARRAY_AMBIENT = register("array_ambient");
    /** An array comes online: first compute after being idle. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ARRAY_ENGAGE = register("array_engage");
    /** An array goes offline — rotation stopped, or a rival controller took ownership. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ARRAY_DISENGAGE = register("array_disengage");

    /** Iterative churn while a chamber is training a model. The longest wait in the mod. */
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
