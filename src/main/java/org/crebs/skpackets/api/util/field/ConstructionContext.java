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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ConstructionContext<K, BaseType> {
    private final K key;
    private final List<FieldAccessor<BaseType, ?>> registeredAccessors;
    private final Map<String, Object> arguments;

    public ConstructionContext(
            K key,
            List<FieldAccessor<BaseType, ?>> registeredAccessors,
            Map<String, Object> arguments) {
        this.key = key;
        this.registeredAccessors = registeredAccessors;
        this.arguments = arguments;
    }

    public K getKey() {
        return key;
    }

    public <T> @NotNull T getRequired(String fieldName, Class<? extends T> clazz) {
        T field = getOptional(fieldName, clazz);
        if (field == null) {
            throw new IllegalStateException(
                    "Required field '" + fieldName + "' was not provided for type " + key);
        }
        return field;
    }

    public <T> @Nullable T getOptional(String fieldName, Class<? extends T> clazz) {
        FieldAccessor<BaseType, ?> targetAccessor = null;
        for (FieldAccessor<BaseType, ?> accessor : registeredAccessors) {
            if (accessor.matches(fieldName)) {
                targetAccessor = accessor;
                break;
            }
        }

        if (targetAccessor == null) {
            throw new IllegalArgumentException(
                    "Attempted to look up field '"
                            + fieldName
                            + "', but it is not defined in this schema.");
        }
        if (arguments.containsKey(targetAccessor.name())) {
            return clazz.cast(arguments.get(targetAccessor.name()));
        }
        for (String alias : targetAccessor.aliases()) {
            if (arguments.containsKey(alias)) {
                return clazz.cast(arguments.get(alias));
            }
        }
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (targetAccessor.matches(entry.getKey())) {
                return clazz.cast(entry.getValue());
            }
        }

        return null;
    }

    public <T> T getOrElse(String fieldName, Class<T> clazz, T defaultValue) {
        T val = getOptional(fieldName, clazz);
        return val != null ? val : defaultValue;
    }
}
