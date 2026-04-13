package com.chevvy.commands.death;

import com.chevvy.DeathLocation;
import com.chevvy.commands.teleport.BackCommand;
import com.chevvy.state.DeathState;
import com.chevvy.util.CommandUtils;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class DeathCommand {
    public static void register(Commands commands) {
        commands.register(
                Commands.literal("death")
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player)) return 0;

                            DeathLocation deathLocation = DeathState.getDeathLocation(player.getUniqueId());

                            if (deathLocation != null) {
                                World world = CommandUtils.getWorldByKey(deathLocation.dimension());

                                if (world != null) {
                                    // Save the current position before teleporting
                                    BackCommand.saveBackLocation(player, world,
                                            deathLocation.pos().x(), deathLocation.pos().y(), deathLocation.pos().z());

                                    CommandUtils.sendBilingual(player,
                                            Component.text("最後の死亡場所に戻りました。", NamedTextColor.GRAY),
                                            Component.text("Teleported to your last death location.", NamedTextColor.GRAY)
                                    );
                                } else {
                                    CommandUtils.sendBilingual(player,
                                            Component.text("死亡したワールドが見つかりませんでした。", NamedTextColor.GRAY),
                                            Component.text("The world you died in could not be found.", NamedTextColor.GRAY)
                                    );
                                }
                            } else {
                                CommandUtils.sendBilingual(player,
                                        Component.text("保存されている死亡場所はありません。", NamedTextColor.GRAY),
                                        Component.text("You have no saved death location.", NamedTextColor.GRAY)
                                );
                            }
                            return 1;
                        })
                        .build(),
                "Teleport to your last death location"
        );
    }
}
