// Inspired by ShaneBeee SkBee implementation of update checking, https://raw.githubusercontent.com/ShaneBeee/SkBee/refs/heads/master/src/main/java/com/shanebeestudios/skbee/api/util/update/UpdateChecker.java

package org.crebs.skpackets.api.update;

import ch.njol.skript.util.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.crebs.skpackets.api.util.LogUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class UpdateChecker implements Listener, AutoCloseable {
    public static final String DOWNLOAD_URL = "https://modrinth.com/plugin/skpackets";
    public static final String PERMISSION = "skpackets.update.check";
    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private final String currentVersion;
    private final Set<UUID> notified = new HashSet<>();
    private volatile boolean closed;
    private volatile ModrinthVersion availableVersion;
    private boolean started;
    private boolean notifyOnJoin;
    private BukkitTask checkTask;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }

    public void start() {
        if (started || closed) return;
        started = true;
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            plugin.getLogger().info("Update checker disabled.");
            return;
        }
        notifyOnJoin = plugin.getConfig().getBoolean("update-checker.notify-on-join", true);
        if (notifyOnJoin) plugin.getServer().getPluginManager().registerEvents(this, plugin);
        checkTask = scheduler.runTaskAsynchronously(plugin, this::check);
    }

    private void check() {
        if (closed) return;
        try {
            ModrinthVersion latest = getLatestVersionFromModrinth();
            if (closed) return;
            if (latest == null) {
                plugin.getLogger().info("No published version was found on Modrinth for the update check.");
                return;
            }
            Version version = latest.getUpdateVersion();
            int comparison = version.compareTo(new Version(currentVersion.trim().replaceFirst("^[vV]", "")));
            if (comparison <= 0) {
                plugin.getLogger().info("No update needed (installed: " + currentVersion + ", latest: " + version + ").");
                return;
            }
            availableVersion = latest;
            plugin.getLogger().info("Update available: " + version + " (installed: " + currentVersion + "). Download: " + latest.getUpdateLink());
            if (notifyOnJoin && !closed) {
                try {
                    scheduler.runTask(plugin, () -> {
                        if (!closed) plugin.getServer().getOnlinePlayers().forEach(this::notifyPlayer);
                    });
                } catch (IllegalPluginAccessException exception) {
                    if (!closed) throw exception;
                }
            }
        } catch (IOException | RuntimeException exception) {
            if (!closed) plugin.getLogger().warning("Could not check for updates: " + exception.getMessage());
        }
    }

    private ModrinthVersion getLatestVersionFromModrinth() throws IOException {
        URI uri = URI.create("https://api.modrinth.com/v2/project/skpackets/version?include_changelog=false");
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("User-Agent", "skPackets/" + currentVersion + " (" + DOWNLOAD_URL + ")");
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Modrinth returned HTTP " + connection.getResponseCode());
            }
            try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();
                return versions.isEmpty() ? null : new ModrinthVersion(versions.get(0));
            }
        } finally {
            connection.disconnect();
        }
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (closed || !notifyOnJoin || availableVersion == null) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(PERMISSION)) return;
        scheduler.runTaskLater(plugin, () -> notifyPlayer(player), 30L);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        notified.remove(event.getPlayer().getUniqueId());
    }

    private void notifyPlayer(Player player) {
        ModrinthVersion update = availableVersion;
        if (closed || update == null || !player.isOnline() || !player.hasPermission(PERMISSION)
                || !notified.add(player.getUniqueId())) return;
        LogUtil.sendMessage(player, "Update available: %s (installed: %s)", update.getUpdateVersion(), currentVersion);
        String link = update.getUpdateLink();
        LogUtil.sendMessage(player, Component.text("Download: ").append(Component.text(link, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl(link))));
    }

    @Override public void close() {
        closed = true;
        if (checkTask != null) checkTask.cancel();
        availableVersion = null;
        notified.clear();
    }
}
