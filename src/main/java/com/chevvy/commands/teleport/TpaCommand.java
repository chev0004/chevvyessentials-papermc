package com.chevvy.commands.teleport;

import com.chevvy.BackLocation;
import com.chevvy.Vec3d;
import com.chevvy.state.BackState;
import com.chevvy.state.TpaState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class TpaCommand {
    public static void register(Commands commands) {
        SuggestionProvider<CommandSourceStack> playerSuggestions = (context, builder) -> {
            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSourceStack> pendingRequestSuggestionProvider = (context, builder) -> {
            if (!(context.getSource().getExecutor() instanceof Player player)) return builder.buildFuture();
            Map<UUID, TpaState.TpaRequest> requests = TpaState.getRequestsForPlayer(player.getUniqueId());
            for (UUID requesterUuid : requests.keySet()) {
                Player onlinePlayer = Bukkit.getPlayer(requesterUuid);
                if (onlinePlayer != null) {
                    builder.suggest(onlinePlayer.getName());
                }
            }
            return builder.buildFuture();
        };

        commands.register(
                Commands.literal("tpa")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player)) return 0;
                            sendTpaHelp(player);
                            return 1;
                        })
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(playerSuggestions)
                                .executes(TpaCommand::run))
                        .then(Commands.literal("accept")
                                .executes(context -> executeResponse(context.getSource(), null, true))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(pendingRequestSuggestionProvider)
                                        .executes(context -> executeResponse(context.getSource(), StringArgumentType.getString(context, "player"), true))))
                        .then(Commands.literal("deny")
                                .executes(context -> executeResponse(context.getSource(), null, false))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(pendingRequestSuggestionProvider)
                                        .executes(context -> executeResponse(context.getSource(), StringArgumentType.getString(context, "player"), false))))
                        .build(),
                "Send a teleport request to a player"
        );
    }

    public static void sendTpaHelp(Player player) {
        player.sendMessage(Component.text("--- ChevvyEssentials TPA Help ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/tpa <player>", NamedTextColor.AQUA));
        player.sendMessage(Component.text("  プレイヤーにテレポートリクエストを送信する。", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Sends a teleport request to a player.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/tpahere <player>", NamedTextColor.AQUA));
        player.sendMessage(Component.text("  プレイヤーにあなたの場所へのテレポートをリクエストする。", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Requests a player to teleport to your location.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/tpa accept [player]", NamedTextColor.AQUA));
        player.sendMessage(Component.text("  保留中のテレポートリクエストを承認する。", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Accepts a pending teleport request.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/tpa deny [player]", NamedTextColor.AQUA));
        player.sendMessage(Component.text("  保留中のテレポートリクエストを拒否する。", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Denies a pending teleport request.", NamedTextColor.GRAY));
    }

    private static int executeResponse(CommandSourceStack source, String requesterName, boolean accept) {
        if (!(source.getExecutor() instanceof Player player)) return 0;

        Map<UUID, TpaState.TpaRequest> requests = TpaState.getRequestsForPlayer(player.getUniqueId());

        if (requests.isEmpty()) {
            CommandUtils.sendBilingual(player,
                    Component.text("保留中のTPAリクエストはありません。", NamedTextColor.GRAY),
                    Component.text("You have no pending TPA requests.", NamedTextColor.GRAY));
            return 0;
        }

        TpaState.TpaRequest requestToProcess = null;

        if (requesterName != null) {
            Player onlinePlayer = Bukkit.getPlayer(requesterName);
            if (onlinePlayer != null) {
                UUID requesterUuid = onlinePlayer.getUniqueId();
                requestToProcess = requests.get(requesterUuid);
            }
            if (requestToProcess == null) {
                CommandUtils.sendBilingual(player,
                        Component.text(requesterName, NamedTextColor.GREEN)
                                .append(Component.text("さんからの保留中のリクエストはありません。", NamedTextColor.GRAY)),
                        Component.text("You have no pending request from ", NamedTextColor.GRAY)
                                .append(Component.text(requesterName, NamedTextColor.GREEN))
                                .append(Component.text(".", NamedTextColor.GRAY))
                );
                return 0;
            }
        } else {
            if (requests.size() > 1) {
                String pendingRequesters = requests.keySet().stream()
                        .map(uuid -> {
                            Player onlinePlayer = Bukkit.getPlayer(uuid);
                            return onlinePlayer != null ? onlinePlayer.getName() : null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", "));
                String command = accept ? "/tpa accept <player>" : "/tpa deny <player>";
                CommandUtils.sendBilingual(player,
                        Component.text("複数のTPAリクエストがあります: ", NamedTextColor.GRAY)
                                .append(Component.text(pendingRequesters, NamedTextColor.GREEN))
                                .append(Component.text("。 ", NamedTextColor.GRAY))
                                .append(Component.text(command, NamedTextColor.AQUA))
                                .append(Component.text(" を使用して選択してください。", NamedTextColor.GRAY)),
                        Component.text("You have multiple TPA requests from: ", NamedTextColor.GRAY)
                                .append(Component.text(pendingRequesters, NamedTextColor.GREEN))
                                .append(Component.text(". Please use ", NamedTextColor.GRAY))
                                .append(Component.text(command, NamedTextColor.AQUA))
                                .append(Component.text(" to choose.", NamedTextColor.GRAY))
                );
                return 0;
            }
            requestToProcess = requests.values().iterator().next();
        }

        if (accept) {
            return acceptRequest(player, requestToProcess);
        } else {
            return denyRequest(player, requestToProcess);
        }
    }

    private static int acceptRequest(Player acceptor, TpaState.TpaRequest request) {
        Player source = Bukkit.getPlayer(request.sourcePlayer());
        Player destination = Bukkit.getPlayer(request.destinationPlayer());

        if (source == null || destination == null) {
            CommandUtils.sendBilingual(acceptor,
                    Component.text("リクエストに関係するプレイヤーが見つかりません。", NamedTextColor.GRAY),
                    Component.text("A player involved in the request could not be found.", NamedTextColor.GRAY));
        } else {
            // Save the current position before teleporting
            Location srcLoc = source.getLocation();
            Vec3d currentPos = new Vec3d(srcLoc.getX(), srcLoc.getY(), srcLoc.getZ());
            BackLocation backLocation = new BackLocation(currentPos, source.getWorld().getKey().toString());
            BackState.setBackLocation(source.getUniqueId(), backLocation);

            Location destLoc = destination.getLocation();
            source.teleport(destLoc);

            CommandUtils.sendBilingual(source,
                    Component.text(destination.getName(), NamedTextColor.GREEN)
                            .append(Component.text("さんにテレポートしました！", NamedTextColor.GRAY)),
                    Component.text("Teleported to ", NamedTextColor.GRAY)
                            .append(Component.text(destination.getName(), NamedTextColor.GREEN))
                            .append(Component.text("!", NamedTextColor.GRAY)));
            CommandUtils.sendBilingual(destination,
                    Component.text(source.getName(), NamedTextColor.GREEN)
                            .append(Component.text("さんがテレポートしてきました！", NamedTextColor.GRAY)),
                    Component.text(source.getName(), NamedTextColor.GREEN)
                            .append(Component.text(" has teleported to you!", NamedTextColor.GRAY)));
        }

        TpaState.clearRequest(acceptor.getUniqueId(), request.originalRequester());
        return 1;
    }

    private static int denyRequest(Player denier, TpaState.TpaRequest request) {
        Player originalRequester = Bukkit.getPlayer(request.originalRequester());
        String requesterName = originalRequester != null ? originalRequester.getName() : "Someone";

        if (originalRequester != null) {
            CommandUtils.sendBilingual(originalRequester,
                    Component.text(denier.getName(), NamedTextColor.GREEN)
                            .append(Component.text("さんがリクエストを拒否しました。", NamedTextColor.GRAY)),
                    Component.text(denier.getName(), NamedTextColor.GREEN)
                            .append(Component.text(" denied your request.", NamedTextColor.GRAY)));
        }
        CommandUtils.sendBilingual(denier,
                Component.text(requesterName, NamedTextColor.GREEN)
                        .append(Component.text("さんのリクエストを拒否しました。", NamedTextColor.GRAY)),
                Component.text("Request from ", NamedTextColor.GRAY)
                        .append(Component.text(requesterName, NamedTextColor.GREEN))
                        .append(Component.text(" denied.", NamedTextColor.GRAY)));

        TpaState.clearRequest(denier.getUniqueId(), request.originalRequester());
        return 1;
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getExecutor() instanceof Player requester)) return 0;

        String targetName = StringArgumentType.getString(context, "player");
        Player target = Bukkit.getPlayer(targetName);
        if (handleTargetNotFound(requester, targetName, target)) return 0;

        assert target != null;
        if (target.getUniqueId().equals(requester.getUniqueId())) {
            CommandUtils.sendBilingual(requester,
                    Component.text("自分自身にはテレポートできません。", NamedTextColor.GRAY),
                    Component.text("You can't send a TPA request to yourself.", NamedTextColor.GRAY));
            return 0;
        }

        TpaState.createTpaRequest(requester, target);
        CommandUtils.sendBilingual(requester,
                Component.text(targetName, NamedTextColor.GREEN)
                        .append(Component.text("さんにTPAリクエストを送りました。", NamedTextColor.GRAY)),
                Component.text("Sent TPA request to ", NamedTextColor.GRAY)
                        .append(Component.text(targetName, NamedTextColor.GREEN)));

        target.sendMessage(Component.text(requester.getName(), NamedTextColor.GREEN)
                .append(Component.text("さんがあなたにテレポートしようとしています。", NamedTextColor.GRAY))
                .append(Component.text("/tpa accept", NamedTextColor.AQUA))
                .append(Component.text(" または ", NamedTextColor.GRAY))
                .append(Component.text("/tpa deny", NamedTextColor.AQUA))
                .append(Component.text(" を入力してください。", NamedTextColor.GRAY)));
        target.sendMessage(Component.text(requester.getName(), NamedTextColor.GREEN)
                .append(Component.text(" wants to teleport to you. Type ", NamedTextColor.GRAY))
                .append(Component.text("/tpa accept", NamedTextColor.AQUA))
                .append(Component.text(" or ", NamedTextColor.GRAY))
                .append(Component.text("/tpa deny", NamedTextColor.AQUA))
                .append(Component.text(".", NamedTextColor.GRAY)));

        return 1;
    }

    static boolean handleTargetNotFound(Player requester, String targetName, Player target) {
        if (target == null) {
            CommandUtils.sendBilingual(requester,
                    Component.text("プレイヤーが見つかりません: ", NamedTextColor.GRAY)
                            .append(Component.text(targetName, NamedTextColor.GREEN)),
                    Component.text("Player not found: ", NamedTextColor.GRAY)
                            .append(Component.text(targetName, NamedTextColor.GREEN)));
            return true;
        }
        return false;
    }
}
