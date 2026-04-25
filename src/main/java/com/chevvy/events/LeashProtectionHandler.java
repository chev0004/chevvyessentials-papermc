package com.chevvy.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityUnleashEvent;

public class LeashProtectionHandler implements Listener {
    @EventHandler
    public void onEntityUnleash(EntityUnleashEvent event) {
        if (event.getReason() != EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH) {
            event.setCancelled(true);
        }
    }
}
