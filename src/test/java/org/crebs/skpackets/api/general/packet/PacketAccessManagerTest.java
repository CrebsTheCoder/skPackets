package org.crebs.skpackets.api.general.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerKeepAlive;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientKeepAlive;
import io.netty.buffer.Unpooled;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketAccessManagerTest {
    @BeforeAll static void initialize() { PacketEventsTestBootstrap.initialize(); }
    @AfterEach void clearFactories() { PacketAccessManager.clearEventFactories(); }

    @Test void bindsPrimitiveAndNullableFieldsOnce() throws Exception {
        Probe probe = new Probe(1);
        var getter = Probe.class.getMethod("getValue");
        var setter = Probe.class.getMethod("setValue", int.class);
        var read = PacketAccessManager.reader(getter);
        var write = PacketAccessManager.writer(setter);
        assertSame(read, PacketAccessManager.reader(getter));
        assertSame(write, PacketAccessManager.writer(setter));
        write.accept(probe, 42);
        assertEquals(42, read.apply(probe));
        var nullable = PacketAccessManager.writer(Probe.class.getMethod("setText", String.class));
        nullable.accept(probe, null);
        assertNull(PacketAccessManager.reader(Probe.class.getMethod("getText")).apply(probe));
    }

    @Test void preservesArraysAndVarargsAsSingleArguments() throws Exception {
        String[] values = {"a", "b"};
        var constructor = Probe.class.getConstructor(String[].class);
        var factory = PacketAccessManager.constructor(constructor);
        assertSame(factory, PacketAccessManager.constructor(constructor));
        Probe probe = (Probe) factory.apply(new Object[] {values});
        assertSame(values, probe.names);
        String[] replacement = {"c"};
        PacketAccessManager.writer(Probe.class.getMethod("setNames", String[].class)).accept(probe, replacement);
        assertSame(replacement, PacketAccessManager.reader(Probe.class.getMethod("getNames")).apply(probe));
    }

    @Test void retainsUnderlyingFailures() throws Exception {
        var read = PacketAccessManager.reader(Probe.class.getMethod("getFailure"));
        assertInstanceOf(IOException.class, assertThrows(IllegalArgumentException.class,
                () -> read.apply(new Probe(0))).getCause());
        var factory = PacketAccessManager.constructor(Probe.class.getConstructor(int.class));
        var failure = assertThrows(IllegalArgumentException.class, () -> factory.apply(new Object[] {-1}));
        assertEquals("negative", failure.getCause().getMessage());
        var fatal = PacketAccessManager.reader(Probe.class.getMethod("getFatal"));
        assertThrows(AssertionError.class, () -> fatal.apply(new Probe(0)));
    }

    @Test void wrapsBothEventDirectionsWithAutomaticFallback() {
        PacketTypeCommon server = type(PacketSide.SERVER);
        PacketTypeCommon client = type(PacketSide.CLIENT);
        assertTrue(((EventWrapper) PacketAccessManager.wrap(server, mock(PacketSendEvent.class))).sent);
        assertFalse(((EventWrapper) PacketAccessManager.wrap(client, mock(PacketReceiveEvent.class))).sent);
    }

    @Test void directFactoryOverridesFallbackAndIsClearedOnClose() {
        PacketTypeCommon type = type(PacketSide.SERVER);
        var event = mock(PacketSendEvent.class);
        PacketWrapper<?> fallback = PacketAccessManager.wrap(type, event);
        PacketAccessManager.registerEventFactory(type, ignored -> fallback);
        assertSame(fallback, PacketAccessManager.wrap(type, event));
        PacketAccessManager.clearEventFactories();
        assertNotSame(fallback, PacketAccessManager.wrap(type, event));
    }

    @Test void unsupportedPacketHasNoWrapper() {
        assertNull(PacketAccessManager.wrap(mock(PacketTypeCommon.class), mock(PacketSendEvent.class)));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void decodesRealPacketsThroughDirectAndDiscoveredFactories(boolean server) {
        for (boolean configuration : new boolean[] {false, true}) {
            PacketTypeCommon type = configuration
                    ? server ? PacketType.Configuration.Server.KEEP_ALIVE : PacketType.Configuration.Client.KEEP_ALIVE
                    : server ? PacketType.Play.Server.KEEP_ALIVE : PacketType.Play.Client.KEEP_ALIVE;
            ProtocolPacketEvent event = server ? mock(PacketSendEvent.class) : mock(PacketReceiveEvent.class);
            var buffer = Unpooled.buffer().writeLong(Long.MAX_VALUE);
            try {
                User user = mock(User.class);
                when(user.getClientVersion()).thenReturn(ServerVersion.V_26_3.toClientVersion());
                when(event.getUser()).thenReturn(user);
                when(event.getServerVersion()).thenReturn(ServerVersion.V_26_3);
                when(event.getByteBuf()).thenReturn(buffer);
                when(event.getPacketType()).thenReturn(type);
                var packet = PacketAccessManager.wrap(type, event);
                long id;
                if (packet instanceof WrapperPlayServerKeepAlive keepAlive) id = keepAlive.getId();
                else if (packet instanceof WrapperPlayClientKeepAlive keepAlive) id = keepAlive.getId();
                else if (packet instanceof WrapperConfigServerKeepAlive keepAlive) id = keepAlive.getId();
                else id = ((WrapperConfigClientKeepAlive) packet).getId();
                assertEquals(Long.MAX_VALUE, id);
                assertEquals(0, buffer.readableBytes());
                assertEquals(type, packet.getPacketTypeData().getPacketType());
                verify(event).setLastUsedWrapper(packet);
            } finally { buffer.release(); }
        }
    }

    private static PacketTypeCommon type(PacketSide side) {
        PacketTypeCommon type = mock(PacketTypeCommon.class);
        doReturn(EventWrapper.class).when(type).getWrapperClass();
        when(type.getSide()).thenReturn(side);
        return type;
    }

    public static class EventWrapper extends PacketWrapper<EventWrapper> {
        final boolean sent;
        public EventWrapper(PacketSendEvent event) { super(PacketType.Play.Server.KEEP_ALIVE); sent = true; }
        public EventWrapper(PacketReceiveEvent event) { super(PacketType.Play.Client.KEEP_ALIVE); sent = false; }
    }

    public static class Probe {
        private int value;
        private String text = "initial";
        private String[] names;
        public Probe(int value) {
            if (value < 0) throw new IllegalArgumentException("negative");
            this.value = value;
        }
        public Probe(String... names) { this.names = names; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public String getText() { return text; }
        public void setText(String value) { text = value; }
        public String[] getNames() { return names; }
        public void setNames(String... values) { names = values; }
        public String getFailure() throws IOException { throw new IOException("failure"); }
        public String getFatal() { throw new AssertionError("fatal"); }
    }
}
