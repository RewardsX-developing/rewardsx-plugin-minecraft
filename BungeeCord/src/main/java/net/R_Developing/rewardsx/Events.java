package net.R_Developing.rewardsx;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.event.EventHandler;

public class Events {
    private final Config config;
    private final Messager messager;
    private final Platform platform;

    public Events(Config config, Messager messager, Platform platform) {
        this.config = config;
        this.messager = messager;
        this.platform = platform;
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player == null) return;

        if (platform.isDebug()) {
            System.out.println("Is allowed showing connect message: " + platform.isAllowedShowingConnectMessageOnJoin());
        }

        if (config.getUserId(player.getUniqueId()) == null && platform.isAllowedShowingConnectMessageOnJoin()) {
            player.sendMessage(messager.custom(messager.get("notConnected")));
        }
    }
}