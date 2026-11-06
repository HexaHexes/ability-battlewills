package org.bitwisemadness.abilitytensura.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.abilitycore.AbilityCore;
import org.bitwisemadness.abilitycore.api.AvailabilityProviders;
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
}
