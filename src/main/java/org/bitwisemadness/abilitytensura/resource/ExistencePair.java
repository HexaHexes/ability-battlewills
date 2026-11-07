package org.bitwisemadness.abilitytensura.resource;

import com.mojang.logging.LogUtils;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.bitwisemadness.abilitycore.api.Affordability;
import org.bitwisemadness.abilitycore.api.ResourceConsumer;
import org.bitwisemadness.abilitycore.api.ResourceConsumers;
import org.bitwisemadness.abilitycore.api.ResourceProviders;
import org.slf4j.Logger;

/**
 * The shared gate-behind resource pair (issue #49's rule of three): one
 * provider/consumer shape over a per-pair balance, parameterised by (resource
 * id, log label, verdict, read/write accessors) with the balance seams owned
 * per instance - the three pairs collapse to configuration over this class.
 * Message text, gate structure, clamp-charge logic and seam semantics are the
 * originals verbatim; only the class-level logger name is shared now.
 *
 * <p>Resolved ONLY behind {@link TensuraResources}' gate, exactly like the
 * pairs it replaces: its verification needs core and Tensura types, so it
 * must never load when either is absent (the Ley Lines bridge's rule,
 * core#96).</p>
 */
final class ExistencePair {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Balance source seam (per pair, tests): the default answers null for a
     * null player (no entity to read - the check fails closed as UNKNOWN)
     * and otherwise reads the Tensura existence storage.
     */
    Function<ServerPlayer, Double> balanceRead;

    /** Balance sink seam (per pair, tests): production writes storage. */
    BiConsumer<ServerPlayer, Double> balanceWrite;

    private final String id;
    private final String label;
    private final BiFunction<Double, Double, Affordability> verdict;

    ExistencePair(String id, String label,
            BiFunction<Double, Double, Affordability> verdict,
            Function<ServerPlayer, Double> balanceRead,
            BiConsumer<ServerPlayer, Double> balanceWrite) {
        this.id = id;
        this.label = label;
        this.verdict = verdict;
        this.balanceRead = balanceRead;
        this.balanceWrite = balanceWrite;
    }

    void declare() {
        ResourceProviders.declare(id, (abilityId, player, amount) -> {
            Double balance = balanceRead.apply(player);
            if (balance == null) {
                // no entity to read (unit fixtures, absent storage): fail closed
                return Affordability.unknown(id);
            }
            return verdict.apply(balance, amount);
        });
        ResourceConsumers.declare(id, new ResourceConsumer() {
            @Override
            public void charge(ResourceLocation abilityId, ServerPlayer player, Affordability snapshot) {
                double amount = snapshot.amount();
                if (amount <= 0.0) {
                    LOGGER.debug(label + " charge for {} was zero - nothing deducted", abilityId);
                    return;
                }
                Double balance = balanceRead.apply(player);
                if (balance == null) {
                    LOGGER.error(label + " charge for {} had no balance to deduct from", abilityId);
                    return;
                }
                double next = TensuraResources.balanceAfterCharge(balance, amount);
                balanceWrite.accept(player, next);
                if (next == 0.0 && balance < amount) {
                    // validation passed moments ago - a concurrent drain is the
                    // only way here; never push the storage negative
                    LOGGER.warn(label + " charge for {} clamped at zero (balance {} < amount {})",
                            abilityId, balance, amount);
                } else {
                    LOGGER.debug(label + " charge for {}: {} -> {}", abilityId, balance, next);
                }
            }
        });
    }
}
