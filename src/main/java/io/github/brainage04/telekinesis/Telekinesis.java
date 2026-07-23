package io.github.brainage04.telekinesis;

import io.github.brainage04.brainagelib.help.ServerModHelpEntry;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import io.github.brainage04.telekinesis.command.core.ModCommands;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Telekinesis implements ModInitializer {
    public static final String MOD_ID = "telekinesis";
    public static final String MOD_NAME = "Telekinesis";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	@Override
	public void onInitialize() {
        LOGGER.info("{} initialising...", MOD_NAME);

        TelekinesisConfigManager.initialize();
        TelekinesisPlayerSettings.initialize();
        ModCommands.initialize();
        ServerModHelpRegistry.register(new ServerModHelpEntry(
                MOD_ID,
                MOD_NAME,
                "Places player-mined drops and experience directly into the player's inventory.",
                "/telekinesis help",
                "/telekinesis config"
        ));
        LOGGER.info("{} initialised.", MOD_NAME);
	}
}
