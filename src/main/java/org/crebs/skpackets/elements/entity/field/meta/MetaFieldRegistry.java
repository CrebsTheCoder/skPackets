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
package org.crebs.skpackets.elements.entity.field.meta;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.crebs.skpackets.api.util.field.BaseFieldRegistry;
import org.crebs.skpackets.api.util.field.FieldRegistrar;

import java.util.List;

public final class MetaFieldRegistry extends BaseFieldRegistry<EntityType, EntityMeta> {
    public static final MetaFieldRegistry INSTANCE = new MetaFieldRegistry();

    @Override
    public List<FieldRegistrar> getRegistrars() {
        return List.of();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void registerAll() {
        for (EntityType type : EntityTypes.values()) {
            var builder = builder(type, EntityMeta.class);
            for (PacketEntityField field : PacketEntityField.values()) {
                if (!field.supports(type)) continue;
                builder.optionalField(
                        (Class) field.metaValueType(),
                        (EntityMeta meta) -> field.getMeta(meta.entity()),
                        field.isWritable()
                                ? (EntityMeta meta, Object value) -> field.set(meta.entity(), value)
                                : null,
                        field.aliases().toArray(String[]::new));
            }
            builder.constructor(
                            context -> {
                                EntityMeta meta = EntityMeta.createMeta(0, type);
                                for (PacketEntityField field : PacketEntityField.values()) {
                                    if (!field.supports(type)) continue;
                                    Object value =
                                            context.getOptional(
                                                    field.aliases().getFirst(), Object.class);
                                    if (value != null && field.isWritable())
                                        field.set(meta.entity(), value);
                                }
                                return meta;
                            })
                    .build();
        }
    }
}
