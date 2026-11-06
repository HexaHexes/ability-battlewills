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
 * The typed half of the EP pair (issue #43): provider and consumer over EP's
 * <b>direct</b> balance on the Tensura existence storage - javap showed
 * {@code getEP()}/{@code setEP()} as first-class accessors, so the issue's
 * composite (aura + magicule) premise is superseded (recorded on the issue):
 * no component reads, no split deduction, no component-provider dependency.
 * Resolved ONLY behind {@link TensuraResources}' gate - the MagiculePair rule.
 */
final class EPPair {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Balance source seam (package-private, tests): null for a null player
     * (fail closed as UNKNOWN), otherwise the Tensura existence storage.
     */
    static Function<ServerPlayer, Double> balanceRead =
            player -> player == null ? null : readEp(player);

    /** Balance sink seam (package-private, tests): production writes storage. */
    static BiConsumer<ServerPlayer, Double> balanceWrite = EPPair::writeEp;

    private EPPair() {
    }

    static void declare() {
        ResourceProviders.declare(TensuraResources.EP, (abilityId, player, amount) -> {
            Double balance = balanceRead.apply(player);
            if (balance == null) {
                return Affordability.unknown(TensuraResources.EP);
            }
            return TensuraResources.epVerdict(balance, amount);
        });
        ResourceConsumers.declare(TensuraResources.EP, new ResourceConsumer() {
            @Override
            public void charge(ResourceLocation abilityId, ServerPlayer player, Affordability snapshot) {
                double amount = snapshot.amount();
                if (amount <= 0.0) {
                    LOGGER.debug("EP charge for {} was zero - nothing deducted", abilityId);
                    return;
                }
                Double balance = balanceRead.apply(player);
                if (balance == null) {
                    LOGGER.error("EP charge for {} had no balance to deduct from", abilityId);
                    return;
                }
                double next = TensuraResources.balanceAfterCharge(balance, amount);
                balanceWrite.accept(player, next);
                if (next == 0.0 && balance < amount) {
                    LOGGER.warn("EP charge for {} clamped at zero (balance {} < amount {})",
                            abilityId, balance, amount);
                } else {
                    LOGGER.debug("EP charge for {}: {} -> {}", abilityId, balance, next);
                }
            }
        });
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
