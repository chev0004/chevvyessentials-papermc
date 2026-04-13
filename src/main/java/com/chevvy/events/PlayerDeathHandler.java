package com.chevvy.events;

import com.chevvy.DeathLocation;
import com.chevvy.Vec3d;
import com.chevvy.state.DeathState;
import com.chevvy.util.CommandUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathHandler implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location loc = player.getLocation();
        Vec3d pos = new Vec3d(loc.getX(), loc.getY(), loc.getZ());
        DeathLocation location = new DeathLocation(pos, player.getWorld().getKey().toString());
        DeathState.setDeathLocation(player.getUniqueId(), location);

        // Notify the player that their death location has been saved.
        // This message will appear in their chat upon respawning.
        CommandUtils.sendBilingual(player,
                Component.text("死亡した場所が保存されました。", NamedTextColor.GRAY)
                        .append(Component.text("/death", NamedTextColor.YELLOW))
                        .append(Component.text(" で戻ることができます。", NamedTextColor.GRAY)),
                Component.text("Your death location has been saved. Use ", NamedTextColor.GRAY)
                        .append(Component.text("/death", NamedTextColor.YELLOW))
                        .append(Component.text(" to return.", NamedTextColor.GRAY))
        );
    }
}
