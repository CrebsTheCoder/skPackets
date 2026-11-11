package org.crebs.skpackets.api.entity;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketEntityManagerTest {
    @BeforeAll
    static void bootstrap() { PacketEventsTestBootstrap.initialize(); }

    @Test
    void chunkDeliveryTouchesOnlyItsEntitiesAndAuthorizedViewers() {
        try (Fixture f = new Fixture()) {
            when(f.player.isChunkSent(anyLong())).thenReturn(false);
            WrapperEntity local = f.spawn(1, -1, -1, f.player, true);
            WrapperEntity distant = f.spawn(2, 10, 10, f.player, true);
            Player other = f.player(UUID.randomUUID());
            WrapperEntity privateEntity = f.spawn(3, -1, -1, other, true);
            clearInvocations(local, distant, privateEntity);
            f.manager.onChunkLoad(new PlayerChunkLoadEvent(f.chunk(-1, -1), f.player));
            verify(local).addViewer(f.viewer);
            verifyNoInteractions(distant);
            verify(privateEntity, never()).addViewer(f.viewer);
            clearInvocations(local, distant, privateEntity);
            f.manager.onChunkUnload(new PlayerChunkUnloadEvent(f.chunk(-1, -1), f.player));
            verify(local).removeViewer(f.viewer);
            assertEquals(Set.of(f.viewer), f.manager.getViewers(local));
            verifyNoInteractions(distant);
            clearInvocations(local);
            f.manager.onChunkLoad(new PlayerChunkLoadEvent(f.chunk(-1, -1), f.player));
            verify(local).addViewer(f.viewer);
        }
    }

    @Test
    void teleportMovesChunkIndexAndHidesEntitiesOutsideSentChunks() {
        try (Fixture f = new Fixture()) {
            WrapperEntity entity = f.spawn(1, 0, 0, f.player, true);
            when(f.player.isChunkSent(anyLong())).thenReturn(false);
            clearInvocations(entity);
            f.manager.teleport(entity, new Location(f.world, -17, 64, 32));
            verify(entity).removeViewer(f.viewer);
            clearInvocations(entity);
            f.manager.onChunkLoad(new PlayerChunkLoadEvent(f.chunk(0, 0), f.player));
            verifyNoInteractions(entity);
            f.manager.onChunkLoad(new PlayerChunkLoadEvent(f.chunk(-2, 2), f.player));
            verify(entity).addViewer(f.viewer);
        }
    }

    @Test
    void settingSameViewersDoesNotDespawnOrRespawnAndChangesApplyOnlyDifferences() {
        try (Fixture f = new Fixture()) {
            WrapperEntity entity = f.spawn(1, 0, 0, f.player, true);
            clearInvocations(entity);
            f.manager.setViewers(entity, List.of(f.player, f.player));
            verify(entity, never()).addViewer(any(UUID.class));
            verify(entity, never()).removeViewer(any(UUID.class));
            Player other = f.player(UUID.randomUUID());
            f.manager.setViewers(entity, List.of(f.player, other));
            verify(entity).addViewer(other.getUniqueId());
            verify(entity, never()).removeViewer(f.viewer);
            f.manager.setViewers(entity, List.of(other));
            verify(entity).removeViewer(f.viewer);
            assertEquals(Set.of(other.getUniqueId()), f.manager.getViewers(entity));
        }
    }

    @Test
    void removalClearsChunkViewerAndUuidIndexes() {
        try (Fixture f = new Fixture()) {
            WrapperEntity entity = f.spawn(1, 0, 0, f.player, true);
            UUID uuid = entity.getUuid();
            assertSame(entity, f.manager.getEntity(uuid).orElseThrow());
            f.manager.remove(entity);
            assertTrue(f.manager.getEntity(uuid).isEmpty());
            clearInvocations(entity);
            f.manager.onChunkLoad(new PlayerChunkLoadEvent(f.chunk(0, 0), f.player));
            f.manager.onJoin(new PlayerJoinEvent(f.player, (net.kyori.adventure.text.Component) null));
            f.runTasks();
            verifyNoInteractions(entity);
        }
    }

    @Test
    void quittingRetainsPersistentViewersButCleansLastTransientViewer() {
        try (Fixture f = new Fixture()) {
            WrapperEntity persistent = f.spawn(1, 0, 0, f.player, true);
            WrapperEntity transientEntity = f.spawn(2, 0, 0, f.player, false);
            PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
            when(quit.getPlayer()).thenReturn(f.player);
            f.manager.onQuit(quit);
            assertEquals(Set.of(f.viewer), f.manager.getViewers(persistent));
            assertTrue(f.manager.getEntity(2).isEmpty());
            verify(transientEntity).remove();
            clearInvocations(persistent, transientEntity);
            f.manager.onChunkLoad(new PlayerChunkLoadEvent(f.chunk(0, 0), f.player));
            verify(persistent).addViewer(f.viewer);
            verifyNoInteractions(transientEntity);
        }
    }

    @Test
    void worldIsPartOfChunkIndexAndCrossWorldTeleportReindexesIt() {
        try (Fixture f = new Fixture()) {
            WrapperEntity entity = f.spawn(1, 0, 0, f.player, true);
            World other = mock(World.class);
            when(other.getUID()).thenReturn(UUID.randomUUID());
            when(other.getName()).thenReturn("other");
            f.manager.teleport(entity, new Location(other, 0, 64, 0));
            clearInvocations(entity);
            f.manager.onChunkLoad(new PlayerChunkLoadEvent(f.chunk(0, 0), f.player));
            verifyNoInteractions(entity);
            when(f.player.getWorld()).thenReturn(other);
            org.bukkit.Chunk chunk = mock(org.bukkit.Chunk.class);
            when(chunk.getWorld()).thenReturn(other);
            f.manager.onChunkLoad(new PlayerChunkLoadEvent(chunk, f.player));
            verify(entity).addViewer(f.viewer);
        }
    }

    @Test
    void ridersAreNotCulledUsingTheirStaleSpawnChunk() {
        try (Fixture f = new Fixture()) {
            WrapperEntity rider = f.spawn(1, 0, 0, f.player, true);
            when(rider.getRiding()).thenReturn(mock(WrapperEntity.class));
            clearInvocations(rider);
            f.manager.onChunkUnload(new PlayerChunkUnloadEvent(f.chunk(0, 0), f.player));
            verify(rider, never()).removeViewer(f.viewer);
        }
    }

    @Test
    void uuidLookupIsSafeForAsyncCallersWhileMutationsStillRequireServerThread() throws Exception {
        try (Fixture f = new Fixture()) {
            WrapperEntity entity = f.spawn(1, 0, 0, f.player, true);
            UUID uuid = entity.getUuid();
            try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
                assertSame(entity, executor.submit(() -> f.manager.getEntity(uuid).orElseThrow()).get());
            }
            f.bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            assertThrows(IllegalStateException.class, () -> f.manager.remove(entity));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final UUID viewer = UUID.randomUUID();
        final World world = mock(World.class);
        final Player player;
        final MockedStatic<Bukkit> bukkit;
        final MockedStatic<EntityLib> library;
        final List<Runnable> tasks = new ArrayList<>();
        final PacketEntityManager manager;

        Fixture() {
            when(world.getUID()).thenReturn(UUID.randomUUID());
            when(world.getName()).thenReturn("world");
            bukkit = mockStatic(Bukkit.class);
            library = mockStatic(EntityLib.class);
            library.when(EntityLib::get).thenReturn(mock(EntityLib.class));
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            doAnswer(call -> { tasks.add(call.getArgument(1)); return null; })
                    .when(scheduler).runTask(any(JavaPlugin.class), any(Runnable.class));
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            player = player(viewer);
            manager = new PacketEntityManager(mock(JavaPlugin.class));
        }

        Player player(UUID id) {
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(id);
            when(player.getWorld()).thenReturn(world);
            when(player.isOnline()).thenReturn(true);
            when(player.isChunkSent(anyLong())).thenReturn(true);
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(player);
            return player;
        }

        WrapperEntity spawn(int id, int chunkX, int chunkZ, Player viewer, boolean persistent) {
            WrapperEntity entity = mock(WrapperEntity.class);
            when(entity.getEntityId()).thenReturn(id);
            when(entity.getUuid()).thenReturn(UUID.randomUUID());
            when(entity.isSpawned()).thenReturn(true);
            Set<UUID> actualViewers = new HashSet<>();
            when(entity.getViewers()).thenAnswer(call -> Set.copyOf(actualViewers));
            when(entity.addViewer(any(UUID.class))).thenAnswer(call -> actualViewers.add(call.getArgument(0)));
            when(entity.removeViewer(any(UUID.class))).thenAnswer(call -> actualViewers.remove(call.getArgument(0)));
            doAnswer(call -> { actualViewers.remove(call.getArgument(0)); return null; })
                    .when(entity).handleViewerDisconnect(any(UUID.class));
            manager.spawn(entity, new Location(world, chunkX * 16, 64, chunkZ * 16), List.of(viewer), persistent);
            return entity;
        }

        org.bukkit.Chunk chunk(int x, int z) {
            org.bukkit.Chunk chunk = mock(org.bukkit.Chunk.class);
            when(chunk.getWorld()).thenReturn(world);
            when(chunk.getX()).thenReturn(x);
            when(chunk.getZ()).thenReturn(z);
            return chunk;
        }

        void runTasks() { List.copyOf(tasks).forEach(Runnable::run); tasks.clear(); }

        @Override public void close() { library.close(); bukkit.close(); }
    }
}
