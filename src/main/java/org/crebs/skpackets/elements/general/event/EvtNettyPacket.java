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
package org.crebs.skpackets.elements.general.event;

import com.github.shanebeee.skr.Registration;

import org.crebs.skpackets.api.general.packet.PacketSendOrReceiveEvent;

public final class EvtNettyPacket extends EvtPacketSendOrReceive {
    public static void register(Registration registration) {
        registration
                .newEvent(
                        EvtNettyPacket.class,
                        PacketSendOrReceiveEvent.NettyPacketEvent.class,
                        "netty packet [with packet[events] priority"
                                + " (:(lowest|low|normal|high|highest|monitor))]")
                .name("On Netty Packet")
                .description(
                        "Listens to any incoming or outgoing packet on the packet's processing"
                            + " thread. Equivalent to 'on any packet netty processed'. Packets can"
                            + " be changed or cancelled except at monitor priority. Bukkit"
                            + " operations requiring the server thread must not be used here.")
                .examples(
                        """
                        on netty packet:
                            set {_type} to packet type of event-packet
                        """)
                .since("1.0.0")
                .register();
    }
}
