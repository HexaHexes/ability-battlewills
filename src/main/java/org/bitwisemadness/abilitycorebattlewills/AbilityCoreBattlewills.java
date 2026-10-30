package org.bitwisemadness.abilitycorebattlewills;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Ability Core: Battlewills entry point.
 */
@Mod(AbilityCoreBattlewills.MOD_ID)
public final class AbilityCoreBattlewills {

    public static final String MOD_ID = "ability_core_battlewills";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AbilityCoreBattlewills(IEventBus modBus) {
        LOGGER.info("Ability Core: Battlewills initialized");
    }
}
