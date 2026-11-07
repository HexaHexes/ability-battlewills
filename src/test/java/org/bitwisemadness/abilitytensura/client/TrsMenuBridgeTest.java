package org.bitwisemadness.abilitytensura.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The #70 routing fixture: with Tensura RPG System present only the
 * equip-skills surface replaces (their chain plus ARR's entry); without
 * TRS nothing does (the byte-identical baseline); their shop, learning,
 * tree and hub screens are never touched (non-goals).
 */
class TrsMenuBridgeTest {

    private static final String ARR_ENTRY =
            "io.github.manasmods.tensura.client.screen.AbilitySelectionScreen";
    private static final String CATEGORY =
            "com.japaeditor.tensuraevolution.client.CategoryActionScreen";
    private static final String DIRECT_LOADOUT =
            "com.japaeditor.tensuraevolution.client.DirectSkillLoadoutScreen";
    private static final String EVOLUTION_LOADOUT =
            "com.japaeditor.tensuraevolution.client.EvolutionSkillLoadoutScreen";
    private static final String CATEGORY_PICKER =
            "com.japaeditor.tensuraevolution.client.SkillCategoryPickerScreen";

    @Test
    void presentTensuraReplacesTheEquipSurface() {
        assertTrue(TrsMenuBridge.replaces(true, ARR_ENTRY),
                "ARR's entry is the equip surface (TRS's swap origin)");
        assertTrue(TrsMenuBridge.replaces(true, CATEGORY),
                "their first landing replaces");
        assertTrue(TrsMenuBridge.replaces(true, DIRECT_LOADOUT),
                "their direct loadout replaces");
        assertTrue(TrsMenuBridge.replaces(true, EVOLUTION_LOADOUT),
                "their evolution loadout replaces");
        assertTrue(TrsMenuBridge.replaces(true, CATEGORY_PICKER),
                "the status screen's Equipar skills landing replaces too");
    }

    @Test
    void absentTensuraChangesNothing() {
        assertFalse(TrsMenuBridge.replaces(false, ARR_ENTRY),
                "without TRS the baseline must be byte-identical");
        assertFalse(TrsMenuBridge.replaces(false, CATEGORY),
                "without TRS the baseline must be byte-identical");
        assertFalse(TrsMenuBridge.replaces(false, CATEGORY_PICKER),
                "without TRS the baseline must be byte-identical");
    }

    @Test
    void nonEquipScreensAreNeverTouched() {
        assertFalse(TrsMenuBridge.replaces(true,
                        "com.japaeditor.tensuraevolution.client.NativeSkillLearningScreen"),
                "learning is a non-goal");
        assertFalse(TrsMenuBridge.replaces(true,
                        "com.japaeditor.tensuraevolution.client.TensuraSkillShopScreen"),
                "their shop is a non-goal");
        assertFalse(TrsMenuBridge.replaces(true,
                        "com.japaeditor.tensuraevolution.client.EvolutionMainScreen"),
                "their hub is a non-goal");
        assertFalse(TrsMenuBridge.replaces(true,
                        "net.minecraft.client.gui.screens.inventory.InventoryScreen"),
                "vanilla screens are untouched");
        assertFalse(TrsMenuBridge.replaces(true, null),
                "a null class name is malformed input");
    }

    @Test
    void nullScreenNeverReplaces() {
        assertFalse(TrsMenuBridge.shouldReplace(true, null),
                "nothing opening means nothing to replace");
        assertFalse(TrsMenuBridge.shouldReplace(false, null));
    }
}
