package com.chevvy.commands.teleport;

import com.chevvy.BackLocation;
import com.chevvy.Vec3d;
import com.chevvy.state.BackState;
import com.chevvy.util.CommandUtils;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class SpawnCommand {
    public static void register(Commands commands) {
        commands.register(
                Commands.literal("spawn")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player)) return 0;

                            World overworld = CommandUtils.getWorldByKey("minecraft:overworld");

                            if (overworld == null) {
                                CommandUtils.sendBilingual(player,
                                        Component.text("オーバーワールドが見つかりませんでした。", NamedTextColor.GRAY),
                                        Component.text("Overworld could not be found.", NamedTextColor.GRAY));
                                return 0;
                            }

                            // Save the current position before teleporting
                            Location loc = player.getLocation();
                            Vec3d currentPos = new Vec3d(loc.getX(), loc.getY(), loc.getZ());
                            BackLocation backLocation = new BackLocation(currentPos, player.getWorld().getKey().toString());
                            BackState.setBackLocation(player.getUniqueId(), backLocation);

                            // Get spawn position from world spawn point
                            Location spawnLoc = overworld.getSpawnLocation();

                            // Teleport to spawn
                            player.teleport(new Location(overworld, spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ(),
                                    loc.getYaw(), loc.getPitch()));

                            CommandUtils.sendBilingual(player,
                                    Component.text("ワールドスポーンにテレポートしました。", NamedTextColor.GRAY),
                                    Component.text("Teleported to world spawn.", NamedTextColor.GRAY));

                            return 1;
                        })
                        .build(),
                "Teleport to world spawn"
        );
    }
}
