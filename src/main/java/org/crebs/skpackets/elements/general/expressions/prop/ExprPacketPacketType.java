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

import ch.njol.skript.expressions.base.SimplePropertyExpression;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.shanebeee.skr.Registration;

import org.jspecify.annotations.Nullable;

public class ExprPacketPacketType
        extends SimplePropertyExpression<PacketWrapper<?>, PacketTypeCommon> {

    public static void register(Registration reg) {
        reg.newPropertyExpression(
                        ExprPacketPacketType.class,
                        PacketTypeCommon.class,
                        "packet[ ]type",
                        "packet")
                .name("General - Packet Type")
                .description("The packet type of a packet")
                .examples(
                        """
                        on any packet:
                            send packet type of event-packet to console
                        """)
                .since("1.0.0")
                .register();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public @Nullable PacketTypeCommon convert(PacketWrapper<?> packet) {
        return packet.getPacketTypeData().getPacketType();
    }

    @Override
    public Class<? extends PacketTypeCommon> getReturnType() {
        return PacketTypeCommon.class;
    }

    @Override
    protected String getPropertyName() {
        return "packet type";
    }
}
