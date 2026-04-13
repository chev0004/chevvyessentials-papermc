package com.chevvy.util;

import com.chevvy.config.ModConfig;
import com.chevvy.state.VoteState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicInteger;

public class VoteManager {

    private static final AtomicInteger tickCounter = new AtomicInteger(0);

    public static void initialize(Plugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (tickCounter.incrementAndGet() >= 20) {
                tickCounter.set(0);
                checkExpiredVote(plugin);
            }
        }, 0L, 1L);
    }

    private static void checkExpiredVote(Plugin plugin) {
        if (!VoteState.isVoteActive()) {
            return;
        }

        VoteState.ActiveVote vote = VoteState.getCurrentVote();
        long timeoutMillis = ModConfig.get().voteTimeoutSeconds * 1000L;

        if (System.currentTimeMillis() - vote.startTime() > timeoutMillis) {
            Component jpMessage = Component.text("投票は時間切れで失敗しました。", NamedTextColor.GRAY);
            Component enMessage = Component.text("The vote failed to pass in time.", NamedTextColor.GRAY);

            CommandUtils.broadcastBilingual(jpMessage, enMessage);

            VoteState.endVote();
        }
    }
}
