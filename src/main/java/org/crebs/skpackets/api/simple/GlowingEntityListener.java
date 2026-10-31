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

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.ArrayList;
import java.util.UUID;

public class GlowingEntityListener implements PacketListener, Listener {

    @Override
    public void onPacketSend(@org.jetbrains.annotations.NotNull PacketSendEvent event) {
        if (!GlowingEntityManager.hasGlowingEntities()
                || !event.getPacketType().equals(PacketType.Play.Server.ENTITY_METADATA)) {
            return;
        }

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        UUID playerUUID = event.getUser().getUUID();
        int entityId = packet.getEntityId();

        if (!GlowingEntityManager.isGlowingFor(entityId, playerUUID)) {
            return;
        }

        var metadata = packet.getEntityMetadata();
        for (int i = 0; i < metadata.size(); i++) {
            var data = metadata.get(i);
            if (data.getIndex() != 0 || !(data.getValue() instanceof Byte flags)) {
                continue;
            }
            if ((flags & 0x40) == 0) {
                var updated = new ArrayList<EntityData<?>>(metadata);
                updated.set(i, new EntityData<>(0, EntityDataTypes.BYTE, (byte) (flags | 0x40)));
                packet.setEntityMetadata(updated);
                event.markForReEncode(true);
            }
            return;
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        GlowingEntityManager.forgetViewer(event.getUser().getUUID());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        GlowingEntityManager.forgetEntity(event.getEntity().getEntityId());
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        GlowingEntityManager.forgetEntity(event.getEntity().getEntityId());
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        event.getEntities().forEach(entity -> GlowingEntityManager.forgetEntity(entity.getEntityId()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GlowingEntityManager.forgetEntity(event.getPlayer().getEntityId());
        GlowingEntityManager.forgetViewer(event.getPlayer().getUniqueId());
    }
}
