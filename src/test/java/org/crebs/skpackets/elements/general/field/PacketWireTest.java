package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.EnumSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketWireTest {
    static Stream<Arguments> bossBarVersionsAndActions() {
        return Stream.of(ServerVersion.V_1_20_5, ServerVersion.V_1_21, ServerVersion.V_26_3)
                .flatMap(version -> Stream.of(WrapperPlayServerBossBar.Action.values()).map(action -> Arguments.of(version, action)));
    }

    @ParameterizedTest @MethodSource("bossBarVersionsAndActions")
    void bossBarActionsWriteAndReadWithAllOptionalFieldsOmitted(ServerVersion version, WrapperPlayServerBossBar.Action action) {
        var packet = (WrapperPlayServerBossBar) PacketCreationCoverageTest.create(PacketType.Play.Server.BOSS_BAR,
                Map.of("uuid", new UUID(1, 2), "action", action.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')));
        var decoded = roundTripBossBar(packet, version);
        assertEquals(packet.getUUID(), decoded.getUUID());
        assertEquals(action, decoded.getAction());
        if (action == WrapperPlayServerBossBar.Action.ADD || action == WrapperPlayServerBossBar.Action.UPDATE_FLAGS)
            assertEquals(EnumSet.noneOf(BossBar.Flag.class), decoded.getFlags());
        if (action == WrapperPlayServerBossBar.Action.ADD || action == WrapperPlayServerBossBar.Action.UPDATE_TITLE)
            assertEquals(Component.empty(), decoded.getTitle());
        if (action == WrapperPlayServerBossBar.Action.ADD || action == WrapperPlayServerBossBar.Action.UPDATE_STYLE) {
            assertEquals(BossBar.Color.WHITE, decoded.getColor());
            assertEquals(BossBar.Overlay.PROGRESS, decoded.getOverlay());
        }
        if (action == WrapperPlayServerBossBar.Action.ADD || action == WrapperPlayServerBossBar.Action.UPDATE_HEALTH)
            assertEquals(1f, decoded.getHealth());
    }

    @Test void documentedBossBarExampleWritesWithoutProvidingFlags() {
        var packet = (WrapperPlayServerBossBar) PacketCreationCoverageTest.create(PacketType.Play.Server.BOSS_BAR,
                Map.of("uuid", new UUID(1, 2), "action", "add", "title", Component.text("Preparing your next quest"),
                        "health", 0.75, "color", ch.njol.skript.util.SkriptColor.DARK_PURPLE, "overlay", org.bukkit.boss.BarStyle.SOLID));
        var decoded = roundTripBossBar(packet, ServerVersion.V_26_3);
        assertEquals(Component.text("Preparing your next quest"), decoded.getTitle());
        assertEquals(0.75f, decoded.getHealth());
        assertEquals(BossBar.Color.PURPLE, decoded.getColor());
        assertTrue(decoded.getFlags().isEmpty());
    }

    @Test void explicitBossBarFlagsOverrideDefaultsAndAreNotSharedBetweenPackets() {
        var packet = (WrapperPlayServerBossBar) PacketCreationCoverageTest.create(PacketType.Play.Server.BOSS_BAR,
                Map.of("uuid", new UUID(1, 2), "action", "add", "flags", new String[]{"darken screen", "play boss music", "create world fog"}));
        var decoded = roundTripBossBar(packet, ServerVersion.V_26_3);
        assertEquals(EnumSet.allOf(BossBar.Flag.class), decoded.getFlags());
        var first = (WrapperPlayServerBossBar) PacketCreationCoverageTest.create(PacketType.Play.Server.BOSS_BAR,
                Map.of("uuid", new UUID(1, 3), "action", "add"));
        var second = (WrapperPlayServerBossBar) PacketCreationCoverageTest.create(PacketType.Play.Server.BOSS_BAR,
                Map.of("uuid", new UUID(1, 4), "action", "add"));
        first.getFlags().add(BossBar.Flag.DARKEN_SCREEN);
        assertTrue(second.getFlags().isEmpty());
    }

    private static WrapperPlayServerBossBar roundTripBossBar(WrapperPlayServerBossBar packet, ServerVersion version) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            packet.setServerVersion(version);
            packet.setBuffer(buffer);
            packet.write();
            var decoded = new WrapperPlayServerBossBar(new UUID(0, 0), WrapperPlayServerBossBar.Action.REMOVE);
            decoded.setServerVersion(version);
            decoded.setBuffer(buffer);
            decoded.read();
            assertEquals(0, buffer.readableBytes());
            return decoded;
        } finally { buffer.release(); }
    }

    @BeforeAll static void initialize() {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    static Stream<Arguments> versionsAndHands() {
        return Stream.of(ServerVersion.V_1_18, ServerVersion.V_1_19, ServerVersion.V_1_21, ServerVersion.V_26_3)
                .flatMap(version -> Stream.of(InteractionHand.values()).map(hand -> Arguments.of(version, hand)));
    }

    @ParameterizedTest @MethodSource("versionsAndHands")
    void useItemWritesExactBytesAndReadsBack(ServerVersion version, InteractionHand hand) {
        var packet = (WrapperPlayClientUseItem) PacketCreationCoverageTest.create(PacketType.Play.Client.USE_ITEM,
                Map.of("hand", hand, "sequence", 300L, "yaw", 12.5d, "pitch", -4d));
        ByteBuf buffer = Unpooled.buffer();
        try {
            packet.setServerVersion(version);
            packet.setBuffer(buffer);
            packet.write();
            ByteBuffer expected = ByteBuffer.allocate(11);
            expected.put((byte) hand.ordinal());
            boolean sequence = version.isNewerThanOrEquals(ServerVersion.V_1_19);
            boolean angles = version.isNewerThanOrEquals(ServerVersion.V_1_21);
            if (sequence) expected.put((byte) 0xac).put((byte) 0x02);
            if (angles) expected.putFloat(12.5f).putFloat(-4f);
            assertArrayEquals(java.util.Arrays.copyOf(expected.array(), expected.position()), ByteBufUtil.getBytes(buffer));
            var decoded = new WrapperPlayClientUseItem(InteractionHand.MAIN_HAND);
            decoded.setServerVersion(version);
            decoded.setBuffer(buffer);
            decoded.read();
            assertSame(hand, decoded.getHand());
            assertEquals(sequence ? 300 : 0, decoded.getSequence());
            assertEquals(angles ? 12.5f : 0f, decoded.getYaw());
            assertEquals(angles ? -4f : 0f, decoded.getPitch());
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test void rawFallbackWritesOnlyTheProvidedPayloadAndCopiesIndependently() {
        var packet = (PacketFieldRegistrar.RawPacket) PacketCreationCoverageTest.create(
                PacketType.Play.Server.STOP_SOUND, Map.of("raw data", new Number[]{0, 255, 127}));
        var copy = new PacketFieldRegistrar.RawPacket(PacketType.Play.Server.STOP_SOUND, new Number[0]);
        copy.copy(packet);
        packet.setData(new Number[]{1});
        ByteBuf buffer = Unpooled.buffer();
        try {
            copy.setBuffer(buffer);
            copy.write();
            assertArrayEquals(new byte[]{0, -1, 127}, ByteBufUtil.getBytes(buffer));
        } finally { buffer.release(); }
    }
}
