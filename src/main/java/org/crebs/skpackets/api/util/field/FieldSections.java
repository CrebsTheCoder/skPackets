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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.entity.EntityData;
import org.skriptlang.skript.bukkit.entity.EntityUtils;
import org.skriptlang.skript.lang.entry.EntryContainer;
import java.util.*;

public final class FieldSections {
    private FieldSections() {}

    public static org.bukkit.entity.@Nullable EntityType entityType(Expression<?> expression) {
        if (!(expression instanceof Literal<?> literal)) {
            Skript.error("The entity type needs to be a literal");
            return null;
        }
        return literal.getSingle() instanceof EntityData<?> data
                ? EntityUtils.toBukkitEntityType(data) : null;
    }

    @SuppressWarnings("unchecked")
    public static boolean parse(
            List<? extends FieldAccessor<?, ?>> fields, EntryContainer container,
            Map<String, Expression<?>> expressions, String description,
            boolean requireSingle, Set<String> ignored) {
        List<String> missing = new ArrayList<>();
        for (var field : fields) {
            if (ignored.contains(field.name())) continue;
            Expression<?> expression = container.getOptional(field.name(), Object.class, false);
            for (String alias : field.aliases()) {
                if (expression != null) break;
                expression = container.getOptional(alias, Object.class, false);
            }
            if (expression == null) {
                if (!field.isOptional()) missing.add(field.name());
                continue;
            }
            Class<?> expected = field.expectedType();
            Class<?> baseType = expected.isArray() ? expected.getComponentType() : expected;
            Expression<?> converted = expression.getConvertedExpression(baseType);
            if (converted == null) {
                Skript.error("The value for '" + field.name() + "' must be of type "
                        + expected.getSimpleName() + ".");
                return false;
            }
            if (requireSingle && !expected.isArray() && !converted.isSingle()) {
                Skript.error("The value for '" + field.name() + "' must be a single value.");
                return false;
            }
            expressions.put(field.name(), converted);
        }
        if (missing.isEmpty()) return true;
        Skript.error("Missing required entries for " + description + ": " + String.join(", ", missing));
        return false;
    }

    public static <K, T> @Nullable T create(
            @Nullable FieldSchema<K, T> schema, K type, Map<String, Expression<?>> expressions,
            Event event, Map<String, Object> defaults) {
        if (schema == null) return null;
        var constructor = schema.constructor();
        if (constructor == null) return null;
        Map<String, Object> values = new HashMap<>(defaults);
        for (var field : schema.accessors()) {
            Expression<?> expression = expressions.get(field.name());
            if (expression == null) continue;
            Object value;
            if (field.expectedType().isArray() || !expression.isSingle()) {
                Object[] array = expression.getArray(event);
                value = array.length == 0 ? null : array;
            } else {
                value = expression.getSingle(event);
            }
            if (value == null) {
                if (!field.isOptional()) return null;
            } else {
                values.put(field.name(), value);
            }
        }
        return constructor.apply(new ConstructionContext<>(type, schema.accessors(), values));
    }
}
