package org.bitwisemadness.abilitytensura.activation;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.AbilitySlot;
import io.github.manasmods.tensura.storage.ability.IAbility;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.bitwisemadness.abilitycore.AbilityCore;
import org.bitwisemadness.abilitycore.api.ActivationOutcome;
import org.bitwisemadness.abilitycore.api.ActivationRequest;
import org.bitwisemadness.abilitycore.api.ActivationResult;
import org.bitwisemadness.abilitycore.api.ChargingModel;
import org.bitwisemadness.abilitytensura.gate.UnlockGate;

/**
 * The single shared Tensura activation service (#42's selected approach:
 * one service, never per-feature {@code startHoldSkill} calls - drift
 * risk). Public so REQ-ISS-06 can consume it later via the cross-repo
 * contract (DEC-02-family placement decision at readiness).
 *
 * <p><b>Native parity is the contract</b> (AC2): a slot activation must
 * produce the same observable effects as ARR's own keybind. Everything
 * below was pinned by bytecode against {@code manascore-skill 4.0.0.2} /
 * {@code tensura-neoforge 2.0.1.3} (ability-tensura#52 findings, extended):</p>
 *
 * <ul>
 *   <li>{@code startHoldSkill(id, int, int)}'s two ints are
 *       <b>(keyNumber, mode)</b> - the request record
 *       {@code (keyNumber, skillId, mode)} names them; mode is
 *       bounds-checked against {@code getModes()} and indexes the per-mode
 *       cooldown; keyNumber flows into {@code onPressed}.</li>
 *   <li>Tensura's own {@code ACTIVATE_SKILL} listener
 *       ({@code AbilityHandler}) <b>interrupts unless the ability sits in
 *       the active preset at that keyNumber with that mode</b>
 *       ({@code isAbilityInActivePreset(int, skill, int)}), and ARR preset
 *       slots store exactly {@code (skill, mode)} - so parity means
 *       resolving both values <b>server-side from the player's own
 *       preset</b>, never from client input.</li>
 *   <li>ARR messages its own cooldown refusal (red chat line) - we read
 *       the same state to carry the matching outcome, no second
 *       messaging.</li>
 * </ul>
 *
 * <p>Fail-closed throughout (the direct-invocation precedent from
 * ability-iss): unlearned refuses RESTRICTED (defense in depth behind the
 * eligibility rule), not-equipped-in-preset refuses RESTRICTED (the native
 * keybind cannot fire either - parity), stale preset mode refuses
 * UNAVAILABLE, cooldown refuses COOLDOWN (ARR has messaged), and a
 * remaining {@code false} from the pipeline refuses UNAVAILABLE with
 * whatever specific reason ARR could not surface as a code (ability-iss'
 * boolean-mapping precedent; core's refusal log carries it).</p>
 *
 * <p>Mode context from ability-core#113 (per-slot mode overrides,
 * hotbar#29) is a future input: when {@code ActivationRequest} carries a
 * mode, prefer it over the preset's stored mode. Today the ARR preset's
 * own mode <i>is</i> the native semantics.</p>
 */
public final class TensuraActivation {

    private TensuraActivation() {
    }

    /**
     * Registers one shared handler per tracked ability on core's pipeline
     * (NATIVE charging: ARR owns every cost). Gated on Tensura - the
     * classes in this file are upstream types.
     */
    public static void registerHandlers() {
        if (ModList.get() == null || !ModList.get().isLoaded("tensura")) {
            return;
        }
        for (ResourceLocation id : UnlockGate.trackedIds()) {
            AbilityCore.HANDLERS.register(id, TensuraActivation::activate,
                    ChargingModel.NATIVE);
        }
    }

    /**
     * The shared activation: learn-gate, preset parity resolve, then the
     * native pipeline entry.
     */
    public static ActivationResult activate(ActivationRequest request, ServerPlayer player) {
        ResourceLocation id = request.abilityId();
        Optional<ManasSkillInstance> maybeInstance =
                SkillAPI.getSkillsFrom(player).getSkill(id);
        if (maybeInstance.isEmpty()) {
            // Defense in depth behind the eligibility rule: a direct
            // handler invocation must fail closed, never guess.
            return ActivationResult.refused(ActivationOutcome.REFUSED_RESTRICTED);
        }
        ManasSkillInstance instance = maybeInstance.get();

        int[] slotAndMode = resolvePresetSlot(player, instance);
        if (slotAndMode == null) {
            // Not in the active preset: the native keybind cannot fire
            // either (AbilityHandler interrupts) - same gate, same refusal.
            return ActivationResult.refused(ActivationOutcome.REFUSED_RESTRICTED);
        }
        int slot = slotAndMode[0];
        int mode = slotAndMode[1];
        if (mode < 0 || mode >= instance.getModes()) {
            // The preset holds a mode this skill no longer offers: stale
            // config, fail closed (startHoldSkill would reject it anyway).
            return ActivationResult.refused(ActivationOutcome.REFUSED_UNAVAILABLE);
        }
        if (instance.onCoolDown(mode)
                && !instance.canIgnoreCoolDown(player, mode)) {
            // ARR messages its own red cooldown line; carry its outcome.
            return ActivationResult.refused(ActivationOutcome.REFUSED_COOLDOWN);
        }
        if (!SkillAPI.getSkillsFrom(player).startHoldSkill(id, slot, mode)) {
            // Boolean-only domain: no stable codes for the rest (mastery,
            // policy, energy) - the ability-iss precedent carries
            // UNAVAILABLE; core's refusal log records the detail.
            return ActivationResult.refused(ActivationOutcome.REFUSED_UNAVAILABLE);
        }
        return ActivationResult.ACTED;
    }

    /**
     * The player's active-preset slot holding this instance's skill, as
     * (slotIndex, mode) - the exact pair Tensura's own listener validates;
     * null when the skill is not equipped (or storage is unavailable).
     */
    private static int[] resolvePresetSlot(ServerPlayer player, ManasSkillInstance instance) {
        IAbility ability = TensuraStorages.getAbilityFrom(player);
        if (ability == null) {
            return null;
        }
        List<AbilitySlot> slots = ability.getAbilitySlots();
        for (int i = 0; i < slots.size(); i++) {
            AbilitySlot slot = slots.get(i);
            if (slot.getSkill() == instance.getSkill()) {
                return new int[] {i, slot.getMode()};
            }
        }
        return null;
    }
}
