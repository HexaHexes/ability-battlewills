package org.bitwisemadness.abilitytensura.resource;

import net.minecraft.server.level.ServerPlayer;

/**
 * The typed half of the aura pair (issue #40): configuration over the shared
 * {@link ExistencePair} (#49), reading the same Tensura existence storage as
 * magicules. Resolved ONLY behind {@link TensuraResources}' gate - its
 * verification needs the core and Tensura types, so it must never load when
 * either is absent (the MagiculePair rule).
 *
 * <p>Battlewills' native single-charge path (ADR-002) is untouched: this pair
 * serves EXTERNAL-priced abilities only.</p>
 */
final class AuraPair {

    /** Id, log label, verdict and the storage seams (the #49 collapse). */
    static final ExistencePair PAIR = new ExistencePair(
            TensuraResources.AURA,
            "Aura",
            TensuraResources::auraVerdict,
            player -> player == null ? null : readAura(player),
            AuraPair::writeAura);

    private AuraPair() {
    }

    static void declare() {
        PAIR.declare();
    }

    private static double readAura(ServerPlayer player) {
        return io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(player)
                .getAura();
    }

    private static void writeAura(ServerPlayer player, double value) {
        io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(player)
                .setAura(value);
    }
}
