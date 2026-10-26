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
import org.crebs.skpackets.elements.general.event.EvtPacketSendOrReceive;
import org.crebs.skpackets.elements.general.event.EvtPacketSendOrReceive.PacketSendOrReceiveParserData;
import org.crebs.skpackets.elements.general.field.PacketFieldRegistry;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

@SuppressWarnings("rawtypes")
public class ExprPacketField extends PropertyExpression<PacketWrapper, Object> {

    public static void register(Registration reg) {
        StringBuilder description = new StringBuilder();
        description.append("Gets or sets a field's value from a packet by its name.\n\n");
        description.append(FieldSchema.describe(PacketFieldRegistry.INSTANCE.getAllSchemas(),
                type -> type.getName().toLowerCase(Locale.ENGLISH).replace("_", " ")));

        reg.newPropertyExpression(
                        ExprPacketField.class,
                        Object.class,
                        "[(fake|packet)] packet [field] <[a-zA-Z0-9_ ]+>",
                        "packet")
                .name("General - Packet Field")
                .description(description.toString())
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

    private FieldAccessor<PacketWrapper<?>, ?> fieldAccessor;
    private String fieldName;
    private boolean globalMode = false;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(
            Expression<?>[] exprs,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {
        String fieldName = parseResult.regexes.getFirst().group().trim();
        PacketSendOrReceiveParserData data =
                getParser().getData(PacketSendOrReceiveParserData.class);

        if (getParser().isCurrentEvent(PacketSendOrReceiveEvent.class)
                && data.getPacketType() != null) {
            PacketTypeCommon eventPacketType = data.getPacketType();
            if (eventPacketType == null) {
                return false;
            }

            FieldSchema<PacketTypeCommon, PacketWrapper<?>> schema =
                    PacketFieldRegistry.INSTANCE.getSchema(eventPacketType);
            if (schema == null) {
                Skript.error(
                        "No fields are currently registered for the "
                                + eventPacketType
                                        .getName()
                                        .toLowerCase(Locale.ENGLISH)
                                        .replace("_", " ")
                                + " packet.");
                return false;
            }

            this.fieldAccessor = schema.getAccessor(fieldName);
            if (this.fieldAccessor == null) {
                Skript.error(
                        "The field '"
                                + fieldName
                                + "' does not exist in a "
                                + eventPacketType
                                        .getName()
                                        .toLowerCase(Locale.ENGLISH)
                                        .replace("_", " ")
                                + " packet.");
                return false;
            }

            this.globalMode = false;

        } else {
            boolean isValidField = false;
            for (FieldSchema<PacketTypeCommon, PacketWrapper<?>> def :
                    PacketFieldRegistry.INSTANCE.getAllSchemas()) {
                FieldAccessor<PacketWrapper<?>, ?> candidate = def.getAccessor(fieldName);
                if (candidate != null) {
                    this.fieldAccessor = candidate;
                    isValidField = true;
                    break;
                }
            }

            if (!isValidField) {
                Skript.error(
                        "The packet field '"
                                + fieldName
                                + "' is not registered or does not exist. Consider checking your"
                                + " spelling.");
                return false;
            }

            this.fieldName = fieldName;
            this.globalMode = true;
        }

        setExpr((Expression<? extends PacketWrapper>) exprs[0]);
        return true;
    }

    @SuppressWarnings("UnstableApiUsage")
    private @Nullable FieldAccessor<PacketWrapper<?>, ?> resolveAccessor(PacketWrapper<?> wrapper) {
        if (!globalMode) {
            return this.fieldAccessor;
        }
        PacketTypeCommon type = wrapper.getPacketTypeData().getPacketType();
        if (type == null) {
            return null;
        }
        FieldSchema<PacketTypeCommon, PacketWrapper<?>> schema =
                PacketFieldRegistry.INSTANCE.getSchema(type);
        if (schema == null) {
            return null;
        }
        return schema.getAccessor(this.fieldName);
    }

    @Override
    protected Object[] get(Event event, PacketWrapper[] source) {
        List<Object> elements = new ArrayList<>();

        for (PacketWrapper<?> wrapper : source) {
            if (wrapper == null) {
                continue;
            }

            FieldAccessor<PacketWrapper<?>, ?> accessor = resolveAccessor(wrapper);
            if (accessor == null) {
                continue;
            }

            Object value = accessor.getter().apply(wrapper);
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

        Class<?> returnType = Primitives.wrap(getReturnType());
        return elements.toArray((Object[]) Array.newInstance(returnType, 0));
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.fieldAccessor == null || this.fieldAccessor.setter() == null) {
            String name =
                    globalMode
                            ? fieldName
                            : (this.fieldAccessor != null ? this.fieldAccessor.name() : "unknown");
            Skript.error("Cannot set " + name + " because it is a read-only field.");
            return null;
        }

        if (mode != Changer.ChangeMode.SET) {
            return null;
        }

        PacketSendOrReceiveParserData data =
                getParser().getData(PacketSendOrReceiveParserData.class);

        if (getParser().isCurrentEvent(PacketSendOrReceiveEvent.class)) {
            if (data.getProcessType() != EvtPacketSendOrReceive.ProcessType.NETTY) {
                Skript.error(
                        "You can't alter packets in a "
                                + (data.getProcessType() == null
                                        ? "unknown"
                                        : data.getProcessType()
                                                .toString()
                                                .toLowerCase(Locale.ENGLISH))
                                + " processed event, the packets have already been processed at"
                                + " that point. Use a netty processed event instead.");
                return null;
            } else if (data.getPriority() == PacketListenerPriority.MONITOR) {
                Skript.error(
                        "You can't alter packets when using the \"monitor\" listening priority.");
                return null;
            }
        }

        Class<?> expected = this.fieldAccessor.expectedType();
        Class<?> typeToAccept = expected.isArray() ? expected.getComponentType() : expected;

        return new Class<?>[] {Primitives.wrap(typeToAccept)};
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void change(Event event, Object[] delta, Changer.ChangeMode mode) {
        if (mode != Changer.ChangeMode.SET || delta == null || delta.length == 0) {
            return;
        }

        for (PacketWrapper<?> wrapper : getExpr().getArray(event)) {
            if (wrapper == null) {
                continue;
            }

            FieldAccessor<PacketWrapper<?>, ?> accessor = resolveAccessor(wrapper);
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
                newValue = delta.length == 1 ? delta[0] : delta;
            }

            boolean isCompatible = expected == Object.class || expected.isInstance(newValue);
            if (!isCompatible && expected.isArray() && newValue.getClass().isArray()) {
                isCompatible = true;
            }

            if (!isCompatible) {
                Skript.warning(
                        "Cannot set the packet field '"
                                + accessor.name()
                                + "' to a value of type "
                                + newValue.getClass().getSimpleName()
                                + ". Expected type: "
                                + expected.getSimpleName());
                continue;
            }

            try {
                ((BiConsumer<PacketWrapper<?>, Object>) accessor.setter())
                        .accept(wrapper, newValue);
            } catch (IllegalArgumentException ex) {
                Skript.error(ex.getMessage());
            }
        }
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
        String wrapper = getExpr() != null ? getExpr().toString(event, debug) : "packet";
        String name = globalMode ? fieldName : this.fieldAccessor.name();
        return "packet field " + name + " of " + wrapper;
    }
}
