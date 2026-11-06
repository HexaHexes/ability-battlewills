package org.bitwisemadness.abilitytensura;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.bitwisemadness.abilitytensura.activation.TensuraActivation;
import org.bitwisemadness.abilitytensura.catalog.BattlewillCatalog;
import org.bitwisemadness.abilitytensura.catalog.TensuraAbilityCatalog;
import org.bitwisemadness.abilitytensura.gate.UnlockGate;

/**
 * The mod-bus listener half of Ability: Tensura. Routing is not spelled
 * out: {@code AutomaticEventSubscriber} places each listener from its event
 * type - {@code RegisterEvent} lands on the mod bus, the rest on the game
 * bus - so only the event type here carries meaning.
 *
 * <p>Registration mirrors the extension-point-1 pattern (core's
 * {@code docs/api-surface.md}): a listener for {@code RegisterEvent} that
 * contributes to {@code AbilityRegistries.ABILITY_REGISTRY_KEY}. The
 * unguarded shape matches ability-iss's and pseudocore-curios's proven
 * listeners (observed running across every registry event in the pack's
 * logs without key-mismatch complaints).</p>
 */
@EventBusSubscriber(modid = AbilityTensura.MOD_ID)
public final class AbilityTensuraEvents {

    private AbilityTensuraEvents() {
    }

    /**
     * Contributes the catalogs to core's registry: the battlewills (#58)
     * and the dynamic skill/magic enumeration (#42 slice 1).
     */
    @SubscribeEvent
    public static void onRegisterAbilities(RegisterEvent event) {
        BattlewillCatalog.register(event);
        TensuraAbilityCatalog.register(event);
    }

    /**
     * Registers the unlock gate and the activation handlers after every
     * catalog id is tracked (the registry phase completes before common
     * setup): eligibility rule, availability declarations, the
     * REMOVE_SKILL listener, and one shared NATIVE handler per ability.
     */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        UnlockGate.register();
        TensuraActivation.registerHandlers();
    }

    /**
     * Login sweep of the removal directive: unlearnt assignments leave the
     * player before their first snapshot reaches the client.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UnlockGate.reconcile(player);
        }
    }
}
