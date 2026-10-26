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

import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.api.util.field.FieldPropertyExpression;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.crebs.skpackets.elements.entity.field.entity.FakeEntityFieldRegistry;
import org.jetbrains.annotations.Nullable;

public abstract class ExprFakeEntityField extends FieldPropertyExpression<WrapperEntity> {
    protected ExprFakeEntityField(String fieldName) {
        super(fieldName);
    }

    protected static boolean isAvailable(String name) {
        return FakeEntityFieldRegistry.INSTANCE.getAllSchemas().stream()
                .anyMatch(schema -> schema.getAccessor(name) != null);
    }

    @Override
    protected Iterable<? extends FieldSchema<?, WrapperEntity>> schemas() {
        return FakeEntityFieldRegistry.INSTANCE.getAllSchemas();
    }

    @Override
    protected @Nullable FieldAccessor<WrapperEntity, ?> resolveAccessor(WrapperEntity entity) {
        var schema = FakeEntityFieldRegistry.INSTANCE.getSchema(entity.getEntityType());
        return schema == null ? null : schema.getAccessor(fieldName);
    }

    @Override
    protected String fieldKind() {
        return "fake entity";
    }
}
