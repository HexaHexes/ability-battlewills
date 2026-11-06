package org.bitwisemadness.abilitytensura.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.abilitycore.discovery.AbilityDiscovery;
import org.bitwisemadness.abilitytensura.AbilityTensura;
import org.bitwisemadness.abilitytensura.catalog.BattlewillCatalog;

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
     * Coverage (#58): all 23 native arts are registered and resolvable
     * through core's discovery, and the registry holds exactly them - a
     * lost registration or a stray entry fails the run.
     */
    @GameTest(template = "empty")
    public static void battlewillCatalogRegistersAllNativeArts(GameTestHelper helper) {
        for (String path : BattlewillCatalog.paths()) {
            ResourceLocation id = BattlewillCatalog.idFor(path);
            check(AbilityDiscovery.get(id).isPresent(),
                    "catalog art must be registered and resolvable: " + id);
        }
        check(AbilityDiscovery.all().size() == BattlewillCatalog.paths().size(),
                "registry must hold exactly the catalog, got "
                        + AbilityDiscovery.all().size() + " expected "
                        + BattlewillCatalog.paths().size());
        helper.succeed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new net.minecraft.gametest.framework.GameTestAssertException(message);
        }
    }
}
