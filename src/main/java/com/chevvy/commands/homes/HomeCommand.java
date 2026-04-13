package com.chevvy.commands.homes;

import com.chevvy.Home;
import com.chevvy.commands.teleport.BackCommand;
import com.chevvy.state.HomeState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class HomeCommand {

    public static void register(Commands commands) {
        SuggestionProvider<CommandSourceStack> homeSuggestions = (context, builder) -> {
            if (!(context.getSource().getExecutor() instanceof Player player)) return builder.buildFuture();
            Map<String, Home> homes = HomeState.getHomes(player.getUniqueId());
            homes.keySet().forEach(builder::suggest);
            return builder.buildFuture();
        };

        commands.register(
                Commands.literal("home")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player)) return 0;

                            Location bedLoc = player.getRespawnLocation();

                            if (bedLoc == null) {
                                CommandUtils.sendBilingual(player,
                                        Component.text("ホームベッドが設定されていません。ベッドを使ってスポーンポイントを設定してください。", NamedTextColor.GRAY),
                                        Component.text("You have no home bed set. Use a bed to set your spawn point.", NamedTextColor.GRAY));
                                return 0;
                            }

                            BackCommand.saveBackLocation(player, bedLoc.getWorld(), bedLoc.getX(), bedLoc.getY(), bedLoc.getZ());
                            CommandUtils.sendBilingual(player,
                                    Component.text("ホームベッドにテレポートしました！", NamedTextColor.GRAY),
                                    Component.text("Teleported to your home bed!", NamedTextColor.GRAY));
                            return 1;
                        })
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(homeSuggestions)
                                .executes(context -> {
                                    if (!(context.getSource().getExecutor() instanceof Player player)) return 0;

                                    String homeName = StringArgumentType.getString(context, "name");
                                    Home home = HomeState.getHome(player.getUniqueId(), homeName);

                                    if (home != null) {
                                        World world = CommandUtils.getWorldByKey(home.dimension());

                                        if (world != null) {
                                            BackCommand.saveBackLocation(player, world, home.pos().x(), home.pos().y(), home.pos().z());
                                            CommandUtils.sendBilingual(player,
                                                    Component.text("ホーム「", NamedTextColor.GRAY)
                                                            .append(Component.text(homeName, NamedTextColor.GREEN))
                                                            .append(Component.text("」にテレポートしました！", NamedTextColor.GRAY)),
                                                    Component.text("Teleported to home '", NamedTextColor.GRAY)
                                                            .append(Component.text(homeName, NamedTextColor.GREEN))
                                                            .append(Component.text("'!", NamedTextColor.GRAY))
                                            );
                                        } else {
                                            CommandUtils.sendBilingual(player,
                                                    Component.text("ホームが存在するワールドが見つかりません。", NamedTextColor.GRAY),
                                                    Component.text("The world for this home could not be found.", NamedTextColor.GRAY));
                                        }
                                    } else {
                                        CommandUtils.sendBilingual(player,
                                                Component.text("ホーム「", NamedTextColor.GRAY)
                                                        .append(Component.text(homeName, NamedTextColor.GREEN))
                                                        .append(Component.text("」が見つかりませんでした！", NamedTextColor.GRAY)),
                                                Component.text("Home '", NamedTextColor.GRAY)
                                                        .append(Component.text(homeName, NamedTextColor.GREEN))
                                                        .append(Component.text("' not found!", NamedTextColor.GRAY))
                                        );
                                    }
                                    return 1;
                                })
                        )
                        .build(),
                "Teleport to your home bed or a named home"
        );
    }
}
