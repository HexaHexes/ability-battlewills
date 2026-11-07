package org.bitwisemadness.abilitytensura.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.abilitycore.AbilityCore;
import org.bitwisemadness.abilitycore.api.AbilityUnlocks;
import org.bitwisemadness.abilitycore.api.ActivationOutcome;
import org.bitwisemadness.abilitycore.api.AvailabilityProviders;
import org.bitwisemadness.abilitycore.api.CooldownOwners;
import org.bitwisemadness.abilitycore.api.ModeChangeHandlers;
import org.bitwisemadness.abilitycore.api.ModeChangeRequest;
import org.bitwisemadness.abilitycore.discovery.AbilityDiscovery;
import org.bitwisemadness.abilitytensura.AbilityTensura;
import org.bitwisemadness.abilitytensura.catalog.BattlewillCatalog;
import org.bitwisemadness.abilitytensura.catalog.TensuraAbilityCatalog;

/**
 * Boot and catalog coverage for the integration layer (#58): the mod loads,
 * and every native battlewill ARR registers is exposed to core - the
 * mission's registry-size == integrated-count pattern, style-free (the
 * style layer rides the deferred Better Spellcasting track, #10).
 *
 * <p>Keep the {@code expectedTests} count in {@code build.gradle} in sync
 * with the number of {@code @GameTest} methods here, or the harness fails
 * the run.</p>
 */
@GameTestHolder(AbilityTensura.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AbilityTensuraGameTests {

    private AbilityTensuraGameTests() {
    }

    /** The mod is loaded and the empty structure template resolves. */
    @GameTest(template = "empty")
    public static void mod_loads(GameTestHelper helper) {
        helper.succeed();
    }

    /**
     * Coverage (#58 + #42 slice 1): the staging is real (Tensura must be on
     * the gametest runtime), ARR's category tags yield their full inventory
     * (213 skills + 119 magic, pinned), the 23 battlewills ride their own
     * catalog, and every id is registered and resolvable through core's
     * discovery with the registry holding exactly that set - a lost
     * registration or a stray entry fails the run.
     */
    @GameTest(template = "empty")
    public static void catalogRegistersAllNativeArts(GameTestHelper helper) {
        check(ModList.get().isLoaded("tensura"),
                "Tensura must be staged for the catalog coverage test (#42)");

        var skillAndMagic = TensuraAbilityCatalog.expectedIds();
        check(skillAndMagic.size() == 332,
                "the native inventory is 213 skills + 119 magic (ARR category tags), got "
                        + skillAndMagic.size());

        var expected = new java.util.HashSet<>(skillAndMagic);
        for (String path : BattlewillCatalog.paths()) {
            expected.add(BattlewillCatalog.idFor(path));
        }
        for (ResourceLocation id : expected) {
            check(AbilityDiscovery.get(id).isPresent(),
                    "catalog art must be registered and resolvable: " + id);
        }
        check(AbilityDiscovery.all().size() == expected.size(),
                "registry must hold exactly the catalogs, got "
                        + AbilityDiscovery.all().size() + " expected " + expected.size());

        // The unlock gate's display half is wired for every tracked id...
        for (ResourceLocation id : expected) {
            check(AvailabilityProviders.has(id),
                    "the unlock gate must declare availability for: " + id);
        }
        // ...its read seam is declared for every tracked id (core#117)...
        for (ResourceLocation id : expected) {
            check(AbilityUnlocks.has(id),
                    "the unlock provider must be declared for: " + id);
        }
        // ...and every tracked id defers its cooldown to ARR through
        // core's handler contract (alpha.45).
        for (ResourceLocation id : expected) {
            check(CooldownOwners.ownerOf(id) == CooldownOwners.Owner.HANDLER,
                    "the cooldown handler must be declared for: " + id);
        }
        // ...carries core's mode-change adapter for the hotbar's intent
        // (#53: the domain half runs ARR's native body)...
        for (ResourceLocation id : expected) {
            check(ModeChangeHandlers.has(id),
                    "the mode-change handler must be registered for: " + id);
        }
        // ...its activation half sits on core's canonical chain...
        check(AbilityCore.ELIGIBILITY.size() > 0,
                "the unlock gate must register an eligibility rule");
        // ...and every tracked ability carries the shared NATIVE handler
        // (#42 slice 2: one service, never per-feature calls).
        for (ResourceLocation id : expected) {
            check(AbilityCore.HANDLERS.get(id).isPresent(),
                    "the activation service must register a handler for: " + id);
        }
        helper.succeed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new net.minecraft.gametest.framework.GameTestAssertException(message);
        }
    }

    /**
     * The mode-change seam (#53): an intent against an unlearned ability
     * refuses RESTRICTED through core's vocabulary before ARR's native
     * body can run - no packet constructed, no state written, zero
     * residue by construction. The cycle parity and the not-slotted
     * refusal are ARR-domain checks carried by the issue's documented
     * in-game line (they need a real preset; the hotbar intent transport
     * arrives with hotbar#29).
     */
    @GameTest(template = "empty")
    public static void unlearnedModeChangeRefusesRestricted(GameTestHelper helper) {
        check(ModList.get().isLoaded("tensura"),
                "Tensura must be staged for the mode-change test (#53)");
        ServerPlayer player = mockPlayer(helper);
        ResourceLocation id = TensuraAbilityCatalog.expectedIds().iterator().next();
        var result = ModeChangeHandlers.run(new ModeChangeRequest(id, 0), player);
        check(result.outcome() == ActivationOutcome.REFUSED_RESTRICTED,
                "an unlearned ability must refuse RESTRICTED, got " + result.outcome());
        helper.succeed();
    }

    /** A bare server-side player - enough for reads; never logged in. */
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(java.util.UUID.randomUUID(), "ModeIntentProbe");
        return new ServerPlayer(server, helper.getLevel(), profile,
                ClientInformation.createDefault());
    }
}
