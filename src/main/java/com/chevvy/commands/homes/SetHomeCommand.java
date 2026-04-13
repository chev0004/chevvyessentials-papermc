package com.chevvy.commands.homes;

import com.chevvy.Home;
import com.chevvy.Vec3d;
import com.chevvy.config.ModConfig;
import com.chevvy.state.HomeState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class SetHomeCommand {
    public static void register(Commands commands) {
        commands.register(
                Commands.literal("sethome")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    if (!(source.getExecutor() instanceof Player player)) return 0;

                                    String homeName = StringArgumentType.getString(context, "name");
                                    Map<String, Home> playerHomes = HomeState.getHomes(player.getUniqueId());

                                    int maxHomes = ModConfig.get().maxHomes;
                                    if (!playerHomes.containsKey(homeName) && playerHomes.size() >= maxHomes) {
                                        CommandUtils.sendBilingual(player,
                                                Component.text("ホームの最大数（", NamedTextColor.GRAY)
                                                        .append(Component.text(String.valueOf(maxHomes), NamedTextColor.GREEN))
                                                        .append(Component.text("）に達しました。", NamedTextColor.GRAY)),
                                                Component.text("You have reached the maximum number of homes (", NamedTextColor.GRAY)
                                                        .append(Component.text(String.valueOf(maxHomes), NamedTextColor.GREEN))
                                                        .append(Component.text(").", NamedTextColor.GRAY))
                                        );
                                        return 0;
                                    }

                                    Location loc = player.getLocation();
                                    Vec3d pos = new Vec3d(loc.getX(), loc.getY(), loc.getZ());
                                    String dimension = player.getWorld().getKey().toString();

                                    Home newHome = new Home(pos, dimension);
                                    HomeState.setHome(player.getUniqueId(), homeName, newHome);

                                    CommandUtils.sendBilingual(player,
                                            Component.text("ホーム「", NamedTextColor.GRAY)
                                                    .append(Component.text(homeName, NamedTextColor.GREEN))
                                                    .append(Component.text("」が設定されました！", NamedTextColor.GRAY)),
                                            Component.text("Home '", NamedTextColor.GRAY)
                                                    .append(Component.text(homeName, NamedTextColor.GREEN))
                                                    .append(Component.text("' set!", NamedTextColor.GRAY))
                                    );
                                    return 1;
                                })
                        )
                        .build(),
                "Set a home at your current location"
        );
    }
}
