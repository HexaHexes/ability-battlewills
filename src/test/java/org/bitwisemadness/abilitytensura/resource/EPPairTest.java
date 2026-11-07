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
import org.bitwisemadness.abilitycore.api.Affordability;
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
 * The EP pair (issue #43): same gated shape as magicules/aura - absence
 * skips, presence registers, the balance seams carry the pipeline
 * end-to-end, and without the pair an EP-costed ability refuses
 * {@code no_provider}. EP is the direct stored balance (getEP/setEP), so
 * there is no component dependency on the other pairs.
 */
class EPPairTest {

    private static final ResourceLocation PAID =
            ResourceLocation.fromNamespaceAndPath("tensura", "ep_paid");
    private static final ResourceLocation SKILL_CATEGORY =
            ResourceLocation.fromNamespaceAndPath("tensura", "skill");
    private static final ResourceCost COST = new ResourceCost(TensuraResources.EP, 10.0);

    private static final Function<ServerPlayer, Double> ORIGINAL_READ = EPPair.PAIR.balanceRead;
    private static final BiConsumer<ServerPlayer, Double> ORIGINAL_WRITE = EPPair.PAIR.balanceWrite;

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
                "tensura.skill.ep_paid", null, SKILL_CATEGORY,
                ActivationType.INSTANT, List.of(COST), ConsumptionStrategies.ALL,
                AmountResolvers.STATIC, Set.of(), Map.of()));
        AbilityRegistries.installRegistryForTests(fixture);
    }

    @AfterEach
    void tearDown() {
        AbilityRegistries.installRegistryForTests(previousRegistry);
        ResourceProviders.remove(TensuraResources.EP);
        ResourceConsumers.remove(TensuraResources.EP);
        EPPair.PAIR.balanceRead = ORIGINAL_READ;
        EPPair.PAIR.balanceWrite = ORIGINAL_WRITE;
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
            assertFalse(TensuraResources.declareEP(false), "absence answers 'not declared'");
            assertFalse(ResourceProviders.has(TensuraResources.EP), "no provider registered");
            assertFalse(ResourceConsumers.has(TensuraResources.EP), "no consumer registered");
            assertTrue(logs.messages().stream().anyMatch(m -> m.contains(TensuraResources.EP)),
                    "the skip names the resource id: " + logs.messages());
        }
        assertFalse(ResourceProviders.has(TensuraResources.EP)
                && ResourceConsumers.has(TensuraResources.EP),
                "absent pair = a cost naming it fails closed with no_provider");
    }

    @Test
    void presentModRegistersThePairExactlyOnce() {
        assertTrue(TensuraResources.declareEP(true), "presence declares the pair");
        assertTrue(ResourceProviders.has(TensuraResources.EP));
        assertTrue(ResourceConsumers.has(TensuraResources.EP));
        assertThrows(IllegalStateException.class, () -> TensuraResources.declareEP(true),
                "duplicate declaration fails loudly (pairing ownership)");
    }

    @Test
    void providerFailsClosedWithoutAnEntity() {
        TensuraResources.declareEP(true);
        var verdict = ResourceProviders.get(TensuraResources.EP).orElseThrow()
                .check(PAID, null, 5.0);
        assertEquals(Affordability.State.UNKNOWN, verdict.state(),
                "no entity to read = not affordable, fail closed");
    }

    @Test
    void verdictAndDeductionHelpersArePure() {
        assertEquals(Affordability.State.SUFFICIENT, TensuraResources.epVerdict(10.0, 10.0).state());
        assertEquals(Affordability.State.INSUFFICIENT, TensuraResources.epVerdict(10.0, 10.5).state());
        assertEquals(6.0, TensuraResources.balanceAfterCharge(10.0, 4.0));
        assertEquals(0.0, TensuraResources.balanceAfterCharge(5.0, 8.0));
    }

    @Test
    void epCostedAbilityChargesExactlyOnceEndToEnd() {
        TensuraResources.declareEP(true);
        EPPair.PAIR.balanceRead = player -> 100.0;
        EPPair.PAIR.balanceWrite = (player, value) -> writes.add(value);
        registerActingHandler();

        var result = run();
        assertEquals(ActivationOutcome.ACTED, result.outcome());
        assertEquals(List.of("activate"), trace, "validation precedes activation");
        assertEquals(List.of(90.0), writes,
                "exactly one deduction of the 10.0 cost from the 100.0 balance");
    }

    @Test
    void withoutThePairTheSameAbilityRefusesNoProvider() {
        registerActingHandler();

        var result = run();
        assertEquals(ActivationOutcome.REFUSED_UNAFFORDABLE, result.outcome());
        assertEquals("ability.refused.unaffordable.no_provider", result.effectiveMessageKey());
        assertTrue(trace.isEmpty(), "refused before activation");
        assertTrue(writes.isEmpty(), "nothing charged");
    }
}
