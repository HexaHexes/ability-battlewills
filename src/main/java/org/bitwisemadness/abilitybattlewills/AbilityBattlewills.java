package org.bitwisemadness.abilitybattlewills;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Ability Battlewills entry point.
 */
@Mod(AbilityBattlewills.MOD_ID)
public final class AbilityBattlewills {

    public static final String MOD_ID = "ability_battlewills";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AbilityBattlewills(IEventBus modBus) {
        LOGGER.info("Ability Battlewills initialized");
    }
}
