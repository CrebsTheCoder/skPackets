package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.chat.RemoteChatSession;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatSessionListenerTest {
    @BeforeAll
    static void initializePacketEvents() {
        PacketEventsTestBootstrap.initialize();
    }

    @Test
    void concurrentProfileUpdatesRetainEverySessionAndDisconnectRemovesThem() throws Exception {
        int workers = 4;
        int profilesPerWorker = 250;
        List<List<UUID>> owners = new ArrayList<>();
        for (int worker = 0; worker < workers; worker++) {
            List<UUID> ids = new ArrayList<>();
            for (int profile = 0; profile < profilesPerWorker; profile++) {
                ids.add(UUID.randomUUID());
            }
            owners.add(ids);
        }
        RemoteChatSession session = mock(RemoteChatSession.class);
        var barrier = new CyclicBarrier(workers);
        try (var pool = Executors.newFixedThreadPool(workers)) {
            List<Future<?>> updates = new ArrayList<>();
            for (List<UUID> ids : owners) {
                updates.add(pool.submit(() -> {
                    PacketSendEvent event = mock(PacketSendEvent.class);
                    when(event.getPacketType()).thenReturn(PacketType.Play.Server.PLAYER_INFO_UPDATE);
                    PlayerInfo entry = mock(PlayerInfo.class);
                    AtomicReference<UUID> owner = new AtomicReference<>();
                    when(entry.getGameProfile()).thenAnswer(call -> new UserProfile(owner.get(), "player1", List.of()));
                    when(entry.getChatSession()).thenReturn(session);
                    try (var ignored = mockConstruction(WrapperPlayServerPlayerInfoUpdate.class, (packet, context) -> when(packet.getEntries()).thenReturn(List.of(entry)))) {
                        barrier.await(10, TimeUnit.SECONDS);
                        ChatSessionListener listener = new ChatSessionListener();
                        for (UUID id : ids) {
                            owner.set(id);
                            listener.onPacketSend(event);
                        }
                    }
                    return null;
                }));
            }
            for (Future<?> update : updates) {
                update.get(30, TimeUnit.SECONDS);
            }
            for (List<UUID> ids : owners) {
                for (UUID id : ids) {
                    assertSame(session, ChatSessionListener.getChatSession(id));
                }
            }
        } finally {
            UserDisconnectEvent disconnect = mock(UserDisconnectEvent.class, RETURNS_DEEP_STUBS);
            AtomicReference<UUID> owner = new AtomicReference<>();
            when(disconnect.getUser().getUUID()).thenAnswer(call -> owner.get());
            ChatSessionListener listener = new ChatSessionListener();
            for (List<UUID> ids : owners) {
                for (UUID id : ids) {
                    owner.set(id);
                    listener.onUserDisconnect(disconnect);
                    assertNull(ChatSessionListener.getChatSession(id));
                }
            }
        }
    }

    @Test
    void disconnectWithoutCachedSessionDoesNotThrow() {
        UserDisconnectEvent disconnect = mock(UserDisconnectEvent.class, RETURNS_DEEP_STUBS);
        when(disconnect.getUser().getUUID()).thenReturn(UUID.randomUUID());
        assertDoesNotThrow(() -> new ChatSessionListener().onUserDisconnect(disconnect));
        assertNull(ChatSessionListener.getChatSession(null));
    }
}
