package me.github.maatijaa.pleaguemanager;

import co.aikar.commands.BukkitCommandManager;
import com.avaje.ebean.Update;
import me.github.maatijaa.pleaguemanager.commands.*;
import me.github.maatijaa.pleaguemanager.commands.timers.*;
import me.github.maatijaa.pleaguemanager.configs.Config;
import me.github.maatijaa.pleaguemanager.configs.Lang;
import lombok.Getter;
import lombok.Setter;
import me.github.maatijaa.pleaguemanager.managers.ConfigManager;
import me.github.maatijaa.pleaguemanager.managers.GUIManager;
import me.github.maatijaa.pleaguemanager.managers.ListenerManager;
import me.github.maatijaa.pleaguemanager.managers.UtilManager;
import net.luckperms.api.LuckPerms;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.github.maatijaa.pleaguemanager.utils.UpdateChecker;

@Setter
@Getter
public class LeagueManager extends JavaPlugin {
  private ConfigManager configManager = new ConfigManager(this, "");
  private YamlConfiguration config;
  private LuckPerms luckPermsAPI = null;
  private boolean essentialsLoaded = false;
  private UtilManager utilManager;
  private GUIManager guiManager;
  private ListenerManager listenerManager;
  @Getter
  private static LeagueManager instance;

  @Override
  public void onEnable() {

    // part of update checker feature
    String versionUrl = "https://raw.githubusercontent.com/Maatijaa/pLeagueManager/refs/heads/master/version";
    new UpdateChecker(this, versionUrl).checkForUpdates();
    // --------------------------------------------------------------

    instance = this;
    getServer().getScheduler().cancelTasks(getInstance());
    setupMessages();
    setupConfig();
    setupLuckPermsAPI();
    setupManagers();
    setupCommands();
    setupListeners();

    getUtilManager().getLogger().sendBanner();
    getUtilManager().getLogger().info("Successfully enabled!");
  }

  @Override
  public void onDisable() {
    if (this.listenerManager != null) {
      getListenerManager().unregisterListeners();
    }
    getServer().getScheduler().cancelTasks(getInstance());
  }

  private void setupConfig() {
    Config.setup(this);
    this.configManager.loadConfig("#pLeague Manager Config", "config.yml");
    this.config = Config.getConfig("config.yml");
  }

  private void setupLuckPermsAPI() {
    RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
      this.luckPermsAPI = provider.getProvider();
    } else {
      getLogger().warning("LuckPerms not found! Disabling plugin due to missing dependency.");
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  private void setupManagers() {
    this.utilManager = new UtilManager(this);
    this.guiManager = new GUIManager();
    this.listenerManager = new ListenerManager(this, this.utilManager, this.guiManager);
  }

  public void setupMessages() {
    getConfigManager().createNewFile("messages.yml", "Loading messages.yml", "LeagueManager Messages");
    loadMessages();
  }

  private void loadMessages() {
    Lang.setFile(getConfigManager().getConfig("messages.yml"));

    for (Lang value : Lang.values())
      getConfigManager().getConfig("messages.yml").addDefault(value.getPath(), value.getDefault());

    getConfigManager().getConfig("messages.yml").options().copyDefaults(true);
    getConfigManager().saveConfig("messages.yml");
  }

  private void setupCommands() {
    BukkitCommandManager commandManager = new BukkitCommandManager(this);
    //noinspection deprecation
    commandManager.enableUnstableAPI("help");
    commandManager.registerCommand(new LMCommand(getUtilManager()));
    commandManager.registerCommand(new VARCommand(getUtilManager()));
    commandManager.registerCommand(new RostersCommand(getUtilManager(), getGuiManager()));
    commandManager.registerCommand(new MigrateCommand(getUtilManager()));
    commandManager.registerCommand(new StatisticsCommand(getUtilManager()));
    commandManager.registerCommand(new ResultCommand(this, getUtilManager()));
    commandManager.registerCommand(new TimerCommand(this, getUtilManager()));
    commandManager.registerCommand(new OXECommand(this, getUtilManager()));
    commandManager.registerCommand(new TXFCommand(this, getUtilManager()));
  }

  private void setupListeners() {
    if (this.listenerManager.isRegistered()) {
      this.listenerManager.unregisterListeners();
    }
    this.listenerManager.registerListeners();
  }
}
