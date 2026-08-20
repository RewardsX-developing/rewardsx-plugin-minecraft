package net.r_developing.rewardsx.api.core.platform;

import net.r_developing.rewardsx.api.core.player.RPlayer;

import java.util.List;

public interface PlatformCommandExecutor {

    /**
     * Esegue un comando come Console, processando anche eventuali placeholder (es. %player%).
     * @param command Il comando da eseguire senza la barra iniziale
     * @param targetPlayer Il giocatore (online) bersaglio, oppure null
     * @param targetName Il nome del giocatore (utile se il giocatore è offline)
     */
    void dispatchConsoleCommand(String command, RPlayer targetPlayer, String targetName);

    /**
     * Esegue un comando.
     * @param sender Il sender astratto (RPlayer o Console)
     * @param args Gli argomenti del comando
     * @return true se il comando è stato gestito
     */
    boolean execute(RPlayer sender, String[] args);

    boolean executeConsole(String senderName, String[] args);

    /**
     * Restituisce i suggerimenti per il tab complete.
     */
    List<String> getTabCompletions(String[] args);
}