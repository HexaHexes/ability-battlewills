package org.bitwisemadness.abilitytensura.gate;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.bitwisemadness.abilityhotbar.registry.MenuTabs;

/**
 * Tensura's registered menu tabs (ability-tensura#75, the owner's
 * directive): **Skills, Magic and Battlewill** as three tabs in the
 * hotbar's menu, each with ARR's own tab icon as its identifier
 * (Icon/Item Stack + Text - hotbar#154) and membership over the
 * definition's category (pure predicates, no upstream reads).
 *
 * <p>Guarded callsite: hotbar is an <b>optional</b> dependency of this
 * mod, so {@link #register} checks both mods and {@link #registerAll}
 * - the only place a hotbar class is referenced - runs behind that check
 * (the {@code HotbarHygiene} pattern: the guarded method is never linked
 * when hotbar is absent). Without Tensura, {@link UnlockGate#register}
 * never runs at all; without hotbar, this class registers nothing.</p>
 */
public final class TensuraMenuTabs {

    private static final ResourceLocation SKILLS_TAB =
            ResourceLocation.fromNamespaceAndPath("ability_tensura", "skills");
    private static final ResourceLocation MAGIC_TAB =
            ResourceLocation.fromNamespaceAndPath("ability_tensura", "magic");
    private static final ResourceLocation BATTLEWILL_TAB =
            ResourceLocation.fromNamespaceAndPath("ability_tensura", "battlewill");

    /** ARR's own tab icons, referenced by location (ADR-005 - never copied). */
    private static final ResourceLocation SKILL_ICON =
            ResourceLocation.fromNamespaceAndPath("tensura",
                    "textures/gui/skill/skill_tab_icon");
    private static final ResourceLocation MAGIC_ICON =
            ResourceLocation.fromNamespaceAndPath("tensura",
                    "textures/gui/magic/magic_tab_icon");
    private static final ResourceLocation BATTLEWILL_ICON =
            ResourceLocation.fromNamespaceAndPath("tensura",
                    "textures/gui/battlewill/battlewill_tab_icon");

    private TensuraMenuTabs() {
    }

    /** Setup entry (hotbar optional): registers the three tabs when both mods are present. */
    public static void register() {
        if (ModList.get() == null || !ModList.get().isLoaded("tensura")
                || !ModList.get().isLoaded("ability_hotbar")) {
            return;
        }
        registerAll();
    }

    /** The registrations themselves (the linkable half - tests call this directly). */
    static void registerAll() {
        MenuTabs.register(SKILLS_TAB, "ability_tensura.tab.skills", SKILL_ICON,
                inCategory("tensura", "skill"));
        MenuTabs.register(MAGIC_TAB, "ability_tensura.tab.magic", MAGIC_ICON,
                inCategory("tensura", "magic"));
        MenuTabs.register(BATTLEWILL_TAB, "ability_tensura.tab.battlewill", BATTLEWILL_ICON,
                inCategory("tensura", "battlewill"));
    }

    /** Membership: entries whose definition carries this exact category. */
    private static java.util.function.BiPredicate<ResourceLocation,
            org.bitwisemadness.abilitycore.api.AbilityDefinition> inCategory(
            String namespace, String path) {
        ResourceLocation category = ResourceLocation.fromNamespaceAndPath(namespace, path);
        return (id, definition) -> definition.category().equals(category);
    }
}
