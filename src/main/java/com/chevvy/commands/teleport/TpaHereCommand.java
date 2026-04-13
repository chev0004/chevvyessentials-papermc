package com.chevvy.commands.teleport;

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
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class TpaHereCommand {
    public static void register(Commands commands) {
        SuggestionProvider<CommandSourceStack> playerSuggestions = (context, builder) -> {
            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
            return builder.buildFuture();
        };

        commands.register(
                Commands.literal("tpahere")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player)) return 0;
                            TpaCommand.sendTpaHelp(player);
                            return 1;
                        })
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(playerSuggestions)
                                .executes(TpaHereCommand::run)
                        )
                        .build(),
                "Request a player to teleport to your location"
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getExecutor() instanceof Player requester)) return 0;

        String targetName = StringArgumentType.getString(context, "player");
        Player target = Bukkit.getPlayer(targetName);

        if (TpaCommand.handleTargetNotFound(requester, targetName, target)) return 0;

        assert target != null;
        if (target.getUniqueId().equals(requester.getUniqueId())) {
            CommandUtils.sendBilingual(requester,
                    Component.text("自分自身をここに呼ぶことはできません。", NamedTextColor.GRAY),
                    Component.text("You can't send a TPAHERE request to yourself.", NamedTextColor.GRAY));
            return 0;
        }

        TpaState.createTpaHereRequest(requester, target);

        CommandUtils.sendBilingual(requester,
                Component.text(targetName, NamedTextColor.GREEN)
                        .append(Component.text("さんにTPAHEREリクエストを送りました。", NamedTextColor.GRAY)),
                Component.text("Sent TPAHERE request to ", NamedTextColor.GRAY)
                        .append(Component.text(targetName, NamedTextColor.GREEN)));

        target.sendMessage(Component.text(requester.getName(), NamedTextColor.GREEN)
                .append(Component.text("さんがあなたを呼んでいます。", NamedTextColor.GRAY))
                .append(Component.text("/tpa accept", NamedTextColor.AQUA))
                .append(Component.text(" または ", NamedTextColor.GRAY))
                .append(Component.text("/tpa deny", NamedTextColor.AQUA))
                .append(Component.text(" を入力してください。", NamedTextColor.GRAY)));
        target.sendMessage(Component.text(requester.getName(), NamedTextColor.GREEN)
                .append(Component.text(" wants you to teleport to them. Type ", NamedTextColor.GRAY))
                .append(Component.text("/tpa accept", NamedTextColor.AQUA))
                .append(Component.text(" or ", NamedTextColor.GRAY))
                .append(Component.text("/tpa deny", NamedTextColor.AQUA))
                .append(Component.text(".", NamedTextColor.GRAY)));

        return 1;
    }
}
