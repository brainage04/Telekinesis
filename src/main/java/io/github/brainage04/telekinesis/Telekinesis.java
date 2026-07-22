package io.github.brainage04.telekinesis;

import io.github.brainage04.telekinesis.command.core.ModCommands;
import io.github.brainage04.telekinesis.gamerule.core.ModGameRules;
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

        ModGameRules.initialize();
        ModCommands.initialize();


        LOGGER.info("{} initialised.", MOD_NAME);
	}
}
