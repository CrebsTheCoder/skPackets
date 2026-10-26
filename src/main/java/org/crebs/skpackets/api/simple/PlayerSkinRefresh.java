package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PlayerSkinRefresh extends PacketListenerAbstract
        implements Listener, AutoCloseable {
    private static PlayerSkinRefresh instance;
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> inventoryStates = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> serverTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> skinTeleports = new ConcurrentHashMap<>();
    private final Set<UUID> deferred = ConcurrentHashMap.newKeySet();
    private final AtomicInteger teleportSequence = new AtomicInteger(-1);
    private volatile boolean closed;

    public PlayerSkinRefresh(JavaPlugin plugin) {
        super(PacketListenerPriority.HIGHEST);
        this.plugin = plugin;
        instance = this;
        PacketEvents.getAPI().getEventManager().registerListener(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    static int inventoryState(Player player) {
        return instance.inventoryStates.getOrDefault(player.getUniqueId(), 0);
    }

    static int nextTeleport(Player player) {
        int id = instance.teleportSequence.getAndDecrement();
        instance.skinTeleports
                .computeIfAbsent(player.getUniqueId(), key -> ConcurrentHashMap.newKeySet())
                .add(id);
        return id;
    }

    static void request(Player player) {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Change displayed skins on the server thread");
        if (instance == null || instance.closed || !player.isOnline()) return;
        UUID id = player.getUniqueId();
        if (player.isDead()
                || (player.getOpenInventory().getType() != InventoryType.CRAFTING
                    && player.getOpenInventory().getType() != InventoryType.CREATIVE)
                || instance.serverTeleports.containsKey(id)
                || !instance.skinTeleports.getOrDefault(id, Set.of()).isEmpty()) {
            instance.deferred.add(id);
            return;
        }
        instance.deferred.remove(id);
        PlayerSkinManager.refreshPlayer(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (closed || event.isCancelled()) return;
        UUID id = event.getUser().getUUID();
        if (id == null) return;
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            var packet = new WrapperPlayServerWindowItems(event);
            if (packet.getWindowId() == 0) inventoryStates.put(id, packet.getStateId());
        } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            var packet = new WrapperPlayServerSetSlot(event);
            if (packet.getWindowId() == 0) inventoryStates.put(id, packet.getStateId());
        } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            int teleport = new WrapperPlayServerPlayerPositionAndLook(event).getTeleportId();
            if (!skinTeleports.getOrDefault(id, Set.of()).contains(teleport))
                serverTeleports.put(id, teleport);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (closed
                || event.isCancelled()
                || event.getPacketType() != PacketType.Play.Client.TELEPORT_CONFIRM) return;
        UUID id = event.getUser().getUUID();
        if (id == null) return;
        int teleport = new WrapperPlayClientTeleportConfirm(event).getTeleportId();
        Set<Integer> ours = skinTeleports.get(id);
        if (ours != null && ours.remove(teleport)) {
            event.setCancelled(true);
        } else {
            if (serverTeleports.remove(id, teleport) && ours != null) {
                ours.clear();
            }
        }
        resume(id);
    }

    private void resume(UUID id) {
        if (closed || !deferred.contains(id)) return;
        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            Player player = Bukkit.getPlayer(id);
                            if (!closed && player != null && deferred.contains(id)) request(player);
                        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        resume(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        resume(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        inventoryStates.remove(id);
        serverTeleports.remove(id);
        skinTeleports.remove(id);
        deferred.remove(id);
        PlayerSkinManager.forget(id);
    }

    @Override
    public void close() {
        closed = true;
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        HandlerList.unregisterAll(this);
        inventoryStates.clear();
        serverTeleports.clear();
        skinTeleports.clear();
        deferred.clear();
        PlayerSkinManager.clear();
        instance = null;
    }
}
