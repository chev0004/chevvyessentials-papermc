package com.chevvy.state;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaState {
    /**
     * Represents a teleport request.
     * @param originalRequester The player who initiated the /tpa or /tpahere command.
     * @param sourcePlayer The player who will be teleported.
     * @param destinationPlayer The player who is the destination of the teleport.
     * @param creationTime The time the request was created, in milliseconds.
     */
    public record TpaRequest(UUID originalRequester, UUID sourcePlayer, UUID destinationPlayer, long creationTime) {}

    // Maps a target player's UUID to a map of their pending requests.
    // The inner map keys are the original requesters' UUIDs.
    private static final Map<UUID, Map<UUID, TpaRequest>> pendingRequests = new ConcurrentHashMap<>();

    public static void createTpaRequest(Player from, Player to) {
        TpaRequest request = new TpaRequest(from.getUniqueId(), from.getUniqueId(), to.getUniqueId(), System.currentTimeMillis());
        pendingRequests.computeIfAbsent(to.getUniqueId(), k -> new ConcurrentHashMap<>()).put(from.getUniqueId(), request);
    }

    public static void createTpaHereRequest(Player requester, Player target) {
        TpaRequest request = new TpaRequest(requester.getUniqueId(), target.getUniqueId(), requester.getUniqueId(), System.currentTimeMillis());
        pendingRequests.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>()).put(requester.getUniqueId(), request);
    }

    public static Map<UUID, TpaRequest> getRequestsForPlayer(UUID targetUuid) {
        return pendingRequests.getOrDefault(targetUuid, Collections.emptyMap());
    }

    public static void clearRequest(UUID targetUuid, UUID requesterUuid) {
        Map<UUID, TpaRequest> playerRequests = pendingRequests.get(targetUuid);
        if (playerRequests != null) {
            playerRequests.remove(requesterUuid);
            if (playerRequests.isEmpty()) {
                pendingRequests.remove(targetUuid);
            }
        }
    }

    public static Map<UUID, Map<UUID, TpaRequest>> getPendingRequests() {
        return pendingRequests;
    }
}
