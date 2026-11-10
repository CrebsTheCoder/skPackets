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
import com.google.common.primitives.Primitives;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class FieldValues {
    private FieldValues() {}

    public static <T> Object @Nullable [] read(T[] source,
            Function<T, @Nullable FieldAccessor<T, ?>> resolver, Class<?> returnType) {
        return read(source, resolver, returnType, true);
    }

    public static <T> Object @Nullable [] read(T[] source,
            Function<T, @Nullable FieldAccessor<T, ?>> resolver, Class<?> returnType,
            boolean flattenArrays) {
        List<Object> elements = new ArrayList<>();

        for (T entity : source) {
            if (entity == null) {
                continue;
            }

            FieldAccessor<T, ?> accessor = resolver.apply(entity);
            if (accessor == null || accessor.getter() == null) {
                continue;
            }

            Object value = accessor.getter().apply(entity);
            if (value == null) {
                continue;
            }

            if (flattenArrays && value.getClass().isArray()) {
                int len = Array.getLength(value);
                int i = 0;
                while (i < len) {
                    elements.add(Array.get(value, i++));
                }
            } else {
                if (value.getClass().isArray() && accessor.expectedType().isArray() && !accessor.expectedType().isInstance(value)) {
                    Object typed = Array.newInstance(accessor.expectedType().getComponentType(), Array.getLength(value));
                    for (int i = 0; i < Array.getLength(value); i++) Array.set(typed, i, Array.get(value, i));
                    value = typed;
                }
                elements.add(value);
            }
        }

        if (elements.isEmpty()) {
            return null;
        }

        Class<?> wrappedType = Primitives.wrap(returnType);
        return elements.toArray((Object[]) Array.newInstance(wrappedType, 0));
    }

    public static <T> void set(T[] source,
            Function<T, @Nullable FieldAccessor<T, ?>> resolver, Object[] delta, String kind) {
        set(source, resolver, delta, kind, Skript::error);
    }

    @SuppressWarnings("unchecked")
    public static <T> void set(T[] source,
            Function<T, @Nullable FieldAccessor<T, ?>> resolver, Object[] delta, String kind,
            java.util.function.Consumer<String> reportError) {
        for (T entity : source) {
            if (entity == null) {
                continue;
            }

            FieldAccessor<T, ?> accessor = resolver.apply(entity);
            if (accessor == null || accessor.setter() == null) {
                continue;
            }

            try {
                Object newValue;
                Class<?> expected = accessor.expectedType();

                if (expected.isArray()) {
                    Class<?> componentType = expected.getComponentType();
                    Object input = delta.length == 1 && delta[0] != null && delta[0].getClass().isArray()
                            ? delta[0] : delta;
                    int length = Array.getLength(input);
                    Object typedArray = Array.newInstance(componentType, length);
                    for (int i = 0; i < length; i++) {
                        Array.set(typedArray, i, Array.get(input, i));
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

                if (!isCompatible) {
                    reportError.accept(
                            "Cannot set the " + kind + " field '"
                                    + accessor.name()
                                    + "' to a value of type "
                                    + newValue.getClass().getSimpleName()
                                    + ". Expected type: "
                                    + expected.getSimpleName());
                    continue;
                }

                ((BiConsumer<T, Object>) accessor.setter()).accept(entity, newValue);
            } catch (IllegalArgumentException exception) {
                reportError.accept(exception.getMessage());
            }
        }
    }
}
