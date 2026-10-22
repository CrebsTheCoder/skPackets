package org.crebs.skpackets.elements.team.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;

import org.bukkit.event.Event;
import org.crebs.skpackets.elements.team.TeamField;

import java.lang.reflect.Array;
import java.util.*;

public abstract class TeamPropertyExpression extends SimpleExpression<Object> {
    protected abstract TeamField field();

    private Expression<PacketWrapper<?>> packets;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(
            Expression<?>[] expressions,
            int pattern,
            Kleenean delayed,
            SkriptParser.ParseResult result) {
        packets = (Expression<PacketWrapper<?>>) expressions[0];
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        List<Object> values = new ArrayList<>();
        for (PacketWrapper<?> packet : packets.getArray(event)) {
            if (!(packet instanceof WrapperPlayServerTeams team)) {
                continue;
            }
            Object value = field().get(team);
            if (value instanceof Object[] array) {
                Collections.addAll(values, array);
            } else if (value != null) {
                values.add(value);
            }
        }
        return values.toArray((Object[]) Array.newInstance(getReturnType(), values.size()));
    }

    @Override
    public Class<?>[] acceptChange(ChangeMode mode) {
        if (mode == ChangeMode.SET) {
            return new Class<?>[] {field().type()};
        }
        if (field() == TeamField.ENTRIES
                && (mode == ChangeMode.ADD
                        || mode == ChangeMode.REMOVE
                        || mode == ChangeMode.DELETE
                        || mode == ChangeMode.RESET)) {
            return new Class<?>[] {String[].class};
        }
        return null;
    }

    @Override
    public void change(Event event, Object[] delta, ChangeMode mode) {
        for (PacketWrapper<?> packet : packets.getArray(event)) {
            if (!(packet instanceof WrapperPlayServerTeams team)) {
                continue;
            }
            try {
                if (field() == TeamField.ENTRIES) {
                    Set<String> entries = new LinkedHashSet<>(team.getPlayers());
                    List<String> change =
                            delta == null
                                    ? List.of()
                                    : Arrays.stream(delta).map(String.class::cast).toList();
                    switch (mode) {
                        case SET -> {
                            entries.clear();
                            entries.addAll(change);
                        }
                        case ADD -> entries.addAll(change);
                        case REMOVE -> entries.removeAll(change);
                        case DELETE, RESET -> entries.clear();
                        default -> {
                            return;
                        }
                    }
                    field().set(team, entries.toArray(String[]::new));
                } else if (mode == ChangeMode.SET && delta != null && delta.length > 0) {
                    field().set(team, delta[0]);
                }
            } catch (IllegalArgumentException ex) {
                Skript.error(ex.getMessage());
            }
        }
    }

    @Override
    public boolean isSingle() {
        return field() != TeamField.ENTRIES && packets.isSingle();
    }

    @Override
    public Class<?> getReturnType() {
        return field().elementType();
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "team " + field().key() + " of " + packets.toString(event, debug);
    }
}
