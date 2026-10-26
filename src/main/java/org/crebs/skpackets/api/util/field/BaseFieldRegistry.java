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

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class BaseFieldRegistry<K, BaseType> {
    protected final Map<K, FieldSchema<K, BaseType>> REGISTRY = new HashMap<>();

    public abstract List<FieldRegistrar> getRegistrars();

    public void registerAll() {
        List<FieldRegistrar> registrars = getRegistrars();
        for (FieldRegistrar registrar : registrars) {
            registrar.register();
        }
    }

    public FieldSchema<K, BaseType> getSchema(K key) {
        return REGISTRY.get(key);
    }

    public Collection<FieldSchema<K, BaseType>> getAllSchemas() {
        return REGISTRY.values();
    }

    public <T extends BaseType> Builder<T> builder(K key, Class<T> clazz) {
        return new Builder<>(key);
    }

    public class Builder<T extends BaseType> {
        protected final K type;
        protected final List<FieldAccessor<BaseType, ?>> accessors = new ArrayList<>();
        protected @Nullable Function<ConstructionContext<K, T>, T> constructor;

        public Builder(K type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        public <V> Builder<T> requiredField(
                Class<V> expectedType,
                Function<T, V> getter,
                @Nullable BiConsumer<T, V> setter,
                String... names) {
            accessors.add(
                    new FieldAccessor<>(
                            names[0],
                            Arrays.copyOfRange(names, 1, names.length),
                            expectedType,
                            false,
                            (Function<BaseType, V>) getter,
                            (BiConsumer<BaseType, V>) setter));
            return this;
        }

        @SuppressWarnings("unchecked")
        public <V> Builder<T> optionalField(
                Class<V> expectedType,
                Function<T, V> getter,
                @Nullable BiConsumer<T, V> setter,
                String... names) {
            accessors.add(
                    new FieldAccessor<>(
                            names[0],
                            Arrays.copyOfRange(names, 1, names.length),
                            expectedType,
                            true,
                            (Function<BaseType, V>) getter,
                            (BiConsumer<BaseType, V>) setter));
            return this;
        }

        public Builder<T> constructor(
                @Nullable Function<ConstructionContext<K, T>, T> constructor) {
            this.constructor = constructor;
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public void build() {
            REGISTRY.put(type, new FieldSchema(type, accessors, constructor));
        }
    }
}
