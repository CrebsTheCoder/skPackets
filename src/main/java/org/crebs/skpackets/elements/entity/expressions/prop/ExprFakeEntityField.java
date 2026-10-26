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
package org.crebs.skpackets.elements.entity.expressions.prop;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.google.common.primitives.Primitives;

import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.event.Event;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.crebs.skpackets.elements.entity.field.entity.FakeEntityFieldRegistry;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;

public abstract class ExprFakeEntityField extends PropertyExpression<WrapperEntity, Object> {

    protected ExprFakeEntityField(String fieldName) {
        this.fieldName = fieldName;
    }

    protected static boolean isAvailable(String name) {
        return FakeEntityFieldRegistry.INSTANCE.getAllSchemas().stream()
                .anyMatch(schema -> schema.getAccessor(name) != null);
    }

    private FieldAccessor<WrapperEntity, ?> fieldAccessor;
    private final String fieldName;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] exprs,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        String fieldName = this.fieldName;

        boolean isValidField = false;
        for (FieldSchema<EntityType, WrapperEntity> def :
                FakeEntityFieldRegistry.INSTANCE.getAllSchemas()) {
            FieldAccessor<WrapperEntity, ?> candidate = def.getAccessor(fieldName);
            if (candidate != null) {
                this.fieldAccessor = candidate;
                isValidField = true;
                break;
            }
        }

        if (!isValidField) {
            Skript.error(
                    "The fake entity field '"
                            + fieldName
                            + "' is not registered or does not exist. Consider checking your"
                            + " spelling.");
            return false;
        }

        setExpr((Expression<? extends WrapperEntity>) exprs[0]);
        return true;
    }

    private @Nullable FieldAccessor<WrapperEntity, ?> resolveAccessor(WrapperEntity entity) {
        EntityType type = entity.getEntityType();
        if (type == null) {
            return null;
        }
        FieldSchema<EntityType, WrapperEntity> schema =
                FakeEntityFieldRegistry.INSTANCE.getSchema(type);
        if (schema == null) {
            return null;
        }
        return schema.getAccessor(this.fieldName);
    }

    @Nullable
    @Override
    protected Object[] get(Event event, WrapperEntity[] source) {
        if (this.fieldAccessor == null) {
            return null;
        }

        List<Object> elements = new ArrayList<>();

        for (WrapperEntity entity : source) {
            if (entity == null) {
                continue;
            }

            FieldAccessor<WrapperEntity, ?> accessor = resolveAccessor(entity);
            if (accessor == null) {
                continue;
            }

            Object value = accessor.getter().apply(entity);
            if (value == null) {
                continue;
            }

            if (value.getClass().isArray()) {
                int len = Array.getLength(value);
                int i = 0;
                while (i < len) {
                    elements.add(Array.get(value, i++));
                }
            } else {
                elements.add(value);
            }
        }

        if (elements.isEmpty()) {
            return null;
        }

        Class<?> returnType = Primitives.wrap(getReturnType());
        return elements.toArray((Object[]) Array.newInstance(returnType, 0));
    }

    @Nullable
    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.fieldAccessor == null) {
            return null;
        }
        if (this.fieldAccessor.setter() == null) {
            Skript.error(
                    "Cannot set "
                            + this.fieldAccessor.name()
                            + " because it is a read-only field.");
            return null;
        }

        if (mode != Changer.ChangeMode.SET) {
            return null;
        }

        Class<?> expected = this.fieldAccessor.expectedType();
        Class<?> typeToAccept = expected.isArray() ? expected.getComponentType() : expected;

        return new Class<?>[] {Primitives.wrap(typeToAccept)};
    }

    @SuppressWarnings("unchecked")
    @Override
    public void change(Event event, Object[] delta, Changer.ChangeMode mode) {
        if (mode != Changer.ChangeMode.SET || delta == null || delta.length == 0) {
            return;
        }

        for (WrapperEntity entity : getExpr().getArray(event)) {
            if (entity == null) {
                continue;
            }

            FieldAccessor<WrapperEntity, ?> accessor = resolveAccessor(entity);
            if (accessor == null || accessor.setter() == null) {
                continue;
            }

            Object newValue;
            Class<?> expected = accessor.expectedType();

            if (expected.isArray()) {
                Class<?> componentType = expected.getComponentType();
                Object typedArray = Array.newInstance(componentType, delta.length);
                for (int i = 0; i < delta.length; i++) {
                    Array.set(typedArray, i, delta[i]);
                }
                newValue = typedArray;
            } else {
                if (delta.length == 1) {
                    newValue = delta[0];
                } else {
                    newValue = delta;
                }
            }

            boolean isCompatible = expected == Object.class || expected.isInstance(newValue);
            if (!isCompatible && expected.isArray() && newValue.getClass().isArray()) {
                isCompatible = true;
            }

            if (!isCompatible) {
                Skript.warning(
                        "Cannot set the fake entity field '"
                                + accessor.name()
                                + "' to a value of type "
                                + newValue.getClass().getSimpleName()
                                + ". Expected type: "
                                + expected.getSimpleName());
                continue;
            }

            try {
                ((BiConsumer<WrapperEntity, Object>) accessor.setter()).accept(entity, newValue);
            } catch (IllegalArgumentException exception) {
                Skript.error(exception.getMessage());
            }
        }
    }

    @Override
    public boolean isSingle() {
        if (this.fieldAccessor == null) {
            return true;
        }
        return getExpr().isSingle() && !this.fieldAccessor.expectedType().isArray();
    }

    @Override
    public Class<?> getReturnType() {
        if (this.fieldAccessor == null) {
            return Object.class;
        }
        Class<?> expected = this.fieldAccessor.expectedType();
        return expected.isArray() ? expected.getComponentType() : expected;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String entity = getExpr() != null ? getExpr().toString(event, debug) : "fake entity";
        String name = this.fieldAccessor != null ? this.fieldAccessor.name() : this.fieldName;
        return "fake entity field " + name + " of " + entity;
    }
}
