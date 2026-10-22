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
package org.crebs.skpackets.api.general.packet;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PacketTypeRegistry {

    private static final Map<String, PacketTypeCommon> CLIENT_BOUND_PACKETS =
            new ConcurrentHashMap<>();
    private static final Map<String, PacketTypeCommon> SERVER_BOUND_PACKETS =
            new ConcurrentHashMap<>();
    private static final List<PacketTypeCommon> ALL_PACKETS;

    static {
        BiConsumer<Map<String, PacketTypeCommon>, PacketTypeCommon[]> populateMap =
                (map, types) -> {
                    for (PacketTypeCommon type : types) {
                        map.put(type.getName().toUpperCase(Locale.ENGLISH), type);
                    }
                };

        populateMap.accept(CLIENT_BOUND_PACKETS, PacketType.Play.Server.values());
        populateMap.accept(CLIENT_BOUND_PACKETS, PacketType.Configuration.Server.values());
        populateMap.accept(CLIENT_BOUND_PACKETS, PacketType.Login.Server.values());
        populateMap.accept(CLIENT_BOUND_PACKETS, PacketType.Handshaking.Server.values());
        populateMap.accept(CLIENT_BOUND_PACKETS, PacketType.Status.Server.values());

        populateMap.accept(SERVER_BOUND_PACKETS, PacketType.Play.Client.values());
        populateMap.accept(SERVER_BOUND_PACKETS, PacketType.Configuration.Client.values());
        populateMap.accept(SERVER_BOUND_PACKETS, PacketType.Login.Client.values());
        populateMap.accept(SERVER_BOUND_PACKETS, PacketType.Handshaking.Client.values());
        populateMap.accept(SERVER_BOUND_PACKETS, PacketType.Status.Client.values());

        List<PacketTypeCommon> packets = new ArrayList<>();
        packets.addAll(CLIENT_BOUND_PACKETS.values());
        packets.addAll(SERVER_BOUND_PACKETS.values());
        ALL_PACKETS = List.copyOf(packets);
    }

    public static List<PacketTypeCommon> getAllPackets() {
        return ALL_PACKETS;
    }

    public static @Nullable PacketTypeCommon getPacket(String rawName, boolean isSend) {
        String key = rawName.replace(" ", "_").toUpperCase(Locale.ENGLISH);

        if (isSend) {
            return CLIENT_BOUND_PACKETS.get(key);
        } else {
            return SERVER_BOUND_PACKETS.get(key);
        }
    }

    public static @Nullable PacketTypeCommon getPacket(String rawName) {
        String key = rawName.replace(" ", "_").toUpperCase(Locale.ENGLISH);

        PacketTypeCommon type = CLIENT_BOUND_PACKETS.get(key);

        if (type == null) {
            type = SERVER_BOUND_PACKETS.get(key);
        }

        return type;
    }
}
