package net.r_developing.rewardsx.config;

import net.r_developing.rewardsx.api.core.platform.PlatformConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class VelocityYamlConfig implements PlatformConfig {
    private final File file;
    private final Yaml yaml;
    private Map<String, Object> data;

    public VelocityYamlConfig(Path dataDirectory, String fileName) {
        File dataFolder = dataDirectory.toFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.file = new File(dataFolder, fileName);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.yaml = new Yaml(options);

        if (!file.exists()) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                System.err.println("[RewardsX] Could not create config file: " + file.getName() + " - " + e.getMessage());
            }
        }

        reload();
    }

    @Override
    public String getString(String path) {
        Object val = get(path);
        return val != null ? String.valueOf(val) : null;
    }

    @Override
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    @Override
    public int getInt(String path, int def) {
        Object val = get(path);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return def;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(String path) {
        if (data == null || path == null) return false;
        if (!path.contains(".")) {
            return data.containsKey(path);
        }
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return false;
            }
        }

        return current.containsKey(parts[parts.length - 1]);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        Object val = get(path);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return def;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(String path) {
        if (data == null || path == null) return null;
        if (!path.contains(".")) {
            return data.get(path);
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return null;
            }
        }
        return current.get(parts[parts.length - 1]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        if (data == null) {
            data = new LinkedHashMap<>();
        }

        if (!path.contains(".")) {
            if (value == null) {
                data.remove(path);
            } else {
                data.put(path, value);
            }
            return;
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                Map<String, Object> newSection = new LinkedHashMap<>();
                current.put(parts[i], newSection);
                current = newSection;
            } else {
                current = (Map<String, Object>) next;
            }
        }

        if (value == null) {
            current.remove(parts[parts.length - 1]);
        } else {
            current.put(parts[parts.length - 1], value);
        }
    }

    @Override
    public void set(String path, String userId) {
        set(path, (Object) userId);
    }

    @Override
    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            yaml.dump(data, writer);
        } catch (IOException e) {
            System.err.println("[RewardsX] Could not save config file: " + file.getName() + " - " + e.getMessage());
        }
    }

    @Override
    public void reload() {
        try (FileInputStream in = new FileInputStream(file)) {
            this.data = yaml.load(in);
            if (this.data == null) {
                this.data = new LinkedHashMap<>();
            }
        } catch (IOException e) {
            System.err.println("[RewardsX] Could not load config file: " + file.getName() + " - " + e.getMessage());
            this.data = new LinkedHashMap<>();
        }
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        Set<String> keys = new LinkedHashSet<>();
        if (data == null) return keys;

        if (!deep) {
            keys.addAll(data.keySet());
            return keys;
        }

        collectKeys(data, "", keys);
        return keys;
    }

    @SuppressWarnings("unchecked")
    private void collectKeys(Map<String, Object> section, String prefix, Set<String> keys) {
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            keys.add(fullKey);

            if (entry.getValue() instanceof Map) {
                collectKeys((Map<String, Object>) entry.getValue(), fullKey, keys);
            }
        }
    }
}