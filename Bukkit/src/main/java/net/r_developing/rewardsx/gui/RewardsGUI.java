package net.r_developing.rewardsx.gui;

import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.buy.Buy;
import net.r_developing.rewardsx.api.core.gui.CoreRewardsGUI;
import net.r_developing.rewardsx.api.core.language.Messager;
import net.r_developing.rewardsx.api.core.network.Fetcher;
import net.r_developing.rewardsx.api.core.platform.PlatformLogger;
import net.r_developing.rewardsx.api.core.player.RPlayer;
import net.r_developing.rewardsx.player.SpigotPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;


public class RewardsGUI extends CoreRewardsGUI implements Listener {
    private final Plugin plugin;
    private final PlatformLogger log;


    public RewardsGUI(Plugin plugin, Fetcher fetcher, Messager messager, Buy buy) {
        super(fetcher, messager, buy);
        this.plugin = plugin;
        this.log = plugin.getPlatformLogger();
    }

    @Override
    protected void createAndOpenInventory(RPlayer player, int page, List<Map<String, String>> rewardsData, int startIndex, int endIndex, int maxPage) {
        log.debug("Attempting to create and open GUI for player: " + player.getPlayerName() + ", page: " + page + " (Bounds: " + startIndex + " to " + endIndex + ", Max Page: " + maxPage + ")");

        if (!(player instanceof SpigotPlayer)) {
            log.debug("Player type check failed. Expected SpigotPlayer, but got: " + player.getClass().getSimpleName());
            plugin.getLogger().warning("Cannot open GUI: player is not a SpigotPlayer");
            return;
        }

        Player bukkitPlayer = (Player) player.getPlayer();
        String guiTitle = String.format(getMessager().get("guiTitle"), page + 1);

        log.debug("Creating 54-slot inventory with title: '" + guiTitle + "'");
        Inventory gui = Bukkit.createInventory(null, 54, guiTitle);

        log.debug("Populating GUI with rewards from index " + startIndex + " to " + (endIndex - 1));
        for (int i = startIndex; i < endIndex; i++) {
            Map<String, String> reward = getRewardAt(i);

            if (reward == null) {
                log.debug("Reward at index " + i + " is null. Skipping.");
                continue;
            }

            String name = String.valueOf(reward.getOrDefault("name", "Unknown"));
            String description = String.valueOf(reward.getOrDefault("description", ""));
            int cost = 0;

            try {
                cost = Integer.parseInt(String.valueOf(reward.getOrDefault("cost", "0")));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse cost for reward '" + name + "' at index " + i + ". Defaulting to 0. Raw value: " + reward.get("cost"));
            }

            log.debug("Constructing ItemStack for reward: '" + name + "' (Cost: " + cost + ")");
            ItemStack chest = new ItemStack(Material.CHEST);
            ItemMeta meta = chest.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + name);

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GOLD + String.valueOf(cost));

                lore.addAll(wrapLore(description, 40, ChatColor.GRAY));

                meta.setLore(lore);
                chest.setItemMeta(meta);
            }

            int slot = i - startIndex;
            gui.setItem(slot, chest);
            log.debug("Placed reward '" + name + "' in GUI slot " + slot);
        }

        if (page > 0) {
            log.debug("Page is > 0. Adding 'Previous Page' button at slot 45.");
            gui.setItem(45, createButton(getMessager().get("previousPage")));
        }

        if (page < maxPage) {
            log.debug("Page is < maxPage. Adding 'Next Page' button at slot 53.");
            gui.setItem(53, createButton(getMessager().get("nextPage")));
        }

        log.debug("Opening inventory for Bukkit player: " + bukkitPlayer.getName());
        bukkitPlayer.openInventory(gui);
    }


    /**
     * Wrap a lore with color
     */
    private List<String> wrapLore(String text, int lineLength, ChatColor color) {
        List<String> wrapped = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return wrapped;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder(color.toString());

        for (String word : words) {
            if (currentLine.length() - 2 + word.length() > lineLength) {
                wrapped.add(currentLine.toString().trim());
                currentLine = new StringBuilder(color.toString());
            }
            currentLine.append(word).append(" ");
        }

        wrapped.add(currentLine.toString().trim());
        return wrapped;
    }

    private ItemStack createButton(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Listener
    @EventHandler
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player bukkitPlayer = (Player) event.getWhoClicked();

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        InventoryView view = event.getView();
        String title;
        try {
            // Supporto legacy cross-version
            Method getTitle = InventoryView.class.getMethod("getTitle");
            getTitle.setAccessible(true);
            title = ChatColor.stripColor((String) getTitle.invoke(view));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        RPlayer player = new SpigotPlayer(bukkitPlayer);

        // CONFIRMATION BUY
        String confirmTitleExpected = ChatColor.stripColor(getMessager().get("confirmTitle"));
        if (confirmTitleExpected != null && confirmTitleExpected.contains(" ")) {
            confirmTitleExpected = confirmTitleExpected.split(" ")[1];
        }

        if (title.contains(confirmTitleExpected != null ? confirmTitleExpected : "Confirm")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            ConfirmData data = getPendingConfirmations().get(player.getUniqueId());
            if (data == null) return;

            if (slot == 11) {
                String rewardId = data.reward.get("id");
                if (rewardId != null && !rewardId.isEmpty())
                    getBuy().send(player, rewardId);

                getPendingConfirmations().remove(player.getUniqueId());
                bukkitPlayer.closeInventory();
            } else if (slot == 15) {
                open(player, data.page);
                getPendingConfirmations().remove(player.getUniqueId());
            }

            return;
        }

        // ALL REWARDS
        String guiTitleExpected = ChatColor.stripColor(getMessager().get("guiTitle"));
        if (guiTitleExpected != null && guiTitleExpected.contains(" ")) {
            guiTitleExpected = guiTitleExpected.split(" ")[1];
        }

        if (!title.contains(guiTitleExpected != null ? guiTitleExpected : "Rewards")) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= clickedInventory.getSize()) return;

        Integer currentPage = getPlayerPages().get(player.getUniqueId());
        if (currentPage == null) return;

        if (slot == 45 && currentPage > 0) {
            open(player, currentPage - 1);
            return;
        } else if (slot == 53) {
            List<Map<String, String>> rewardsData = getFetchers().getRewardsList();
            int maxPage = (rewardsData.size() - 1) / getItemsPerPage();
            if (currentPage < maxPage) open(player, currentPage + 1);
            return;
        }

        List<Map<String, String>> rewardsData = getFetchers().getRewardsList();
        int startIndex = currentPage * getItemsPerPage();
        int index = startIndex + slot;
        if (index >= rewardsData.size()) return;
        Map<String, String> selectedReward = rewardsData.get(index);

        openConfirmation(bukkitPlayer, selectedReward, currentPage);
    }

    private void openConfirmation(Player player, Map<String, String> reward, int previousPage) {
        Inventory confirmGUI = Bukkit.createInventory(null, 27, getMessager().get("confirmTitle"));

        String name = reward.getOrDefault("name", "Unknown");
        int cost = 0;
        try {
            cost = Integer.parseInt(reward.getOrDefault("cost", "0"));
        } catch (Exception ignored) {}

        ItemStack info = new ItemStack(Material.CHEST);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            meta.setLore(Arrays.asList(
                    ChatColor.GOLD + "" + cost,
                    ChatColor.GRAY + reward.getOrDefault("description", "")
            ));
            info.setItemMeta(meta);
        }
        confirmGUI.setItem(13, info);

        ItemStack yes = coloredWool(false);
        ItemMeta yesMeta = yes.getItemMeta();
        if (yesMeta != null) {
            yesMeta.setDisplayName(ChatColor.GREEN + getMessager().getNoPrefix("confirmYes"));
            yes.setItemMeta(yesMeta);
        }
        confirmGUI.setItem(11, yes);

        ItemStack no = coloredWool(true);
        ItemMeta noMeta = no.getItemMeta();
        if (noMeta != null) {
            noMeta.setDisplayName(ChatColor.RED + getMessager().getNoPrefix("confirmNo"));
            no.setItemMeta(noMeta);
        }
        confirmGUI.setItem(15, no);

        getPendingConfirmations().put(player.getUniqueId(), new ConfirmData(reward, previousPage));
        player.openInventory(confirmGUI);
    }

    private ItemStack coloredWool(boolean red) {
        int v = 8;
        try { v = Integer.parseInt(Bukkit.getBukkitVersion().split("\\.")[1]); } catch (Exception ignored) {}
        if (v >= 13) {
            try { return new ItemStack(Material.valueOf((red ? "RED" : "GREEN") + "_WOOL")); }
            catch (IllegalArgumentException e) { return new ItemStack(Material.WOOL); }
        }
        return new ItemStack(Material.WOOL, 1, (red ? DyeColor.RED.getWoolData() : DyeColor.GREEN.getWoolData()));
    }
}