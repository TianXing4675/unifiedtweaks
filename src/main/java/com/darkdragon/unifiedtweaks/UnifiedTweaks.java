package com.darkdragon.unifiedtweaks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.darkdragon.unifiedtweaks.bot.BotManager;
import com.darkdragon.unifiedtweaks.command.BotCommands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnifiedTweaks implements ModInitializer {
	public static final String MOD_ID = "UnifiedTweaks";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("UnifiedTweaks Initialized!");

        BotCommands.register();

        // 驱动 bot 的 tick（后续你要做“右键连点/攻击/走路”等，都挂这里）
//        ServerTickEvents.END_SERVER_TICK.register(server -> BotManager.tick(server));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            com.darkdragon.unifiedtweaks.bind.NetBindManager.tick(server);
        });

    }
}