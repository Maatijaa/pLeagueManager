package me.github.maatijaa.pleaguemanager.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final Plugin plugin;
    private final String versionUrl;

    public UpdateChecker(Plugin plugin, String versionUrl) {
        this.plugin = plugin;
        this.versionUrl = versionUrl;
    }

    // checks if update is av if, it is avilable it will send notification to console.
    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(versionUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String latestVersion = reader.readLine().trim();
                    String currentVersion = plugin.getDescription().getVersion();

                    if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                        Bukkit.getLogger().warning("==========================================");
                        Bukkit.getLogger().warning("              Update Dostupan!          ");
                        Bukkit.getLogger().warning("Trenutna verzija plugina: " + currentVersion);
                        Bukkit.getLogger().warning("Najnovija dostupna verzija: " + latestVersion);
                        Bukkit.getLogger().warning(".");
                        Bukkit.getLogger().warning("==========================================");
                    } else {
                        Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "[&4p&bLeague&4Manager] &fTrenutno si na najnovijoj verziji: &c" + currentVersion));
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&',"&e[&4p&bLeague&4Manager&e] &4Propao pokusaj za pregledom verzije: &c" + e.getMessage()));
            }
        });
    }
}
