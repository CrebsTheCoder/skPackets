package org.crebs.skpackets.api.general.packet;

import ch.njol.skript.lang.ParseContext;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.crebs.skpackets.elements.general.Types.PacketTypeParser;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PacketTypeRegistryTest {
    private final PacketTypeParser parser = new PacketTypeParser();

    @BeforeAll
    static void initializePacketEvents() {
        PacketEventsTestBootstrap.initialize();
    }

    @Test
    void unqualifiedNamesResolveEveryGameplayPacketInItsDirection() {
        for (PacketTypeCommon type : PacketType.Play.Server.values()) {
            assertSame(type, PacketTypeRegistry.getPacket(type.getName(), true), type.getName());
        }
        for (PacketTypeCommon type : PacketType.Play.Client.values()) {
            assertSame(type, PacketTypeRegistry.getPacket(type.getName(), false), type.getName());
        }
    }

    @Test
    void explicitStateQualifiersResolveEveryProtocolVariant() {
        assertState("play", PacketType.Play.Server.values());
        assertState("play", PacketType.Play.Client.values());
        assertState("configuration", PacketType.Configuration.Server.values());
        assertState("configuration", PacketType.Configuration.Client.values());
        assertState("login", PacketType.Login.Server.values());
        assertState("login", PacketType.Login.Client.values());
        assertState("handshaking", PacketType.Handshaking.Server.values());
        assertState("handshaking", PacketType.Handshaking.Client.values());
        assertState("status", PacketType.Status.Server.values());
        assertState("status", PacketType.Status.Client.values());
    }

    @Test
    void allPacketTypesKeepEveryDirectionAndProtocolStateExactlyOnce() {
        List<PacketTypeCommon> expected = new ArrayList<>();
        expected.addAll(Arrays.asList(PacketType.Play.Server.values()));
        expected.addAll(Arrays.asList(PacketType.Play.Client.values()));
        expected.addAll(Arrays.asList(PacketType.Configuration.Server.values()));
        expected.addAll(Arrays.asList(PacketType.Configuration.Client.values()));
        expected.addAll(Arrays.asList(PacketType.Login.Server.values()));
        expected.addAll(Arrays.asList(PacketType.Login.Client.values()));
        expected.addAll(Arrays.asList(PacketType.Handshaking.Server.values()));
        expected.addAll(Arrays.asList(PacketType.Handshaking.Client.values()));
        expected.addAll(Arrays.asList(PacketType.Status.Server.values()));
        expected.addAll(Arrays.asList(PacketType.Status.Client.values()));
        assertEquals(expected.size(), PacketTypeRegistry.getAllPackets().size());
        assertEquals(new HashSet<>(expected), new HashSet<>(PacketTypeRegistry.getAllPackets()));
    }

    @Test
    void displayNamesRoundTripEveryPacketTypeWithoutChangingGameplayNames() {
        assertEquals("clientbound keep alive", parser.toString(PacketType.Play.Server.KEEP_ALIVE, 0));
        assertEquals("serverbound keep alive", parser.toString(PacketType.Play.Client.KEEP_ALIVE, 0));
        assertEquals("clientbound configuration keep alive", parser.toString(PacketType.Configuration.Server.KEEP_ALIVE, 0));
        for (PacketTypeCommon type : PacketTypeRegistry.getAllPackets()) {
            String text = parser.toString(type, 0);
            assertSame(type, parser.parse(text, ParseContext.DEFAULT), text);
            assertSame(type, parser.parse(text + " packet", ParseContext.DEFAULT), text);
        }
    }

    @Test
    void variableKeysDistinguishEveryDirectionAndProtocolState() {
        var keys = new HashSet<String>();
        for (PacketTypeCommon type : PacketTypeRegistry.getAllPackets()) {
            String key = parser.toVariableNameString(type);
            assertTrue(keys.add(key), "Duplicate packet type variable key: " + key);
        }
        assertEquals("packettype:clientbound:play:keep alive", parser.toVariableNameString(PacketType.Play.Server.KEEP_ALIVE));
        assertEquals("packettype:serverbound:play:keep alive", parser.toVariableNameString(PacketType.Play.Client.KEEP_ALIVE));
        assertEquals("packettype:clientbound:configuration:keep alive", parser.toVariableNameString(PacketType.Configuration.Server.KEEP_ALIVE));
    }

    @Test
    void existingUniqueNamesAndNormalizedInputRemainSupported() {
        assertSame(PacketType.Login.Server.LOGIN_SUCCESS, PacketTypeRegistry.getPacket("login success", true));
        assertSame(PacketType.Play.Server.KEEP_ALIVE, parser.parse("  CLIENTBOUND   KEEP ALIVE PACKET  ", ParseContext.DEFAULT));
        assertSame(PacketType.Configuration.Client.KEEP_ALIVE, parser.parse("serverbound configuration keep alive packet", ParseContext.DEFAULT));
        assertSame(PacketType.Play.Server.KEEP_ALIVE, PacketTypeRegistry.getPacket("keep alive"));
        assertNull(parser.parse("keep alive", ParseContext.DEFAULT));
        assertNull(parser.parse("clientbound unknown keep alive", ParseContext.DEFAULT));
        assertNull(PacketTypeRegistry.getPacket("configuration not a real packet", true));
    }

    private void assertState(String state, PacketTypeCommon[] types) {
        for (PacketTypeCommon type : types) {
            String name = state + " " + type.getName().replace('_', ' ');
            assertSame(type, PacketTypeRegistry.getPacket(name, type.getSide() == PacketSide.SERVER), name);
        }
    }
}
