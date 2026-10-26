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
package org.crebs.skpackets.api.util.field;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.google.common.primitives.Primitives;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;


public abstract class FieldPropertyExpression<T> extends PropertyExpression<T, Object> {

    protected FieldPropertyExpression(String fieldName) {
        this.fieldName = fieldName;
    }

    protected abstract Iterable<? extends FieldSchema<?, T>> schemas();

    protected abstract @Nullable FieldAccessor<T, ?> resolveAccessor(T value);

    protected abstract String fieldKind();

    private FieldAccessor<T, ?> fieldAccessor;
    protected final String fieldName;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] exprs,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        String fieldName = this.fieldName;

        boolean isValidField = false;
        for (FieldSchema<?, T> def : schemas()) {
            FieldAccessor<T, ?> candidate = def.getAccessor(fieldName);
            if (candidate != null) {
                this.fieldAccessor = candidate;
                isValidField = true;
                break;
            }
        }

        if (!isValidField) {
            Skript.error(
                    "The " + fieldKind() + " field '"
                            + fieldName
                            + "' is not registered or does not exist. Consider checking your"
                            + " spelling.");
            return false;
        }

        setExpr((Expression<? extends T>) exprs[0]);
        return true;
    }

    @Nullable
    @Override
    protected Object[] get(Event event, T[] source) {
        if (this.fieldAccessor == null) {
            return null;
        }

        return FieldValues.read(source, this::resolveAccessor, getReturnType());
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

    @Override
    public void change(Event event, Object[] delta, Changer.ChangeMode mode) {
        if (mode != Changer.ChangeMode.SET || delta == null || delta.length == 0) {
            return;
        }

        FieldValues.set(getExpr().getArray(event), this::resolveAccessor, delta, fieldKind());
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
        String entity = getExpr() != null ? getExpr().toString(event, debug) : fieldKind();
        String name = this.fieldAccessor != null ? this.fieldAccessor.name() : this.fieldName;
        return fieldKind() + " field " + name + " of " + entity;
    }
}
