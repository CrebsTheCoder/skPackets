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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record FieldSchema<K, O>(
        K type,
        List<FieldAccessor<O, ?>> accessors,
        @Nullable Function<ConstructionContext<K, O>, O> constructor) {
    public FieldAccessor<O, ?> getAccessor(String name) {
        for (FieldAccessor<O, ?> accessor : accessors) {
            if (accessor.matches(name)) {
                return accessor;
            }
        }
        return null;
    }

    public static <K, O> String describe(
            Iterable<FieldSchema<K, O>> schemas, java.util.function.Function<K, String> name) {
        StringBuilder description = new StringBuilder("### Available Packets and their fields\n");
        for (var schema : schemas) {
            String fields = schema.getReadableFields();
            if (!fields.isEmpty()) {
                description.append("* **").append(name.apply(schema.type()))
                        .append("** fields:\n").append(fields).append("\n");
            }
        }
        return description.toString();
    }

    public String getReadableFields() {
        List<String> fieldStrings = new ArrayList<>();
        for (FieldAccessor<O, ?> accessor : accessors) {
            if (accessor.aliases().length > 0) {
                fieldStrings.add(
                        accessor.name() + " (" + String.join(", ", accessor.aliases()) + ")");
            } else {
                fieldStrings.add(accessor.name());
            }
        }

        if (fieldStrings.isEmpty()) {
            return "";
        }

        String joined = String.join("`\n  - `", fieldStrings);
        return "  - `" + joined + "`";
    }
}
