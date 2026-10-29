package org.bitwisemadness.tensurabattlewills.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.tensurabattlewills.TensuraBattlewills;

/**
 * Smoke test: proves the mod loads and the GameTest harness runs.
 *
 * <p>Replace this with real tests for the mod's behaviour. Keep the
 * {@code expectedTests} count in {@code build.gradle} in sync with the number
 * of {@code @GameTest} methods here, or the harness fails the run.</p>
 */
@GameTestHolder(TensuraBattlewills.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TensuraBattlewillsGameTests {

    private TensuraBattlewillsGameTests() {
    }

    /** The mod is loaded and the empty structure template resolves. */
    @GameTest(template = "empty")
    public static void mod_loads(GameTestHelper helper) {
        helper.succeed();
    }
}
