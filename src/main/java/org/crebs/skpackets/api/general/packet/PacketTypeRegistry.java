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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PacketTypeRegistry {

    private static final Map<String, PacketTypeCommon> CLIENT_BOUND_PACKETS = new HashMap<>();
    private static final Map<String, PacketTypeCommon> SERVER_BOUND_PACKETS = new HashMap<>();
    private static final Map<PacketTypeCommon, String> PROTOCOL_STATES = new HashMap<>();
    private static final List<String> STATES = List.of("play", "configuration", "login", "handshaking", "status");
    private static final List<PacketTypeCommon> ALL_PACKETS;

    static {
        List<PacketTypeCommon> packets = new ArrayList<>();
        register(CLIENT_BOUND_PACKETS, "play", PacketType.Play.Server.values(), packets);
        register(CLIENT_BOUND_PACKETS, "configuration", PacketType.Configuration.Server.values(), packets);
        register(CLIENT_BOUND_PACKETS, "login", PacketType.Login.Server.values(), packets);
        register(CLIENT_BOUND_PACKETS, "handshaking", PacketType.Handshaking.Server.values(), packets);
        register(CLIENT_BOUND_PACKETS, "status", PacketType.Status.Server.values(), packets);
        register(SERVER_BOUND_PACKETS, "play", PacketType.Play.Client.values(), packets);
        register(SERVER_BOUND_PACKETS, "configuration", PacketType.Configuration.Client.values(), packets);
        register(SERVER_BOUND_PACKETS, "login", PacketType.Login.Client.values(), packets);
        register(SERVER_BOUND_PACKETS, "handshaking", PacketType.Handshaking.Client.values(), packets);
        register(SERVER_BOUND_PACKETS, "status", PacketType.Status.Client.values(), packets);
        ALL_PACKETS = List.copyOf(packets);
    }

    private static void register(Map<String, PacketTypeCommon> registry, String state, PacketTypeCommon[] types, List<PacketTypeCommon> packets) {
        for (PacketTypeCommon type : types) {
            String name = normalize(type.getName());
            registry.putIfAbsent(name, type);
            registry.put(state + ":" + name, type);
            PROTOCOL_STATES.put(type, state);
            packets.add(type);
        }
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "_");
    }

    private static @Nullable PacketTypeCommon lookup(Map<String, PacketTypeCommon> registry, String rawName) {
        String name = normalize(rawName);
        PacketTypeCommon type = registry.get(name);
        if (type != null) {
            return type;
        }
        for (String state : STATES) {
            String prefix = state + "_";
            if (name.startsWith(prefix)) {
                return registry.get(state + ":" + name.substring(prefix.length()));
            }
        }
        return null;
    }

    public static List<PacketTypeCommon> getAllPackets() {
        return ALL_PACKETS;
    }

    public static @Nullable PacketTypeCommon getPacket(String rawName, boolean isSend) {
        return lookup(isSend ? CLIENT_BOUND_PACKETS : SERVER_BOUND_PACKETS, rawName);
    }

    public static @Nullable PacketTypeCommon getPacket(String rawName) {
        PacketTypeCommon type = getPacket(rawName, true);
        return type != null ? type : getPacket(rawName, false);
    }

    public static String getProtocolState(PacketTypeCommon type) {
        String state = PROTOCOL_STATES.get(type);
        if (state == null) {
            throw new IllegalArgumentException("Unknown packet type: " + type);
        }
        return state;
    }
}
