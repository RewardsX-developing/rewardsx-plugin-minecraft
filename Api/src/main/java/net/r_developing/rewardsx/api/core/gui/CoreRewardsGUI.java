package net.r_developing.rewardsx.api.core.gui;

import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.language.Messager;

import java.util.*;

/**
 * Abstract base class for the rewards shop GUI.
 *
 * <p>Defines the pagination logic and state management for browsing rewards across
 * multiple pages. The actual GUI rendering (inventory/screen display) is delegated
 * to platform-specific subclasses (SpigotRewardsGUI, VelocityRewardsGUI, etc.) via
 * the abstract createAndOpenInventory() method.
 *
 * <p>Manages two main pieces of per-player state:
 *   - playerPages - which page each player is currently viewing
 *   - pendingConfirmations - reward details for a player who just clicked "purchase"
 *
 * <p>Rewards are fetched from the backend via Fetcher and paginated client-side
 * (45 items per page). When a player clicks a reward, the GUI updates the pending
 * confirmation and the platform-specific handler takes it from there (usually
 * redirecting to the web purchase flow or executing a command).
 */
public abstract class CoreRewardsGUI implements PlatformGUI {
    /** HTTP client to fetch the rewards list from the backend. */
    private final Fetcher fetcher;
    /** Localized message strings. */
    private final Messager messager;
    /** Handler for purchase initiation and reward grant. */
    private final Buy buy;

    /** Tracks which page each player is currently viewing (keyed by UUID). */
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    /** Number of reward items displayed per page. */
    private static final int ITEMS_PER_PAGE = 45;

    /** Stores reward details for a player who clicked purchase - used for confirmation dialogs. */
    private final Map<UUID, ConfirmData> pendingConfirmations = new HashMap<>();

    /**
     * Holds the reward data and page number for a pending purchase.
     *
     * <p>Used when a player clicks on a reward and a confirmation screen is shown
     * before actually initiating the purchase flow.
     */
    public static class ConfirmData {
        /** The reward data (name, description, price, etc.). */
        public Map<String, String> reward;
        /** The page on which this reward appeared (so we can return to it after purchase). */
        public int page;

        /** Simple constructor. */
        public ConfirmData(Map<String, String> reward, int page) {
            this.reward = reward;
            this.page = page;
        }
    }

    /** Constructor injection - no logic here, just wiring dependencies. */
    public CoreRewardsGUI(Fetcher fetcher, Messager messager, Buy buy) {
        this.fetcher = fetcher;
        this.messager = messager;
        this.buy = buy;
    }

    /**
     * Opens the rewards GUI for a player, starting at page 0 (first page).
     *
     * <p>Convenience method that delegates to open(player, 0).
     *
     * @param player the player to show the GUI to
     */
    @Override
    public void open(RPlayer player) {
        open(player, 0);
    }

    /**
     * Opens the rewards GUI for a player at a specific page.
     *
     * <p>Fetches the rewards list from the backend, validates the page number
     * (clamps to valid range), calculates the slice of rewards to display
     * (startIndex to endIndex), and delegates the actual inventory/screen creation
     * to the abstract createAndOpenInventory() method.
     *
     * <p>Also updates the playerPages map so that if the player navigates away
     * and returns, they are on the same page they left.
     *
     * @param player the player to show the GUI to
     * @param page   the zero-indexed page number (0 is the first page)
     */
    public void open(RPlayer player, int page) {
        // Fetch the full rewards list from the backend.
        List<Map<String, String>> rewardsData = fetcher.getRewardsList();
        int totalRewards = rewardsData.size();

        // Calculate the max page index, avoiding division by zero and ensuring maxPage is 0
        // if there are fewer than ITEMS_PER_PAGE rewards (so all fit on page 0).
        int maxPage = Math.max(0, (totalRewards - 1) / ITEMS_PER_PAGE);

        // Clamp the requested page to the valid range [0, maxPage].
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        // Remember which page this player is on (for later navigation or if they close/reopen).
        playerPages.put(player.getUniqueId(), page);

        // Calculate which rewards to show on this page.
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalRewards);

        // Delegate to the platform-specific implementation to build and display the inventory.
        createAndOpenInventory(player, page, rewardsData, startIndex, endIndex, maxPage);
    }

    /**
     * Abstract method for platform-specific GUI rendering.
     *
     * <p>Subclasses (SpigotRewardsGUI, VelocityRewardsGUI, etc.) implement this
     * to create a native inventory or web UI and display it to the player.
     * The rewards slice (from startIndex to endIndex) is already prepared by open().
     *
     * @param player        the player to show the GUI to
     * @param page          the current page number
     * @param rewardsData   the complete rewards list from the backend
     * @param startIndex    the index of the first reward to display
     * @param endIndex      the index after the last reward to display
     * @param maxPage       the highest valid page number
     */
    protected abstract void createAndOpenInventory(RPlayer player, int page, List<Map<String, String>> rewardsData, int startIndex, int endIndex, int maxPage);

    /**
     * Helper to retrieve a single reward by its index in the rewards list.
     *
     * <p>Used by subclasses to fetch reward details when building the inventory.
     * Returns null if the index is out of bounds.
     *
     * @param index the zero-indexed position in the rewards list
     * @return the reward data, or null if not found
     */
    protected Map<String, String> getRewardAt(int index) {
        if (index < 0 || index >= fetcher.getRewardsList().size()) return null;
        return fetcher.getRewardsList().get(index);
    }

    /**
     * Returns the page size constant.
     *
     * <p>Subclasses use this to know how many items fit on each page.
     *
     * @return the number of rewards displayed per page
     */
    protected int getItemsPerPage() {
        return ITEMS_PER_PAGE;
    }

    /**
     * Returns the messager for localized strings.
     *
     * <p>Subclasses use this to fetch button labels, error messages, etc.
     *
     * @return the messager instance
     */
    protected Messager getMessager() {
        return messager;
    }

    /**
     * Returns the buy handler.
     *
     * <p>Subclasses call this to initiate a purchase when a player clicks a reward.
     *
     * @return the buy instance
     */
    protected Buy getBuy() {
        return buy;
    }

    /**
     * Returns the pending confirmations map.
     *
     * <p>Subclasses use this to store reward data when a player clicks, and to
     * retrieve it later for confirmation dialogs.
     *
     * @return the pending confirmations map keyed by player UUID
     */
    protected Map<UUID, ConfirmData> getPendingConfirmations() {
        return pendingConfirmations;
    }

    /**
     * Returns the fetcher (backend API client).
     *
     * <p>Subclasses use this to access the rewards list and other backend data.
     *
     * @return the fetcher instance
     */
    protected Fetcher getFetchers() {
        return fetcher;
    }

    /**
     * Returns the player pages map.
     *
     * <p>Subclasses use this to check which page a player is on, or to implement
     * next/previous page navigation.
     *
     * @return the player pages map keyed by player UUID
     */
    protected Map<UUID, Integer> getPlayerPages() {
        return playerPages;
    }

    /**
     * Called when the plugin is reloaded (e.g. /rewardsx reload).
     *
     * <p>Subclasses can override this to refresh cached data if needed.
     * Currently, a no-op in the base class.
     */
    @Override
    public void reload() {
        // Reload any cached data if necessary (subclass implementation).
    }
}