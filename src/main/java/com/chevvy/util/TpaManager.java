package com.chevvy.util;

import com.chevvy.config.ModConfig;
import com.chevvy.state.TpaState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TpaManager {

    private static final AtomicInteger tickCounter = new AtomicInteger(0);

    public static void initialize(Plugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (tickCounter.incrementAndGet() >= 20) {
                tickCounter.set(0);
                checkExpiredRequests();
            }
        }, 0L, 1L);
    }

    private static void checkExpiredRequests() {
        int timeoutSeconds = ModConfig.get().tpaTimeoutSeconds;
        long timeoutMillis = timeoutSeconds * 1000L;
        long currentTime = System.currentTimeMillis();

        for (UUID targetUuid : new ArrayList<>(TpaState.getPendingRequests().keySet())) {
            Map<UUID, TpaState.TpaRequest> playerRequests = TpaState.getRequestsForPlayer(targetUuid);
            if (playerRequests.isEmpty()) continue;

            for (UUID requesterUuid : new ArrayList<>(playerRequests.keySet())) {
                TpaState.TpaRequest request = playerRequests.get(requesterUuid);
                if (request == null) continue;

                if (currentTime - request.creationTime() > timeoutMillis) {
                    TpaState.clearRequest(targetUuid, requesterUuid);

                    Player target = Bukkit.getPlayer(targetUuid);
                    Player originalRequester = Bukkit.getPlayer(request.originalRequester());

                    String requesterName = originalRequester != null ? originalRequester.getName() : "Someone";
                    String targetName = target != null ? target.getName() : "Someone";

                    if (target != null) {
                        CommandUtils.sendBilingual(target,
                                Component.text(requesterName, NamedTextColor.GREEN)
                                        .append(Component.text("さんからのTPAリクエストは", NamedTextColor.GRAY))
                                        .append(Component.text(String.valueOf(timeoutSeconds), NamedTextColor.GREEN))
                                        .append(Component.text("秒後に期限切れになりました。", NamedTextColor.GRAY)),
                                Component.text("The TPA request from ", NamedTextColor.GRAY)
                                        .append(Component.text(requesterName, NamedTextColor.GREEN))
                                        .append(Component.text(" expired after ", NamedTextColor.GRAY))
                                        .append(Component.text(String.valueOf(timeoutSeconds), NamedTextColor.GREEN))
                                        .append(Component.text(" seconds.", NamedTextColor.GRAY))
                        );
                    }
                    if (originalRequester != null) {
                        CommandUtils.sendBilingual(originalRequester,
                                Component.text(targetName, NamedTextColor.GREEN)
                                        .append(Component.text("さんへのTPAリクエストは", NamedTextColor.GRAY))
                                        .append(Component.text(String.valueOf(timeoutSeconds), NamedTextColor.GREEN))
                                        .append(Component.text("秒後にタイムアウトしました。", NamedTextColor.GRAY)),
                                Component.text("Your TPA request to ", NamedTextColor.GRAY)
                                        .append(Component.text(targetName, NamedTextColor.GREEN))
                                        .append(Component.text(" timed out after ", NamedTextColor.GRAY))
                                        .append(Component.text(String.valueOf(timeoutSeconds), NamedTextColor.GREEN))
                                        .append(Component.text(" seconds.", NamedTextColor.GRAY))
                        );
                    }
                }
            }
        }
    }
}
