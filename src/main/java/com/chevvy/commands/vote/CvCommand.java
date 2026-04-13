package com.chevvy.commands.vote;

import com.chevvy.config.ModConfig;
import com.chevvy.state.VoteState;
import com.chevvy.util.CommandUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class CvCommand {

    public static void register(Commands commands) {
        commands.register(
                Commands.literal("cv")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player)) return 0;
                            sendCvHelp(player);
                            return 1;
                        })
                        .then(Commands.literal("d")
                                .executes(context -> handleVoteExecution(context.getSource(), VoteState.VoteType.DAY, "時間を昼にする", "change the time to day", "/cv d")))
                        .then(Commands.literal("n")
                                .executes(context -> handleVoteExecution(context.getSource(), VoteState.VoteType.NIGHT, "時間を夜にする", "change the time to night", "/cv n")))
                        .then(Commands.literal("c")
                                .executes(context -> handleVoteExecution(context.getSource(), VoteState.VoteType.CLEAR, "天候を晴れにする", "make the weather clear", "/cv c")))
                        .then(Commands.literal("r")
                                .executes(context -> handleVoteExecution(context.getSource(), VoteState.VoteType.RAIN, "天候を雨にする", "change the weather to rain", "/cv r")))
                        .build(),
                "Vote to change time or weather"
        );
    }

    private static int handleVoteExecution(CommandSourceStack source, VoteState.VoteType type, String descriptionJp, String descriptionEn, String command) {
        if (!(source.getExecutor() instanceof Player player)) return 0;

        if (VoteState.isVoteActive()) {
            VoteState.ActiveVote currentVote = VoteState.getCurrentVote();
            // If the command matches the current vote type, treat it as a 'yes' vote.
            if (currentVote.type() == type) {
                if (currentVote.votes().contains(player.getUniqueId())) {
                    CommandUtils.sendBilingual(player,
                            Component.text("既にこの投票に投票済みです。", NamedTextColor.GRAY),
                            Component.text("You have already voted in this poll.", NamedTextColor.GRAY));
                    return 0;
                }

                VoteState.addVote(player.getUniqueId());

                int onlinePlayers = player.getServer().getOnlinePlayers().size();
                int requiredPercent = ModConfig.get().voteThresholdPercent;
                int currentVotes = currentVote.votes().size();
                int currentPercent = (int) Math.floor((double) currentVotes / onlinePlayers * 100);

                Component jpAnnounce = Component.text(player.getName(), NamedTextColor.AQUA)
                        .append(Component.text("さんが", NamedTextColor.GRAY))
                        .append(Component.text(currentVote.descriptionJp(), NamedTextColor.GREEN))
                        .append(Component.text("に賛成票を投じました。現在の投票率: ", NamedTextColor.GRAY))
                        .append(Component.text(currentPercent + "%", NamedTextColor.YELLOW))
                        .append(Component.text(" (" + requiredPercent + "% 必要)", NamedTextColor.GRAY));

                Component enAnnounce = Component.text(player.getName(), NamedTextColor.AQUA)
                        .append(Component.text(" voted to ", NamedTextColor.GRAY))
                        .append(Component.text(currentVote.descriptionEn(), NamedTextColor.GREEN))
                        .append(Component.text(". Vote is now at ", NamedTextColor.GRAY))
                        .append(Component.text(currentPercent + "%", NamedTextColor.YELLOW))
                        .append(Component.text(" (" + requiredPercent + "% required).", NamedTextColor.GRAY));

                CommandUtils.broadcastBilingual(jpAnnounce, enAnnounce);

                checkVoteCompletion(player);

            } else { // A different vote is active
                CommandUtils.sendBilingual(player,
                        Component.text("別の投票が進行中です: ", NamedTextColor.GRAY)
                                .append(Component.text(currentVote.descriptionJp(), NamedTextColor.GREEN)),
                        Component.text("Another vote is already in progress to ", NamedTextColor.GRAY)
                                .append(Component.text(currentVote.descriptionEn(), NamedTextColor.GREEN))
                );
            }
        } else { // No vote is active, so start a new one.
            VoteState.startVote(type, player.getUniqueId(), descriptionJp, descriptionEn);

            Component jpMessage = Component.text(player.getName(), NamedTextColor.AQUA)
                    .append(Component.text("さんが", NamedTextColor.GRAY))
                    .append(Component.text(descriptionJp, NamedTextColor.GREEN))
                    .append(Component.text("ための投票を開始しました！ ", NamedTextColor.GRAY))
                    .append(Component.text(command, NamedTextColor.YELLOW))
                    .append(Component.text(" で投票してください。", NamedTextColor.GRAY));

            Component enMessage = Component.text(player.getName(), NamedTextColor.AQUA)
                    .append(Component.text(" has started a vote to ", NamedTextColor.GRAY))
                    .append(Component.text(descriptionEn, NamedTextColor.GREEN))
                    .append(Component.text("! Vote with ", NamedTextColor.GRAY))
                    .append(Component.text(command, NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.GRAY));

            CommandUtils.broadcastBilingual(jpMessage, enMessage);

            checkVoteCompletion(player);
        }
        return 1;
    }

    private static void checkVoteCompletion(Player player) {
        if (!VoteState.isVoteActive()) return;

        int onlinePlayers = player.getServer().getOnlinePlayers().size();
        double threshold = ModConfig.get().voteThresholdPercent / 100.0;
        int requiredVotes = (int) Math.max(1, Math.ceil(onlinePlayers * threshold));

        VoteState.ActiveVote vote = VoteState.getCurrentVote();
        if (vote.votes().size() >= requiredVotes) {
            executeVoteAction(player, vote);
            VoteState.endVote();
        }
    }

    private static void executeVoteAction(Player player, VoteState.ActiveVote vote) {
        Component jpMessage;
        Component enMessage;

        World overworld = CommandUtils.getWorldByKey("minecraft:overworld");
        if (overworld == null) return;

        switch (vote.type()) {
            case DAY:
                overworld.setTime(1000); // Morning
                jpMessage = Component.text("投票が可決されました！ 時間が昼になります。", NamedTextColor.GRAY);
                enMessage = Component.text("Vote passed! The time will now be day.", NamedTextColor.GRAY);
                break;
            case NIGHT:
                overworld.setTime(13000); // Night
                jpMessage = Component.text("投票が可決されました！ 時間が夜になります。", NamedTextColor.GRAY);
                enMessage = Component.text("Vote passed! The time will now be night.", NamedTextColor.GRAY);
                break;
            case CLEAR:
                overworld.setStorm(false);
                overworld.setThundering(false);
                overworld.setClearWeatherDuration(120000); // Clear for 100 minutes
                jpMessage = Component.text("投票が可決されました！ 天候が晴れになります。", NamedTextColor.GRAY);
                enMessage = Component.text("Vote passed! The weather will now be clear.", NamedTextColor.GRAY);
                break;
            case RAIN:
                overworld.setStorm(true);
                overworld.setThundering(false);
                overworld.setWeatherDuration(6000); // Rain for 5 minutes
                jpMessage = Component.text("投票が可決されました！ 天候が雨になります。", NamedTextColor.GRAY);
                enMessage = Component.text("Vote passed! The weather will now be rain.", NamedTextColor.GRAY);
                break;
            default:
                return;
        }

        CommandUtils.broadcastBilingual(jpMessage, enMessage);
    }

    public static void sendCvHelp(Player player) {
        player.sendMessage(Component.text("--- ChevvyEssentials Vote Help ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/cv d", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  時間を昼にする投票を開始/参加する", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Starts/joins a vote to change the time to day.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/cv n", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  時間を夜にする投票を開始/参加する", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Starts/joins a vote to change the time to night.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/cv c", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  天候を晴れにする投票を開始/参加する", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Starts/joins a vote to change the weather to clear.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/cv r", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  天候を雨にする投票を開始/参加する", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Starts/joins a vote to change the weather to rain.", NamedTextColor.GRAY));
    }
}
