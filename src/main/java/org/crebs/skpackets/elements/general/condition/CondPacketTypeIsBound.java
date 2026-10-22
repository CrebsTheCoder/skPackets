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
package org.crebs.skpackets.elements.general.condition;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;

import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.shanebeee.skr.Registration;

public class CondPacketTypeIsBound extends PropertyCondition<PacketTypeCommon> {

    public static void register(Registration reg) {
        reg.newPropertyCondition(
                        CondPacketTypeIsBound.class,
                        PropertyType.BE,
                        "(:(client|server))[ ]bound",
                        "packettypes")
                .name("General - Packet Type Bound")
                .description("See in which direction a packet (or packettype) is bound")
                .examples(
                        """
                        on any packet:
                            if packet type of event-packet is clientbound:
                                send packet type of event-packet to console
                        """)
                .since("1.0.0")
                .register();
    }

    private boolean isClientbound = false;

    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            SkriptParser.ParseResult parseResult) {

        if (!super.init(expressions, matchedPattern, isDelayed, parseResult)) {
            return false;
        }

        isClientbound = parseResult.hasTag("client");

        return true;
    }

    @Override
    public boolean check(PacketTypeCommon value) {

        if (isClientbound) {
            return value.getSide() == PacketSide.SERVER;
        } else {
            return value.getSide() == PacketSide.CLIENT;
        }
    }

    @Override
    protected String getPropertyName() {
        return isClientbound ? "clientbound" : "serverbound";
    }
}
