package com.chevvy.commands.homes;

import com.chevvy.state.HomeState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class DelHomeCommand {
    public static void register(Commands commands) {
        SuggestionProvider<CommandSourceStack> homeSuggestions = (context, builder) -> {
            if (!(context.getSource().getExecutor() instanceof Player player)) return builder.buildFuture();
            HomeState.getHomes(player.getUniqueId()).keySet().forEach(builder::suggest);
            return builder.buildFuture();
        };

        commands.register(
                Commands.literal("delhome")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(homeSuggestions)
                                .executes(context -> {
                                    if (!(context.getSource().getExecutor() instanceof Player player)) return 0;

                                    String homeName = StringArgumentType.getString(context, "name");
                                    if (HomeState.removeHome(player.getUniqueId(), homeName)) {
                                        CommandUtils.sendBilingual(player,
                                                Component.text("ホーム「", NamedTextColor.GRAY)
                                                        .append(Component.text(homeName, NamedTextColor.GREEN))
                                                        .append(Component.text("」を削除しました。", NamedTextColor.GRAY)),
                                                Component.text("Home '", NamedTextColor.GRAY)
                                                        .append(Component.text(homeName, NamedTextColor.GREEN))
                                                        .append(Component.text("' removed.", NamedTextColor.GRAY))
                                        );
                                    } else {
                                        CommandUtils.sendBilingual(player,
                                                Component.text("ホーム「", NamedTextColor.GRAY)
                                                        .append(Component.text(homeName, NamedTextColor.GREEN))
                                                        .append(Component.text("」が見つかりませんでした。", NamedTextColor.GRAY)),
                                                Component.text("Home '", NamedTextColor.GRAY)
                                                        .append(Component.text(homeName, NamedTextColor.GREEN))
                                                        .append(Component.text("' not found.", NamedTextColor.GRAY))
                                        );
                                    }
                                    return 1;
                                })
                        )
                        .build(),
                "Delete a named home"
        );
    }
}
