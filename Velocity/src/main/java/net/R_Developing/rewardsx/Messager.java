package net.R_Developing.rewardsx;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

public class Messager {
    private final Config config;
    private Map<String, Object> messagesConfig;
    private String prefix;

    public Messager(Config config) {
        this.config = config;
        reload();
    }

    public Component getMessage(String key) {
        String rawMessage = get(key);
        return LegacyComponentSerializer.legacySection().deserialize(rawMessage);
    }

    public Component custom(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message.replace("&", "§"));
    }

    public String get(String key) {
        Object messageObj = this.messagesConfig.get(key);
        String message = messageObj != null ? messageObj.toString() : "";
        return (prefix + message).replace("&", "§");
    }

    public String getNoPrefix(String key) {
        Object messageObj = this.messagesConfig.get(key);
        return messageObj != null ? messageObj.toString() : "";
    }

    public void reload() {
        this.messagesConfig = config.getMessagesConfig();
        Object prefixObj = this.messagesConfig.get("prefix");
        this.prefix = prefixObj != null ? prefixObj.toString() : "";
    }
}