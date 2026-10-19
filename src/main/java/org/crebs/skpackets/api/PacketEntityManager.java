package org.crebs.skpackets.api;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
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
import java.util.function.Consumer;

public final class PacketEntityManager implements Listener, AutoCloseable {
    private record Tracked(
            WrapperEntity entity, UUID world, boolean persistent, Set<UUID> viewers) {}

    private final Map<Integer, Tracked> entities = new HashMap<>();
    private final JavaPlugin plugin;
    private boolean closed;

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
        if (type == EntityType.PLAYER || !type.isSpawnable()) {
            throw new IllegalArgumentException("Unsupported packet entity type: " + type);
        }
        var packetType = SpigotConversionUtil.fromBukkitEntityType(type);
        WrapperEntity entity =
                type.isAlive()
                        ? new WrapperLivingEntity(packetType)
                        : new WrapperEntity(packetType);
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
        Tracked tracked = new Tracked(entity, location.getWorld().getUID(), persistent, ids);
        configure.accept(entity);
        entities.put(entity.getEntityId(), tracked);
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
        tracked.viewers().add(player.getUniqueId());
        show(tracked, player);
    }

    public void removeViewer(WrapperEntity entity, UUID viewer) {
        checkThread();
        Tracked tracked = requireTracked(entity);
        tracked.viewers().remove(viewer);
        entity.removeViewer(viewer);
    }

    public void teleport(WrapperEntity entity, Location location) {
        checkThread();
        Tracked tracked = requireTracked(entity);
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location must have a world");
        }
        location.checkFinite();
        UUID world = location.getWorld().getUID();
        boolean changedWorld = !tracked.world().equals(world);
        if (changedWorld) {
            for (UUID viewer : Set.copyOf(entity.getViewers())) {
                entity.removeViewer(viewer);
            }
        }
        entity.setWorld(location.getWorld().getName());
        entity.teleport(SpigotConversionUtil.fromBukkitLocation(location));
        Tracked updated = new Tracked(entity, world, tracked.persistent(), tracked.viewers());
        entities.put(entity.getEntityId(), updated);
        if (changedWorld) {
            for (UUID id : updated.viewers()) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    show(updated, player);
                }
            }
        }
    }

    public void remove(WrapperEntity entity) {
        checkThread();
        entities.remove(entity.getEntityId());
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
                && tracked.world().equals(player.getWorld().getUID())
                && tracked.entity().isSpawned()) {
            tracked.entity().addViewer(player.getUniqueId());
        }
    }

    private void restore(Player player) {
        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            if (closed || !player.isOnline()) {
                                return;
                            }
                            entities.values().removeIf(tracked -> !tracked.entity().isSpawned());
                            for (Tracked tracked : entities.values()) {
                                show(tracked, player);
                            }
                        });
    }

    private void hide(Player player) {
        for (Tracked tracked : entities.values()) {
            tracked.entity().removeViewer(player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        restore(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        for (Tracked tracked : List.copyOf(entities.values())) {
            tracked.entity().handleViewerDisconnect(id);
            if (!tracked.persistent() && tracked.viewers().remove(id)) {
                if (tracked.viewers().isEmpty()) {
                    remove(tracked.entity());
                }
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        hide(event.getPlayer());
        restore(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        hide(event.getPlayer());
        restore(event.getPlayer());
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
        for (Tracked tracked : entities.values()) {
            tracked.entity().remove();
        }
        entities.clear();
        closed = true;
        HandlerList.unregisterAll(this);
    }
}
