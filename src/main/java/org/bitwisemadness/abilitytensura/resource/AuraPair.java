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
 * The typed half of the aura pair (issue #40): provider and consumer over the
 * same Tensura existence storage as magicules. Resolved ONLY behind
 * {@link TensuraResources}' gate - its verification needs the core and Tensura
 * types, so it must never load when either is absent (the MagiculePair rule).
 *
 * <p>Battlewills' native single-charge path (ADR-002) is untouched: this pair
 * serves EXTERNAL-priced abilities only.</p>
 */
final class AuraPair {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Balance source seam (package-private, tests): null for a null player
     * (fail closed as UNKNOWN), otherwise the Tensura existence storage.
     */
    static Function<ServerPlayer, Double> balanceRead =
            player -> player == null ? null : readAura(player);

    /** Balance sink seam (package-private, tests): production writes storage. */
    static BiConsumer<ServerPlayer, Double> balanceWrite = AuraPair::writeAura;

    private AuraPair() {
    }

    static void declare() {
        ResourceProviders.declare(TensuraResources.AURA, (abilityId, player, amount) -> {
            Double balance = balanceRead.apply(player);
            if (balance == null) {
                return Affordability.unknown(TensuraResources.AURA);
            }
            return TensuraResources.auraVerdict(balance, amount);
        });
        ResourceConsumers.declare(TensuraResources.AURA, new ResourceConsumer() {
            @Override
            public void charge(ResourceLocation abilityId, ServerPlayer player, Affordability snapshot) {
                double amount = snapshot.amount();
                if (amount <= 0.0) {
                    LOGGER.debug("Aura charge for {} was zero - nothing deducted", abilityId);
                    return;
                }
                Double balance = balanceRead.apply(player);
                if (balance == null) {
                    LOGGER.error("Aura charge for {} had no balance to deduct from", abilityId);
                    return;
                }
                double next = TensuraResources.balanceAfterCharge(balance, amount);
                balanceWrite.accept(player, next);
                if (next == 0.0 && balance < amount) {
                    LOGGER.warn("Aura charge for {} clamped at zero (balance {} < amount {})",
                            abilityId, balance, amount);
                } else {
                    LOGGER.debug("Aura charge for {}: {} -> {}", abilityId, balance, next);
                }
            }
        });
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
