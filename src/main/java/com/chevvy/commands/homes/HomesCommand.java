package com.chevvy.commands.homes;

import com.chevvy.Home;
import com.chevvy.state.HomeState;
import com.chevvy.util.CommandUtils;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class HomesCommand {
    public static void register(Commands commands) {
        commands.register(
                Commands.literal("homes")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player)) return 0;

                            Map<String, Home> homes = HomeState.getHomes(player.getUniqueId());
                            if (homes.isEmpty()) {
                                CommandUtils.sendBilingual(player,
                                        Component.text("ホームが設定されていません。", NamedTextColor.GRAY)
                                                .append(Component.text("/sethome <name>", NamedTextColor.AQUA))
                                                .append(Component.text(" を使用してください。", NamedTextColor.GRAY)),
                                        Component.text("You have no homes set. Use ", NamedTextColor.GRAY)
                                                .append(Component.text("/sethome <name>", NamedTextColor.AQUA))
                                                .append(Component.text(".", NamedTextColor.GRAY))
                                );
                            } else {
                                Component jpList = Component.text("ホーム一覧: ", NamedTextColor.GRAY);
                                Component enList = Component.text("Your homes: ", NamedTextColor.GRAY);

                                Iterator<String> iterator = homes.keySet().iterator();
                                while (iterator.hasNext()) {
                                    String homeName = iterator.next();
                                    jpList = jpList.append(Component.text(homeName, NamedTextColor.GREEN));
                                    enList = enList.append(Component.text(homeName, NamedTextColor.GREEN));
                                    if (iterator.hasNext()) {
                                        jpList = jpList.append(Component.text(", ", NamedTextColor.GRAY));
                                        enList = enList.append(Component.text(", ", NamedTextColor.GRAY));
                                    }
                                }
                                CommandUtils.sendBilingual(player, jpList, enList);
                            }
                            return 1;
                        })
                        .build(),
                "List all your homes"
        );
    }
}
