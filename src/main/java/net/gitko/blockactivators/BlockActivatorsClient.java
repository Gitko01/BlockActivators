package net.gitko.blockactivators;

import net.fabricmc.api.ClientModInitializer;
import net.gitko.blockactivators.gui.BlockActivatorScreen;
import net.gitko.blockactivators.network.UpdateClickModePacket;
import net.gitko.blockactivators.network.UpdateRoundRobinPacket;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockActivatorsClient implements ClientModInitializer {
    public static final String MOD_ID = "blockactivators";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Block Activators] Client-side mod initialized.");

        HandledScreens.register(BlockActivators.BLOCK_ACTIVATOR_SCREEN_HANDLER, BlockActivatorScreen::new);
        UpdateClickModePacket.register();
        UpdateRoundRobinPacket.register();
    }
}
