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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.*;

public class RewardsGUI extends CoreRewardsGUI implements Listener {
    private final Plugin plugin;
    private final PlatformLogger log;

    // Map to track which category each player is currently viewing
    private final Map<UUID, String> playerCategories = new HashMap<>();

    public RewardsGUI(Plugin plugin, Fetcher fetcher, Messager messager, Buy buy) {
        super(fetcher, messager, buy);
        this.plugin = plugin;
        this.log = plugin.getPlatformLogger();
    }

    @Override
    public void open(RPlayer rPlayer, int page) {
        Player player = (Player) rPlayer.getPlayer();
        String currentCat = playerCategories.getOrDefault(player.getUniqueId(), "All");
        openCategoryGUI(player, page, currentCat);
    }

    @Override
    protected void createAndOpenInventory(RPlayer player, int page, List<Map<String, String>> rewardsData, int startIndex, int endIndex, int maxPage) {
        // We ignore the standard Core parameters and redirect to our custom category system
        open(player, page);
    }

    /**
     * Builds the GUI with the category bar at the top and filtered rewards in the center.
     */
    private void openCategoryGUI(Player bukkitPlayer, int page, String category) {
        UUID uuid = bukkitPlayer.getUniqueId();
        playerCategories.put(uuid, category);
        getPlayerPages().put(uuid, page);

        String rawTitle = getMessager().get("guiTitle");
        String guiTitle = rawTitle != null ? rawTitle.replace("%s", String.valueOf(page + 1)) : "Rewards - Page " + (page + 1);

        Inventory gui = Bukkit.createInventory(null, 54, guiTitle);

        List<Map<String, String>> allRewards = getFetchers().getRewardsList();

        List<String> categories = new ArrayList<>();
        categories.add("All");
        for (Map<String, String> r : allRewards) {
            String cat = r.getOrDefault("category", "Default");
            if (!categories.contains(cat)) categories.add(cat);
        }

        for (int i = 0; i < 9; i++) {
            if (i < categories.size()) {
                String catName = categories.get(i);
                boolean isActive = catName.equals(category);

                ItemStack catItem = new ItemStack(isActive ? Material.ENCHANTED_BOOK : Material.BOOK);
                ItemMeta meta = catItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + catName);
                    if (isActive) {
                        meta.setLore(Collections.singletonList(ChatColor.GREEN + "● Selected"));
                    } else {
                        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to filter"));
                    }
                    catItem.setItemMeta(meta);
                }
                gui.setItem(i, catItem);
            } else {
                gui.setItem(i, createFiller());
            }
        }

        List<Map<String, String>> filteredRewards = new ArrayList<>();
        for (Map<String, String> r : allRewards) {
            if (category.equals("All") || r.getOrDefault("category", "Default").equals(category)) {
                filteredRewards.add(r);
            }
        }

        int itemsPerPage = 36;
        int maxPage = Math.max(0, (filteredRewards.size() - 1) / itemsPerPage);
        if (page > maxPage) page = maxPage;
        getPlayerPages().put(uuid, page);

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredRewards.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map<String, String> reward = filteredRewards.get(i);
            String name = reward.getOrDefault("name", "Unknown");
            String description = reward.getOrDefault("description", "");
            int cost = 0;
            try { cost = Integer.parseInt(reward.getOrDefault("cost", "0")); } catch (Exception ignored) {}

            ItemStack chest = new ItemStack(Material.CHEST);
            ItemMeta meta = chest.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + name);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GOLD + "Cost: " + cost + " bits");
                lore.addAll(wrapLore(description));
                meta.setLore(lore);
                chest.setItemMeta(meta);
            }
            gui.setItem(9 + (i - startIndex), chest);
        }

        for (int i = 45; i < 54; i++) gui.setItem(i, createFiller());

        if (page > 0) {
            gui.setItem(45, createButton(getMessager().get("previousPage")));
        }
        if (page < maxPage) {
            gui.setItem(53, createButton(getMessager().get("nextPage")));
        }

        bukkitPlayer.openInventory(gui);
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
            Method getTitle = InventoryView.class.getMethod("getTitle");
            getTitle.setAccessible(true);
            title = ChatColor.stripColor((String) getTitle.invoke(view));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        RPlayer player = new SpigotPlayer(bukkitPlayer);
        UUID uuid = bukkitPlayer.getUniqueId();

        // ------------------ CONFIRM PURCHASE ------------------
        String confirmTitleExpected = ChatColor.stripColor(getMessager().get("confirmTitle"));
        if (confirmTitleExpected != null && confirmTitleExpected.contains(" ")) {
            confirmTitleExpected = confirmTitleExpected.split(" ")[1];
        }

        if (title.contains(confirmTitleExpected != null ? confirmTitleExpected : "Confirm")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            ConfirmData data = getPendingConfirmations().get(uuid);
            if (data == null) return;

            if (slot == 11) { // YES
                String rewardId = data.reward.get("id");
                if (rewardId != null && !rewardId.isEmpty())
                    getBuy().send(player, rewardId);

                getPendingConfirmations().remove(uuid);
                bukkitPlayer.closeInventory();
            } else if (slot == 15) { // NO
                String cat = playerCategories.getOrDefault(uuid, "All");
                openCategoryGUI(bukkitPlayer, data.page, cat);
                getPendingConfirmations().remove(uuid);
            }
            return;
        }

        // ------------------ REWARDS / CATEGORY MENU ------------------
        String guiTitleExpected = ChatColor.stripColor(getMessager().get("guiTitle"));
        if (guiTitleExpected != null && guiTitleExpected.contains(" ")) {
            guiTitleExpected = guiTitleExpected.split(" ")[1];
        }

        if (!title.contains(guiTitleExpected != null ? guiTitleExpected : "Rewards")) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= clickedInventory.getSize()) return;

        String currentCategory = playerCategories.getOrDefault(uuid, "All");
        Integer currentPage = getPlayerPages().getOrDefault(uuid, 0);

        // Click on a Category (Slots 0-8)
        if (slot >= 0 && slot <= 8) {
            List<Map<String, String>> allRewards = getFetchers().getRewardsList();
            List<String> categories = new ArrayList<>();
            categories.add("All");
            for (Map<String, String> r : allRewards) {
                String cat = r.getOrDefault("category", "Default");
                if (!categories.contains(cat)) categories.add(cat);
            }
            if (slot < categories.size()) {
                String newCategory = categories.get(slot);
                if (!newCategory.equals(currentCategory)) {
                    openCategoryGUI(bukkitPlayer, 0, newCategory); // Reset to page 0 when switching categories
                }
            }
            return;
        }

        List<Map<String, String>> filteredRewards = new ArrayList<>();
        for (Map<String, String> r : getFetchers().getRewardsList()) {
            if (currentCategory.equals("All") || r.getOrDefault("category", "Default").equals(currentCategory)) {
                filteredRewards.add(r);
            }
        }

        // Click on pagination buttons (Slots 45 and 53)
        if (slot == 45 && currentPage > 0) {
            openCategoryGUI(bukkitPlayer, currentPage - 1, currentCategory);
            return;
        } else if (slot == 53) {
            int maxPage = Math.max(0, (filteredRewards.size() - 1) / 36);
            if (currentPage < maxPage) {
                openCategoryGUI(bukkitPlayer, currentPage + 1, currentCategory);
            }
            return;
        }

        // Click on a reward (Slots 9-44)
        if (slot >= 9 && slot <= 44) {
            int index = (currentPage * 36) + (slot - 9);
            if (index < filteredRewards.size()) {
                Map<String, String> selectedReward = filteredRewards.get(index);
                openConfirmation(bukkitPlayer, selectedReward, currentPage);
            }
        }
    }

    private void openConfirmation(Player player, Map<String, String> reward, int previousPage) {
        Inventory confirmGUI = Bukkit.createInventory(null, 27, getMessager().get("confirmTitle"));

        String name = reward.getOrDefault("name", "Unknown");
        int cost = 0;
        try { cost = Integer.parseInt(reward.getOrDefault("cost", "0")); } catch (Exception ignored) {}

        ItemStack info = new ItemStack(Material.CHEST);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            meta.setLore(Arrays.asList(
                    ChatColor.GOLD + "Cost: " + cost,
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

    private List<String> wrapLore(String text) {
        List<String> wrapped = new ArrayList<>();
        if (text == null || text.isBlank()) return wrapped;
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder(ChatColor.GRAY.toString());
        for (String word : words) {
            if (currentLine.length() - 2 + word.length() > 40) {
                wrapped.add(currentLine.toString().trim());
                currentLine = new StringBuilder(ChatColor.GRAY.toString());
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

    /**
     * Creates a black glass pane compatible with both old and new Minecraft versions
     * to decorate empty slots in the category menu.
     */
    @SuppressWarnings("deprecation")
    private ItemStack createFiller() {
        int v = 8;
        try { v = Integer.parseInt(Bukkit.getBukkitVersion().split("\\.")[1]); } catch (Exception ignored) {}
        if (v >= 13) {
            try { return new ItemStack(Material.valueOf("BLACK_STAINED_GLASS_PANE")); }
            catch (IllegalArgumentException e) {
                // Fallback if somehow 1.13+ doesn't have it
                return new ItemStack(Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 15);
            }
        }
        // Legacy support < 1.13
        return new ItemStack(Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 15);
    }

    @SuppressWarnings("deprecation")
    private ItemStack coloredWool(boolean red) {
        int v = 8;
        try { v = Integer.parseInt(Bukkit.getBukkitVersion().split("\\.")[1]); } catch (Exception ignored) {}
        if (v >= 13) {
            try { return new ItemStack(Material.valueOf((red ? "RED" : "GREEN") + "_WOOL")); }
            catch (IllegalArgumentException e) {
                // Fallback
                return new ItemStack(Material.valueOf("WOOL"), 1, (short)(red ? 14 : 5));
            }
        }
        // Legacy support < 1.13
        return new ItemStack(Material.valueOf("WOOL"), 1, (short)(red ? 14 : 5));
    }
}