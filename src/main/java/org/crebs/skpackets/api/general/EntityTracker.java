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
package org.crebs.skpackets.api.general;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTracker implements PacketListener {

    private static final Map<UUID, Map<Integer, EntityType>> PLAYER_ENTITIES =
            new ConcurrentHashMap<>();

    public static void clear() {
        PLAYER_ENTITIES.clear();
    }

    public static EntityType getType(UUID playerUuid, int entityId) {
        Map<Integer, EntityType> playerCache = PLAYER_ENTITIES.get(playerUuid);
        if (playerCache != null) {
            EntityType type = playerCache.get(entityId);
            if (type != null) {
                return type;
            }
        }

        return getType(entityId);
    }

    public static EntityType getType(int entityId) {
        Map<EntityType, Integer> frequencies = new HashMap<>();
        int maxCount = 0;
        EntityType mostFrequent = null;

        for (Map<Integer, EntityType> playerMap : PLAYER_ENTITIES.values()) {
            EntityType type = playerMap.get(entityId);
            if (type != null) {
                int count = frequencies.getOrDefault(type, 0) + 1;
                frequencies.put(type, count);

                if (count > maxCount) {
                    maxCount = count;
                    mostFrequent = type;
                }
            }
        }

        return mostFrequent != null ? mostFrequent : EntityTypes.ENTITY;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getUser().getUUID() == null) return;
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            handleSpawn(event);
        } else if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            handleDestroy(event);
        }
    }

    private void handleSpawn(PacketSendEvent event) {
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(event);
        int entityId = spawnPacket.getEntityId();
        EntityType type = spawnPacket.getEntityType();
        UUID userUuid = event.getUser().getUUID();

        PLAYER_ENTITIES
                .computeIfAbsent(userUuid, k -> new ConcurrentHashMap<>())
                .put(entityId, type);
    }

    private void handleDestroy(PacketSendEvent event) {
        WrapperPlayServerDestroyEntities destroyPacket =
                new WrapperPlayServerDestroyEntities(event);
        UUID userUuid = event.getUser().getUUID();
        Map<Integer, EntityType> playerCache = PLAYER_ENTITIES.get(userUuid);

        if (playerCache != null) {
            for (int id : destroyPacket.getEntityIds()) {
                playerCache.remove(id);
            }
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        UUID userUuid = event.getUser().getUUID();
        if (userUuid != null) PLAYER_ENTITIES.remove(userUuid);
    }
}
