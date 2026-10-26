package org.crebs.skpackets.api.entity.metadata;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import me.tofaa.entitylib.meta.Metadata;
import me.tofaa.entitylib.wrapper.WrapperEntity;

import java.util.*;

public final class EntityMeta {
    private final WrapperEntity entity;
    private final Map<Integer, EntityData<?>> original = new LinkedHashMap<>();
    private final int protocolVersion;

    private EntityMeta(WrapperEntity entity) {
        this.entity = entity;
        protocolVersion =
                PacketEvents.getAPI()
                        .getServerManager()
                        .getVersion()
                        .toClientVersion()
                        .getProtocolVersion();
    }

    public static EntityMeta of(WrapperEntity entity) {
        return new EntityMeta(entity);
    }

    public static EntityMeta createMeta(int id, EntityType type) {
        return new EntityMeta(new WrapperEntity(id, UUID.randomUUID(), type));
    }

    public WrapperEntity entity() {
        return entity;
    }

    public EntityType getEntityType() {
        return entity.getEntityType();
    }

    public Metadata getMetadata() {
        return entity.getMetadata();
    }

    public void readPacket(WrapperPlayServerEntityMetadata packet) {
        original.clear();
        packet.getEntityMetadata().forEach(data -> original.put(data.getIndex(), data));
        getMetadata().setMetaFromPacket(packet, getEntityType(), protocolVersion);
    }

    public List<EntityData<?>> entityData() {
        Map<Integer, EntityData<?>> merged = new LinkedHashMap<>(original);
        getMetadata().encode(protocolVersion).forEach(data -> merged.put(data.getIndex(), data));
        return new ArrayList<>(merged.values());
    }

    public WrapperPlayServerEntityMetadata createPacket() {
        return new WrapperPlayServerEntityMetadata(entity.getEntityId(), entityData());
    }

    public void applyTo(WrapperEntity target) {
        target.setMetaFromPacket(createPacket(), protocolVersion);
    }
}
