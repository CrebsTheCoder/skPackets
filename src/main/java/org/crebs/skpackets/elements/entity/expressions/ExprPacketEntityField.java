package org.crebs.skpackets.elements.entity.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.event.Event;
import org.crebs.skpackets.api.metadata.PacketEntityField;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ExprPacketEntityField extends SimpleExpression<Object> {
    private static final List<PacketEntityField> PATTERN_FIELDS = new ArrayList<>();
    private Expression<WrapperEntity> entities;
    private PacketEntityField field;

    public static void register(Registration registration) {
        List<String> patterns = new ArrayList<>();
        PATTERN_FIELDS.clear();
        for (PacketEntityField field : PacketEntityField.values()) {
            for (String alias : field.aliases()) {
                patterns.add(
                        "[meta field] " + alias + " of [packet entit(y|ies)] %packetentities%");
                PATTERN_FIELDS.add(field);
            }
        }
        registration
                .newSimpleExpression(
                        ExprPacketEntityField.class, Object.class, patterns.toArray(String[]::new))
                .name("Packet Entity Metadata")
                .description(
                        "Gets, sets, deletes or resets packet entity metadata. Entity id is"
                                + " read-only. Fields are validated against the entity category;"
                                + " inherited fields are supported.")
                .examples(
                        "set meta field sneaking of packet entity {_mob} to false",
                        "set meta field display text of packet entity {_display} to \"Hello!\"",
                        "set {_hp} to meta field health of packet entity {_mob}")
                .since("1.0.0")
                .register();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(
            Expression<?>[] expressions,
            int pattern,
            Kleenean delayed,
            SkriptParser.ParseResult result) {
        entities = (Expression<WrapperEntity>) expressions[0];
        field = PATTERN_FIELDS.get(pattern);
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        List<Object> values = new ArrayList<>();
        for (WrapperEntity entity : entities.getArray(event)) {
            try {
                values.add(field.get(entity));
            } catch (IllegalArgumentException exception) {
                Skript.error(exception.getMessage());
            }
        }
        return values.toArray((Object[]) Array.newInstance(field.valueType(), values.size()));
    }

    @Override
    public Class<?>[] acceptChange(ChangeMode mode) {
        if (!field.isWritable()) {
            return null;
        }
        return switch (mode) {
            case SET, RESET, DELETE -> new Class<?>[] {field.valueType()};
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object[] delta, ChangeMode mode) {
        for (WrapperEntity entity : entities.getArray(event)) {
            try {
                if (mode == ChangeMode.DELETE || mode == ChangeMode.RESET) {
                    field.reset(entity);
                } else if (mode == ChangeMode.SET && delta != null && delta.length > 0) {
                    field.set(entity, delta[0]);
                }
            } catch (IllegalArgumentException exception) {
                Skript.error(exception.getMessage());
            }
        }
    }

    @Override
    public boolean isSingle() {
        return entities.isSingle();
    }

    @Override
    public Class<?> getReturnType() {
        return field.valueType();
    }

    @Override
    public String toString(Event event, boolean debug) {
        return field.aliases().getFirst() + " of " + entities.toString(event, debug);
    }
}
