package org.bitwisemadness.abilitytensura;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
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
     * Registers the unlock gate after every catalog id is tracked (the
     * registry phase completes before common setup): the eligibility rule
     * and the per-id availability declarations.
     */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        UnlockGate.register();
    }
}
