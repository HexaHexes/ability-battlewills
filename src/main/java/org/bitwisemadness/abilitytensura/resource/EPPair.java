package org.bitwisemadness.abilitytensura.resource;

import net.minecraft.server.level.ServerPlayer;

/**
 * The typed half of the EP pair (issue #43): configuration over the shared
 * {@link ExistencePair} (#49) over EP's <b>direct</b> balance on the Tensura
 * existence storage - javap showed {@code getEP()}/{@code setEP()} as
 * first-class accessors, so the issue's composite (aura + magicule) premise
 * is superseded (recorded on the issue): no component reads, no split
 * deduction, no component-provider dependency. Resolved ONLY behind
 * {@link TensuraResources}' gate - the MagiculePair rule.
 */
final class EPPair {

    /** Id, log label, verdict and the storage seams (the #49 collapse). */
    static final ExistencePair PAIR = new ExistencePair(
            TensuraResources.EP,
            "EP",
            TensuraResources::epVerdict,
            player -> player == null ? null : readEp(player),
            EPPair::writeEp);

    private EPPair() {
    }

    static void declare() {
        PAIR.declare();
    }

    private static double readEp(ServerPlayer player) {
        return io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(player)
                .getEP();
    }

    private static void writeEp(ServerPlayer player, double value) {
        io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(player)
                .setEP(value);
    }
}
