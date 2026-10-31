package org.crebs.skpackets.api.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

final class RealEntityPassengerManager {
    private record Key(UUID viewer, UUID vehicle) {}
    private record Mount(Entity vehicle, Map<Integer, WrapperEntity> passengers) {}

    private final Map<Key, Mount> mounts = new LinkedHashMap<>();

    void update(Entity vehicle, Collection<WrapperEntity> passengers, boolean mount) {
        Map<Key, Mount> changed = new LinkedHashMap<>();
        for (WrapperEntity passenger : passengers) {
            for (UUID viewer : passenger.getViewers()) {
                Key key = new Key(viewer, vehicle.getUniqueId());
                if (mount) {
                    mounts.forEach((oldKey, oldMount) -> {
                        if (oldKey.viewer().equals(viewer) && !oldKey.equals(key)
                                && oldMount.passengers().remove(passenger.getEntityId(), passenger)) {
                            changed.put(oldKey, oldMount);
                        }
                    });
                }
                Mount current = mounts.computeIfAbsent(key, ignored -> new Mount(vehicle, new LinkedHashMap<>()));
                if (mount) current.passengers().put(passenger.getEntityId(), passenger);
                else current.passengers().remove(passenger.getEntityId(), passenger);
                changed.put(key, current);
            }
        }
        refresh(changed);
    }

    void remove(WrapperEntity passenger) {
        Map<Key, Mount> changed = new LinkedHashMap<>();
        mounts.forEach((key, mount) -> {
            if (mount.passengers().remove(passenger.getEntityId(), passenger)) changed.put(key, mount);
        });
        refresh(changed);
    }

    void removeViewer(WrapperEntity passenger, UUID viewer) {
        Map<Key, Mount> changed = new LinkedHashMap<>();
        mounts.forEach((key, mount) -> {
            if (key.viewer().equals(viewer) && mount.passengers().remove(passenger.getEntityId(), passenger)) {
                changed.put(key, mount);
            }
        });
        refresh(changed);
    }

    void forgetViewer(UUID viewer) {
        mounts.keySet().removeIf(key -> key.viewer().equals(viewer));
    }

    void forgetVehicle(Entity vehicle) {
        mounts.keySet().removeIf(key -> key.vehicle().equals(vehicle.getUniqueId()));
    }

    void clear() {
        Map<Key, Mount> changed = new LinkedHashMap<>(mounts);
        changed.values().forEach(mount -> mount.passengers().clear());
        refresh(changed);
    }

    private void refresh(Map<Key, Mount> changed) {
        changed.forEach((key, mount) -> {
            mount.passengers().values().removeIf(passenger -> !passenger.isSpawned() || !passenger.getViewers().contains(key.viewer()));
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
