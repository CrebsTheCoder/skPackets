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
        List<Object> elements = new ArrayList<>();

        for (T entity : source) {
            if (entity == null) {
                continue;
            }

            FieldAccessor<T, ?> accessor = resolver.apply(entity);
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

        Class<?> wrappedType = Primitives.wrap(returnType);
        return elements.toArray((Object[]) Array.newInstance(wrappedType, 0));
    }

    @SuppressWarnings("unchecked")
    public static <T> void set(T[] source,
            Function<T, @Nullable FieldAccessor<T, ?>> resolver, Object[] delta, String kind) {
        for (T entity : source) {
            if (entity == null) {
                continue;
            }

            FieldAccessor<T, ?> accessor = resolver.apply(entity);
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
                        "Cannot set the " + kind + " field '"
                                + accessor.name()
                                + "' to a value of type "
                                + newValue.getClass().getSimpleName()
                                + ". Expected type: "
                                + expected.getSimpleName());
                continue;
            }

            try {
                ((BiConsumer<T, Object>) accessor.setter()).accept(entity, newValue);
            } catch (IllegalArgumentException exception) {
                Skript.error(exception.getMessage());
            }
        }
    }
}
