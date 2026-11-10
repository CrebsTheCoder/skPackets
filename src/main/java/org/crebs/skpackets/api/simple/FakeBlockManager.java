package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitBlockData;

public final class FakeBlockManager extends PacketListenerAbstract
        implements Listener, AutoCloseable {
    private record Chunk(UUID world, int x, int z) {}

    private record Position(int x, int y, int z) {}

    private record Fake(int stateId, boolean persistent) {}

    private record Refresh(UUID viewer, Chunk chunk) {}

    private final Map<UUID, Map<Chunk, Map<Position, Fake>>> blocks = new HashMap<>();
    private final Map<UUID, UUID> worlds = new ConcurrentHashMap<>();
    private final Set<Refresh> pending = ConcurrentHashMap.newKeySet();
    private final Queue<Refresh> refreshes = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean drainScheduled = new AtomicBoolean();
    private final Map<UUID, Map<Chunk, Set<Position>>> overrides = new ConcurrentHashMap<>();
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
        show(viewer, List.of(location), data, persistent);
    }

    public void show(Player viewer, Collection<Location> locations, BlockData data, boolean persistent) {
        checkThread();
        if (!viewer.isOnline() || locations.isEmpty()) return;
        UUID world = viewer.getWorld().getUID();
        Map<Chunk, Set<Position>> grouped = new HashMap<>();
        for (Location location : locations) {
            validate(location);
            UUID target = location.getWorld().getUID();
            if (!persistent && !world.equals(target)) continue;
            Chunk chunk = new Chunk(target, location.getBlockX() >> 4, location.getBlockZ() >> 4);
            grouped.computeIfAbsent(chunk, ignored -> new HashSet<>())
                    .add(new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        }
        if (grouped.isEmpty()) return;
        UUID id = viewer.getUniqueId();
        worlds.put(id, world);
        Fake fake = new Fake(fromBukkitBlockData(data).getGlobalId(), persistent);
        var chunks = blocks.computeIfAbsent(id, ignored -> new HashMap<>());
        var watchedChunks = persistent
                ? overrides.computeIfAbsent(id, ignored -> new ConcurrentHashMap<>()) : overrides.get(id);
        Map<Chunk, Map<Position, Integer>> changes = new HashMap<>();
        for (var group : grouped.entrySet()) {
            Chunk chunk = group.getKey();
            var entries = chunks.computeIfAbsent(chunk, ignored -> new HashMap<>());
            Set<Position> watched = watchedChunks == null ? null : persistent
                    ? watchedChunks.computeIfAbsent(chunk, ignored -> ConcurrentHashMap.newKeySet()) : watchedChunks.get(chunk);
            boolean visible = visible(viewer, chunk);
            Map<Position, Integer> updates = visible ? new HashMap<>() : null;
            for (Position position : group.getValue()) {
                Fake previous = entries.put(position, fake);
                if (watched != null) {
                    if (persistent) watched.add(position);
                    else watched.remove(position);
                }
                if (visible && (!persistent || !fake.equals(previous))) updates.put(position, fake.stateId());
            }
            if (watched != null && watched.isEmpty()) watchedChunks.remove(chunk);
            if (updates != null && !updates.isEmpty()) changes.put(chunk, updates);
        }
        if (watchedChunks != null && watchedChunks.isEmpty()) overrides.remove(id);
        sendChanges(viewer, changes);
    }

    private void validate(Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "Location needs a world.");
        location.checkFinite();
        if (location.getBlockY() < world.getMinHeight()
                || location.getBlockY() >= world.getMaxHeight())
            throw new IllegalArgumentException("Fake block is outside the world height.");
    }

    public void reset(Player viewer, Location location) {
        reset(viewer, List.of(location));
    }

    public void reset(Player viewer, Collection<Location> locations) {
        checkThread();
        for (Location location : locations) {
            Objects.requireNonNull(location.getWorld(), "Location needs a world.");
            location.checkFinite();
        }
        Map<Chunk, Map<Position, Integer>> changes = new HashMap<>();
        List<org.bukkit.block.TileState> tiles = new ArrayList<>();
        for (Location location : locations) reset(viewer, location, changes, tiles);
        sendChanges(viewer, changes);
        restoreTiles(viewer, tiles);
    }

    private void reset(Player viewer, Location location, Map<Chunk, Map<Position, Integer>> changes,
                       List<org.bukkit.block.TileState> tiles) {
        World world = Objects.requireNonNull(location.getWorld(), "Location needs a world.");
        var chunks = blocks.get(viewer.getUniqueId());
        if (chunks == null) return;
        Chunk chunk =
                new Chunk(world.getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        var entries = chunks.get(chunk);
        if (entries == null) return;
        Position pos =
                new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (entries.remove(pos) != null && visible(viewer, chunk))
            restore(viewer, chunk, pos, changes, tiles);
        if (entries.isEmpty()) chunks.remove(chunk);
        unwatch(viewer.getUniqueId(), chunk, pos);
        prune(viewer.getUniqueId());
    }

    public void clear(Player viewer) {
        checkThread();
        var chunks = blocks.remove(viewer.getUniqueId());
        overrides.remove(viewer.getUniqueId());
        if (chunks == null) return;
        if (!viewer.isOnline()) return;
        Map<Chunk, Map<Position, Integer>> changes = new HashMap<>();
        List<org.bukkit.block.TileState> tiles = new ArrayList<>();
        UUID world = viewer.getWorld().getUID();
        for (long key : viewer.getSentChunkKeys()) {
            Chunk chunk = new Chunk(world, (int) key, (int) (key >> 32));
            var entries = chunks.get(chunk);
            if (entries != null && visible(viewer, chunk))
                entries.keySet().forEach(pos -> restore(viewer, chunk, pos, changes, tiles));
        }
        sendChanges(viewer, changes);
        restoreTiles(viewer, tiles);
    }

    private void unwatch(UUID id, Chunk chunk, Position position) {
        var chunks = overrides.get(id);
        if (chunks == null) return;
        Set<Position> positions = chunks.get(chunk);
        if (positions != null) {
            positions.remove(position);
            if (positions.isEmpty()) chunks.remove(chunk);
        }
        if (chunks.isEmpty()) overrides.remove(id);
    }

    private void prune(UUID id) {
        var chunks = blocks.get(id);
        if (chunks != null && chunks.isEmpty()) {
            blocks.remove(id);
        }
    }

    private static boolean visible(Player viewer, Chunk chunk) {
        return viewer.isOnline() && viewer.getWorld().getUID().equals(chunk.world())
                && viewer.isChunkSent(chunkKey(chunk.x(), chunk.z()))
                && viewer.getWorld().isChunkLoaded(chunk.x(), chunk.z());
    }

    private static long chunkKey(int x, int z) {
        return (x & 0xffffffffL) | ((long) z << 32);
    }

    private void restore(Player viewer, Chunk chunk, Position pos,
                         Map<Chunk, Map<Position, Integer>> changes, List<org.bukkit.block.TileState> tiles) {
        var block = viewer.getWorld().getBlockAt(pos.x(), pos.y(), pos.z());
        changes.computeIfAbsent(chunk, ignored -> new HashMap<>())
                .put(pos, fromBukkitBlockData(block.getBlockData()).getGlobalId());
        if (block.getState(false) instanceof org.bukkit.block.TileState state) tiles.add(state);
    }

    private void restoreTiles(Player viewer, List<org.bukkit.block.TileState> tiles) {
        for (var state : tiles) viewer.sendBlockUpdate(state.getLocation(), state);
    }

    private void sendChanges(Player viewer, Map<Chunk, Map<Position, Integer>> changes) {
        changes.forEach((chunk, entries) -> {
            Map<Integer, List<WrapperPlayServerMultiBlockChange.EncodedBlock>> sections = new HashMap<>();
            entries.forEach((pos, state) -> sections.computeIfAbsent(pos.y() >> 4, ignored -> new ArrayList<>())
                    .add(new WrapperPlayServerMultiBlockChange.EncodedBlock(state, pos.x(), pos.y(), pos.z())));
            sections.forEach((y, updates) -> PacketEvents.getAPI().getPlayerManager().sendPacketSilently(viewer,
                    new WrapperPlayServerMultiBlockChange(new Vector3i(chunk.x(), y, chunk.z()), null,
                            updates.toArray(WrapperPlayServerMultiBlockChange.EncodedBlock[]::new))));
        });
    }

    @Override
    @SuppressWarnings("ConstantValue")
    public void onPacketSend(@org.jetbrains.annotations.NotNull PacketSendEvent event) {
        if (closed || event.isCancelled()) return;
        var type = event.getPacketType();
        if (type != PacketType.Play.Server.CHUNK_DATA && type != PacketType.Play.Server.BLOCK_CHANGE
                && type != PacketType.Play.Server.MULTI_BLOCK_CHANGE) return;
        UUID id = event.getUser().getUUID();
        var watchedChunks = id == null ? null : overrides.get(id);
        if (watchedChunks == null) return;
        UUID world = worlds.get(id);
        if (world == null) return;
        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            var column = new WrapperPlayServerChunkData(event).getColumn();
            Refresh refresh = new Refresh(id, new Chunk(world, column.getX(), column.getZ()));
            if (watchedChunks.containsKey(refresh.chunk()))
                event.getTasksAfterSend().add(() -> queue(refresh));
        } else if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            var packet = new WrapperPlayServerBlockChange(event);
            var pos = packet.getBlockPosition();
            var positions = watchedChunks.get(new Chunk(world, pos.getX() >> 4, pos.getZ() >> 4));
            if (positions != null
                    && positions.contains(new Position(pos.getX(), pos.getY(), pos.getZ())))
                event.setCancelled(true);
        } else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            var packet = new WrapperPlayServerMultiBlockChange(event);
            var chunk = packet.getChunkPosition();
            var positions = watchedChunks.get(new Chunk(world, chunk.getX(), chunk.getZ()));
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
        var watchedChunks = overrides.get(refresh.viewer());
        if (closed || watchedChunks == null || !watchedChunks.containsKey(refresh.chunk()) || !pending.add(refresh)) return;
        refreshes.add(refresh);
        scheduleDrain();
    }

    private void scheduleDrain() {
        if (!closed && drainScheduled.compareAndSet(false, true))
            Bukkit.getScheduler().runTask(plugin, this::drain);
    }

    private void drain() {
        try {
            for (int i = 0; i < 64 && !closed; i++) {
                Refresh refresh = refreshes.poll();
                if (refresh == null) break;
                pending.remove(refresh);
                Player player = Bukkit.getPlayer(refresh.viewer());
                if (player == null || !visible(player, refresh.chunk())) continue;
                var chunks = blocks.get(refresh.viewer());
                var entries = chunks == null ? null : chunks.get(refresh.chunk());
                var watchedChunks = overrides.get(refresh.viewer());
                var positions = watchedChunks == null ? null : watchedChunks.get(refresh.chunk());
                if (entries == null || positions == null) continue;
                Map<Position, Integer> changes = new HashMap<>(positions.size());
                for (Position pos : positions) {
                    Fake fake = entries.get(pos);
                    if (fake != null && fake.persistent()) changes.put(pos, fake.stateId());
                }
                sendChanges(player, Map.of(refresh.chunk(), changes));
            }
        } finally {
            drainScheduled.set(false);
            if (!refreshes.isEmpty()) scheduleDrain();
        }
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
        refreshWorldLater(event.getPlayer());
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
        refreshWorldLater(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        worlds.put(id, event.getRespawnLocation().getWorld().getUID());
        discardTransient(id);
        refreshWorldLater(event.getPlayer());
    }

    private void refreshWorldLater(Player player) {
        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            if (!closed && player.isOnline()) refreshWorld(player);
                        });
    }

    private void refreshWorld(Player player) {
        UUID id = player.getUniqueId();
        if (!overrides.containsKey(id)) return;
        UUID world = player.getWorld().getUID();
        for (long key : player.getSentChunkKeys())
            queue(new Refresh(id, new Chunk(world, (int) key, (int) (key >> 32))));
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
        pending.clear();
        refreshes.clear();
        overrides.clear();
    }
}
