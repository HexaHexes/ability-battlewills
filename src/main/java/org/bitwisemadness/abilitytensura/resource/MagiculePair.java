package org.bitwisemadness.abilitytensura.resource;

import com.mojang.logging.LogUtils;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.bitwisemadness.abilitycore.api.Affordability;
import org.bitwisemadness.abilitycore.api.ResourceConsumer;
import org.bitwisemadness.abilitycore.api.ResourceConsumers;
import org.bitwisemadness.abilitycore.api.ResourceProviders;
import org.slf4j.Logger;

/**
 * The typed half of the magicule pair (issue #41): provider and consumer
 * lambdas over the Tensura existence storage. This class is resolved ONLY
 * when {@link TensuraResources}' gate saw the mod present - its verification
 * (which needs the core and Tensura types) runs on class load, so it must
 * never load when either is absent (the Ley Lines bridge's rule, core#96).
 */
final class MagiculePair {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Balance source seam (package-private, tests): the default answers null
     * for a null player (no entity to read - the check fails closed as
     * UNKNOWN) and otherwise reads the Tensura existence storage.
     */
    static Function<ServerPlayer, Double> balanceRead =
            player -> player == null ? null : readMagicules(player);

    /** Balance sink seam (package-private, tests): production writes storage. */
    static BiConsumer<ServerPlayer, Double> balanceWrite = MagiculePair::writeMagicules;

    private MagiculePair() {
    }

    static void declare() {
        ResourceProviders.declare(TensuraResources.MAGICULE, (abilityId, player, amount) -> {
            Double balance = balanceRead.apply(player);
            if (balance == null) {
                // no entity to read (unit fixtures, absent storage): fail closed
                return Affordability.unknown(TensuraResources.MAGICULE);
            }
            return TensuraResources.magiculeVerdict(balance, amount);
        });
        ResourceConsumers.declare(TensuraResources.MAGICULE, new ResourceConsumer() {
            @Override
            public void charge(ResourceLocation abilityId, ServerPlayer player, Affordability snapshot) {
                double amount = snapshot.amount();
                if (amount <= 0.0) {
                    LOGGER.debug("Magicule charge for {} was zero - nothing deducted", abilityId);
                    return;
                }
                Double balance = balanceRead.apply(player);
                if (balance == null) {
                    LOGGER.error("Magicule charge for {} had no balance to deduct from", abilityId);
                    return;
                }
                double next = TensuraResources.balanceAfterCharge(balance, amount);
                balanceWrite.accept(player, next);
                if (next == 0.0 && balance < amount) {
                    // validation passed moments ago - a concurrent drain is the
                    // only way here; never push the storage negative
                    LOGGER.warn("Magicule charge for {} clamped at zero (balance {} < amount {})",
                            abilityId, balance, amount);
                } else {
                    LOGGER.debug("Magicule charge for {}: {} -> {}", abilityId, balance, next);
                }
            }
        });
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
