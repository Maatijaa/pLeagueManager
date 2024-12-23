package me.github.maatijaa.pleaguemanager.commands.timers;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.github.maatijaa.pleaguemanager.configs.Config;
import me.github.maatijaa.pleaguemanager.configs.Lang;
import me.github.maatijaa.pleaguemanager.managers.DataManager;
import me.github.maatijaa.pleaguemanager.managers.UtilManager;
import lombok.Getter;
import me.github.maatijaa.pleaguemanager.utils.*;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@CommandAlias("result|rs")
@CommandPermission("pleaguemanager.command.result")
public class ResultCommand extends BaseCommand {
  private final Plugin plugin;
  private final UtilManager utilManager;
  private final Logger logger;
  private final Helper helper;
  private Time time, extraTime;
  private String home, away, prefix, cleanPrefix;
  private int home_result, away_result, extraTimeNew, timerId;
  private static String HOME_NAME, AWAY_NAME;
  private boolean secondHalf = false, league = false, noTpAll = false;
  private static YamlConfiguration config;
  private DiscordWebhook webhook = null;
  private final List<DiscordWebhook.EmbedObject> embedObjects = new ArrayList<>();
  private final List<String> webhookMessages = new ArrayList<>();
  private String matchTime = null;
  private final DataManager dataManager;

  public ResultCommand(final Plugin plugin, final UtilManager utilManager) {
    this.plugin = plugin;
    this.utilManager = utilManager;
    this.logger = utilManager.getLogger();
    this.helper = utilManager.getHelper();
    this.dataManager = new DataManager(plugin);
    config = Config.getConfig("config.yml");

    if (config.getStringList("discordWebhookURL") != null)
      webhook = new DiscordWebhook(config.getStringList("discordWebhookURL"));

    reset();
  }

  @Default
  @CatchUnknown
  @Subcommand("help")
  @CommandPermission("pleaguemanager.command.result.help")
  public void onHelp(CommandSender sender) {
    getLogger().send(sender, Lang.RESULT_HELP.getConfigValue(null));
  }

  @Subcommand("coinflip|cf")
  @CommandPermission("pleaguemanager.command.coinflip")
  @CommandCompletion("heads|tails")
  public void onCoinFlip(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      getLogger().send(sender, Lang.INGAME_ONLY.getConfigValue(null));
    } else {
      if (!getUtilManager().isTaskQueued(getTimerId()) && isSetup() && !Timer.isRunning()) {
        if (args.length < 1) {
          getLogger().broadcast(Lang.RESULT_COINFLIP_MESSAGE.getConfigValue(new String[]{home, away}));
        } else if (args.length == 1 && (args[0].equalsIgnoreCase("heads") || args[0].equalsIgnoreCase("tails"))) {
          getLogger().send(sender, Lang.RESULT_COINFLIP_SET.getConfigValue(new String[]{args[0], home}));
          double flippy = Math.random() > 0.5 ? 1 : 2;
          getLogger().broadcast(Lang.RESULT_COINFLIP_WINNER.getConfigValue(new String[]{flippy == 1 ? "heads" : "tails", flippy == 1 ? home : away}));
        } else getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"rs cf &2<&aheads&2|&atails&2>"}));
      } else getLogger().send(sender, Lang.TIMER_NOT_AVAILABLE.getConfigValue(null));
    }
  }

  @Subcommand("start|s")
  @CommandPermission("pleaguemanager.command.result.start")
  public void onStart(CommandSender sender) {
    if (!isSetup()) {
      getLogger().send(sender, Lang.RESULT_NOT_SETUP.getConfigValue(null));
      return;
    }
    if (!getUtilManager().isTaskQueued(getTimerId())&& !Timer.isRunning()) {
      timerId = firstHalf().startTask();
      Timer.isRunning = true;
      matchTime = UtilManager.formatTime(Timer.getSecondsParsed());
      getLogger().send("fcfa", Lang.TIMER_CREATE.getConfigValue(new String[]{String.valueOf(getTimerId())}));
    } else getLogger().send(sender, Lang.TIMER_ALREADY_RUNNING.getConfigValue(null));
  }

  @Subcommand("stop")
  @CommandPermission("pleaguemanager.command.result.stop")
  public void onStop(CommandSender sender) {
    if (getUtilManager().isTaskQueued(getTimerId())) {
      matchTime = UtilManager.formatTime(Timer.getSecondsParsed());
      getLogger().send("default", Lang.RESULT_OVER.getConfigValue(new String[]{matchTime, getPrefix(), home, "" + home_result, "" + away_result, away}));
      if (webhook != null && isLeague()) {
        webhookMessages.add(Lang.WEBHOOK_MATCH_ENDED.getConfigValue(new String[]{HOME_NAME, String.valueOf(home_result), String.valueOf(away_result), AWAY_NAME, matchTime}));
      }
      secondHalf().getAfterTimer().run();
      Bukkit.getScheduler().cancelTasks(getPlugin());
      reset();
      if (noTpAll && config.get("spawn") != null && sender instanceof Player) {
        Location spawn = (Location) config.get("spawn");
        Player player = (Player) sender;
        player.teleport(spawn);
        Bukkit.dispatchCommand(player, "setspawn");
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> Bukkit.dispatchCommand(player, "back"), 10L);
      } else getLogger().send(sender, Lang.ROSTERS_NOT_FOUND.getConfigValue(new String[]{"spawn"}));
    } else getLogger().send(sender, Lang.TIMER_NOT_AVAILABLE.getConfigValue(null));
  }

  @Subcommand("pause|p")
  @CommandPermission("pleaguemanager.command.result.pause")
  public void onPause(CommandSender sender) {
    if (getUtilManager().isTaskQueued(getTimerId())) {
      matchTime = UtilManager.formatTime(Timer.getSecondsParsed());
      getLogger().send("fcfa", Lang.TIMER_STOP.getConfigValue(new String[]{String.valueOf(getTimerId())}));
      firstHalf().cancelTask(getTimerId());
      secondHalf = true;
      timerId = halfTime().startTask();
    } else getLogger().send(sender, Lang.TIMER_NOT_AVAILABLE.getConfigValue(null));
  }

  @Subcommand("resume|r")
  @CommandPermission("pleaguemanager.command.result.resume")
  public void onResume(CommandSender sender) {
    if (getUtilManager().isTaskQueued(getTimerId())) {
      matchTime = UtilManager.formatTime(Timer.getSeconds() + 1);
      halfTime().cancelTask(getTimerId());
      timerId = secondHalf().startTask();
      if (webhook != null && isLeague()) {
        webhookMessages.add(Lang.WEBHOOK_MATCH_SECONDHALF.getConfigValue(new String[]{HOME_NAME, String.valueOf(home_result), String.valueOf(away_result), AWAY_NAME}));
      }
      getLogger().send("hoster", Lang.TIMER_CREATE.getConfigValue(new String[]{String.valueOf(getTimerId())}));
      getPlugin().getServer().getScheduler().runTaskLaterAsynchronously(getPlugin(), () -> Timer.secondsParsed = (Timer.getSeconds() - 60) / 2, 20L);
    } else getLogger().send(sender, Lang.TIMER_NOT_AVAILABLE.getConfigValue(null));
  }

  @Subcommand("extend")
  @CommandCompletion("add|remove")
  @CommandPermission("pleaguemanager.command.result.extend")
  public void onExtend(CommandSender sender, String[] args) {
    if (getUtilManager().isTaskQueued(getTimerId())) {
      if (args.length < 2) {
        getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"rs extend &2<&aadd&2|&aremove&2> <&avreme&2>"}));
        return;
      }
      try {
        extraTime = Time.parseString(args[1]);
      } catch (Time.TimeParseException timeParseException) {
        getLogger().send(sender, Lang.INVALID_TIME.getConfigValue(new String[]{args[0]}));
        return;
      }
      matchTime = UtilManager.formatTime(Timer.getSecondsParsed());
      switch (args[0]) {
        case "add":
          Timer.seconds = (int) (Timer.getSeconds() + extraTime.toSeconds());
          if (isSecondHalf()) extraTimeNew = (int) (Timer.seconds - time.toSeconds()) - 60;
          else extraTimeNew = (int) (Timer.seconds - time.toSeconds());
          break;
        case "remove":
          Timer.seconds = (int) (Timer.getSeconds() - extraTime.toSeconds());
          if (isSecondHalf()) extraTimeNew = (int) (Timer.seconds - time.toSeconds()) - 60;
          else extraTimeNew = (int) (Timer.seconds - time.toSeconds());
          break;
        default:
          break;
      }
      getLogger().send("fcfa", Lang.TIMER_TIME_SET.getConfigValue(new String[]{UtilManager.formatTime(extraTimeNew)}));
    } else getLogger().send(sender, Lang.TIMER_NOT_AVAILABLE.getConfigValue(null));
  }

  @Subcommand("add")
  @CommandCompletion("home|away|@players")
  @CommandPermission("pleaguemanager.command.result.add")
  public void onAdd(CommandSender sender, String[] args) {
    if (getUtilManager().isTaskQueued(getTimerId()) && (args.length == 2 || args.length == 3)) {
      matchTime = UtilManager.formatTime(Timer.getSecondsParsed());
      if (args[0].equalsIgnoreCase("home")) {
        home_result++;
        if (args.length == 2)
          getLogger().send("default", Lang.RESULT_ADD.getConfigValue(new String[]{args[1], home}));
        else getLogger().send("default", Lang.RESULT_ADD_ASSIST.getConfigValue(new String[]{args[1], home, args[2]}));
        if (webhook != null && isLeague()) {
          if (args.length == 2) {
            embedObjects.add(new DiscordWebhook.EmbedObject()
                .setColor(Color.decode(Lang.WEBHOOK_MATCH_SCORE_COLOR.getConfigValue(null)))
                .setAuthor(Lang.WEBHOOK_MATCH_SCORE_AUTHOR_NAME.getConfigValue(new String[]{HOME_NAME}), null, Lang.WEBHOOK_MATCH_SCORE_AUTHOR_ICON.getConfigValue(null))
                .setDescription(Lang.WEBHOOK_MATCH_SCORE_DESC.getConfigValue(new String[]{args[1], HOME_NAME, matchTime, HOME_NAME, String.valueOf(home_result), String.valueOf(away_result), AWAY_NAME})));
          } else {
            embedObjects.add(new DiscordWebhook.EmbedObject()
                .setColor(Color.decode(Lang.WEBHOOK_MATCH_SCORE_COLOR.getConfigValue(null)))
                .setAuthor(Lang.WEBHOOK_MATCH_SCORE_AUTHOR_NAME.getConfigValue(new String[]{HOME_NAME}), null, Lang.WEBHOOK_MATCH_SCORE_AUTHOR_ICON.getConfigValue(null))
                .setDescription(Lang.WEBHOOK_MATCH_ASSIST.getConfigValue(new String[]{args[1], HOME_NAME, matchTime, args[2], HOME_NAME, String.valueOf(home_result), String.valueOf(away_result), AWAY_NAME})));
          }
        } else getLogger().send(sender, Lang.WEBHOOK_NOT_SETUP.getConfigValue(null));
      } else if (args[0].equalsIgnoreCase("away")) {
        away_result++;
        if (args.length == 2)
          getLogger().send("default", Lang.RESULT_ADD.getConfigValue(new String[]{args[1], away}));
        else getLogger().send("default", Lang.RESULT_ADD_ASSIST.getConfigValue(new String[]{args[1], away, args[2]}));
        if (webhook != null && isLeague()) {
          if (args.length == 2) {
            embedObjects.add(new DiscordWebhook.EmbedObject()
                .setColor(Color.decode(Lang.WEBHOOK_MATCH_SCORE_COLOR.getConfigValue(null)))
                .setAuthor(Lang.WEBHOOK_MATCH_SCORE_AUTHOR_NAME.getConfigValue(new String[]{AWAY_NAME}), null, Lang.WEBHOOK_MATCH_SCORE_AUTHOR_ICON.getConfigValue(null))
                .setDescription(Lang.WEBHOOK_MATCH_SCORE_DESC.getConfigValue(new String[]{args[1], AWAY_NAME, matchTime, HOME_NAME, String.valueOf(home_result), String.valueOf(away_result), AWAY_NAME})));
          } else {
            embedObjects.add(new DiscordWebhook.EmbedObject()
                .setColor(Color.decode(Lang.WEBHOOK_MATCH_SCORE_COLOR.getConfigValue(null)))
                .setAuthor(Lang.WEBHOOK_MATCH_SCORE_AUTHOR_NAME.getConfigValue(new String[]{AWAY_NAME}), null, Lang.WEBHOOK_MATCH_SCORE_AUTHOR_ICON.getConfigValue(null))
                .setDescription(Lang.WEBHOOK_MATCH_ASSIST.getConfigValue(new String[]{args[1], AWAY_NAME, matchTime, args[2], HOME_NAME, String.valueOf(home_result), String.valueOf(away_result), AWAY_NAME})));
          }
        } else getLogger().send(sender, Lang.WEBHOOK_NOT_SETUP.getConfigValue(null));
      } else getLogger().send(sender, Lang.RESULT_USAGE.getConfigValue(null));
    } else getLogger().send(sender, Lang.TIMER_NOT_AVAILABLE.getConfigValue(null));
  }

  @Subcommand("remove")
  @CommandCompletion("home|away")
  @CommandPermission("pleaguemanager.command.result.remove")
  public void onRemove(CommandSender sender, String[] args) {
    if (getUtilManager().isTaskQueued(getTimerId()) && args.length == 1) {
      matchTime = UtilManager.formatTime(Timer.getSecondsParsed());
      if (args[0].equalsIgnoreCase("home")) {
        if (home_result != 0) {
          home_result--;
          getLogger().send("fcfa", Lang.RESULT_REMOVE.getConfigValue(new String[]{home}));
        } else getLogger().send(sender, Lang.RESULT_ELIMINATED.getConfigValue(new String[]{home}));
      } else if (args[0].equalsIgnoreCase("away")) {
        if (away_result != 0) {
          away_result--;
          getLogger().send("fcfa", Lang.RESULT_REMOVE.getConfigValue(new String[]{away}));
        } else getLogger().send(sender, Lang.RESULT_ELIMINATED.getConfigValue(new String[]{away}));
      } else getLogger().send(sender, Lang.RESULT_HELP.getConfigValue(null));
    } else getLogger().send(sender, Lang.TIMER_NOT_AVAILABLE.getConfigValue(null));
  }

  @Subcommand("time")
  @CommandPermission("pleaguemanager.command.result.time")
  public void onTime(CommandSender sender, String[] args) {
    if (!getUtilManager().isTaskQueued(getTimerId())) {
      if (args.length == 0) {
        getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"rs time &2<&avreme&2>"}));
        return;
      }
      try {
        time = Time.parseString(args[0]);
        if (time.toSeconds() < Time.parseString("10min").toSeconds())
          time = Time.parseString("10min");
        getLogger().send("fcfa", Lang.TIMER_TIME_SET.getConfigValue(new String[]{UtilManager.formatTime((int) time.toSeconds())}));
      } catch (Time.TimeParseException timeParseException) {
        getLogger().send(sender, Lang.INVALID_TIME.getConfigValue(new String[]{args[1]}));
      }
    } else getLogger().send(sender, Lang.TIMER_ALREADY_RUNNING.getConfigValue(null));
  }

  @Subcommand("teams")
  @CommandPermission("pleaguemanager.command.result.teams")
  @CommandCompletion("noTpAll")
  public void onTeams(CommandSender sender, String[] args) {
    if (args.length == 2 || args.length == 3) {
      HOME_NAME = args[0].toUpperCase();
      AWAY_NAME = args[1].toUpperCase();
      league = false;
      if (sender.hasPermission("group.fcfa")) {
        if (!getHelper().groupExists(args[0]) && !getHelper().groupExists(args[1])) {
          league = false;
          home = args[0];
          away = args[1];
        } else {
          if (getPrefix().equals("&bEvent")) {
            getLogger().send(sender, Lang.WEBHOOK_PREFIX_NOT_SETUP.getConfigValue(null));
            return;
          }
          league = true;
          home = getHelper().groupHasMeta(args[0], "team") ?
              getHelper().getGroupMeta(args[0], "team") :
              getHelper().groupHasMeta(args[0], "b") ?
                  getHelper().getGroupMeta(args[0], "b") : HOME_NAME;
          away = getHelper().groupHasMeta(args[1], "team") ?
              getHelper().getGroupMeta(args[1], "team") :
              getHelper().groupHasMeta(args[1], "b") ?
                  getHelper().getGroupMeta(args[1], "b") : AWAY_NAME;
          noTpAll = false;
          if (!args[2].equalsIgnoreCase("noTpAll")) {
            noTpAll = true;
            Bukkit.dispatchCommand(sender, "warp " + args[0]);
            Bukkit.dispatchCommand(sender, "setspawn");
            for (Player player : getPlugin().getServer().getOnlinePlayers()) {
              getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () ->
                  Bukkit.dispatchCommand(player, "spawn"), 20L);
            }
          }
        }
      } else {
        league = false;
        home = args[0];
        away = args[1];
      }
      getLogger().send(sender, Lang.TIMER_TEAMS_SET.getConfigValue(new String[]{home, away}));
      if (!isLeague()) return;
      if (webhook != null) {
        webhookMessages.add(Lang.WEBHOOK_TEAMS_SET.getConfigValue(new String[]{getCleanPrefix(), HOME_NAME, AWAY_NAME}));
      } else getLogger().send(sender, Lang.WEBHOOK_NOT_SETUP.getConfigValue(null));
    } else getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"rs teams &2<&ahome&2|&aaway&2> &3[&bnoTpAll&3]"}));
  }

  @Subcommand("prefix")
  @CommandPermission("pleaguemanager.command.result.prefix")
  public void onPrefix(CommandSender sender, String[] args) {
    if (args.length > 0) {
      prefix = getUtilManager().color(StringUtils.join(args, " ", 0, args.length));
      cleanPrefix = ChatColor.stripColor(prefix);
      getLogger().send("fcfa", Lang.TIMER_PREFIX_SET.getConfigValue(new String[]{getPrefix()}));
    } else getLogger().send(sender, Lang.INCORRECT_USAGE.getConfigValue(new String[]{"rs prefix &2<&aporuka sa razmacima&2>"}));
  }

  private Timer firstHalf() {
    return new Timer(getPlugin(), (int) time.toSeconds(), () -> {
      matchTime = UtilManager.formatTime(Timer.getSecondsParsed());
      if (webhook != null && isLeague()) {
        webhookMessages.add(Lang.WEBHOOK_MATCH_START.getConfigValue(new String[]{matchTime, HOME_NAME, AWAY_NAME}));
      }
      getLogger().send("default", Lang.TIMER_STARTING.getConfigValue(new String[]{getPrefix()}));
      getLogger().broadcastBar(Lang.RESULT_STARTING.getConfigValue(new String[]{getPrefix()}));
    }, () -> Timer.isRunning = false, (t) -> {
      String secondsParsed = UtilManager.formatTime(Timer.getSecondsParsed());
      String seconds = UtilManager.formatTime(Timer.seconds);
      String extraTimeString = UtilManager.formatTime(extraTimeNew);
      String formatted, color;

      if (Timer.getSeconds() != (int) time.toSeconds())
        formatted = getUtilManager().color("&7 ┃ &c(+" + extraTimeString + " ET)");
      else formatted = "";

      if (Timer.getSecondsParsed() > (time.toSeconds() / 2)) color = getUtilManager().color("&c");
      else color = getUtilManager().color("&a");

      getLogger().broadcastBar(Lang.RESULT_ACTIONBAR.getConfigValue(new String[]{getPrefix(), home, "" + home_result, "" + away_result, away, color, secondsParsed, seconds, formatted}));
    });
  }

  private Timer halfTime() {
    return new Timer(getPlugin(), 600, () -> {
      matchTime = UtilManager.formatTime(Timer.getSeconds());
      getLogger().send("default", Lang.RESULT_HALFTIME.getConfigValue(new String[]{matchTime, getPrefix(), home, "" + home_result, "" + away_result, away}));
      if (webhook != null && isLeague()) {
        webhookMessages.add(Lang.WEBHOOK_MATCH_HALFTIME.getConfigValue(new String[]{matchTime, HOME_NAME, String.valueOf(home_result), String.valueOf(away_result), AWAY_NAME}));
      }
    }, () -> Timer.isRunning = false, (t ->
        getLogger().broadcastBar(Lang.RESULT_ACTIONBAR_HT.getConfigValue(new String[]{getPrefix(), home, "" + home_result, "" + away_result, away})))
    );
  }

  private Timer secondHalf() {
    return new Timer(getPlugin(), (int) (time.toSeconds() + 60), () -> getLogger().send("default", Lang.RESULT_SECONDHALF.getConfigValue(new String[]{matchTime, getPrefix(), home, "" + home_result, "" + away_result, away})), () -> {
      if (webhook != null && isLeague()) {
        if (home_result != away_result) {
          getDataManager().setConfig("teamdata", "main");
          if (getDataManager().getConfig("main").getString(HOME_NAME + ".win-video") != null &&
              getDataManager().getConfig("main").getString(AWAY_NAME + ".win-video") != null) {
            webhookMessages.add(getDataManager().getConfig("main").getString(home_result > away_result ?
                HOME_NAME + ".win-video" : away_result > home_result ?
                AWAY_NAME + ".win-video" : null));
          }
        }

        try {
          String finalMessage = "";
          for (String message : webhookMessages) {
            finalMessage += message + "\\n";
          }

          webhook.setContent(finalMessage);
          for (DiscordWebhook.EmbedObject embedObject : getEmbedObjects()) {
            webhook.addEmbed(embedObject);
          }

          webhook.execute();
        } catch (IOException e) {
          getLogger().send("hoster", e.getMessage());
        }
      }
      getLogger().broadcastBar(Lang.RESULT_END.getConfigValue(new String[]{getPrefix(), home, "" + home_result, "" + away_result, away}));
      reset();
    }, (t) -> {
      String secondsParsed = UtilManager.formatTime(Timer.getSecondsParsed());
      String seconds = UtilManager.formatTime(Timer.getSeconds() - 60);
      String extraTimeString = UtilManager.formatTime(extraTimeNew);
      String formatted = "", color = "&a";

      // if there's et
      if ((Timer.getSeconds() - 60) != (int) time.toSeconds()) {
        if (extraTimeNew != 0) formatted = getUtilManager().color("&7 ┃ &e2HT &c(+" + extraTimeString + " ET)");
        else color = getUtilManager().color("&a");
      } else formatted = getUtilManager().color("&7 ┃ &e2HT");

      // format last attack
      if (Timer.getSecondsParsed() > ((Timer.getSeconds() - 60) - 5))
        color = getUtilManager().color("&c");

      getLogger().broadcastBar(Lang.RESULT_ACTIONBAR.getConfigValue(new String[]{getPrefix(), home, "" + home_result, "" + away_result, away, color, secondsParsed, seconds, formatted}));
    });
  }

  private boolean isSetup() {
    return getTime() != null && getHome() != null && getAway() != null;
  }

  private void reset() {
    time = Time.parseString("20min");
    home = null;
    away = null;
    HOME_NAME = null;
    AWAY_NAME = null;
    prefix = "&bEvent";
    home_result = 0;
    away_result = 0;
    Timer.isRunning = false;
    league = false;
    webhookMessages.clear();
    embedObjects.clear();
  }
}