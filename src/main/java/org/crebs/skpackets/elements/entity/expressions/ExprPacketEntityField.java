package org.crebs.skpackets.elements.entity.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.event.Event;
import org.crebs.skpackets.api.entity.metadata.PacketEntityField;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public abstract class ExprPacketEntityField extends SimpleExpression<Object> {
    private Expression<WrapperEntity> entities;
    private final PacketEntityField field;

    protected ExprPacketEntityField(PacketEntityField field) {
        this.field = field;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(
            Expression<?>[] expressions,
            int pattern,
            Kleenean delayed,
            SkriptParser.ParseResult result) {
        entities = (Expression<WrapperEntity>) expressions[0];
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        List<Object> values = new ArrayList<>();
        for (WrapperEntity entity : entities.getArray(event)) {
            try {
                Object value = field.get(entity);
                values.add(
                        value instanceof org.bukkit.inventory.ItemStack item
                                ? new ch.njol.skript.aliases.ItemType(item)
                                : value);
            } catch (IllegalArgumentException exception) {
                Skript.error(exception.getMessage());
            }
        }
        return values.toArray((Object[]) Array.newInstance(getReturnType(), values.size()));
    }

    @Override
    public Class<?>[] acceptChange(ChangeMode mode) {
        if (!field.isWritable()) {
            return null;
        }
        return switch (mode) {
            case SET, RESET, DELETE -> new Class<?>[] {getReturnType()};
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
        return field.valueType() == org.bukkit.inventory.ItemStack.class
                ? ch.njol.skript.aliases.ItemType.class
                : field.valueType();
    }

    @Override
    public String toString(Event event, boolean debug) {
        return field.aliases().getFirst() + " of " + entities.toString(event, debug);
    }
}
