package net.r_developing.rewardsx.gui;

import net.r_developing.rewardsx.api.core.gui.PlatformGUI;
import net.r_developing.rewardsx.api.core.player.RPlayer;

public class VelocityDummyGUI implements PlatformGUI {
    @Override
    public void open(RPlayer player) {
        // Puoi mandare un messaggio testuale tipo:
        player.sendMessage("§cGUI doesn't work on proxy. Use /rewardsx buy on Spigot/Paper server.");
    }

    @Override
    public void reload() {
        // Nessuna GUI da ricaricare
    }
}