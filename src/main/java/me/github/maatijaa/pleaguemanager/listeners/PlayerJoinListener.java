package me.github.maatijaa.pleaguemanager.listeners;

import me.github.maatijaa.pleaguemanager.managers.DataManager;
import me.github.maatijaa.pleaguemanager.managers.UtilManager;
import me.github.maatijaa.pleaguemanager.utils.Helper;
import me.github.maatijaa.pleaguemanager.utils.Logger;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

@Getter
public class PlayerJoinListener implements Listener {
  private final UtilManager utilManager;
  private final DataManager dataManager;
  private final Helper helper;
  private final Logger logger;

  public PlayerJoinListener(UtilManager utilManager) {
    this.utilManager = utilManager;
    this.dataManager = new DataManager(utilManager.getPlugin());
    this.helper = utilManager.getHelper();
    this.logger = utilManager.getLogger();
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    String playerName = player.getName(), folderName = "playerdata";
    UUID playerUUID = player.getUniqueId();

    if (!getDataManager().configExists(folderName, playerUUID.toString())) {
      getDataManager().createNewFile(playerUUID.toString(), null);
      getLogger().info("Creating playerdata file for &b" + playerName + " (&o" + playerUUID + "&b)");
    }

    getDataManager().setConfig(folderName, playerUUID.toString());
    getDataManager().getConfig(playerUUID.toString()).set("name", playerName);

    if (getDataManager().getConfig(playerUUID.toString()).get("head") == null) {
      ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
      SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
      skullMeta.setOwner(player.getName());
      skull.setItemMeta(skullMeta);
      getLogger().info("Setting head for player &b" + playerName);
      getDataManager().getConfig(playerUUID.toString()).set("head", skull);
    }

    if (!getDataManager().getConfig(playerUUID.toString()).contains("league-stats")) {
      String[] type = new String[]{"goals","assists","yellow-cards","red-cards","clean-sheets"};
      for (String each : type) {
        getDataManager().getConfig(playerUUID.toString()).set("league-stats." + each, 0);
      }
      getLogger().info("Adding default league stats entries for player &b" + playerName);
    }

    if (!player.hasPermission("leaguemanager.banned") && getDataManager().getConfig(playerUUID.toString()).get("ban") != null) {
      getLogger().info("Removing ban strings from player's &b" + playerName + " &fconfig.");
      getDataManager().getConfig(playerUUID.toString()).set("ban", null);
    }

    if (!player.hasPermission("group.suspend") && getDataManager().getConfig(playerUUID.toString()).get("suspend") != null) {
      getLogger().info("Removing suspend strings from player's &b" + playerName + " &fconfig.");
      getDataManager().getConfig(playerUUID.toString()).set("suspend", null);
    }

    if (getDataManager().getPlayerName(playerUUID) == null) {
      getDataManager().addPlayerUUID(playerUUID, playerName);
    }

    getDataManager().saveConfig(playerUUID.toString());
  }
}
