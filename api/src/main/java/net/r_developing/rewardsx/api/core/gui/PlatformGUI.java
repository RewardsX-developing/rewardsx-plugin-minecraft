package net.r_developing.rewardsx.api.core.gui;

import net.r_developing.rewardsx.api.core.player.RPlayer;

/**
 * Interfaccia astratta per la gestione delle GUI multi-piattaforma.
 */
public interface PlatformGUI {

    /**
     * Apre la GUI per il giocatore specificato.
     * @param player Il giocatore a cui aprire la GUI
     */
    void open(RPlayer player);

    /**
     * Ricarica la configurazione della GUI (es. dopo un /reload).
     */
    void reload();
}