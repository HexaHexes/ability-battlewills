package org.bitwisemadness.abilitytensura.resource;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.bitwisemadness.abilitycore.api.Affordability;
import org.slf4j.Logger;

/**
 * First-tier Tensura resource pairs (issues #40 aura, #41 magicule, #43 EP).
 *
 * <p>Magicule (issue #41): the pair for Tensura's current magicules - balance
 * from {@code TensuraStorages.getExistenceFrom(player).getMagicule()} and the
 * deduction through the same storage (SPIKE-iss-3 section 3: no "current"
 * attribute exists; the attribute {@code tensura:max_magicule} is capacity and
 * is NOT the spendable balance). Resource id {@link #MAGICULE} mirrors the
 * domain, like the Ley Lines bridge's currency-id rule (core#96).</p>
 *
 * <p><b>Gate before touch - method-level isolation (the lesson core#96's
 * bridge learned the hard way):</b> this class contains no {@code new} of a
 * core or Tensura type, so loading it (constructor time, mod absent) never
 * needs those classes. Everything typed lives behind
 * {@code MagiculePair.declare()}, whose class is resolved only when the gate
 * sees the mod present - verification of a method runs on entry, so an
 * in-method gate would be too late. Absence skips with a debug line and the
 * #92 chain owns downstream behaviour: {@code has()} false, magicule costs
 * refuse {@code no_provider}.</p>
 *
 * <p>Declared from {@link org.bitwisemadness.abilitytensura.AbilityTensura}'s
 * constructor (mod construction - before {@code RegisterEvent}, so
 * registration-time cost decisions are order-safe).</p>
 */
public final class TensuraResources {

    /** The magicule resource id (pseudocore#174 queries this pair by name). */
    public static final String MAGICULE = "tensura:magicule";

    /** The aura resource id (battlewill prices; issue #40). */
    public static final String AURA = "tensura:aura";

    static final String REQUIRED_MOD_ID = "tensura";

    private static final Logger LOGGER = LogUtils.getLogger();

    private TensuraResources() {
    }

    /**
     * Declares the aura provider/consumer pair (issue #40): current aura from
     * the same Tensura existence storage - no ManasCore type appears anywhere
     * in the signature chain, so ADR-010's GPLv3 linkage question does not
     * trigger for this pair.
     *
     * @return true when declared, false when skipped (Tensura absent)
     */
    public static boolean declareAura() {
        return declareAura(isTensuraLoaded());
    }

    /** Presence-resolved seam (package-private, tests), as for magicules. */
    static boolean declareAura(boolean tensuraPresent) {
        if (!tensuraPresent) {
            LOGGER.debug("Aura pair '{}' skipped: {} is not loaded", AURA, REQUIRED_MOD_ID);
            return false;
        }
        AuraPair.declare();
        return true;
    }

    /**
     * Declares the magicule provider/consumer pair.
     *
     * @return true when declared, false when skipped (Tensura absent)
     */
    public static boolean declareMagicules() {
        return declareMagicules(isTensuraLoaded());
    }

    /**
     * Presence-resolved seam (package-private, tests): the gate without
     * touching ModList - fixtures pass the resolved boolean directly, so no
     * test ever boots FML.
     */
    static boolean declareMagicules(boolean tensuraPresent) {
        if (!tensuraPresent) {
            LOGGER.debug("Magicule pair '{}' skipped: {} is not loaded",
                    MAGICULE, REQUIRED_MOD_ID);
            return false;
        }
        MagiculePair.declare();
        return true;
    }

    /** Amount-vs-balance verdict for aura (fixtures): pure, as for magicules. */
    static Affordability auraVerdict(double balance, double amount) {
        return balance >= amount
                ? Affordability.sufficient(AURA, amount)
                : Affordability.insufficient(AURA, amount);
    }

    /** Amount-vs-balance verdict: the pure half of the read side (fixtures). */
    static Affordability magiculeVerdict(double balance, double amount) {
        return balance >= amount
                ? Affordability.sufficient(MAGICULE, amount)
                : Affordability.insufficient(MAGICULE, amount);
    }

    /**
     * New balance after charging {@code amount}: exact deduction, clamped at
     * zero (a concurrent drain between validation and charge can never push
     * the storage negative - their storage is a plain double).
     */
    static double balanceAfterCharge(double balance, double amount) {
        return Math.max(0.0, balance - amount);
    }

    private static boolean isTensuraLoaded() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(REQUIRED_MOD_ID);
    }
}
