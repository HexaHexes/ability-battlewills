package org.bitwisemadness.abilitytensura;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Ability: Tensura entry point.
 */
@Mod(AbilityTensura.MOD_ID)
public final class AbilityTensura {

    public static final String MOD_ID = "ability_tensura";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AbilityTensura(IEventBus modBus) {
        LOGGER.info("Ability: Tensura initialized");
    }
}
