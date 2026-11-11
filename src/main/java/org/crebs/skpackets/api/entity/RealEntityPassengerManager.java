package org.crebs.skpackets.api.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

final class RealEntityPassengerManager {
    private record Key(UUID viewer, UUID vehicle) {}
    private record Mount(Entity vehicle, Map<Integer, WrapperEntity> passengers) {}

    private final Map<Key, Mount> mounts = new LinkedHashMap<>();
    private final Map<Integer, Set<Key>> byPassenger = new HashMap<>();

    boolean isMounted(WrapperEntity passenger) {
        return byPassenger.containsKey(passenger.getEntityId());
    }

    private boolean detach(Key key, Mount mount, WrapperEntity passenger) {
        if (!mount.passengers().remove(passenger.getEntityId(), passenger)) return false;
        unindex(passenger.getEntityId(), key);
        return true;
    }

    private void unindex(int passenger, Key key) {
        Set<Key> keys = byPassenger.get(passenger);
        if (keys != null && keys.remove(key) && keys.isEmpty()) byPassenger.remove(passenger);
    }

    void update(Entity vehicle, Collection<WrapperEntity> passengers, boolean mount) {
        Map<Key, Mount> changed = new LinkedHashMap<>();
        for (WrapperEntity passenger : passengers) {
            for (UUID viewer : passenger.getViewers()) {
                Key key = new Key(viewer, vehicle.getUniqueId());
                if (mount) {
                    for (Key oldKey : Set.copyOf(byPassenger.getOrDefault(passenger.getEntityId(), Set.of()))) {
                        Mount oldMount = mounts.get(oldKey);
                        if (oldKey.viewer().equals(viewer) && !oldKey.equals(key)
                                && detach(oldKey, oldMount, passenger)) {
                            changed.put(oldKey, oldMount);
                        }
                    }
                }
                Mount current = mounts.computeIfAbsent(key, ignored -> new Mount(vehicle, new LinkedHashMap<>()));
                if (mount) {
                    current.passengers().put(passenger.getEntityId(), passenger);
                    byPassenger.computeIfAbsent(passenger.getEntityId(), ignored -> new HashSet<>()).add(key);
                } else detach(key, current, passenger);
                changed.put(key, current);
            }
        }
        refresh(changed);
    }

    void remove(WrapperEntity passenger) {
        Map<Key, Mount> changed = new LinkedHashMap<>();
        for (Key key : Set.copyOf(byPassenger.getOrDefault(passenger.getEntityId(), Set.of()))) {
            Mount mount = mounts.get(key);
            if (detach(key, mount, passenger)) changed.put(key, mount);
        }
        refresh(changed);
    }

    void removeViewer(WrapperEntity passenger, UUID viewer) {
        Map<Key, Mount> changed = new LinkedHashMap<>();
        for (Key key : Set.copyOf(byPassenger.getOrDefault(passenger.getEntityId(), Set.of()))) {
            Mount mount = mounts.get(key);
            if (key.viewer().equals(viewer) && detach(key, mount, passenger)) {
                changed.put(key, mount);
            }
        }
        refresh(changed);
    }

    void forgetViewer(UUID viewer) {
        forget(key -> key.viewer().equals(viewer));
    }

    void forgetVehicle(Entity vehicle) {
        forget(key -> key.vehicle().equals(vehicle.getUniqueId()));
    }

    private void forget(java.util.function.Predicate<Key> predicate) {
        mounts.entrySet().removeIf(entry -> {
            if (!predicate.test(entry.getKey())) return false;
            for (int passenger : entry.getValue().passengers().keySet()) unindex(passenger, entry.getKey());
            return true;
        });
    }

    void clear() {
        Map<Key, Mount> changed = new LinkedHashMap<>(mounts);
        changed.values().forEach(mount -> mount.passengers().clear());
        byPassenger.clear();
        refresh(changed);
    }

    private void refresh(Map<Key, Mount> changed) {
        changed.forEach((key, mount) -> {
            mount.passengers().values().removeIf(passenger -> {
                if (passenger.isSpawned() && passenger.getViewers().contains(key.viewer())) return false;
                unindex(passenger.getEntityId(), key);
                return true;
            });
            Player viewer = Bukkit.getPlayer(key.viewer());
            if (viewer != null && viewer.isOnline() && viewer.getWorld().equals(mount.vehicle().getWorld())) {
                LinkedHashSet<Integer> ids = new LinkedHashSet<>();
                mount.vehicle().getPassengers().forEach(passenger -> ids.add(passenger.getEntityId()));
                ids.addAll(mount.passengers().keySet());
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                        new WrapperPlayServerSetPassengers(mount.vehicle().getEntityId(), ids.stream().mapToInt(Integer::intValue).toArray()));
            }
            if (mount.passengers().isEmpty()) mounts.remove(key);
        });
    }
}
