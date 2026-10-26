package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class FakeBlockManager extends PacketListenerAbstract
        implements Listener, AutoCloseable {
    private record Chunk(UUID world, int x, int z) {}

    private record Position(int x, int y, int z) {}

    private record Fake(BlockData data, boolean persistent) {}

    private record Refresh(UUID viewer, Chunk chunk) {}

    private final Map<UUID, Map<Chunk, Map<Position, Fake>>> blocks = new HashMap<>();
    private final Map<UUID, UUID> worlds = new ConcurrentHashMap<>();
    private final Set<Refresh> pending = ConcurrentHashMap.newKeySet();
    private final Map<Refresh, Set<Position>> overrides = new ConcurrentHashMap<>();
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final JavaPlugin plugin;
    private volatile boolean closed;

    public FakeBlockManager(JavaPlugin plugin) {
        super(PacketListenerPriority.HIGHEST);
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        PacketEvents.getAPI().getEventManager().registerListener(this);
        for (Player player : Bukkit.getOnlinePlayers())
            worlds.put(player.getUniqueId(), player.getWorld().getUID());
    }

    private void checkThread() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Fake blocks must be managed on the server thread.");
        if (closed) throw new IllegalStateException("Fake block manager is closed.");
    }

    public void show(Player viewer, Location location, BlockData data, boolean persistent) {
        checkThread();
        World world = Objects.requireNonNull(location.getWorld(), "Location needs a world.");
        location.checkFinite();
        if (location.getBlockY() < world.getMinHeight()
                || location.getBlockY() >= world.getMaxHeight())
            throw new IllegalArgumentException("Fake block is outside the world height.");
        if (!viewer.isOnline()) return;
        if (!persistent && !viewer.getWorld().equals(world)) return;
        UUID id = viewer.getUniqueId();
        worlds.put(id, viewer.getWorld().getUID());
        Chunk chunk =
                new Chunk(world.getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        Position position =
                new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        blocks.computeIfAbsent(id, ignored -> new HashMap<>())
                .computeIfAbsent(chunk, ignored -> new HashMap<>())
                .put(position, new Fake(data.clone(), persistent));
        active.add(id);
        updateWatch(id, chunk);
        if (viewer.getWorld().equals(world)) send(viewer, position, data);
    }

    public void reset(Player viewer, Location location) {
        checkThread();
        World world = Objects.requireNonNull(location.getWorld(), "Location needs a world.");
        var chunks = blocks.get(viewer.getUniqueId());
        if (chunks == null) return;
        Chunk chunk =
                new Chunk(world.getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        var entries = chunks.get(chunk);
        if (entries == null) return;
        Position pos =
                new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (entries.remove(pos) != null
                && viewer.isOnline()
                && viewer.getWorld().equals(world)
                && world.isChunkLoaded(chunk.x(), chunk.z())) restore(viewer, pos);
        if (entries.isEmpty()) chunks.remove(chunk);
        updateWatch(viewer.getUniqueId(), chunk);
        prune(viewer.getUniqueId());
    }

    public void clear(Player viewer) {
        checkThread();
        var chunks = blocks.remove(viewer.getUniqueId());
        active.remove(viewer.getUniqueId());
        overrides.keySet().removeIf(refresh -> refresh.viewer().equals(viewer.getUniqueId()));
        if (chunks == null || !viewer.isOnline()) return;
        chunks.forEach(
                (chunk, entries) -> {
                    if (chunk.world().equals(viewer.getWorld().getUID())
                            && viewer.getWorld().isChunkLoaded(chunk.x(), chunk.z()))
                        entries.keySet().forEach(pos -> restore(viewer, pos));
                });
    }

    private void updateWatch(UUID id, Chunk chunk) {
        var chunks = blocks.get(id);
        var entries = chunks == null ? null : chunks.get(chunk);
        Refresh key = new Refresh(id, chunk);
        Set<Position> positions = new HashSet<>();
        if (entries != null)
            entries.forEach((pos, fake) -> {
                if (fake.persistent()) positions.add(pos);
            });
        if (positions.isEmpty()) overrides.remove(key);
        else overrides.put(key, Set.copyOf(positions));
    }

    private void prune(UUID id) {
        var chunks = blocks.get(id);
        if (chunks != null && chunks.isEmpty()) {
            blocks.remove(id);
            active.remove(id);
        }
    }

    private void restore(Player viewer, Position pos) {
        var block = viewer.getWorld().getBlockAt(pos.x(), pos.y(), pos.z());
        send(viewer, pos, block.getBlockData());
        if (block.getState() instanceof org.bukkit.block.TileState state)
            viewer.sendBlockUpdate(block.getLocation(), state);
    }

    private void send(Player viewer, Position pos, BlockData data) {
        var packet =
                new WrapperPlayServerBlockChange(
                        new com.github.retrooper.packetevents.util.Vector3i(
                                pos.x(), pos.y(), pos.z()),
                        io.github.retrooper.packetevents.util.SpigotConversionUtil
                                .fromBukkitBlockData(data));
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(viewer, packet);
    }

    @Override
    public void onPacketSend(@org.jetbrains.annotations.NotNull PacketSendEvent event) {
        if (closed || event.isCancelled()) return;
        UUID id = event.getUser().getUUID();
        if (!active.contains(id)) return;
        UUID world = worlds.get(id);
        if (world == null) return;
        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            var column = new WrapperPlayServerChunkData(event).getColumn();
            Refresh refresh = new Refresh(id, new Chunk(world, column.getX(), column.getZ()));
            if (overrides.containsKey(refresh))
                event.getTasksAfterSend().add(() -> queue(refresh));
        } else if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            var packet = new WrapperPlayServerBlockChange(event);
            var pos = packet.getBlockPosition();
            var positions = overrides.get(new Refresh(id,
                    new Chunk(world, pos.getX() >> 4, pos.getZ() >> 4)));
            if (positions != null
                    && positions.contains(new Position(pos.getX(), pos.getY(), pos.getZ())))
                event.setCancelled(true);
        } else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            var packet = new WrapperPlayServerMultiBlockChange(event);
            var chunk = packet.getChunkPosition();
            var positions = overrides.get(new Refresh(id,
                    new Chunk(world, chunk.getX(), chunk.getZ())));
            if (positions == null) return;
            var updates = packet.getBlocks();
            List<WrapperPlayServerMultiBlockChange.EncodedBlock> remaining = null;
            for (int i = 0; i < updates.length; i++) {
                var block = updates[i];
                if (positions.contains(new Position(block.getX(), block.getY(), block.getZ()))) {
                    if (remaining == null) {
                        remaining = new ArrayList<>(updates.length);
                        remaining.addAll(Arrays.asList(updates).subList(0, i));
                    }
                } else if (remaining != null) remaining.add(block);
            }
            if (remaining == null) return;
            if (remaining.isEmpty()) event.setCancelled(true);
            else {
                packet.setBlocks(remaining.toArray(WrapperPlayServerMultiBlockChange.EncodedBlock[]::new));
                event.markForReEncode(true);
            }
        }
    }

    private void queue(Refresh refresh) {
        if (closed || !overrides.containsKey(refresh) || !pending.add(refresh)) return;
        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            pending.remove(refresh);
                            if (closed) return;
                            Player player = Bukkit.getPlayer(refresh.viewer());
                            if (player == null
                                    || !player.getWorld().getUID().equals(refresh.chunk().world()))
                                return;
                            var chunks = blocks.get(refresh.viewer());
                            if (chunks == null) return;
                            var entries = chunks.get(refresh.chunk());
                            if (entries == null) return;
                            entries.forEach(
                                    (pos, fake) -> {
                                        if (fake.persistent()) send(player, pos, fake.data());
                                    });
                        });
    }

    private void discardTransient(UUID id) {
        var chunks = blocks.get(id);
        if (chunks == null) return;
        chunks.values().forEach(entries -> entries.values().removeIf(fake -> !fake.persistent()));
        chunks.values().removeIf(Map::isEmpty);
        prune(id);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        worlds.put(event.getPlayer().getUniqueId(), event.getPlayer().getWorld().getUID());
        refreshWorld(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        worlds.remove(id);
        discardTransient(id);
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        worlds.put(player.getUniqueId(), player.getWorld().getUID());
        discardTransient(player.getUniqueId());
        refreshWorld(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        worlds.put(id, event.getRespawnLocation().getWorld().getUID());
        discardTransient(id);
        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            if (!closed && event.getPlayer().isOnline())
                                refreshWorld(event.getPlayer());
                        });
    }

    private void refreshWorld(Player player) {
        var chunks = blocks.get(player.getUniqueId());
        if (chunks == null) return;
        for (Chunk chunk : chunks.keySet())
            if (chunk.world().equals(player.getWorld().getUID()))
                queue(new Refresh(player.getUniqueId(), chunk));
    }

    @Override
    public void close() {
        if (closed) return;
        for (Player player : Bukkit.getOnlinePlayers()) clear(player);
        closed = true;
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        HandlerList.unregisterAll(this);
        blocks.clear();
        worlds.clear();
        active.clear();
        pending.clear();
        overrides.clear();
    }
}
