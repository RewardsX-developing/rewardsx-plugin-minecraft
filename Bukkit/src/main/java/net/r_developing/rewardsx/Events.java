package net.r_developing.rewardsx;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class Events implements Listener {
    private final RewardsGUI rewardsGUI;
    private final Config config;
    private final Messager messager;
    private final Platform platform;

    public Events(RewardsGUI rewardsGUI, Config config, Messager messager, Platform platform) {
        this.rewardsGUI = rewardsGUI;
        this.config = config;
        this.messager = messager;
        this.platform = platform;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(player == null) return;
        if(platform.isProxy()) return;

        if(config.getUserId(player.getUniqueId()) == null && platform.isAllowedShowingConnectMessageOnJoin()) {
            player.sendMessage(messager.get("notConnected"));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        rewardsGUI.handleClick(event);
    }
}
