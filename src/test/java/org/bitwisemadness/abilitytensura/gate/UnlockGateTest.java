package org.bitwisemadness.abilitytensura.gate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.bitwisemadness.abilitycore.api.ActivationOutcome;
import org.bitwisemadness.abilitycore.api.ActivationResult;
import org.junit.jupiter.api.Test;

/**
 * The unlock gate's decision core (owner directive 2026-11-06): a tracked
 * ability the player has not learned refuses RESTRICTED; everything else -
 * learned abilities and ids outside our catalogs - passes untouched. Pure,
 * fixture-shaped after the hotbar's {@code CoreScopeCompat.refusalFor}.
 */
class UnlockGateTest {

    private static final ResourceLocation OURS =
            ResourceLocation.fromNamespaceAndPath("tensura", "aura_slash");
    private static final ResourceLocation NOT_OURS =
            ResourceLocation.fromNamespaceAndPath("someone", "theirs");

    @Test
    void unlearnedTrackedAbilityRefusesRestricted() {
        UnlockGate.track(OURS);
        Optional<ActivationResult> refusal = UnlockGate.refusalFor(OURS, false);
        assertTrue(refusal.isPresent(), "a tracked unlearned ability must refuse");
        assertTrue(refusal.get().outcome() == ActivationOutcome.REFUSED_RESTRICTED,
                "the refusal must be RESTRICTED, got " + refusal.get().outcome());
    }

    @Test
    void learnedTrackedAbilityPasses() {
        UnlockGate.track(OURS);
        assertTrue(UnlockGate.refusalFor(OURS, true).isEmpty(),
                "a learned ability must never be gated");
    }

    @Test
    void foreignAbilityIsNeverGated() {
        assertTrue(UnlockGate.refusalFor(NOT_OURS, false).isEmpty(),
                "ids outside our catalogs are not ours to gate, learned or not");
    }
}
