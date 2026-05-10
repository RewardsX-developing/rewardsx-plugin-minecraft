package net.r_developing.rewardsx;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.*;

public class RewardsGUI {
    private final Fetcher fetcher;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 45;
    private final Messager messager;
    private final Buy buy;
    private final Map<UUID, ConfirmData> pendingConfirmations = new HashMap<>();

    private static class ConfirmData {
        Map<String, String> reward;
        int page;

        ConfirmData(Map<String, String> reward, int page) {
            this.reward = reward;
            this.page = page;
        }
    }

    public RewardsGUI(Fetcher fetcher, Messager messager, Buy buy) {
        this.fetcher = fetcher;
        this.messager = messager;
        this.buy = buy;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<Map<String, String>> rewardsData = fetcher.getRewardsList();
        int totalRewards = rewardsData.size();
        int maxPage = (totalRewards - 1) / ITEMS_PER_PAGE;

        if(page < 0) page = 0;
        if(page > maxPage) page = maxPage;

        playerPages.put(player.getUniqueId(), page);

        Inventory gui = Bukkit.createInventory(null, 54, String.format(this.messager.get("guiTitle"), page + 1));

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalRewards);

        for(int i = startIndex; i < endIndex; i++) {
            Map<String, String> reward = rewardsData.get(i);

            String name = String.valueOf(reward.getOrDefault("name", "Unknown"));
            String description = String.valueOf(reward.getOrDefault("description", ""));
            int cost = 0;
            try {
                cost = Integer.parseInt(String.valueOf(reward.getOrDefault("cost", "0")));
            } catch (Exception ignored) {}

            ItemStack chest = new ItemStack(Material.CHEST);
            ItemMeta meta = chest.getItemMeta();
            if(meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + name);
                meta.setLore(Arrays.asList(
                    ChatColor.GOLD + "" + cost,
                    ChatColor.GRAY + description
                ));
                chest.setItemMeta(meta);
            }

            gui.setItem(i - startIndex, chest);
        }

        if(page > 0) gui.setItem(45, createButton(this.messager.get("previousPage")));
        if(page < maxPage) gui.setItem(53, createButton(this.messager.get("nextPage")));

        player.openInventory(gui);
    }

    private ItemStack createButton(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if(meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if(clickedInventory == null) return;

        InventoryView view = event.getView();
        String title;
        try {
            Method getTitle = InventoryView.class.getMethod("getTitle");
            getTitle.setAccessible(true);
            title = ChatColor.stripColor((String) getTitle.invoke(view));
        } catch(Exception e) {
            throw new RuntimeException();
        }

        // CONFIRMATION BUY
        if(title.contains(ChatColor.stripColor(messager.get("confirmTitle")).split(" ")[1])) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            ConfirmData data = pendingConfirmations.get(player.getUniqueId());
            if(data == null) return;

            if(slot == 11) {
                String rewardId = data.reward.get("id");
                if(rewardId != null && !rewardId.isEmpty())
                    buy.send(player, rewardId);

                pendingConfirmations.remove(player.getUniqueId());
                player.closeInventory();
            } else if(slot == 15) {
                open(player, data.page);
                pendingConfirmations.remove(player.getUniqueId());
            }

            return;
        }

        // ALL REWARDS
        if(!title.contains(ChatColor.stripColor(messager.get("guiTitle")).split(" ")[1])) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if(slot < 0 || slot >= clickedInventory.getSize()) return;

        Integer currentPage = playerPages.get(player.getUniqueId());
        if(currentPage == null) return;

        if(slot == 45 && currentPage > 0) {
            open(player, currentPage - 1);
            return;
        } else if(slot == 53) {
            List<Map<String, String>> rewardsData = fetcher.getRewardsList();
            int maxPage = (rewardsData.size() - 1) / ITEMS_PER_PAGE;
            if(currentPage < maxPage) open(player, currentPage + 1);
            return;
        }

        List<Map<String, String>> rewardsData = fetcher.getRewardsList();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int index = startIndex + slot;
        if (index >= rewardsData.size()) return;
        Map<String, String> selectedReward = rewardsData.get(index);

        openConfirmation(player, selectedReward, currentPage);
    }

    private void openConfirmation(Player player, Map<String, String> reward, int previousPage) {
        Inventory confirmGUI = Bukkit.createInventory(null, 27, messager.get("confirmTitle"));

        String name = reward.getOrDefault("name", "Unknown");
        int cost = 0;
        try {
            cost = Integer.parseInt(reward.getOrDefault("cost", "0"));
        } catch(Exception ignored){}

        ItemStack info = new ItemStack(Material.CHEST);
        ItemMeta meta = info.getItemMeta();
        if(meta != null) {
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
        if(yesMeta != null) {
            yesMeta.setDisplayName(ChatColor.GREEN + messager.getNoPrefix("confirmYes"));
            yes.setItemMeta(yesMeta);
        }
        confirmGUI.setItem(11, yes);

        ItemStack no = coloredWool(true);
        ItemMeta noMeta = no.getItemMeta();
        if(noMeta != null) {
            noMeta.setDisplayName(ChatColor.RED + messager.getNoPrefix("confirmNo"));
            no.setItemMeta(noMeta);
        }
        confirmGUI.setItem(15, no);

        pendingConfirmations.put(player.getUniqueId(), new ConfirmData(reward, previousPage));
        player.openInventory(confirmGUI);
    }

    private ItemStack coloredWool(boolean red) {
        int v = 8;
        try { v = Integer.parseInt(Bukkit.getBukkitVersion().split("\\.")[1]); } catch (Exception ignored) {}
        if(v >= 13) {
            try { return new ItemStack(Material.valueOf((red ? "RED" : "GREEN") + "_WOOL")); }
            catch (IllegalArgumentException e) { return new ItemStack(Material.WOOL); }
        }
        return new ItemStack(Material.WOOL, 1, (red ? DyeColor.RED.getWoolData() : DyeColor.GREEN.getWoolData()));
    }
}
