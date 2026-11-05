package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.meta.OwnershipCheck;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlowingEntityManagerTest {
    private record SentGlow(UUID viewer, boolean glowing) {}

    @BeforeAll
    static void initializePacketEvents() throws ClassNotFoundException {
        PacketEventsTestBootstrap.initialize();
        Class.forName("me.tofaa.entitylib.meta.MetaFields$Entity");
    }

    @AfterEach
    void clearViewers() {
        GlowingEntityManager.clear();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void switchingViewersThenClearingRestoresEachPlayersGlow(boolean naturallyGlowing) {
        List<SentGlow> sent = new ArrayList<>();
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        withPlayers(naturallyGlowing, sent, player1Id, player2Id, () -> {
            GlowingEntityManager.setGlowingReceivers(42, List.of(player1Id));
            assertEquals(List.of(new SentGlow(player1Id, true)), sent);

            sent.clear();
            GlowingEntityManager.setGlowingReceivers(42, List.of(player2Id));
            assertEquals(List.of(
                    new SentGlow(player1Id, naturallyGlowing),
                    new SentGlow(player2Id, true)), sent);
            assertFalse(GlowingEntityManager.isGlowingFor(42, player1Id));
            assertTrue(GlowingEntityManager.isGlowingFor(42, player2Id));

            sent.clear();
            GlowingEntityManager.clearGlowingReceivers(42);
            assertEquals(List.of(new SentGlow(player2Id, naturallyGlowing)), sent);
            assertFalse(GlowingEntityManager.hasGlowingEntities());
        });
    }

    @Test
    void settingEmptyViewersRemovesGlowBeforeOutgoingListenersRun() {
        List<SentGlow> sent = new ArrayList<>();
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        withPlayers(false, sent, player1Id, player2Id, () -> {
            GlowingEntityManager.setGlowingReceivers(42, List.of(player1Id));
            sent.clear();
            GlowingEntityManager.setGlowingReceivers(42, List.of());
            assertEquals(List.of(new SentGlow(player1Id, false)), sent);
            assertFalse(GlowingEntityManager.hasGlowingEntities());
        });
    }

    @Test
    void removingViewersAfterTheEntityDisappearsStillClearsState() {
        List<SentGlow> sent = new ArrayList<>();
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        withPlayers(false, sent, player1Id, player2Id, () -> {
            GlowingEntityManager.setGlowingReceivers(42, List.of(player1Id));
            sent.clear();
            when(Bukkit.getWorlds()).thenReturn(List.of());
            GlowingEntityManager.removeGlowingReceivers(42, List.of(player1Id));
            assertFalse(GlowingEntityManager.hasGlowingEntities());
            assertTrue(sent.isEmpty());
        });
    }

    @Test
    void settingViewersAfterTheEntityDisappearsDiscardsStaleState() {
        List<SentGlow> sent = new ArrayList<>();
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        withPlayers(false, sent, player1Id, player2Id, () -> {
            GlowingEntityManager.setGlowingReceivers(42, List.of(player1Id));
            when(Bukkit.getWorlds()).thenReturn(List.of());
            GlowingEntityManager.setGlowingReceivers(42, List.of());
            assertFalse(GlowingEntityManager.hasGlowingEntities());
        });
    }

    @Test
    void disconnectingViewersArePrunedWithoutRemovingOtherViewers() {
        List<SentGlow> sent = new ArrayList<>();
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        withPlayers(false, sent, player1Id, player2Id, () -> {
            GlowingEntityManager.setGlowingReceivers(42, List.of(player1Id, player2Id));
            GlowingEntityManager.forgetViewer(player1Id);
            assertEquals(List.of(player2Id), GlowingEntityManager.getGlowingReceivers(42));
            GlowingEntityManager.forgetViewer(player2Id);
            assertFalse(GlowingEntityManager.hasGlowingEntities());
        });
    }

    @Test
    void nonLivingEntityRemovalClearsGlowRegistrations() {
        List<SentGlow> sent = new ArrayList<>();
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        withPlayers(false, sent, player1Id, player2Id, () -> {
            GlowingEntityManager.setGlowingReceivers(42, List.of(player1Id));
            Entity target = mock(Entity.class);
            when(target.getEntityId()).thenReturn(42);
            var removed = new org.bukkit.event.entity.EntityRemoveEvent(target, org.bukkit.event.entity.EntityRemoveEvent.Cause.DESPAWN);
            new GlowingEntityListener().onEntityRemove(removed);
            assertFalse(GlowingEntityManager.hasGlowingEntities());
        });
    }

    @Test
    void entityDeathAndUnloadClearRegistrations() {
        List<SentGlow> sent = new ArrayList<>();
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        withPlayers(false, sent, player1Id, player2Id, () -> {
            var listener = new GlowingEntityListener();
            var death = mock(org.bukkit.event.entity.EntityDeathEvent.class, RETURNS_DEEP_STUBS);
            when(death.getEntity().getEntityId()).thenReturn(42);
            GlowingEntityManager.setGlowingReceivers(42, List.of(player1Id));
            listener.onEntityDeath(death);
            assertFalse(GlowingEntityManager.hasGlowingEntities());
            var unload = mock(org.bukkit.event.world.EntitiesUnloadEvent.class);
            Entity target = mock(Entity.class);
            when(target.getEntityId()).thenReturn(42);
            when(unload.getEntities()).thenReturn(List.of(target));
            GlowingEntityManager.setGlowingReceivers(42, List.of(player1Id));
            listener.onEntitiesUnload(unload);
            assertFalse(GlowingEntityManager.hasGlowingEntities());
        });
    }

    private void withPlayers(boolean naturallyGlowing, List<SentGlow> sent, UUID player1Id, UUID player2Id, Runnable scenario) {
        Entity target = mock(Entity.class);
        when(target.getEntityId()).thenReturn(42);
        when(target.getType()).thenReturn(EntityType.ARMOR_STAND);
        when(target.isGlowing()).thenReturn(naturallyGlowing);
        World world = mock(World.class);
        when(world.getEntities()).thenReturn(List.of(target));
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        when(player1.getUniqueId()).thenReturn(player1Id);
        when(player2.getUniqueId()).thenReturn(player2Id);
        when(player1.isOnline()).thenReturn(true);
        when(player2.isOnline()).thenReturn(true);
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class, RETURNS_DEEP_STUBS);
        var settings = PacketEvents.getAPI().getSettings();
        var version = PacketEvents.getAPI().getServerManager().getVersion();
        when(api.getSettings()).thenReturn(settings);
        when(api.getServerManager().getVersion()).thenReturn(version);
        EntityLib entityLib = mock(EntityLib.class, RETURNS_DEEP_STUBS);
        when(entityLib.getConfig().getOwnershipCheck()).thenReturn(OwnershipCheck.THROW);
        var playerManager = api.getPlayerManager();
        doAnswer(call -> {
            Player viewer = call.getArgument(0);
            WrapperPlayServerEntityMetadata packet = call.getArgument(1);
            sent.add(new SentGlow(viewer.getUniqueId(),
                    outgoingGlowAfterListener(viewer, packet)));
            return null;
        }).when(playerManager).sendPacket(any(Player.class), any(WrapperPlayServerEntityMetadata.class));

        try (var bukkit = mockStatic(Bukkit.class);
             var packets = mockStatic(PacketEvents.class);
             var entities = mockStatic(EntityLib.class);
             var conversion = mockStatic(SpigotConversionUtil.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            bukkit.when(() -> Bukkit.getPlayer(player1Id)).thenReturn(player1);
            bukkit.when(() -> Bukkit.getPlayer(player2Id)).thenReturn(player2);
            packets.when(PacketEvents::getAPI).thenReturn(api);
            entities.when(EntityLib::get).thenReturn(entityLib);
            conversion.when(() -> SpigotConversionUtil.fromBukkitEntityType(EntityType.ARMOR_STAND))
                    .thenReturn(EntityTypes.ARMOR_STAND);
            conversion.when(() -> SpigotConversionUtil.getEntityMetadata(target)).thenReturn(
                    List.of(new EntityData<>(0, EntityDataTypes.BYTE,
                            (byte) (naturallyGlowing ? 0x40 : 0))));
            scenario.run();
        }
    }

    private boolean outgoingGlowAfterListener(Player viewer, WrapperPlayServerEntityMetadata packet) {
        PacketSendEvent event = mock(PacketSendEvent.class, RETURNS_DEEP_STUBS);
        UUID viewerId = viewer.getUniqueId();
        when(event.getPacketType()).thenReturn(PacketType.Play.Server.ENTITY_METADATA);
        when(event.getUser().getUUID()).thenReturn(viewerId);
        AtomicReference<List<EntityData<?>>> metadata = new AtomicReference<>(packet.getEntityMetadata());
        try (var ignored = mockConstruction(WrapperPlayServerEntityMetadata.class, (wrapper, context) -> {
            when(wrapper.getEntityId()).thenReturn(packet.getEntityId());
            when(wrapper.getEntityMetadata()).thenAnswer(call -> metadata.get());
            doAnswer(call -> {
                metadata.set(call.getArgument(0));
                return null;
            }).when(wrapper).setEntityMetadata(anyList());
        })) {
            new GlowingEntityListener().onPacketSend(event);
        }
        return metadata.get().stream()
                .filter(data -> data.getIndex() == 0)
                .anyMatch(data -> (((Byte) data.getValue()) & 0x40) != 0);
    }
}
