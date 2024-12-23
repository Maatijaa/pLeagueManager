package me.github.maatijaa.pleaguemanager.listeners;

import me.github.maatijaa.pleaguemanager.configs.Config;
import me.github.maatijaa.pleaguemanager.configs.Lang;
import me.github.maatijaa.pleaguemanager.managers.UtilManager;
import me.github.maatijaa.pleaguemanager.utils.Logger;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

@Getter
public class DisabledItemsListener implements Listener {
  private static final YamlConfiguration config = Config.getConfig("config.yml");
  private final UtilManager utilManager;
  private final Logger logger;
  private final Set<String> disabledItems = new HashSet<>();

  public DisabledItemsListener(final UtilManager utilManager) {
    this.utilManager = utilManager;
    this.logger = utilManager.getLogger();

    if (config.contains("disabled-items")) {
      this.disabledItems.addAll(config.getStringList("disabled-items"));
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInteract(final PlayerInteractEvent event) {
    if (event.getItem() == null || event.isCancelled() || getDisabledItems().isEmpty() || getUtilManager().isFcEnabled()) return;

    final Player player = event.getPlayer();
    final Material itemMaterial = player.getItemInHand().getType();

    if (getDisabledItems().contains(itemMaterial.name())) {
      getLogger().send(player, Lang.DISABLED_ITEM.getConfigValue(new String[]{itemMaterial.name().toLowerCase()}));
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onProjectileLaunch(final ProjectileLaunchEvent event) {
    if (event.isCancelled() || getDisabledItems().isEmpty() || getUtilManager().isFcEnabled()) return;

    if (event.getEntity().getShooter() instanceof Player) {
      final Player player = (Player) event.getEntity().getShooter();
      final String entity = event.getEntity().getType().name().toLowerCase();

      getLogger().send(player, Lang.DISABLED_ITEM.getConfigValue(new String[]{entity}));
      event.setCancelled(true);
    }
  }
}