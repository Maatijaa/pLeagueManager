package me.github.maatijaa.pleaguemanager.configs;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

@Getter
public class Config {
  private static Plugin plugin;

  public static void setup(Plugin plugin) {
    Config.plugin = plugin;
  }

  public static YamlConfiguration getConfig(String configName) {
    String folder = plugin.getDataFolder().getAbsolutePath() + File.separatorChar;
    File file = new File(folder + configName);
    file.getParentFile().mkdirs();
    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
    if (!file.exists()) {
      plugin.getLogger().info("Config " + configName + " not found! Creating.");
      try {
        config.save(file);
      } catch (Exception exception) {
        plugin.getLogger().warning("Unable to create " + configName + ".yml!");
      }
    } else config = YamlConfiguration.loadConfiguration(file);
    return config;
  }

  public static void saveConfig(YamlConfiguration config, String configName) {
    File file = new File(plugin.getDataFolder().getAbsolutePath() + File.separatorChar + configName);
    try {
      config.save(file);
    } catch (IOException exception) {
      plugin.getLogger().warning("Unable to save " + configName + ".yml");
    }
  }
}
