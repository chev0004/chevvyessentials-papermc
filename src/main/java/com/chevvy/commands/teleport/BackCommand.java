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
public class BackCommand {
    public static void register(Commands commands) {
        commands.register(
                Commands.literal("back")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player)) return 0;

                            BackLocation backLocation = BackState.getBackLocation(player.getUniqueId());

                            if (backLocation != null) {
                                World world = CommandUtils.getWorldByKey(backLocation.dimension());
                                Vec3d pos = backLocation.pos();

                                if (world != null) {
                                    // Save the current position before teleporting (so /back can be chained)
                                    saveBackLocation(player, world, pos.x(), pos.y(), pos.z());

                                    CommandUtils.sendBilingual(player,
                                            Component.text("前の場所に戻りました。", NamedTextColor.GRAY),
                                            Component.text("Teleported back to your previous location.", NamedTextColor.GRAY));
                                } else {
                                    CommandUtils.sendBilingual(player,
                                            Component.text("前の場所が存在するワールドが見つかりませんでした。", NamedTextColor.GRAY),
                                            Component.text("The world for your previous location could not be found.", NamedTextColor.GRAY));
                                }
                            } else {
                                CommandUtils.sendBilingual(player,
                                        Component.text("戻る場所がありません。", NamedTextColor.GRAY),
                                        Component.text("You have no previous location to return to.", NamedTextColor.GRAY));
                            }
                            return 1;
                        })
                        .build(),
                "Teleport to your previous location"
        );
    }

    public static void saveBackLocation(Player player, World world, double destX, double destY, double destZ) {
        Location loc = player.getLocation();
        Vec3d currentPos = new Vec3d(loc.getX(), loc.getY(), loc.getZ());
        BackLocation newBackLocation = new BackLocation(currentPos, player.getWorld().getKey().toString());
        BackState.setBackLocation(player.getUniqueId(), newBackLocation);

        player.teleport(new Location(world, destX, destY, destZ, loc.getYaw(), loc.getPitch()));
    }
}
