package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import org.bukkit.block.sign.Side;
import org.bukkit.util.Vector;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.general.EntityTracker;

import java.util.Map;

import static java.util.Map.entry;
import static org.crebs.skpackets.elements.general.field.PacketFieldRule.named;
import static org.crebs.skpackets.elements.general.field.PacketFieldRule.DEFAULT;

final class PacketFieldRules {
    private static final PacketFieldAdapter SIGN_SIDE = new PacketFieldAdapter(Side.class,
            (packet, value) -> Boolean.TRUE.equals(value) ? Side.FRONT : Side.BACK,
            value -> value == Side.FRONT);
    private static final PacketFieldAdapter ENTITY_META = new PacketFieldAdapter(EntityMeta.class,
            (packet, value) -> readMetadata((WrapperPlayServerEntityMetadata) packet),
            value -> ((EntityMeta) value).entityData());
    private static final PacketFieldAdapter STATUS_BYTE = new PacketFieldAdapter(Number.class,
            (packet, value) -> value, value -> ((Number) value).byteValue());

    private static final PacketFieldAdapter SNEAKING = new PacketFieldAdapter(Boolean.class,
            (packet, value) -> Boolean.TRUE.equals(value instanceof java.util.Optional<?> optional ? optional.orElse(null) : value),
            Boolean.TRUE::equals, true);

    private static final Map<Class<?>, Map<String, PacketFieldRule>> RULES = Map.ofEntries(
            entry(WrapperPlayServerEntityStatus.class, Map.of("status", DEFAULT.adapting(STATUS_BYTE))),
            entry(WrapperPlayServerGameTestHighlightPos.class, Map.of("relativepos", DEFAULT.defaulting(Vector::new))),
            entry(WrapperPlayServerEntityMetadata.class, Map.of("entitymetadata", DEFAULT.adapting(ENTITY_META))),
            entry(WrapperPlayServerOpenSignEditor.class, Map.of("isfronttext", named("sign side").adapting(SIGN_SIDE))),
            entry(WrapperPlayServerCloseWindow.class, Map.of("id", DEFAULT.defaulting(() -> 0))),
            entry(WrapperPlayClientUpdateSign.class, Map.of("isfronttext", named("sign side").adapting(SIGN_SIDE))),
            entry(WrapperPlayClientInteractEntity.class, Map.of("sneaking", DEFAULT.adapting(SNEAKING))),
            entry(WrapperPlayClientPlayerFlying.class, Map.of(
                    "positionchanged", DEFAULT.defaulting(() -> false),
                    "rotationchanged", DEFAULT.defaulting(() -> false),
                    "horizontalcollision", DEFAULT.defaulting(() -> false),
                    "location", DEFAULT.defaulting(() -> new org.bukkit.Location(null, 0, 0, 0))))
    );

    private PacketFieldRules() {}

    static PacketFieldRule find(Class<?> wrapper, String parameter) {
        return RULES.getOrDefault(wrapper, Map.of()).getOrDefault(PacketReflection.normalized(parameter), PacketFieldRule.DEFAULT);
    }

    private static EntityMeta readMetadata(WrapperPlayServerEntityMetadata packet) {
        EntityType type = EntityTracker.getType(packet.getEntityId());
        if (type == null) throw new IllegalStateException("Failed to find entity type of entity with id " + packet.getEntityId());
        EntityMeta meta = EntityMeta.createMeta(packet.getEntityId(), type);
        meta.readPacket(packet);
        return meta;
    }
}
