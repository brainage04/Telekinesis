package com.github.brainage04.telekinesis;

import com.github.brainage04.telekinesis.command.core.ModCommands;
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

        ModCommands.initialize();

        LOGGER.info("{} initialised.", MOD_NAME);
	}
}