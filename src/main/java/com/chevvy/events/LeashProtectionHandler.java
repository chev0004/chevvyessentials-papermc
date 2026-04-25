package com.chevvy.events;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LeashProtectionHandler implements Listener {
    private static final double MAX_DISTANCE_SQUARED = 36.0;
    private static final double LEAD_DROP_RADIUS_SQUARED = 64.0;

    private final JavaPlugin plugin;
    private final Map<UUID, LeashState> playerLeashes = new HashMap<>();

    public LeashProtectionHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshPlayerLeashes, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::scanForUntrackedLeashes, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityLeash(PlayerLeashEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }
        Entity holder = event.getPlayer();
        if (holder instanceof Player player) {
            trackLeash(livingEntity, player);
        } else {
            untrackLeash(livingEntity.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityUnleash(EntityUnleashEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }
        if (event.getReason() == EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH) {
            untrackLeash(livingEntity.getUniqueId());
            return;
        }

        event.setCancelled(true);

        LeashState state = playerLeashes.get(livingEntity.getUniqueId());
        if (state == null) {
            try {
                Entity currentHolder = livingEntity.getLeashHolder();
                if (currentHolder instanceof Player player) {
                    trackLeash(livingEntity, player);
                    state = playerLeashes.get(livingEntity.getUniqueId());
                }
            } catch (IllegalStateException ignored) {
            }
            if (state == null) {
                return;
            }
        }

        LeashState scheduled = state;
        Bukkit.getScheduler().runTask(plugin, () -> refreshLeash(scheduled));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (playerLeashes.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (item.getItemStack().getType() != Material.LEAD) {
            return;
        }
        Location dropLocation = item.getLocation();
        for (LeashState state : playerLeashes.values()) {
            LivingEntity entity = state.entity();
            if (!entity.isValid() || entity.getWorld() != dropLocation.getWorld()) {
                continue;
            }
            if (entity.getLocation().distanceSquared(dropLocation) <= LEAD_DROP_RADIUS_SQUARED) {
                event.setCancelled(true);
                return;
            }
            Player holder = Bukkit.getPlayer(state.holderId());
            if (holder != null && holder.getWorld() == dropLocation.getWorld()
                    && holder.getLocation().distanceSquared(dropLocation) <= LEAD_DROP_RADIUS_SQUARED) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        untrackLeash(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        playerLeashes.entrySet().removeIf(entry -> {
            boolean matches = entry.getValue().holderId().equals(playerId);
            if (matches) {
                removeChunkTicket(entry.getValue().chunk());
            }
            return matches;
        });
    }

    private void refreshPlayerLeashes() {
        playerLeashes.entrySet().removeIf(entry -> !refreshLeash(entry.getValue()));
    }

    private void scanForUntrackedLeashes() {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (!entity.isLeashed() || playerLeashes.containsKey(entity.getUniqueId())) {
                    continue;
                }
                try {
                    Entity holder = entity.getLeashHolder();
                    if (holder instanceof Player player) {
                        trackLeash(entity, player);
                    }
                } catch (IllegalStateException ignored) {
                }
            }
        }
    }

    private boolean refreshLeash(LeashState state) {
        LivingEntity entity = state.entity();
        Player holder = Bukkit.getPlayer(state.holderId());
        if (!entity.isValid() || holder == null || !holder.isOnline()) {
            removeChunkTicket(state.chunk());
            return false;
        }

        Location holderLocation = holder.getLocation();
        if (entity.getWorld() != holder.getWorld()
                || entity.getLocation().distanceSquared(holderLocation) > MAX_DISTANCE_SQUARED) {
            Location entityLocation = entity.getLocation();
            Location target = new Location(
                    holderLocation.getWorld(),
                    holderLocation.getX(),
                    holderLocation.getY(),
                    holderLocation.getZ(),
                    entityLocation.getYaw(),
                    entityLocation.getPitch());
            entity.teleport(target,
                    TeleportFlag.EntityState.RETAIN_PASSENGERS,
                    TeleportFlag.EntityState.RETAIN_VEHICLE);
            entity.setFallDistance(0f);
            entity.setVelocity(new Vector(0.0, 0.0, 0.0));
        }

        Chunk currentChunk = entity.getLocation().getChunk();
        if (!currentChunk.equals(state.chunk())) {
            removeChunkTicket(state.chunk());
            currentChunk.addPluginChunkTicket(plugin);
            state.setChunk(currentChunk);
        }

        Entity leashHolder = null;
        try {
            leashHolder = entity.getLeashHolder();
        } catch (IllegalStateException ignored) {
        }
        if (!entity.isLeashed() || leashHolder == null || !leashHolder.getUniqueId().equals(holder.getUniqueId())) {
            entity.setLeashHolder(holder);
        }
        return true;
    }

    private void trackLeash(LivingEntity entity, Player holder) {
        Chunk chunk = entity.getLocation().getChunk();
        chunk.addPluginChunkTicket(plugin);
        playerLeashes.put(entity.getUniqueId(), new LeashState(entity, holder.getUniqueId(), chunk));
    }

    private void untrackLeash(UUID entityId) {
        LeashState state = playerLeashes.remove(entityId);
        if (state != null) {
            removeChunkTicket(state.chunk());
        }
    }

    private void removeChunkTicket(Chunk chunk) {
        if (chunk != null) {
            chunk.removePluginChunkTicket(plugin);
        }
    }

    private static class LeashState {
        private final LivingEntity entity;
        private final UUID holderId;
        private Chunk chunk;

        private LeashState(LivingEntity entity, UUID holderId, Chunk chunk) {
            this.entity = entity;
            this.holderId = holderId;
            this.chunk = chunk;
        }

        private LivingEntity entity() {
            return entity;
        }

        private UUID holderId() {
            return holderId;
        }

        private Chunk chunk() {
            return chunk;
        }

        private void setChunk(Chunk chunk) {
            this.chunk = chunk;
        }
    }
}
