package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.bukkit.Bukkit;
import org.bukkit.block.sign.Side;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.general.EntityTracker;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketFieldBehaviorTest {
    @BeforeAll static void initialize() {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    @Test void cameraDestroyAndSignPacketsUseDiscoveredFieldsAndNativeValues() {
        var camera = (WrapperPlayServerCamera) PacketCreationCoverageTest.create(PacketType.Play.Server.CAMERA, Map.of("camera id", 42));
        assertEquals(42, camera.getCameraId());
        var destroy = (WrapperPlayServerDestroyEntities) PacketCreationCoverageTest.create(PacketType.Play.Server.DESTROY_ENTITIES,
                Map.of("entity ids", new Number[]{1, 2, 3}));
        assertArrayEquals(new int[]{1, 2, 3}, destroy.getEntityIds());
        var sign = (WrapperPlayServerOpenSignEditor) PacketCreationCoverageTest.create(PacketType.Play.Server.OPEN_SIGN_EDITOR,
                Map.of("position", new Vector(1, 2, 3), "sign side", Side.BACK));
        assertFalse(sign.isFrontText());
        var side = field(PacketType.Play.Server.OPEN_SIGN_EDITOR, "sign side");
        assertSame(Side.class, side.expectedType());
        assertSame(Side.BACK, side.getter().apply(sign));
        side.setter().accept(sign, Side.FRONT);
        assertTrue(sign.isFrontText());
    }

    @Test void serverboundPacketsUseTheSameRegistrarWithNativeValuesAndDefaults() {
        var flying = (WrapperPlayClientPlayerFlying) PacketCreationCoverageTest.create(PacketType.Play.Client.PLAYER_FLYING,
                Map.of("on ground", true));
        assertTrue(flying.isOnGround());
        assertFalse(flying.hasPositionChanged());
        assertFalse(flying.hasRotationChanged());
        assertFalse(flying.isHorizontalCollision());

        var sign = (WrapperPlayClientUpdateSign) PacketCreationCoverageTest.create(PacketType.Play.Client.UPDATE_SIGN,
                Map.of("block position", new Vector(1, 2, 3), "text lines", new String[]{"one", "two", "three", "four"}, "sign side", Side.BACK));
        assertFalse(sign.isFrontText());
        assertArrayEquals(new String[]{"one", "two", "three", "four"}, sign.getTextLines());
        var side = field(PacketType.Play.Client.UPDATE_SIGN, "sign side");
        assertSame(Side.BACK, side.getter().apply(sign));
        side.setter().accept(sign, Side.FRONT);
        assertTrue(sign.isFrontText());
    }

    @Test void closeWindowAndHighlightKeepTheirOmittedFieldDefaults() {
        var close = (WrapperPlayServerCloseWindow) PacketCreationCoverageTest.create(PacketType.Play.Server.CLOSE_WINDOW, Map.of());
        assertEquals(0, close.getWindowId());
        assertTrue(PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.CLOSE_WINDOW).accessors().stream().allMatch(FieldAccessor::isOptional));
        var highlight = (WrapperPlayServerGameTestHighlightPos) PacketCreationCoverageTest.create(PacketType.Play.Server.GAME_TEST_HIGHLIGHT_POS,
                Map.of("absolute pos", new Vector(1, 2, 3)));
        assertEquals(new com.github.retrooper.packetevents.util.Vector3i(0, 0, 0), highlight.getRelativePos());
        assertTrue(field(PacketType.Play.Server.GAME_TEST_HIGHLIGHT_POS, "relative pos").isOptional());
    }

    @Test void blockStateRemainsBlockDataRatherThanANumericBlockId() {
        var data = mock(org.bukkit.block.data.BlockData.class);
        var state = mock(com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState.class);
        when(state.getGlobalId()).thenReturn(17);
        try (var conversion = mockStatic(io.github.retrooper.packetevents.util.SpigotConversionUtil.class)) {
            conversion.when(() -> io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitBlockData(data)).thenReturn(state);
            var block = (WrapperPlayServerBlockChange) PacketCreationCoverageTest.create(PacketType.Play.Server.BLOCK_CHANGE,
                    Map.of("block position", new Vector(1, 2, 3), "block state", data));
            assertEquals(17, block.getBlockId());
            assertSame(org.bukkit.block.data.BlockData.class, field(PacketType.Play.Server.BLOCK_CHANGE, "block state").expectedType());
        }
    }

    @Test void metadataStillAcceptsAndReturnsEntityMeta() {
        var source = mock(EntityMeta.class);
        var snapshot = mock(EntityMeta.class);
        when(source.entityData()).thenReturn(List.of());
        var packet = (WrapperPlayServerEntityMetadata) PacketCreationCoverageTest.create(PacketType.Play.Server.ENTITY_METADATA,
                Map.of("entity id", 71, "entity metadata", source));
        assertTrue(packet.getEntityMetadata().isEmpty());
        var meta = field(PacketType.Play.Server.ENTITY_METADATA, "entity metadata");
        assertSame(EntityMeta.class, meta.expectedType());
        var entityType = EntityTypes.ZOMBIE;
        try (var tracker = mockStatic(EntityTracker.class); var metadata = mockStatic(EntityMeta.class)) {
            tracker.when(() -> EntityTracker.getType(71)).thenReturn(entityType);
            metadata.when(() -> EntityMeta.createMeta(71, entityType)).thenReturn(snapshot);
            assertSame(snapshot, meta.getter().apply(packet));
            verify(snapshot).readPacket(packet);
        }
    }

    @Test void entityStatusKeepsItsSignedByteBehavior() {
        var packet = (WrapperPlayServerEntityStatus) PacketCreationCoverageTest.create(PacketType.Play.Server.ENTITY_STATUS,
                Map.of("entity id", 71, "status", 255));
        assertEquals(-1, packet.getStatus());
        field(PacketType.Play.Server.ENTITY_STATUS, "status").setter().accept(packet, 128);
        assertEquals(-128, packet.getStatus());
    }

    @SuppressWarnings("unchecked")
    private static FieldAccessor<PacketWrapper<?>, Object> field(PacketTypeCommon type, String name) {
        return (FieldAccessor<PacketWrapper<?>, Object>) PacketFieldRegistry.INSTANCE.getSchema(type).getAccessor(name);
    }
}
