package net.r_developing.rewardsx.api.core.platform;

public interface PlatformScheduler {
    /**
     * Esegue un task asincrono ripetitivo (fondamentale per chiamate API)
     * @param task L'azione da eseguire
     * @param delayTicks Ritardo iniziale
     * @param periodTicks Intervallo tra le esecuzioni
     */
    void runAsyncRepeatingTask(Runnable task, long delayTicks, long periodTicks);

    void runSync(Runnable task);

    void runAsync(Runnable task);

    void runTaskLater(Runnable task, long delayTicks);

    void cancelAll();
}