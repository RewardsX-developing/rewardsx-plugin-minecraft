package net.R_Developing.rewardsx;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.event.connection.PostLoginEvent;

public class Events {
    private final Config config;
    private final Messager messager;
    private final Platform platform;

    public Events(Config config, Messager messager, Platform platform) {
        this.config = config;
        this.messager = messager;
        this.platform = platform;
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        if (platform.isDebug()) {
            System.out.println("Is allowed showing connect message: " + platform.isAllowedShowingConnectMessageOnJoin());
        }

        if (config.getUserId(player.getUniqueId()) == null && platform.isAllowedShowingConnectMessageOnJoin()) {
            player.sendMessage(messager.custom(messager.get("notConnected")));
        }
    }
}