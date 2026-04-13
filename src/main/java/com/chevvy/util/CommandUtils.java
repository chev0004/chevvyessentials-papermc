package com.chevvy.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CommandUtils {
    public static void sendBilingual(Player player, Component jp, Component en) {
        if (player != null) {
            player.sendMessage(jp);
            player.sendMessage(en);
        }
    }

    public static void broadcastBilingual(Component jp, Component en) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(jp);
            p.sendMessage(en);
        }
    }

    public static World getWorldByKey(String dimensionKey) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getKey().toString().equals(dimensionKey)) {
                return world;
            }
        }
        return null;
    }
}
