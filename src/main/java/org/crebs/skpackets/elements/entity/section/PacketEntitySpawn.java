package org.crebs.skpackets.elements.entity.section;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.skriptlang.skript.bukkit.entity.EntityData;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

final class PacketEntitySpawn {
    private static final EntryValidator ENTRIES =
            EntryValidator.builder()
                    .addEntryData(
                            new ExpressionEntryData<EntityData>(
                                    "entity", null, false, EntityData.class) {
                                @Override
                                protected Expression<? extends EntityData> getValue(String value) {
                                    EntityData<?> literal = EntityData.parse(value);
                                    return literal != null
                                            ? new ch.njol.skript.lang.util.SimpleLiteral<>(
                                                    literal, false)
                                            : super.getValue(value);
                                }
                            })
                    .addEntryData(new ExpressionEntryData<>("viewers", null, false, Player.class))
                    .addEntryData(
                            new ExpressionEntryData<>("location", null, false, Location.class))
                    .addEntryData(
                            new ExpressionEntryData<>(
                                    "skin", null, true, org.crebs.skpackets.api.entity.Skin.class))
                    .addEntryData(new ExpressionEntryData<>("username", null, true, String.class))
                    .addSection("meta field", true)
                    .build();
    private static final EntryValidator META_ENTRIES = metadataEntries();
    private final Map<PacketEntityField, Expression<?>> metadata = new LinkedHashMap<>();

    private static EntryValidator metadataEntries() {
        var builder = EntryValidator.builder();
        for (PacketEntityField field : PacketEntityField.values()) {
            if (field.isWritable()) {
                for (String alias : field.aliases()) {
                    builder.addEntryData(
                            new ExpressionEntryData<>(alias, null, true, field.valueType()));
                }
            }
        }
        return builder.build();
    }

    private Expression<Location> location;
    private Expression<Player> viewers;
    private Expression<EntityData<?>> type;
    private boolean persistent;
    private Expression<org.crebs.skpackets.api.entity.Skin> skin;
    private Expression<String> username;

    @SuppressWarnings("unchecked")
    boolean init(SectionNode node, boolean persistent) {
        this.persistent = persistent;
        if (node == null) {
            return error(
                    "A new packet entity requires a section with entity, viewers and location"
                            + " entries.");
        }
        var entries = ENTRIES.validate(node);
        if (entries == null) {
            return false;
        }
        type = (Expression<EntityData<?>>) entries.getOptional("entity", false);
        viewers = (Expression<Player>) entries.getOptional("viewers", false);
        location = (Expression<Location>) entries.getOptional("location", false);
        if (type == null || !type.isSingle()) {
            return error("entity must be one entity type");
        }
        if (viewers == null) {
            return error("viewers must be a player or players");
        }
        if (location == null || !location.isSingle()) {
            return error("location must be one location");
        }

        skin = (Expression<org.crebs.skpackets.api.entity.Skin>) entries.getOptional("skin", false);
        username = (Expression<String>) entries.getOptional("username", false);
        if ((skin != null && !skin.isSingle()) || (username != null && !username.isSingle()))
            return error("skin and username require one value");
        SectionNode metaNode = (SectionNode) entries.getOptional("meta field", false);
        if (metaNode == null) {
            return true;
        }
        var metaEntries = META_ENTRIES.validate(metaNode);
        if (metaEntries == null) {
            return false;
        }
        for (PacketEntityField field : PacketEntityField.values()) {
            if (!field.isWritable()) {
                continue;
            }
            for (String alias : field.aliases()) {
                if (!metaEntries.hasEntry(alias)) {
                    continue;
                }
                if (metadata.containsKey(field)) {
                    return error("Duplicate field: " + field.aliases().getFirst());
                }
                Expression<?> expression = (Expression<?>) metaEntries.getOptional(alias, false);
                if (expression == null || !expression.isSingle()) {
                    return error(alias + " requires one value");
                }
                metadata.put(field, expression);
            }
        }
        return true;
    }

    private boolean error(String message) {
        Skript.error(message);
        return false;
    }

    WrapperEntity spawn(Event event) {
        Location at = location.getSingle(event);
        Player[] players = viewers.getArray(event);
        if (at == null || at.getWorld() == null || players.length == 0) {
            return null;
        }
        EntityData<?> data = type.getSingle(event);
        if (data == null) {
            Skript.error("The packet entity's entity entry evaluated to no entity type.");
            return null;
        }
        EntityType entityType =
                Arrays.stream(EntityType.values())
                        .filter(candidate -> candidate.getEntityClass() == data.getType())
                        .findFirst()
                        .orElse(null);
        if (entityType == null) {
            Skript.error(
                    "This packet entity requires a concrete entity type, such as zombie or pig.");
            return null;
        }
        var texture = skin == null ? null : skin.getSingle(event);
        String name = username == null ? "NPC" : username.getSingle(event);
        if (entityType == EntityType.PLAYER
                && (texture == null || name == null || !name.matches("[A-Za-z0-9_]{1,16}"))) {
            Skript.error(
                    "Player entities require a skin and a username of 1 to 16 letters, numbers or"
                            + " underscores.");
            return null;
        }
        if (entityType != EntityType.PLAYER && (skin != null || username != null)) {
            Skript.error("skin and username are only available for player entities.");
            return null;
        }
        try {
            Map<PacketEntityField, Object> values = new LinkedHashMap<>();
            for (var entry : metadata.entrySet()) {
                Object value = entry.getValue().getSingle(event);
                if (value == null) {
                    Skript.error("Missing value for " + entry.getKey().aliases().getFirst());
                    return null;
                }
                values.put(entry.getKey(), value);
            }
            return SkPackets.getInstance()
                    .getPacketEntities()
                    .spawn(
                            entityType,
                            at,
                            Arrays.asList(players),
                            persistent,
                            entity -> {
                                if (entity
                                        instanceof
                                        me.tofaa.entitylib.wrapper.WrapperPlayer player
                                        && name != null && texture != null) {
                                    player.setProfile(
                                            new com.github.retrooper.packetevents.protocol.player
                                                    .UserProfile(player.getUuid(), name));
                                    player.setTextureProperties(texture.properties());
                                }
                                values.forEach((field, value) -> field.set(entity, value));
                            });
        } catch (IllegalArgumentException exception) {
            Skript.error(exception.getMessage());
            return null;
        }
    }
}
