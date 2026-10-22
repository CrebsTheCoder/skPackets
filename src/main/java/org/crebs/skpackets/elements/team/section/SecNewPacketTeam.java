package org.crebs.skpackets.elements.team.section;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.shanebeee.skr.Registration;

import org.bukkit.event.Event;
import org.crebs.skpackets.elements.team.TeamField;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;

import java.util.*;

public class SecNewPacketTeam extends SectionExpression<PacketWrapper> {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        SecNewPacketTeam.class, PacketWrapper.class, "[a] new packet team")
                .name("New Packet Team")
                .description(
                        "Creates a team packet. Name is required. Sending creates or updates the"
                                + " team automatically for each viewer.")
                .examples(
                        """
                        command /red-squad:
                            trigger:
                                set {_team} to a new packet team:
                                    name: "red"
                                    color: "red"
                                send packet {_team} to player
                        """)
                .since("1.0.0")
                .register();
    }

    private final Map<TeamField, Expression<?>> fields = new EnumMap<>(TeamField.class);

    @Override
    public boolean init(
            Expression<?>[] expressions,
            int pattern,
            Kleenean delayed,
            SkriptParser.ParseResult result,
            SectionNode node,
            List<TriggerItem> items) {
        if (node == null) {
            Skript.error("A new packet team requires a section with a name entry.");
            return false;
        }
        var builder = EntryValidator.builder();
        for (TeamField field : TeamField.values()) {
            builder.addEntryData(
                    new ExpressionEntryData<>(
                            field.key(), null, field != TeamField.NAME, field.elementType()));
        }
        var entries = builder.build().validate(node);
        if (entries == null) {
            return false;
        }
        for (TeamField field : TeamField.values()) {
            if (!entries.hasEntry(field.key())) {
                continue;
            }
            Expression<?> expression = (Expression<?>) entries.getOptional(field.key(), false);
            if (expression == null || (field != TeamField.ENTRIES && !expression.isSingle())) {
                Skript.error(
                        field.key()
                                + " requires "
                                + (field == TeamField.ENTRIES ? "strings" : "one value"));
                return false;
            }
            fields.put(field, expression);
        }
        return fields.containsKey(TeamField.NAME);
    }

    @Override
    protected PacketWrapper[] get(Event event) {
        try {
            String name = (String) fields.get(TeamField.NAME).getSingle(event);
            if (name == null) {
                return new PacketWrapper[0];
            }
            var packet = TeamField.create(name);
            for (var entry : fields.entrySet()) {
                Object value =
                        entry.getKey() == TeamField.ENTRIES
                                ? Arrays.stream(entry.getValue().getArray(event))
                                        .map(String.class::cast)
                                        .toArray(String[]::new)
                                : entry.getValue().getSingle(event);
                if (value == null) {
                    return new PacketWrapper[0];
                }
                entry.getKey().set(packet, value);
            }
            org.crebs.skpackets.SkPackets.getInstance()
                    .getPacketTeams()
                    .track((org.crebs.skpackets.elements.team.PacketTeam) packet);
            return new PacketWrapper[] {packet};
        } catch (IllegalArgumentException ex) {
            Skript.error(ex.getMessage());
            return new PacketWrapper[0];
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends PacketWrapper> getReturnType() {
        return PacketWrapper.class;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "new packet team";
    }
}
