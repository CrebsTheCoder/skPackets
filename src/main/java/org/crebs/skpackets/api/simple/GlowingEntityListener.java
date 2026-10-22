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
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import org.crebs.skpackets.api.entity.metadata.EntityMeta;

import java.util.UUID;

public class GlowingEntityListener implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!GlowingEntityManager.hasGlowingEntities()
                || !event.getPacketType().equals(PacketType.Play.Server.ENTITY_METADATA)) {
            return;
        }

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        UUID playerUUID = event.getUser().getUUID();
        int entityId = packet.getEntityId();

        if (GlowingEntityManager.isGlowingFor(entityId, playerUUID)) {
            EntityMeta meta =
                    EntityMeta.createMeta(
                            packet.getEntityId(),
                            com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
                                    .ENTITY);
            meta.readPacket(packet);
            meta.entity().setFlag(me.tofaa.entitylib.meta.MetaFields.Entity.GLOWING, true);

            packet.setEntityMetadata(meta.entityData());
            event.markForReEncode(true);
        }
    }
}
