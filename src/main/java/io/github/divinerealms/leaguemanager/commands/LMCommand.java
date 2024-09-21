package io.github.divinerealms.leaguemanager.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import io.github.divinerealms.leaguemanager.LeagueManager;
import io.github.divinerealms.leaguemanager.configs.Config;
import io.github.divinerealms.leaguemanager.configs.Lang;
import io.github.divinerealms.leaguemanager.managers.DataManager;
import io.github.divinerealms.leaguemanager.managers.UtilManager;
import io.github.divinerealms.leaguemanager.utils.CubeCleaner;
import io.github.divinerealms.leaguemanager.utils.Helper;
import io.github.divinerealms.leaguemanager.utils.Logger;
import io.github.divinerealms.leaguemanager.utils.Time;
import lombok.Getter;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
@CommandAlias("leaguemanager|lm")
public class LMCommand extends BaseCommand {
  private final LeagueManager instance;
  private final UtilManager utilManager;
  private final Logger logger;
  private final Helper helper;
  private final CubeCleaner cubeCleaner;
  private final DataManager dataManager;
  private final String playerDataFolder = "playerdata";

  public LMCommand(final UtilManager utilManager) {
    this.instance = LeagueManager.getInstance();
    this.utilManager = utilManager;
    this.logger = utilManager.getLogger();
    this.helper = utilManager.getHelper();
    this.cubeCleaner = utilManager.getCubeCleaner();
    this.dataManager = new DataManager(utilManager.getPlugin());
  }

  @Default
  public void onBase(CommandSender sender) {
    if (sender instanceof Player) {
      for (String message : startupBanner()) {
        getLogger().send(sender, ChatColor.translateAlternateColorCodes('&', message));
      }
    } else {
      getLogger().sendBanner();
    }
  }

  @CatchUnknown
  public void onUnknown(CommandSender sender) {
    getLogger().send(sender, Lang.UNKNOWN_COMMAND.getConfigValue(null));
  }

  @Subcommand("help")
  @CommandPermission("leaguemanager.command.help")
  public void onHelp(CommandSender sender, CommandHelp help) {
    getLogger().send(sender, Lang.HELP.getConfigValue(null));
  }

  @Subcommand("reload")
  @CommandPermission("leaguemanager.command.reload")
  public void onReload(CommandSender sender) {
    getInstance().onEnable();
    getLogger().send(sender, Lang.RELOAD.getConfigValue(null));
  }

  @Subcommand("clearcubes|cc")
  @CommandPermission("leaguemanager.command.clearcubes")
  public void onClearCube(CommandSender sender) {
    if (getCubeCleaner().isEmpty()) {
      getLogger().send(sender, Lang.CUBES_EMPTY.getConfigValue(null));
      return;
    }

    getCubeCleaner().clearCubes();
    getLogger().broadcast(Lang.CLEARED_CUBES.getConfigValue(new String[]{String.valueOf(getCubeCleaner().getAmount())}));
  }

  @Subcommand("setspawn")
  @CommandPermission("leaguemanager.command.setspawn")
  public void onSetSpawn(CommandSender sender) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      YamlConfiguration config = Config.getConfig("config.yml");
      Location location = player.getLocation();

      config.set("spawn", location);
      Config.saveConfig(config, "config.yml");

      getLogger().send(player, Lang.PRACTICE_AREA_SET.getConfigValue(new String[]{"spawn", String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ())}));
    } else {
      getLogger().send(sender, Lang.INGAME_ONLY.getConfigValue(null));
    }
  }

  @Subcommand("toggle")
  @CommandPermission("leaguemanager.command.toggle")
  public void onToggle(CommandSender sender) {
    String state;
    Server server = getInstance().getServer();
    if (getUtilManager().isFcEnabled()) {
      state = Lang.OFF.getConfigValue(null);
      getHelper().groupAddPermission("default", "leaguemanager.footcube", "football", false);
      getUtilManager().setFcEnabled(false);
    } else {
      state = Lang.ON.getConfigValue(null);
      getHelper().groupAddPermission("default", "leaguemanager.footcube", "football", true);
      getUtilManager().setFcEnabled(true);
    }
    server.broadcastMessage(Lang.TOGGLE.getConfigValue(new String[]{state, sender.getName()}));
  }

  @Subcommand("setPracticeArea|spa")
  @CommandPermission("leaguemanager.command.setPracticeArea")
  public void onPracticeAreaSet(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      getLogger().send(sender, Lang.INGAME_ONLY.getConfigValue(null));
      return;
    }

    Player player = (Player) sender;

    if (args.length < 1) {
      getLogger().send(player, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"lm spa &2<&anaziv&2>"}));
    } else {
      YamlConfiguration config = Config.getConfig("config.yml");
      String locName = args[0];
      Location loc = player.getLocation();

      config.set("practice-areas." + locName, loc);
      Config.saveConfig(config, "config.yml");

      getLogger().send(player, Lang.PRACTICE_AREA_SET.getConfigValue(new String[]{locName, String.valueOf(loc.getX()), String.valueOf(loc.getY()), String.valueOf(loc.getZ())}));
    }
  }

  @Subcommand("ban")
  @CommandCompletion("@players")
  @CommandPermission("leaguemanager.command.ban")
  public void onBan(CommandSender sender, String[] args) {
    if (args.length < 1) {
      getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"lm ban &2<&aigrač&2> <&atrajanje&2> &3[&brazlog&3]"}));
    } else if (args.length >= 2) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

      if (target != null && target.hasPlayedBefore()) {
        Time time = Time.parseString("5min");
        User user = getHelper().getPlayer(target.getUniqueId());

        try {
          time = Time.parseString(args[1]);
        } catch (Time.TimeParseException | NullPointerException e) {
          getLogger().send(sender, Lang.INVALID_TIME.getConfigValue(null));
        }

        Node node = Node.builder("leaguemanager.banned")
            .value(true)
            .expiry(time.toMilliseconds(), TimeUnit.MILLISECONDS)
            .withContext("server", "football")
            .build();
        DataMutateResult result = user.data().add(node);

        if (result.wasSuccessful()) {
          String reason = "Kršenje pravila";

          if (args.length != 2) {
            reason = StringUtils.join(args, ' ', 2, args.length);
          }

          getLogger().send(sender, Lang.USER_BAN.getConfigValue(new String[]{target.getName(), time.toString(), reason}));
          if (target.isOnline()) {
            getLogger().send(target.getPlayer(), Lang.USER_BANNED.getConfigValue(new String[]{time.toString(), reason}));
          }

          saveBanData(target.getUniqueId(), time.toString(), reason, sender.getName());
        } else {
          getLogger().send(sender, Lang.USER_ALREADY_BANNED.getConfigValue(new String[]{target.getName()}));
        }
      } else {
        getLogger().send(sender, Lang.USER_NOT_FOUND.getConfigValue(null));
      }
    } else {
      getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"lm ban &2<&aigrač&2> <&atrajanje&2> &3[&brazlog&3]"}));
    }
  }

  @Subcommand("unban")
  @CommandCompletion("@players")
  @CommandPermission("leaguemanager.command.unban")
  public void onUnban(CommandSender sender, String[] args) {
    if (args.length < 1) {
      getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"lm unban &2<&aigrač&2>"}));
    } else if (args.length == 1) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

      if (target != null && target.hasPlayedBefore()) {
        User user = getHelper().getPlayer(target.getUniqueId());
        Node node = user.getCachedData().getPermissionData().queryPermission("leaguemanager.banned").node();

        if (node != null && node.hasExpiry()) {
          DataMutateResult result = user.data().remove(node);
          if (result.wasSuccessful()) {
            removeBanData(target.getUniqueId());
            getLogger().send(sender, Lang.USER_UNBAN.getConfigValue(new String[]{target.getName()}));

            if (target.isOnline()) {
              getLogger().send(target.getPlayer(), Lang.USER_UNBANNED.getConfigValue(null));
            }

            getHelper().getUserManager().saveUser(user);
          } else {
            getLogger().send(sender, Lang.USER_NOT_BANNED.getConfigValue(new String[]{target.getName()}));
          }
        } else {
          getLogger().send(sender, Lang.USER_NOT_BANNED.getConfigValue(new String[]{target.getName()}));
        }
      } else {
        getLogger().send(sender, Lang.USER_NOT_FOUND.getConfigValue(null));
      }
    } else {
      getLogger().send(sender, Lang.UNKNOWN_COMMAND.getConfigValue(null));
    }
  }

  @Subcommand("checkban")
  @CommandCompletion("@players")
  @CommandPermission("leaguemanager.command.checkban")
  public void onCheckBan(CommandSender sender, String[] args) {
    if (args.length < 1) {
      getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"lm checkban &2<&aigrač&2>"}));
    } else if (args.length == 1) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

      if (target != null && target.hasPlayedBefore()) {
        User user = getHelper().getPlayer(target.getUniqueId());
        Node node = user.getCachedData().getPermissionData().queryPermission("leaguemanager.banned").node();

        if (node != null && node.hasExpiry()) {
          String[] banData = getBanData(target.getUniqueId());
          if (banData != null) {
            String executor = banData[2];
            String reason = banData[1];
            String time = banData[0];
            String expiry = new Time(node.getExpiryDuration().getSeconds(), TimeUnit.SECONDS).toString();

            getLogger().send(sender, Lang.USER_CHECKBAN.getConfigValue(new String[]{target.getName(), executor, reason, time, expiry}));
          } else {
            getLogger().send(sender, Lang.ROSTERS_NOT_FOUND.getConfigValue(new String[]{"string nije pronađen"}));
          }
        } else {
          getLogger().send(sender, Lang.USER_NOT_BANNED.getConfigValue(new String[]{target.getName()}));
        }
      } else {
        getLogger().send(sender, Lang.USER_NOT_FOUND.getConfigValue(null));
      }
    }
  }

  @Subcommand("suspend")
  @CommandCompletion("@players")
  @CommandPermission("leaguemanager.command.suspend")
  public void onSuspend(CommandSender sender, String[] args) {
    if (args.length < 1) {
      getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"lm suspend &2<&aigrač&2> <&aduration&2> &3[&breason&3]"}));
    } else if (args.length >= 2) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

      if (target != null && target.hasPlayedBefore()) {
        Time time = Time.parseString("5min");
        User user = getHelper().getPlayer(target.getUniqueId());

        try {
          time = Time.parseString(args[1]);
        } catch (Time.TimeParseException | NullPointerException e) {
          getLogger().send(sender, Lang.INVALID_TIME.getConfigValue(null));
        }

        Node node = Node.builder("group.suspend")
            .value(true)
            .expiry(time.toMilliseconds(), TimeUnit.MILLISECONDS)
            .withContext("server", "football")
            .build();
        DataMutateResult result = user.data().add(node);

        if (result.wasSuccessful()) {
          String reason = "Kršenje pravila";
          if (args.length != 2) reason = StringUtils.join(args, ' ', 2, args.length);

          getLogger().send(sender, Lang.USER_SUSPEND.getConfigValue(new String[]{target.getName(), time.toString(), reason}));

          if (target.isOnline()) {
            getLogger().send(target.getPlayer(), Lang.USER_SUSPENDED.getConfigValue(new String[]{time.toString(), reason}));
          }

          saveSuspendData(target.getUniqueId(), time.toString(), reason, sender.getName());
          getHelper().getUserManager().saveUser(user);
        } else {
          getLogger().send(sender, Lang.USER_ALREADY_SUSPENDED.getConfigValue(new String[]{target.getName()}));
        }
      } else {
        getLogger().send(sender, Lang.USER_NOT_FOUND.getConfigValue(null));
      }
    } else {
      getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"lm suspend &2<&aigrač&2> <&aduration&2> &3[&breason&3]"}));
    }
  }

  @Subcommand("unsuspend")
  @CommandCompletion("@players")
  @CommandPermission("leaguemanager.command.unsuspend")
  public void onUnSuspend(CommandSender sender, String[] args) {
    if (args.length < 1) {
      getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"lm unsuspend &2<&aigrač&2>"}));
    } else if (args.length == 1) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

      if (target != null && target.hasPlayedBefore()) {
        User user = getHelper().getPlayer(target.getUniqueId());
        Node node = user.getCachedData().getPermissionData().queryPermission("group.suspend").node();

        if (node != null && node.hasExpiry()) {
          DataMutateResult result = user.data().remove(node);
          if (result.wasSuccessful()) {
            removeSuspendData(target.getUniqueId());
            getLogger().send(sender, Lang.USER_UNSUSPEND.getConfigValue(new String[]{target.getName()}));

            if (target.isOnline()) {
              getLogger().send(target.getPlayer(), Lang.USER_UNSUSPENDED.getConfigValue(null));
            }

            getHelper().getUserManager().saveUser(user);
          } else {
            getLogger().send(sender, Lang.USER_NOT_SUSPENDED.getConfigValue(new String[]{target.getName()}));
          }
        } else {
          getLogger().send(sender, Lang.USER_NOT_SUSPENDED.getConfigValue(new String[]{target.getName()}));
        }
      } else {
        getLogger().send(sender, Lang.USER_NOT_FOUND.getConfigValue(null));
      }
    } else {
      getLogger().send(sender, Lang.UNKNOWN_COMMAND.getConfigValue(null));
    }
  }

  @Subcommand("checksuspend")
  @CommandCompletion("@players")
  @CommandPermission("leaguemanager.command.checksuspend")
  public void onCheckSuspend(CommandSender sender, String[] args) {
    if (args.length < 1) {
      getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"lm checksuspend &2<&aigrač&2>"}));
    } else if (args.length == 1) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

      if (target != null && target.hasPlayedBefore()) {
        User user = getHelper().getPlayer(target.getUniqueId());
        Node node = user.getCachedData().getPermissionData().queryPermission("group.suspend").node();

        if (node != null && node.hasExpiry()) {
          String[] suspendData = getSuspendData(target.getUniqueId());
          if (suspendData != null) {
            String executor = suspendData[2];
            String reason = suspendData[1];
            String time = suspendData[0];
            String expiry = new Time(node.getExpiryDuration().getSeconds(), TimeUnit.SECONDS).toString();

            getLogger().send(sender, Lang.USER_CHECKSUSPEND.getConfigValue(new String[]{target.getName(), executor, reason, time, expiry}));
          } else {
            getLogger().send(sender, Lang.ROSTERS_NOT_FOUND.getConfigValue(new String[]{"string nije pronađen"}));
          }
        } else {
          getLogger().send(sender, Lang.USER_NOT_SUSPENDED.getConfigValue(new String[]{target.getName()}));
        }
      } else {
        getLogger().send(sender, Lang.USER_NOT_FOUND.getConfigValue(null));
      }
    }
  }

  private void saveSuspendData(UUID playerId, String time, String reason, String executor) {
    dataManager.setConfig(playerDataFolder, playerId.toString());

    FileConfiguration config = dataManager.getConfig(playerId.toString());
    config.set("suspend.time", time);
    config.set("suspend.reason", reason);
    config.set("suspend.executor", executor);

    dataManager.saveConfig(playerId.toString());
  }

  private void removeSuspendData(UUID playerId) {
    dataManager.setConfig(playerDataFolder, playerId.toString());

    FileConfiguration config = dataManager.getConfig(playerId.toString());
    config.set("suspend", null);

    dataManager.saveConfig(playerId.toString());
  }

  private String[] getSuspendData(UUID playerId) {
    dataManager.setConfig(playerDataFolder, playerId.toString());

    FileConfiguration config = dataManager.getConfig(playerId.toString());
    String time = config.getString("suspend.time");
    String reason = config.getString("suspend.reason");
    String executor = config.getString("suspend.executor");

    if (time != null && reason != null && executor != null) {
      return new String[]{time, reason, executor};
    } else {
      return null;
    }
  }

  private void saveBanData(UUID playerId, String time, String reason, String executor) {
    dataManager.setConfig(playerDataFolder, playerId.toString());

    FileConfiguration config = dataManager.getConfig(playerId.toString());
    config.set("ban.time", time);
    config.set("ban.reason", reason);
    config.set("ban.executor", executor);

    dataManager.saveConfig(playerId.toString());
  }

  private void removeBanData(UUID playerId) {
    dataManager.setConfig(playerDataFolder, playerId.toString());

    FileConfiguration config = dataManager.getConfig(playerId.toString());
    config.set("ban", null);

    dataManager.saveConfig(playerId.toString());
  }

  private String[] getBanData(UUID playerId) {
    dataManager.setConfig(playerDataFolder, playerId.toString());

    FileConfiguration config = dataManager.getConfig(playerId.toString());
    String time = config.getString("ban.time");
    String reason = config.getString("ban.reason");
    String executor = config.getString("ban.executor");

    if (time != null && reason != null && executor != null) {
      return new String[]{time, reason, executor};
    } else {
      return null;
    }
  }

  private String[] startupBanner() {
    return new String[]{"&8▎ &r","&8▎ &d  88       &e8b      d8","&8▎ &d  88       &e88b   d88   &a" + getLogger().getPluginName(), "&8▎ &d  88    .o &e88YbdP88   &3Authors: &b" + getLogger().getAuthors(),"&8▎ &d  88ood8 &e88 Y||Y 88","&8▎ &r"};
  }
}