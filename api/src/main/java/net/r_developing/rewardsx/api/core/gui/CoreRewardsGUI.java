package net.r_developing.rewardsx.api.core.gui;

import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.language.Messager;

import java.util.*;

// DEVE essere astratta se contiene metodi da sovrascrivere obbligatoriamente
public abstract class CoreRewardsGUI implements PlatformGUI {
    private final Fetcher fetcher;
    private final Messager messager;
    private final Buy buy;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 45;
    private final Map<UUID, ConfirmData> pendingConfirmations = new HashMap<>();

    public static class ConfirmData {
        public Map<String, String> reward;
        public int page;

        public ConfirmData(Map<String, String> reward, int page) {
            this.reward = reward;
            this.page = page;
        }
    }

    public CoreRewardsGUI(Fetcher fetcher, Messager messager, Buy buy) {
        this.fetcher = fetcher;
        this.messager = messager;
        this.buy = buy;
    }

    @Override
    public void open(RPlayer player) {
        open(player, 0);
    }

    public void open(RPlayer player, int page) {
        List<Map<String, String>> rewardsData = fetcher.getRewardsList();
        int totalRewards = rewardsData.size();

        // Evita divisione per 0 se ITEMS_PER_PAGE fosse 0, e calcola maxPage correttamente
        int maxPage = Math.max(0, (totalRewards - 1) / ITEMS_PER_PAGE);

        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        playerPages.put(player.getUniqueId(), page);

        // Calcolo indici
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalRewards);

        // Chiama il metodo astratto
        createAndOpenInventory(player, page, rewardsData, startIndex, endIndex, maxPage);
    }

    // Metodo astratto che le implementazioni specifiche (SpigotRewardsGUI) dovranno usare
    protected abstract void createAndOpenInventory(RPlayer player, int page, List<Map<String, String>> rewardsData, int startIndex, int endIndex, int maxPage);

    // Metodi helper per i dati (usati da SpigotRewardsGUI)
    protected Map<String, String> getRewardAt(int index) {
        if (index < 0 || index >= fetcher.getRewardsList().size()) return null;
        return fetcher.getRewardsList().get(index);
    }

    protected int getItemsPerPage() {
        return ITEMS_PER_PAGE;
    }

    protected Messager getMessager() {
        return messager;
    }

    protected Buy getBuy() {
        return buy;
    }

    protected Map<UUID, ConfirmData> getPendingConfirmations() {
        return pendingConfirmations;
    }

    protected Fetcher getFetchers() {
        return fetcher;
    }

    protected Map<UUID, Integer> getPlayerPages() {
        return playerPages;
    }

    @Override
    public void reload() {
        // Ricarica eventuali dati se necessario
    }
}