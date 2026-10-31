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
package org.crebs.skpackets.elements.general.expressions.prop;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.shanebeee.skr.Registration;
import com.google.common.primitives.Primitives;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.general.packet.PacketSendOrReceiveEvent;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.crebs.skpackets.api.util.field.FieldValues;
import org.crebs.skpackets.elements.general.event.EvtPacketSendOrReceive;
import org.crebs.skpackets.elements.general.event.EvtPacketSendOrReceive.PacketSendOrReceiveParserData;
import org.crebs.skpackets.elements.general.field.PacketFieldRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("rawtypes")
public class ExprPacketField extends PropertyExpression<PacketWrapper, Object> {

    public static void register(Registration reg) {
        String description = "Gets or sets a field's value from a packet by its name.\n\n"
                + FieldSchema.describe(PacketFieldRegistry.INSTANCE.getAllSchemas(),
                type -> type.getName().toLowerCase(Locale.ENGLISH).replace("_", " "));

        reg.newPropertyExpression(
                        ExprPacketField.class,
                        Object.class,
                        "[(fake|packet)] packet [field] <[a-zA-Z0-9_ ]+>",
                        "packet")
                .name("General - Packet Field")
                .description(description)
                .examples(
                        """
                        on clientbound entity metadata:
                            set {_meta} to packet meta of event-packet
                            set meta glowing state of {_meta} to true

                            set packet meta of event-packet to {_meta}
                        """)
                .since("1.0.0")
                .register();
    }

    private String fieldName;
    private List<FieldAccessor<PacketWrapper<?>, ?>> accessors = List.of();
    private Class<?> returnType = Object.class;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        fieldName = parseResult.regexes.getFirst().group().trim();
        accessors = PacketFieldRegistry.INSTANCE.getAllSchemas().stream()
                .<FieldAccessor<PacketWrapper<?>, ?>>map(schema -> schema.getAccessor(fieldName))
                .filter(Objects::nonNull)
                .toList();
        if (accessors.isEmpty()) {
            Skript.error("The packet field '" + fieldName + "' is not registered or does not exist. Consider checking your spelling.");
            return false;
        }
        returnType = valueType(accessors.getFirst());
        for (var accessor : accessors) {
            Class<?> candidate = valueType(accessor);
            while (!returnType.isAssignableFrom(candidate)) {
                returnType = returnType.isInterface() ? Object.class : returnType.getSuperclass();
            }
        }
        setExpr((Expression<? extends PacketWrapper>) exprs[0]);
        return true;
    }

    private static Class<?> valueType(FieldAccessor<?, ?> accessor) {
        Class<?> expected = accessor.expectedType();
        Class<?> type = expected.isArray() ? expected.getComponentType() : expected;
        return Primitives.wrap(type);
    }

    @SuppressWarnings("UnstableApiUsage")
    private @Nullable FieldAccessor<PacketWrapper<?>, ?> resolveAccessor(PacketWrapper<?> wrapper) {
        PacketTypeCommon type = wrapper.getPacketTypeData().getPacketType();
        if (type == null) {
            return null;
        }
        FieldSchema<PacketTypeCommon, PacketWrapper<?>> schema = PacketFieldRegistry.INSTANCE.getSchema(type);
        return schema == null ? null : schema.getAccessor(fieldName);
    }

    @Override
    protected Object[] get(Event event, PacketWrapper[] source) {
        return FieldValues.read(source, this::resolveAccessor, getReturnType());
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (accessors.stream().noneMatch(accessor -> accessor.setter() != null)) {
            Skript.error("Cannot set " + fieldName + " because it is a read-only field.");
            return null;
        }
        if (mode != Changer.ChangeMode.SET) {
            return null;
        }
        PacketSendOrReceiveParserData data = getParser().getData(PacketSendOrReceiveParserData.class);
        if (getParser().isCurrentEvent(PacketSendOrReceiveEvent.class)) {
            if (data.getProcessType() != EvtPacketSendOrReceive.ProcessType.NETTY) {
                Skript.error("You can't alter packets in a "
                        + (data.getProcessType() == null ? "unknown" : data.getProcessType().toString().toLowerCase(Locale.ENGLISH))
                        + " processed event, the packets have already been processed at that point. Use a netty processed event instead.");
                return null;
            }
            if (data.getPriority() == PacketListenerPriority.MONITOR) {
                Skript.error("You can't alter packets when using the \"monitor\" listening priority.");
                return null;
            }
        }
        return accessors.stream()
                .filter(accessor -> accessor.setter() != null)
                .map(ExprPacketField::valueType)
                .distinct()
                .toArray(Class<?>[]::new);
    }

    @Override
    public void change(Event event, Object[] delta, Changer.ChangeMode mode) {
        if (mode != Changer.ChangeMode.SET || delta == null || delta.length == 0) {
            return;
        }
        FieldValues.set(getExpr().getArray(event), this::resolveAccessor, delta, "packet");
    }

    @Override
    public boolean isSingle() {
        return getExpr().isSingle() && accessors.stream().noneMatch(accessor -> accessor.expectedType().isArray());
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String wrapper = getExpr() != null ? getExpr().toString(event, debug) : "packet";
        return "packet field " + fieldName + " of " + wrapper;
    }
}
