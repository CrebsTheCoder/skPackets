package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlowingEntityListenerTest {
    @BeforeAll
    static void initializePacketEvents() {
        PacketEventsTestBootstrap.initialize();
    }

    @Test
    void sparseMetadataDoesNotInjectOrResetEntityFlags() {
        List<EntityData<?>> metadata = List.of(new EntityData<>(1, EntityDataTypes.INT, 300));
        var result = intercept(metadata);
        assertSame(metadata, result.metadata());
        assertFalse(result.reencoded());
    }

    @Test
    void addingGlowPreservesOtherFlagsAndUnrelatedMetadata() {
        EntityData<?> other = new EntityData<>(1, EntityDataTypes.INT, 300);
        List<EntityData<?>> metadata = List.of(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x21), other);
        var result = intercept(metadata);
        assertEquals((byte) 0x61, result.metadata().getFirst().getValue());
        assertSame(other, result.metadata().get(1));
        assertEquals((byte) 0x21, metadata.getFirst().getValue());
        assertTrue(result.reencoded());
    }

    @Test
    void packetsAlreadyContainingGlowAreNotRewritten() {
        List<EntityData<?>> metadata = List.of(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x61));
        var result = intercept(metadata);
        assertSame(metadata, result.metadata());
        assertFalse(result.reencoded());
    }

    private record Result(List<EntityData<?>> metadata, boolean reencoded) {}

    private Result intercept(List<EntityData<?>> original) {
        PacketSendEvent event = mock(PacketSendEvent.class, RETURNS_DEEP_STUBS);
        UUID player1Id = UUID.randomUUID();
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.ENTITY_METADATA);
        when(event.getUser().getUUID()).thenReturn(player1Id);
        AtomicReference<List<EntityData<?>>> metadata = new AtomicReference<>(original);
        List<Boolean> reencoded = new ArrayList<>();
        doAnswer(call -> {
            reencoded.add(call.getArgument(0));
            return null;
        }).when(event).markForReEncode(anyBoolean());
        try (var glowing = mockStatic(GlowingEntityManager.class);
             var ignored = mockConstruction(WrapperPlayServerEntityMetadata.class, (packet, context) -> {
                 when(packet.getEntityId()).thenReturn(42);
                 when(packet.getEntityMetadata()).thenAnswer(call -> metadata.get());
                 doAnswer(call -> {
                     metadata.set(call.getArgument(0));
                     return null;
                 }).when(packet).setEntityMetadata(anyList());
             })) {
            glowing.when(GlowingEntityManager::hasGlowingEntities).thenReturn(true);
            glowing.when(() -> GlowingEntityManager.isGlowingFor(42, player1Id)).thenReturn(true);
            new GlowingEntityListener().onPacketSend(event);
        }
        return new Result(metadata.get(), reencoded.contains(true));
    }
}
