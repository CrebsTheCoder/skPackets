package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class FakeInventoryManager extends PacketListenerAbstract
        implements Listener, AutoCloseable {
    private record Key(UUID player, int window) {}

    private final Map<Key, Map<Integer, com.github.retrooper.packetevents.protocol.item.ItemStack>>
            overrides = new ConcurrentHashMap<>();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final JavaPlugin plugin;
    private volatile boolean closed;

    private record Window(int id, int state, int slots) {}

    private final Map<UUID, Window> containers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerStates = new ConcurrentHashMap<>();
    private final Set<UUID> modified = new HashSet<>();

    public FakeInventoryManager(JavaPlugin plugin) {
        super(PacketListenerPriority.HIGHEST);
        this.plugin = plugin;
        PacketEvents.getAPI().getEventManager().registerListener(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (closed || event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.WINDOW_ITEMS
                && event.getPacketType() != PacketType.Play.Server.SET_SLOT
                && event.getPacketType() != PacketType.Play.Server.SET_PLAYER_INVENTORY) return;
        UUID id = event.getUser().getUUID();
        if (id == null) return;
        if (event.getPacketType() == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            var packet = new WrapperPlayServerSetPlayerInventory(event);
            var values = overrides.get(new Key(id, 0));
            var item = values == null ? null : values.get(playerRawSlot(packet.getSlot()));
            if (item != null) {
                packet.setStack(item.copy());
                event.markForReEncode(true);
            }
            return;
        }
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            var packet = new WrapperPlayServerWindowItems(event);
            var replacements = overrides.get(new Key(id, packet.getWindowId()));
            if (replacements != null) {
                var items = new ArrayList<>(packet.getItems());
                replacements.forEach(
                        (slot, item) -> {
                            if (slot < items.size()) items.set(slot, item.copy());
                        });
                packet.setItems(items);
                event.markForReEncode(true);
            }
            if (packet.getWindowId() == 0) playerStates.put(id, packet.getStateId());
            else if (packet.getWindowId() > 0)
                containers.put(
                        id,
                        new Window(
                                packet.getWindowId(),
                                packet.getStateId(),
                                packet.getItems().size()));
        } else {
            var packet = new WrapperPlayServerSetSlot(event);
            var replacements =
                    overrides.get(
                            new Key(id, packet.getWindowId() == -2 ? 0 : packet.getWindowId()));
            var replacement =
                    replacements == null
                            ? null
                            : replacements.get(
                                    packet.getWindowId() == -2
                                            ? playerRawSlot(packet.getSlot())
                                            : packet.getSlot());
            if (replacement != null) {
                packet.setItem(replacement.copy());
                event.markForReEncode(true);
            }
            if (packet.getWindowId() == 0) playerStates.put(id, packet.getStateId());
            else
                containers.computeIfPresent(
                        id,
                        (key, old) ->
                                old.id() == packet.getWindowId()
                                        ? new Window(old.id(), packet.getStateId(), old.slots())
                                        : old);
        }
    }

    private Window window(Player player) {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException(
                    "Fake inventory slots must be changed on the server thread.");
        if (player.getOpenInventory().getType() == InventoryType.CRAFTING
                || player.getOpenInventory().getType() == InventoryType.CREATIVE)
            return new Window(0, playerStates.getOrDefault(player.getUniqueId(), 0), 46);
        Window window = containers.get(player.getUniqueId());
        if (window == null)
            throw new IllegalArgumentException(
                    "The open inventory is not synchronized yet. Wait one tick after opening it.");
        return window;
    }

    public void show(Player player, int slot, ItemStack item, boolean persistent) {
        if (!player.isOnline()) return;
        Window window = window(player);
        validateSlot(slot, window.slots());
        var encoded =
                SpigotConversionUtil.fromBukkitItemStack(
                        item == null ? new ItemStack(Material.AIR) : item.clone());
        Key key = new Key(player.getUniqueId(), window.id());
        overrides.compute(
                key,
                (ignored, previous) -> {
                    var values =
                            previous == null
                                    ? new HashMap<
                                            Integer,
                                            com.github.retrooper.packetevents.protocol.item
                                                    .ItemStack>()
                                    : new HashMap<>(previous);
                    if (persistent) values.put(slot, encoded.copy());
                    else values.remove(slot);
                    return values.isEmpty() ? null : Map.copyOf(values);
                });
        send(player, window, slot, item);
        modified.add(player.getUniqueId());
    }

    public void reset(Player player, int slot) {
        if (!player.isOnline()) return;
        Window window = window(player);
        validateSlot(slot, window.slots());
        overrides.computeIfPresent(new Key(player.getUniqueId(), window.id()), (key, previous) -> {
            var values = new HashMap<>(previous);
            values.remove(slot);
            return values.isEmpty() ? null : Map.copyOf(values);
        });
        send(player, window, slot, player.getOpenInventory().getItem(slot));
    }

    public static void validateSlot(int slot, int count) {
        if (slot < 0 || slot >= count)
            throw new IllegalArgumentException(
                    "Inventory slot must be between 0 and " + (count - 1) + ".");
    }

    private void send(Player player, Window window, int slot, ItemStack item) {
        PacketEvents.getAPI()
                .getPlayerManager()
                .sendPacketSilently(
                        player,
                        new WrapperPlayServerSetSlot(
                                window.id(),
                                window.state(),
                                slot,
                                SpigotConversionUtil.fromBukkitItemStack(
                                        item == null ? new ItemStack(Material.AIR) : item)));
    }

    public void clear(Player player) {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException(
                    "Fake inventory slots must be changed on the server thread.");
        overrides.keySet().removeIf(key -> key.player().equals(player.getUniqueId()));
        if (player.isOnline()) player.updateInventory();
        modified.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        discardContainer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        discardContainer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        containers.remove(id);
        overrides.keySet().removeIf(key -> key.player().equals(id));
        playerStates.remove(id);
        modified.remove(id);
    }

    public static int playerRawSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot <= 8) return inventorySlot + 36;
        if (inventorySlot >= 9 && inventorySlot <= 35) return inventorySlot;
        if (inventorySlot >= 36 && inventorySlot <= 39) return 44 - inventorySlot;
        return inventorySlot == 40 ? 45 : -1;
    }

    private void discardContainer(UUID id) {
        containers.remove(id);
        overrides.keySet().removeIf(key -> key.player().equals(id) && key.window() != 0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (closed
                || event.isCancelled()
                || event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;
        UUID id = event.getUser().getUUID();
        if (id == null) return;
        Window tracked = containers.get(id);
        if (!overrides.containsKey(new Key(id, 0))
                && (tracked == null || !overrides.containsKey(new Key(id, tracked.id())))) return;
        if (!pending.add(id)) return;
        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {
                            pending.remove(id);
                            if (closed) return;
                            Player player = Bukkit.getPlayer(id);
                            if (player == null) return;
                            Window window;
                            try {
                                window = window(player);
                            } catch (IllegalArgumentException ex) {
                                return;
                            }
                            var values = overrides.get(new Key(id, window.id()));
                            if (values == null) return;
                            values.forEach(
                                    (slot, item) ->
                                            PacketEvents.getAPI()
                                                    .getPlayerManager()
                                                    .sendPacketSilently(
                                                            player,
                                                            new WrapperPlayServerSetSlot(
                                                                    window.id(),
                                                                    window.state(),
                                                                    slot,
                                                                    item.copy())));
                        },
                        2L);
    }

    @Override
    public void close() {
        closed = true;
        overrides.clear();
        pending.clear();
        for (UUID id : Set.copyOf(modified)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) clear(player);
        }
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        HandlerList.unregisterAll(this);
        containers.clear();
        playerStates.clear();
        modified.clear();
    }
}
