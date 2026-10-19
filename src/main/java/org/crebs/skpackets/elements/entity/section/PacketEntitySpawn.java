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
import org.crebs.skpackets.api.metadata.PacketEntityField;
import org.skriptlang.skript.bukkit.entity.EntityData;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;

import java.util.*;

final class PacketEntitySpawn {
    private static final EntryValidator ENTRIES =
            EntryValidator.builder()
                    .addEntryData(
                            new ExpressionEntryData<>("entity", null, false, EntityData.class))
                    .addEntryData(new ExpressionEntryData<>("viewers", null, false, Player.class))
                    .addEntryData(
                            new ExpressionEntryData<>("location", null, false, Location.class))
                    .addEntryData(
                            new ExpressionEntryData<>("persistent", null, true, Boolean.class))
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
    private Expression<Boolean> persistent;

    @SuppressWarnings("unchecked")
    boolean init(SectionNode node) {
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

        if (entries.hasEntry("persistent")) {
            persistent = (Expression<Boolean>) entries.getOptional("persistent", false);
            if (persistent == null || !persistent.isSingle()) {
                return error("persistent must be one boolean");
            }
        }
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
        Boolean keep = persistent == null ? Boolean.TRUE : persistent.getSingle(event);
        if (keep == null) {
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
                            keep,
                            entity -> values.forEach((field, value) -> field.set(entity, value)));
        } catch (IllegalArgumentException exception) {
            Skript.error(exception.getMessage());
            return null;
        }
    }
}
