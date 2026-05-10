package net.R_Developing.rewardsx;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.config.Configuration;

public class Messager {
    private final Config config;
    private Configuration messagesConfig;
    private String prefix;

    public Messager(Config config) {
        this.config = config;
        reload();
    }

    public BaseComponent[] getMessage(String key) {
        String rawMessage = get(key);
        Component comp = LegacyComponentSerializer.legacySection().deserialize(rawMessage.replace("&", "§").trim());
        return toBaseComponents(comp);
    }

    public BaseComponent[] custom(String message) {
        Component comp = LegacyComponentSerializer.legacySection().deserialize(message.replace("&", "§").trim());
        return toBaseComponents(comp);
    }

    public String get(String key) {
        Object messageObj = this.messagesConfig.get(key);
        String message = messageObj != null ? messageObj.toString() : "";
        return (prefix + message).replace("&", "§").trim();
    }

    public String getNoPrefix(String key) {
        Object messageObj = this.messagesConfig.get(key);
        return messageObj != null ? messageObj.toString().replace("&", "§").trim() : "";
    }

    public void reload() {
        this.messagesConfig = config.getMessagesConfig();
        Object prefixObj = this.messagesConfig.get("prefix");
        this.prefix = prefixObj != null ? prefixObj.toString() : "";
    }

    private BaseComponent[] toBaseComponents(Component component) {
        String legacyText = LegacyComponentSerializer.legacySection().serialize(component);
        return new BaseComponent[] { new TextComponent(legacyText) };
    }
}
