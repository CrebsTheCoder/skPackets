package org.crebs.skpackets.api.entity;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PacketEntityManager implements Listener, AutoCloseable {
    private record Chunk(UUID world, int x, int z) {}
    private record Tracked(
            WrapperEntity entity, Chunk chunk, boolean persistent, Set<UUID> viewers) {}

    private final Map<Integer, Tracked> entities = new HashMap<>();
    private final Map<UUID, Set<Integer>> byViewer = new HashMap<>();
    private final Map<Chunk, Set<Integer>> byChunk = new HashMap<>();
    private final Map<UUID, WrapperEntity> byUuid = new ConcurrentHashMap<>();
    private final Set<UUID> pendingRestores = new HashSet<>();
    private final RealEntityPassengerManager realPassengers = new RealEntityPassengerManager();
    private final JavaPlugin plugin;
    private volatile boolean closed;

    public PacketEntityManager(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public WrapperEntity spawn(
            EntityType type, Location location, Collection<? extends Player> viewers) {
        return spawn(type, location, viewers, true);
    }

    public WrapperEntity spawn(
            EntityType type,
            Location location,
            Collection<? extends Player> viewers,
            boolean persistent) {
        return spawn(type, location, viewers, persistent, entity -> {});
    }

    public WrapperEntity spawn(
            EntityType type,
            Location location,
            Collection<? extends Player> viewers,
            boolean persistent,
            Consumer<WrapperEntity> configure) {
        checkThread();
        Objects.requireNonNull(location.getWorld(), "Location must have a world");
        location.checkFinite();
        if (type != EntityType.PLAYER && !type.isSpawnable()) {
            throw new IllegalArgumentException("Unsupported packet entity type: " + type);
        }
        var packetType = SpigotConversionUtil.fromBukkitEntityType(type);
        WrapperEntity entity =
                type == EntityType.PLAYER
                        ? new me.tofaa.entitylib.wrapper.WrapperPlayer(
                                new com.github.retrooper.packetevents.protocol.player.UserProfile(
                                        UUID.randomUUID(), "NPC"))
                        : type.isAlive()
                                ? new WrapperLivingEntity(packetType)
                                : new WrapperEntity(packetType);
        configure.accept(entity);
        return spawn(entity, location, viewers, persistent);
    }

    public WrapperEntity spawn(
            WrapperEntity entity,
            Location location,
            Collection<? extends Player> viewers,
            boolean persistent) {
        checkThread();
        Objects.requireNonNull(location.getWorld(), "Location must have a world");
        location.checkFinite();
        entity.setWorld(location.getWorld().getName());
        Set<UUID> ids = new HashSet<>();
        for (Player viewer : viewers) {
            if (viewer.isOnline()) {
                ids.add(viewer.getUniqueId());
            }
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("At least one online viewer is required");
        }
        if (entities.containsKey(entity.getEntityId())
                || me.tofaa.entitylib.EntityLib.get().getEntity(entity.getEntityId()) != null) {
            throw new IllegalArgumentException(
                    "Entity ID is already in use: " + entity.getEntityId());
        }
        Tracked tracked = new Tracked(entity, chunk(location), persistent, ids);
        entities.put(entity.getEntityId(), tracked);
        byUuid.put(entity.getUuid(), entity);
        byChunk.computeIfAbsent(tracked.chunk(), ignored -> new HashSet<>()).add(entity.getEntityId());
        for (UUID id : ids) indexViewer(id, entity.getEntityId());
        entity.spawn(SpigotConversionUtil.fromBukkitLocation(location));
        for (Player viewer : viewers) {
            show(tracked, viewer);
        }
        return entity;
    }

    public Optional<WrapperEntity> getEntity(int id) {
        checkThread();
        Tracked tracked = entities.get(id);
        return tracked == null ? Optional.empty() : Optional.of(tracked.entity());
    }

    public Collection<WrapperEntity> getEntities() {
        checkThread();
        return entities.values().stream().map(Tracked::entity).toList();
    }

    public Optional<WrapperEntity> getEntity(UUID uuid) {
        if (closed) throw new IllegalStateException("Packet entity manager is closed");
        WrapperEntity entity = byUuid.get(uuid);
        return entity != null && entity.isSpawned() ? Optional.of(entity) : Optional.empty();
    }

    private static Chunk chunk(Location location) {
        return new Chunk(location.getWorld().getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private void indexViewer(UUID viewer, int id) {
        byViewer.computeIfAbsent(viewer, ignored -> new HashSet<>()).add(id);
    }

    private static <K> void unindex(Map<K, Set<Integer>> index, K key, int id) {
        Set<Integer> ids = index.get(key);
        if (ids != null && ids.remove(id) && ids.isEmpty()) index.remove(key);
    }

    private void forget(Tracked tracked) {
        int id = tracked.entity().getEntityId();
        entities.remove(id);
        byUuid.remove(tracked.entity().getUuid(), tracked.entity());
        unindex(byChunk, tracked.chunk(), id);
        for (UUID viewer : tracked.viewers()) unindex(byViewer, viewer, id);
        realPassengers.remove(tracked.entity());
    }

    public Set<UUID> getViewers(WrapperEntity entity) {
        checkThread();
        return Set.copyOf(requireTracked(entity).viewers());
    }

    public boolean isPersistent(WrapperEntity entity) {
        checkThread();
        return requireTracked(entity).persistent();
    }

    public void addViewer(WrapperEntity entity, Player player) {
        checkThread();
        Tracked tracked = requireTracked(entity);
        if (!player.isOnline()) {
            throw new IllegalArgumentException("Viewer must be online");
        }
        if (tracked.viewers().add(player.getUniqueId())) indexViewer(player.getUniqueId(), entity.getEntityId());
        show(tracked, player);
    }

    public void removeViewer(WrapperEntity entity, UUID viewer) {
        checkThread();
        Tracked tracked = requireTracked(entity);
        tracked.viewers().remove(viewer);
        unindex(byViewer, viewer, entity.getEntityId());
        realPassengers.removeViewer(entity, viewer);
        entity.removeViewer(viewer);
    }

    public void setViewers(WrapperEntity entity, Collection<? extends Player> viewers) {
        checkThread();
        Tracked tracked = requireTracked(entity);
        Map<UUID, Player> desired = new HashMap<>();
        for (Player player : viewers) {
            if (!player.isOnline()) throw new IllegalArgumentException("Viewer must be online");
            desired.put(player.getUniqueId(), player);
        }
        for (UUID id : Set.copyOf(tracked.viewers()))
            if (!desired.containsKey(id)) removeViewer(entity, id);
        for (var entry : desired.entrySet())
            if (!tracked.viewers().contains(entry.getKey())) addViewer(entity, entry.getValue());
    }

    public void updateRealPassengers(Entity vehicle, Collection<WrapperEntity> passengers, boolean mount) {
        checkThread();
        if (mount) {
            for (WrapperEntity passenger : passengers) {
                if (passenger.getRiding() != null) passenger.getRiding().removePassenger(passenger);
            }
        }
        realPassengers.update(vehicle, passengers, mount);
    }

    public void removeRealPassenger(WrapperEntity passenger) {
        checkThread();
        realPassengers.remove(passenger);
    }

    public void teleport(WrapperEntity entity, Location location) {
        checkThread();
        Tracked tracked = requireTracked(entity);
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location must have a world");
        }
        location.checkFinite();
        UUID world = location.getWorld().getUID();
        Chunk destination = chunk(location);
        boolean changedWorld = !tracked.chunk().world().equals(world);
        if (changedWorld) {
            realPassengers.remove(entity);
            for (UUID viewer : Set.copyOf(entity.getViewers())) {
                entity.removeViewer(viewer);
            }
        }
        boolean changedChunk = !tracked.chunk().equals(destination);
        if (changedChunk) {
            unindex(byChunk, tracked.chunk(), entity.getEntityId());
            tracked = new Tracked(entity, destination, tracked.persistent(), tracked.viewers());
            entities.put(entity.getEntityId(), tracked);
            byChunk.computeIfAbsent(destination, ignored -> new HashSet<>()).add(entity.getEntityId());
            for (UUID id : Set.copyOf(entity.getViewers())) {
                Player player = Bukkit.getPlayer(id);
                if (player == null || !visible(tracked, player)) {
                    realPassengers.removeViewer(entity, id);
                    entity.removeViewer(id);
                }
            }
        }
        entity.setWorld(location.getWorld().getName());
        entity.teleport(SpigotConversionUtil.fromBukkitLocation(location));
        if (changedChunk) for (UUID id : tracked.viewers()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) show(tracked, player);
        }
    }

    public void remove(WrapperEntity entity) {
        checkThread();
        if (!entities.containsKey(entity.getEntityId())) {
            return;
        }
        forget(requireTracked(entity));
        entity.remove();
    }

    private Tracked requireTracked(WrapperEntity entity) {
        Tracked tracked = entities.get(entity.getEntityId());
        if (tracked == null || tracked.entity() != entity) {
            throw new IllegalArgumentException("Entity is not managed here");
        }
        return tracked;
    }

    private void show(Tracked tracked, Player player) {
        if (player.isOnline()
                && tracked.viewers().contains(player.getUniqueId())
                && visible(tracked, player)
                && tracked.entity().isSpawned()) {
            tracked.entity().addViewer(player.getUniqueId());
        }
    }

    private boolean visible(Tracked tracked, Player player) {
        Chunk chunk = tracked.chunk();
        return chunk.world().equals(player.getWorld().getUID())
                && (hasMount(tracked.entity())
                    || player.isChunkSent((chunk.x() & 0xffffffffL) | ((long) chunk.z() << 32)));
    }

    private boolean hasMount(WrapperEntity entity) {
        return entity.getRiding() != null || !entity.getPassengers().isEmpty() || realPassengers.isMounted(entity);
    }

    private void restore(Player player) {
        UUID id = player.getUniqueId();
        if (!pendingRestores.add(id)) return;
        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            pendingRestores.remove(id);
                            if (closed || !player.isOnline()) {
                                return;
                            }
                            for (int entityId : Set.copyOf(byViewer.getOrDefault(id, Set.of()))) {
                                Tracked tracked = entities.get(entityId);
                                if (tracked == null) continue;
                                if (!tracked.entity().isSpawned()) forget(tracked);
                                else show(tracked, player);
                            }
                        });
    }

    private void hide(Player player) {
        realPassengers.forgetViewer(player.getUniqueId());
        for (int id : Set.copyOf(byViewer.getOrDefault(player.getUniqueId(), Set.of()))) {
            Tracked tracked = entities.get(id);
            if (tracked != null) tracked.entity().removeViewer(player.getUniqueId());
        }
    }

    @EventHandler
    public void onChunkLoad(PlayerChunkLoadEvent event) {
        if (closed) return;
        Chunk chunk = new Chunk(event.getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ());
        UUID viewer = event.getPlayer().getUniqueId();
        for (int id : Set.copyOf(byChunk.getOrDefault(chunk, Set.of()))) {
            Tracked tracked = entities.get(id);
            if (tracked == null || !tracked.viewers().contains(viewer)) continue;
            if (!tracked.entity().isSpawned()) forget(tracked);
            else tracked.entity().addViewer(viewer);
        }
    }

    @EventHandler
    public void onChunkUnload(PlayerChunkUnloadEvent event) {
        if (closed) return;
        Chunk chunk = new Chunk(event.getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ());
        UUID viewer = event.getPlayer().getUniqueId();
        for (int id : Set.copyOf(byChunk.getOrDefault(chunk, Set.of()))) {
            Tracked tracked = entities.get(id);
            if (tracked == null || !tracked.viewers().contains(viewer)) continue;
            if (hasMount(tracked.entity())) continue;
            realPassengers.removeViewer(tracked.entity(), viewer);
            tracked.entity().removeViewer(viewer);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        restore(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        realPassengers.forgetViewer(id);
        realPassengers.forgetVehicle(event.getPlayer());
        for (int entityId : Set.copyOf(byViewer.getOrDefault(id, Set.of()))) {
            Tracked tracked = entities.get(entityId);
            if (tracked == null) continue;
            tracked.entity().handleViewerDisconnect(id);
            if (!tracked.persistent()) {
                tracked.viewers().remove(id);
                unindex(byViewer, id, entityId);
                if (tracked.viewers().isEmpty()) {
                    forget(tracked);
                    tracked.entity().remove();
                }
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        realPassengers.forgetVehicle(event.getPlayer());
        hide(event.getPlayer());
        restore(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        realPassengers.forgetVehicle(event.getPlayer());
        hide(event.getPlayer());
        restore(event.getPlayer());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        realPassengers.forgetVehicle(event.getEntity());
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        realPassengers.forgetVehicle(event.getEntity());
    }

    @EventHandler
    public void onUnload(EntitiesUnloadEvent event) {
        event.getEntities().forEach(realPassengers::forgetVehicle);
    }

    private void checkThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Use the packet entity API on the server thread");
        }
        if (closed) {
            throw new IllegalStateException("Packet entity manager is closed");
        }
    }

    @Override
    public void close() {
        checkThread();
        realPassengers.clear();
        for (Tracked tracked : entities.values()) {
            tracked.entity().remove();
        }
        entities.clear();
        byViewer.clear();
        byChunk.clear();
        byUuid.clear();
        pendingRestores.clear();
        closed = true;
        HandlerList.unregisterAll(this);
    }
}
