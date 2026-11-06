package org.bitwisemadness.abilitytensura.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Lifecycle;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.bitwisemadness.abilitycore.api.AbilityDefinition;
import org.bitwisemadness.abilitycore.api.ActivationType;
import org.bitwisemadness.abilitycore.api.AmountResolvers;
import org.bitwisemadness.abilitycore.api.ConsumptionStrategies;
import org.bitwisemadness.abilitycore.discovery.AbilityDiscovery;
import org.bitwisemadness.abilitycore.registry.AbilityRegistries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The classification contract (ability-tensura#10, sections AC1/AC2/AC4):
 * the datapack style mapping covers the catalog exactly once, every style
 * tag is in the declared vocabulary (fail-closed - an unknown style fails
 * the build, not the game), style queries run through core's own
 * {@link AbilityDiscovery#byTag}, and the unarmed style exists with zero
 * native members (its content lives on the deferred additive track).
 *
 * <p>The mapping itself is data ({@code battlewill_styles.json}) - this
 * test pins it, so changing a classification is a deliberate, reviewed
 * edit in both places.</p>
 */
class BattlewillClassificationTest {

    /** The style vocabulary this repo declares (the brief's weapon styles + utility). */
    private static final Set<String> VOCABULARY = Set.of(
            "style:sword", "style:spear", "style:axe", "style:ranged",
            "style:shield", "style:unarmed", "style:utility");

    /** The brief's taxonomy has no weapon analogue for native utility arts. */
    private static final Map<String, Integer> EXPECTED_COUNTS = Map.of(
            "style:sword", 3,
            "style:spear", 2,
            "style:axe", 3,
            "style:ranged", 7,
            "style:shield", 4,
            "style:utility", 4);

    private static final String MAPPING_PATH =
            "/data/ability_tensura/data_maps/ability_core/abilities/battlewill_styles.json";

    private Map<String, List<String>> tagsById;
    private Registry<AbilityDefinition> previousRegistry;

    @BeforeEach
    void setUp() throws Exception {
        tagsById = loadMapping();

        previousRegistry = AbilityRegistries.registry();
        var fixture = new MappedRegistry<AbilityDefinition>(
                AbilityRegistries.ABILITY_REGISTRY_KEY, Lifecycle.stable(), false);
        for (String path : BattlewillCatalog.ART_PATHS) {
            ResourceLocation id = BattlewillCatalog.idFor(path);
            List<String> tags = tagsById.get(id.toString());
            assertTrue(tags != null, "catalog art must be classified: " + id);
            Registry.register(fixture, id, new AbilityDefinition(
                    BattlewillCatalog.NAME_PREFIX + path,
                    ResourceLocation.fromNamespaceAndPath("tensura",
                            "textures/battlewill/" + path + ".png"),
                    BattlewillCatalog.CATEGORY,
                    ActivationType.UNKNOWN,
                    List.of(),
                    ConsumptionStrategies.ALL,
                    AmountResolvers.STATIC,
                    Set.copyOf(tags),
                    Map.of()));
        }
        AbilityRegistries.installRegistryForTests(fixture);
    }

    @AfterEach
    void tearDown() {
        AbilityRegistries.installRegistryForTests(previousRegistry);
    }

    /** AC1: every catalog art classified exactly once, no stale entries. */
    @Test
    void classificationCoversTheCatalogExactlyOnce() {
        Set<String> catalogIds = new HashSet<>();
        for (String path : BattlewillCatalog.ART_PATHS) {
            catalogIds.add(BattlewillCatalog.idFor(path).toString());
        }
        assertEquals(BattlewillCatalog.ART_PATHS.size(), catalogIds.size(),
                "catalog ids must be unique");
        assertEquals(catalogIds, tagsById.keySet(),
                "datapack mapping must cover exactly the catalog (no stale, no missing)");
        assertEquals(23, tagsById.size(), "the native inventory is 23 arts (SPIKE-bw-1 #2)");
    }

    /** AC1 fail-closed: one style per art, every style in the vocabulary. */
    @Test
    void everyArtCarriesExactlyOneVocabularyStyle() {
        Map<String, Integer> seen = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : tagsById.entrySet()) {
            List<String> styles = entry.getValue().stream()
                    .filter(tag -> tag.startsWith("style:"))
                    .toList();
            assertEquals(1, styles.size(),
                    "exactly one style tag per art, got " + styles + " for " + entry.getKey());
            String style = styles.get(0);
            assertTrue(VOCABULARY.contains(style),
                    "unknown style '" + style + "' for " + entry.getKey()
                            + " - extend the vocabulary deliberately, never silently");
            seen.merge(style, 1, Integer::sum);
        }
        assertEquals(EXPECTED_COUNTS, seen, "per-style counts must match the classified table");
    }

    /** AC2: style queries run through core's tag query over the definitions. */
    @Test
    void styleQueriesRunThroughCoreDiscovery() {
        Set<String> rangedNames = AbilityDiscovery.byTag("style:ranged").stream()
                .map(AbilityDefinition::displayNameKey)
                .collect(Collectors.toSet());
        Set<String> expectedRanged = Set.of(
                "tensura.skill.dark_eight_palms", "tensura.skill.death_march_dance",
                "tensura.skill.elephant_stampede", "tensura.skill.magic_bullet",
                "tensura.skill.maximum_magic_bullet", "tensura.skill.ogre_flame",
                "tensura.skill.ogre_sword_cannon");
        assertEquals(expectedRanged, rangedNames,
                "ranged must be exactly the native projectile group");
        assertEquals(3, AbilityDiscovery.byTag("style:sword").size(),
                "sword must be exactly the three sword-flavored melee arts");
        assertEquals(23, AbilityDiscovery.byTag(BattlewillCatalog.CATEGORY_TAG).size(),
                "every catalog entry carries the category tag");
    }

    /** AC4: the unarmed style exists in the vocabulary with zero members. */
    @Test
    void unarmedStyleExistsAndStaysEmpty() {
        assertTrue(VOCABULARY.contains("style:unarmed"),
                "the unarmed style must exist in the declared vocabulary");
        assertTrue(AbilityDiscovery.byTag("style:unarmed").isEmpty(),
                "no native art may classify as unarmed - its content arrives "
                        + "only via the deferred additive track (#14/#16, DEC-06)");
    }

    /** Parses the shipped datapack mapping from the test classpath. */
    private static Map<String, List<String>> loadMapping() throws Exception {
        try (InputStream in = BattlewillClassificationTest.class
                .getResourceAsStream(MAPPING_PATH)) {
            assertTrue(in != null, "the shipped style mapping must be on the classpath");
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject values = root.getAsJsonObject("values");
            Map<String, List<String>> out = new HashMap<>();
            for (String id : values.keySet()) {
                JsonArray tags = values.getAsJsonObject(id).getAsJsonArray("tags");
                List<String> tagList = new java.util.ArrayList<>();
                tags.forEach(tag -> tagList.add(tag.getAsString()));
                out.put(id, tagList);
            }
            return out;
        }
    }
}
