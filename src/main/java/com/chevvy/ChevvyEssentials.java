package com.chevvy;

import com.chevvy.commands.death.DeathCommand;
import com.chevvy.commands.homes.*;
import com.chevvy.commands.teleport.BackCommand;
import com.chevvy.commands.teleport.SpawnCommand;
import com.chevvy.commands.teleport.TpaCommand;
import com.chevvy.commands.teleport.TpaHereCommand;
import com.chevvy.commands.vote.CvCommand;
import com.chevvy.config.ModConfig;
import com.chevvy.events.LeashProtectionHandler;
import com.chevvy.events.PlayerDeathHandler;
import com.chevvy.state.BackState;
import com.chevvy.state.DeathState;
import com.chevvy.state.HomeState;
import com.chevvy.util.TpaManager;
import com.chevvy.util.VoteManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ChevvyEssentials extends JavaPlugin {
    public static final String PLUGIN_ID = "chevvyessentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(PLUGIN_ID);

    @Override
    public void onEnable() {
        ModConfig.initialize();
        HomeState.initialize();
        TpaManager.initialize(this);

        DeathState.initialize();
        BackState.initialize();
        getServer().getPluginManager().registerEvents(new PlayerDeathHandler(), this);
        getServer().getPluginManager().registerEvents(new LeashProtectionHandler(), this);
        VoteManager.initialize(this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var commands = event.registrar();
            LOGGER.info("Registering commands for " + PLUGIN_ID);

            // Home Commands
            SetHomeCommand.register(commands);
            HomeCommand.register(commands);
            DelHomeCommand.register(commands);
            HomesCommand.register(commands);

            // Teleport Commands
            TpaCommand.register(commands);
            TpaHereCommand.register(commands);
            SpawnCommand.register(commands);
            BackCommand.register(commands);

            // Vote Commands
            CvCommand.register(commands);

            // Death Commands
            DeathCommand.register(commands);
        });

        LOGGER.info(PLUGIN_ID + " has been enabled!");
    }

    @Override
    public void onDisable() {
        HomeState.save();
        BackState.save();
        DeathState.save();
        LOGGER.info(PLUGIN_ID + " has been disabled.");
    }
}
