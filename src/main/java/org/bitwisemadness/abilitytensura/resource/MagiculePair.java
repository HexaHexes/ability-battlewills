package org.bitwisemadness.abilitytensura.resource;

import net.minecraft.server.level.ServerPlayer;

/**
 * The typed half of the magicule pair (issue #41): configuration over the
 * shared {@link ExistencePair} (#49). This class is resolved ONLY when
 * {@link TensuraResources}' gate saw the mod present - its verification
 * (which needs the core and Tensura types) runs on class load, so it must
 * never load when either is absent (the Ley Lines bridge's rule, core#96).
 */
final class MagiculePair {

    /** Id, log label, verdict and the storage seams (the #49 collapse). */
    static final ExistencePair PAIR = new ExistencePair(
            TensuraResources.MAGICULE,
            "Magicule",
            TensuraResources::magiculeVerdict,
            player -> player == null ? null : readMagicules(player),
            MagiculePair::writeMagicules);

    private MagiculePair() {
    }

    static void declare() {
        PAIR.declare();
    }

    private static double readMagicules(ServerPlayer player) {
        return io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(player)
                .getMagicule();
    }

    private static void writeMagicules(ServerPlayer player, double value) {
        io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(player)
                .setMagicule(value);
    }
}
