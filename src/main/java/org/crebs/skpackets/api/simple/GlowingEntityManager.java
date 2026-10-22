/*
 * MIT License
 *
 * Copyright (c) 2026 3add
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlowingEntityManager {

    private static final Map<Integer, Set<UUID>> glowingMap = new ConcurrentHashMap<>();

    public static boolean hasGlowingEntities() {
        return !glowingMap.isEmpty();
    }

    public static void clear() {
        glowingMap.clear();
    }

    private static @Nullable Entity getById(Integer id) {
        return Bukkit.getWorlds().stream()
                .map(World::getEntities)
                .flatMap(Collection::stream)
                .filter(entity -> entity.getEntityId() == id)
                .findAny()
                .orElse(null);
    }

    public static void setGlowingReceivers(int glowingEntityId, List<UUID> glowingReceivers) {
        Entity glowingEntity = getById(glowingEntityId);
        if (glowingEntity == null || glowingReceivers == null) {
            return;
        }

        Set<UUID> oldReceivers = glowingMap.get(glowingEntityId);
        if (oldReceivers != null) {
            Set<UUID> toRemove = new HashSet<>(oldReceivers);
            glowingReceivers.forEach(toRemove::remove);
            if (!toRemove.isEmpty()) {
                sendGlowPacket(glowingEntity, toRemove, false);
            }
        }

        Set<UUID> newReceivers = new HashSet<>(glowingReceivers);
        if (oldReceivers != null) {
            oldReceivers.forEach(newReceivers::remove);
        }

        if (glowingReceivers.isEmpty()) {
            glowingMap.remove(glowingEntityId);
        } else {
            Set<UUID> receivers = ConcurrentHashMap.newKeySet();
            receivers.addAll(glowingReceivers);
            glowingMap.put(glowingEntityId, receivers);
        }

        if (!newReceivers.isEmpty()) {
            sendGlowPacket(glowingEntity, newReceivers, true);
        }
    }

    public static void setGlowingReceivers(int glowingEntity, UUID glowingReceiver) {
        setGlowingReceivers(glowingEntity, Collections.singletonList(glowingReceiver));
    }

    public static void addGlowingReceivers(int glowingEntityId, Collection<UUID> playersToAdd) {
        Entity glowingEntity = getById(glowingEntityId);
        if (glowingEntity == null || playersToAdd == null || playersToAdd.isEmpty()) {
            return;
        }

        Set<UUID> newlyAddedReceivers = new HashSet<>();

        Set<UUID> receiversList =
                glowingMap.computeIfAbsent(glowingEntityId, k -> ConcurrentHashMap.newKeySet());

        for (UUID playerToAdd : playersToAdd) {
            if (receiversList.add(playerToAdd)) {
                newlyAddedReceivers.add(playerToAdd);
            }
        }

        sendGlowPacket(glowingEntity, newlyAddedReceivers, true);
    }

    public static void addGlowingReceiver(int glowingEntity, UUID playerToAdd) {
        addGlowingReceivers(glowingEntity, Collections.singletonList(playerToAdd));
    }

    public static void removeGlowingReceivers(
            int glowingEntityId, Collection<UUID> playersToRemove) {
        Entity glowingEntity = getById(glowingEntityId);
        if (glowingEntity == null || playersToRemove == null || playersToRemove.isEmpty()) {
            return;
        }

        Set<UUID> receivers = glowingMap.get(glowingEntityId);

        if (receivers != null) {
            Set<UUID> removedReceivers = new HashSet<>();

            for (UUID playerToRemove : playersToRemove) {
                if (receivers.remove(playerToRemove)) {
                    removedReceivers.add(playerToRemove);
                }
            }

            sendGlowPacket(glowingEntity, removedReceivers, false);

            if (receivers.isEmpty()) {
                glowingMap.remove(glowingEntityId);
            }
        }
    }

    public static void removeGlowingReceiver(int glowingEntity, UUID playerToRemove) {
        removeGlowingReceivers(glowingEntity, Collections.singletonList(playerToRemove));
    }

    public static void clearGlowingReceivers(int glowingEntityId) {
        Entity glowingEntity = getById(glowingEntityId);
        Set<UUID> oldReceivers = glowingMap.remove(glowingEntityId);

        if (glowingEntity != null && oldReceivers != null) {
            sendGlowPacket(glowingEntity, oldReceivers, false);
        }
    }

    public static boolean isGlowingFor(int entityId, UUID receiverId) {
        if (receiverId == null) {
            return false;
        }
        Set<UUID> receiverIds = glowingMap.get(entityId);
        if (receiverIds == null) {
            return false;
        }

        return receiverIds.contains(receiverId);
    }

    public static List<UUID> getGlowingReceivers(int glowingEntity) {
        Set<UUID> receivers = glowingMap.get(glowingEntity);
        return receivers == null ? List.of() : List.copyOf(receivers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void sendGlowPacket(
            Entity glowingEntity, Collection<UUID> receivers, boolean shouldGlow) {
        if (glowingEntity == null || receivers == null || receivers.isEmpty()) {
            return;
        }

        EntityMeta meta =
                EntityMeta.createMeta(
                        glowingEntity.getEntityId(),
                        SpigotConversionUtil.fromBukkitEntityType(glowingEntity.getType()));
        meta.readPacket(
                new WrapperPlayServerEntityMetadata(
                        glowingEntity.getEntityId(),
                        SpigotConversionUtil.getEntityMetadata(glowingEntity)));
        meta.entity()
                .setFlag(
                        me.tofaa.entitylib.meta.MetaFields.Entity.GLOWING,
                        shouldGlow || glowingEntity.isGlowing());
        WrapperPlayServerEntityMetadata packet = meta.createPacket();

        for (UUID receiverUUID : receivers) {
            Player receiver = Bukkit.getPlayer(receiverUUID);
            if (receiver != null && receiver.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().getUser(receiver).sendPacket(packet);
            }
        }
    }
}
