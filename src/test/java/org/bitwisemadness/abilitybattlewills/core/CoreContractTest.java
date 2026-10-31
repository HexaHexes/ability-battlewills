package org.bitwisemadness.abilitybattlewills.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bitwisemadness.abilitycore.api.ActivationSource;
import org.junit.jupiter.api.Test;

/**
 * Proves this mod compiles and runs against the published
 * {@code org.bitwisemadness.abilitycore:ability_core} artifact - the classpath
 * entry this test needs comes only from the GitHub Packages dependency.
 *
 * <p>Battlewills activate through the core pipeline as native adapters, so the
 * {@code NATIVE_ADAPTER} mapping and the fail-closed {@code unknown -> null}
 * rule are part of the contract this mod relies on.</p>
 */
class CoreContractTest {

    @Test
    void nativeAdapterSourceMapsFromItsWireNames() {
        assertEquals(ActivationSource.NATIVE_ADAPTER, ActivationSource.fromWire("native_adapter"));
        assertEquals(ActivationSource.NATIVE_ADAPTER, ActivationSource.fromWire("adapter"));
    }

    @Test
    void unknownSourceFailsClosedToNull() {
        assertNull(ActivationSource.fromWire("not_a_source"));
        assertNull(ActivationSource.fromWire(null));
    }

    @Test
    void wireNameRoundTrips() {
        for (ActivationSource source : ActivationSource.values()) {
            assertEquals(source, ActivationSource.fromWire(source.wireName()),
                    "wireName() must round-trip through fromWire() for " + source);
        }
    }
}
