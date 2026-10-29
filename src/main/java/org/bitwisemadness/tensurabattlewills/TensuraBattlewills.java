package org.bitwisemadness.tensurabattlewills;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Tensura Battlewills entry point.
 */
@Mod(TensuraBattlewills.MOD_ID)
public final class TensuraBattlewills {

    public static final String MOD_ID = "tensura_battlewills";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TensuraBattlewills(IEventBus modBus) {
        LOGGER.info("Tensura Battlewills initialized");
    }
}