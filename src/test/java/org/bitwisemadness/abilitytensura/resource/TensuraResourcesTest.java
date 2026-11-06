package org.bitwisemadness.abilitytensura.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;
import org.bitwisemadness.abilitycore.activation.ActivationHandlerRegistry;
import org.bitwisemadness.abilitycore.activation.ActivationPipeline;
import org.bitwisemadness.abilitycore.activation.ResourceChargeStep;
import org.bitwisemadness.abilitycore.api.AbilityDefinition;
import org.bitwisemadness.abilitycore.api.ActivationOutcome;
import org.bitwisemadness.abilitycore.api.ActivationRequest;
import org.bitwisemadness.abilitycore.api.ActivationResult;
import org.bitwisemadness.abilitycore.api.ActivationSource;
import org.bitwisemadness.abilitycore.api.ActivationType;
import org.bitwisemadness.abilitycore.api.AmountResolvers;
import org.bitwisemadness.abilitycore.api.ChargingModel;
import org.bitwisemadness.abilitycore.api.ConsumptionStrategies;
import org.bitwisemadness.abilitycore.api.ResourceConsumers;
import org.bitwisemadness.abilitycore.api.ResourceCost;
import org.bitwisemadness.abilitycore.api.ResourceProviders;
import org.bitwisemadness.abilitycore.registry.AbilityRegistries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The magicule pair (issue #41): gated declaration, fail-closed reads, pure
 * verdict/deduction helpers, and the end-to-end pipeline contract (charges
 * exactly once post-ACTED; without the pair the same ability refuses
 * {@code no_provider}). The balance seams stand in for a live entity - no
 * player fixture exists in this ecosystem, so production storage reads stay
 * compile-verified and out of unit reach.
 */
class TensuraResourcesTest {

    private static final ResourceLocation PAID =
            ResourceLocation.fromNamespaceAndPath("tensura", "magicule_paid");
    private static final ResourceLocation SKILL_CATEGORY =
            ResourceLocation.fromNamespaceAndPath("tensura", "skill");
    private static final ResourceCost COST = new ResourceCost(TensuraResources.MAGICULE, 10.0);

    private static final Function<ServerPlayer, Double> ORIGINAL_READ = MagiculePair.balanceRead;
    private static final BiConsumer<ServerPlayer, Double> ORIGINAL_WRITE =
            MagiculePair.balanceWrite;

    private ActivationHandlerRegistry handlers;
    private ActivationPipeline pipeline;
    private Registry<AbilityDefinition> previousRegistry;
    private final List<String> trace = new ArrayList<>();
    private final List<Double> writes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        handlers = new ActivationHandlerRegistry();
        pipeline = new ActivationPipeline(handlers);
        pipeline.registerStep(ActivationPipeline.PipelinePhase.RESOURCE, ResourceChargeStep.INSTANCE);
        previousRegistry = AbilityRegistries.registry();
        var fixture = new MappedRegistry<AbilityDefinition>(
                AbilityRegistries.ABILITY_REGISTRY_KEY, Lifecycle.stable(), false);
        Registry.register(fixture, PAID, new AbilityDefinition(
                "tensura.skill.magicule_paid", null, SKILL_CATEGORY,
                ActivationType.INSTANT, List.of(COST), ConsumptionStrategies.ALL,
                AmountResolvers.STATIC, Set.of(), Map.of()));
        AbilityRegistries.installRegistryForTests(fixture);
    }

    @AfterEach
    void tearDown() {
        AbilityRegistries.installRegistryForTests(previousRegistry);
        ResourceProviders.remove(TensuraResources.MAGICULE);
        ResourceConsumers.remove(TensuraResources.MAGICULE);
        MagiculePair.balanceRead = ORIGINAL_READ;
        MagiculePair.balanceWrite = ORIGINAL_WRITE;
        trace.clear();
        writes.clear();
    }

    private ActivationResult run() {
        return pipeline.run(ActivationRequest.of(PAID, ActivationSource.DIRECT_SLOT), null);
    }

    private void registerActingHandler() {
        handlers.register(PAID, (req, player) -> {
            trace.add("activate");
            return ActivationResult.ACTED;
        }, ChargingModel.EXTERNAL);
    }

    @Test
    void absentModSkipsWithDebugAndRegistersNothing() {
        try (var logs = TestLogCapture.open(TensuraResources.class, Level.DEBUG)) {
            assertFalse(TensuraResources.declareMagicules(false),
                    "absence answers 'not declared'");
            assertFalse(ResourceProviders.has(TensuraResources.MAGICULE), "no provider registered");
            assertFalse(ResourceConsumers.has(TensuraResources.MAGICULE), "no consumer registered");
            assertTrue(logs.messages().stream().anyMatch(m -> m.contains(TensuraResources.MAGICULE)),
                    "the skip names the resource id: " + logs.messages());
        }
        // the downstream auto-disable chain reads exactly this shape (core#92)
        assertFalse(ResourceProviders.has(TensuraResources.MAGICULE)
                && ResourceConsumers.has(TensuraResources.MAGICULE),
                "absent pair = a cost naming it fails closed with no_provider");
    }

    @Test
    void presentModRegistersThePairExactlyOnce() {
        assertTrue(TensuraResources.declareMagicules(true), "presence declares the pair");
        assertTrue(ResourceProviders.has(TensuraResources.MAGICULE));
        assertTrue(ResourceConsumers.has(TensuraResources.MAGICULE));
        assertThrows(IllegalStateException.class, () -> TensuraResources.declareMagicules(true),
                "duplicate declaration fails loudly (pairing ownership)");
    }

    @Test
    void providerFailsClosedWithoutAnEntity() {
        TensuraResources.declareMagicules(true);
        var verdict = ResourceProviders.get(TensuraResources.MAGICULE).orElseThrow()
                .check(PAID, null, 5.0);
        assertEquals(org.bitwisemadness.abilitycore.api.Affordability.State.UNKNOWN,
                verdict.state(), "no entity to read = not affordable, fail closed");
    }

    @Test
    void verdictAndDeductionHelpersArePure() {
        // boundary: exact balance is affordable, a hair over is not
        assertEquals(org.bitwisemadness.abilitycore.api.Affordability.State.SUFFICIENT,
                TensuraResources.magiculeVerdict(10.0, 10.0).state());
        assertEquals(org.bitwisemadness.abilitycore.api.Affordability.State.INSUFFICIENT,
                TensuraResources.magiculeVerdict(10.0, 10.5).state());
        // exact deduction, clamped at zero - the storage is a plain double
        assertEquals(6.0, TensuraResources.balanceAfterCharge(10.0, 4.0));
        assertEquals(0.0, TensuraResources.balanceAfterCharge(5.0, 8.0));
        assertEquals(7.0, TensuraResources.balanceAfterCharge(7.0, 0.0));
    }

    @Test
    void magiculeCostedAbilityChargesExactlyOnceEndToEnd() {
        // issue #41 AC: through ResourceChargeStep, post-ACTED, once, exact
        TensuraResources.declareMagicules(true);
        MagiculePair.balanceRead = player -> 100.0;
        MagiculePair.balanceWrite = (player, value) -> writes.add(value);
        registerActingHandler();

        var result = run();
        assertEquals(ActivationOutcome.ACTED, result.outcome());
        assertEquals(List.of("activate"), trace, "validation precedes activation");
        assertEquals(List.of(90.0), writes,
                "exactly one deduction of the 10.0 cost from the 100.0 balance");
    }

    @Test
    void withoutThePairTheSameAbilityRefusesNoProvider() {
        // control: pair absent (never declared in this test)
        registerActingHandler();

        var result = run();
        assertEquals(ActivationOutcome.REFUSED_UNAFFORDABLE, result.outcome());
        assertEquals("ability.refused.unaffordable.no_provider", result.effectiveMessageKey());
        assertTrue(trace.isEmpty(), "refused before activation");
        assertTrue(writes.isEmpty(), "nothing charged");
    }
}
