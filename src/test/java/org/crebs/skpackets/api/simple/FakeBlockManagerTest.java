package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
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

class FakeBlockManagerTest {
    @BeforeAll
    static void bootstrap() { PacketEventsTestBootstrap.initialize(); }

    @Test
    void bulkEditConvertsOnceAndSendsOnePacketPerSectionIncludingNegativeCoordinates() {
        try (Fixture f = new Fixture()) {
            f.manager.show(f.player, List.of(f.pos(-1, -1, -1), f.pos(-2, -2, -2),
                    f.pos(-1, 0, -1), f.pos(0, 0, 0)), f.data, true);
            assertEquals(3, f.sent.size());
            var negative = f.sent.stream().filter(p -> p.getChunkPosition().equals(new Vector3i(-1, -1, -1)))
                    .findFirst().orElseThrow();
            assertEquals(2, negative.getBlocks().length);
            assertArrayEquals(new int[]{-2, -1}, Arrays.stream(negative.getBlocks()).mapToInt(b -> b.getX()).sorted().toArray());
            assertTrue(Arrays.stream(negative.getBlocks()).allMatch(b -> b.getBlockId() == 42 && b.getY() < 0));
            f.conversion.verify(() -> SpigotConversionUtil.fromBukkitBlockData(f.data), times(1));
        }
    }

    @Test
    void distantBlocksWaitForTheirChunkAndChunkRefreshesShareOneTask() {
        try (Fixture f = new Fixture()) {
            when(f.player.isChunkSent(anyLong())).thenReturn(false);
            f.manager.show(f.player, List.of(f.pos(0, 0, 0), f.pos(16, 0, 0)), f.data, true);
            assertTrue(f.sent.isEmpty());
            f.chunk(0, 0); f.chunk(0, 0); f.chunk(1, 0);
            assertEquals(1, f.tasks.size());
            when(f.player.isChunkSent(anyLong())).thenReturn(true);
            f.runTasks();
            assertEquals(2, f.sent.size());
            f.conversion.verify(() -> SpigotConversionUtil.fromBukkitBlockData(f.data), times(1));
        }
    }

    @Test
    void resetBeforeQueuedRefreshPreventsResurrectionAndDoesNotLoadDistantChunks() {
        try (Fixture f = new Fixture()) {
            when(f.player.isChunkSent(anyLong())).thenReturn(false);
            f.manager.show(f.player, f.pos(0, 0, 0), f.data, true);
            f.chunk(0, 0);
            f.manager.reset(f.player, f.pos(0, 0, 0));
            when(f.player.isChunkSent(anyLong())).thenReturn(true);
            f.runTasks();
            assertTrue(f.sent.isEmpty());
            verify(f.world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
        }
    }

    @Test
    void onlyPersistentPositionsBlockRealUpdatesAndReplacingThemUpdatesTheWatch() {
        try (Fixture f = new Fixture()) {
            f.manager.show(f.player, f.pos(0, 0, 0), f.data, true);
            assertTrue(f.blockUpdate(0, 0, 0));
            assertFalse(f.blockUpdate(1, 0, 0));
            f.manager.show(f.player, f.pos(0, 0, 0), f.data, false);
            assertFalse(f.blockUpdate(0, 0, 0));
            f.sent.clear();
            f.manager.show(f.player, f.pos(0, 0, 0), f.data, false);
            assertEquals(1, f.sent.size());
        }
    }

    @Test
    void unloadedChunksAreSkippedWhenQueuedWorkRunsAndReappliedOnNextDelivery() {
        try (Fixture f = new Fixture()) {
            f.manager.show(f.player, f.pos(0, 0, 0), f.data, true);
            f.sent.clear();
            f.chunk(0, 0);
            when(f.player.isChunkSent(anyLong())).thenReturn(false);
            f.runTasks();
            assertTrue(f.sent.isEmpty());
            when(f.player.isChunkSent(anyLong())).thenReturn(true);
            f.chunk(0, 0);
            f.runTasks();
            assertEquals(1, f.sent.size());
        }
    }

    @Test
    void invalidBulkEditDoesNotPartiallyChangeStorage() {
        try (Fixture f = new Fixture()) {
            assertThrows(IllegalArgumentException.class, () -> f.manager.show(f.player,
                    List.of(f.pos(0, 0, 0), f.pos(1, 400, 0)), f.data, true));
            assertFalse(f.blockUpdate(0, 0, 0));
            assertTrue(f.sent.isEmpty());
        }
    }

    @Test
    void refreshBurstIsBoundedAndRemainingChunksRunOnFollowingTick() {
        try (Fixture f = new Fixture()) {
            List<Location> positions = new ArrayList<>();
            for (int x = 0; x < 100; x++) positions.add(f.pos(x * 16, 0, 0));
            f.manager.show(f.player, positions, f.data, true);
            f.sent.clear();
            for (int x = 0; x < 100; x++) f.chunk(x, 0);
            assertEquals(1, f.tasks.size());
            f.runTasks();
            assertFalse(f.sent.isEmpty());
            assertTrue(f.sent.size() < 100);
            assertEquals(1, f.tasks.size());
            f.runTasks();
            assertEquals(100, f.sent.size());
            assertTrue(f.tasks.isEmpty());
        }
    }

    @Test
    void multiBlockUpdatesKeepUnrelatedRealChanges() {
        try (Fixture f = new Fixture()) {
            f.manager.show(f.player, f.pos(-1, -1, -1), f.data, true);
            var fake = new WrapperPlayServerMultiBlockChange.EncodedBlock(1, -1, -1, -1);
            var real = new WrapperPlayServerMultiBlockChange.EncodedBlock(2, -2, -1, -1);
            var event = f.event(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
            try (var wrappers = mockConstruction(WrapperPlayServerMultiBlockChange.class, (packet, context) -> {
                when(packet.getChunkPosition()).thenReturn(new Vector3i(-1, -1, -1));
                when(packet.getBlocks()).thenReturn(new WrapperPlayServerMultiBlockChange.EncodedBlock[]{fake, real});
            })) {
                f.manager.onPacketSend(event);
                verify(wrappers.constructed().getFirst()).setBlocks(argThat(blocks ->
                        blocks.length == 1 && blocks[0] == real));
                verify(event).markForReEncode(true);
                verify(event, never()).setCancelled(true);
            }
        }
    }

    @Test
    void clearBatchesRealBlocksAndRestoresTileDataWithoutLeavingAnOverride() {
        try (Fixture f = new Fixture()) {
            var first = f.pos(0, 0, 0);
            var second = f.pos(1, 0, 0);
            f.manager.show(f.player, List.of(first, second), f.data, true);
            BlockData realData = mock(BlockData.class);
            var realState = mock(WrappedBlockState.class);
            when(realState.getGlobalId()).thenReturn(7);
            f.conversion.when(() -> SpigotConversionUtil.fromBukkitBlockData(realData)).thenReturn(realState);
            var tile = mock(org.bukkit.block.TileState.class);
            var tileBlock = mock(org.bukkit.block.Block.class);
            when(tileBlock.getState(false)).thenReturn(tile);
            when(tile.getLocation()).thenReturn(first);
            when(tileBlock.getBlockData()).thenReturn(realData);
            var plainBlock = mock(org.bukkit.block.Block.class);
            when(plainBlock.getBlockData()).thenReturn(realData);
            when(f.world.getBlockAt(0, 0, 0)).thenReturn(tileBlock);
            when(f.world.getBlockAt(1, 0, 0)).thenReturn(plainBlock);
            f.sent.clear();
            f.manager.clear(f.player);
            assertEquals(1, f.sent.size());
            assertEquals(2, f.sent.getFirst().getBlocks().length);
            assertTrue(Arrays.stream(f.sent.getFirst().getBlocks()).allMatch(b -> b.getBlockId() == 7));
            verify(f.player).sendBlockUpdate(first, tile);
            assertFalse(f.blockUpdate(0, 0, 0));
            f.chunk(0, 0);
            assertTrue(f.tasks.isEmpty());
        }
    }

    @Test
    void bulkVisibilityIsCheckedOncePerChunkAndDuplicatePositionsAreSentOnce() {
        try (Fixture f = new Fixture()) {
            List<Location> positions = new ArrayList<>();
            for (int y = 0; y < 32; y++) {
                positions.add(f.pos(0, y, 0));
                positions.add(f.pos(0, y, 0));
            }
            clearInvocations(f.player);
            f.manager.show(f.player, positions, f.data, true);
            verify(f.player, times(1)).isChunkSent(0L);
            assertEquals(32, f.sent.stream().mapToInt(p -> p.getBlocks().length).sum());
        }
    }

    @Test
    void clearingLargeDistantOverridesRestoresOnlySentChunks() {
        try (Fixture f = new Fixture()) {
            f.manager.show(f.player, List.of(f.pos(1600, 0, 0), f.pos(3200, 0, 0)), f.data, true);
            f.sent.clear();
            clearInvocations(f.player, f.world);
            f.manager.clear(f.player);
            assertTrue(f.sent.isEmpty());
            verify(f.player, never()).isChunkSent(anyLong());
            verify(f.world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
        }
    }

    @Test
    void reconnectWaitsForChunkLoaderBeforeReadingSentChunksAndRestoresPersistentBlocks() {
        try (Fixture f = new Fixture()) {
            f.manager.show(f.player, f.pos(0, 0, 0), f.data, true);
            f.sent.clear();
            when(f.player.getSentChunkKeys()).thenThrow(new NullPointerException("chunk loader not initialized"));
            clearInvocations(f.player);
            PlayerJoinEvent event = mock(PlayerJoinEvent.class);
            when(event.getPlayer()).thenReturn(f.player);
            assertDoesNotThrow(() -> f.manager.onJoin(event));
            verify(f.player, never()).getSentChunkKeys();
            assertEquals(1, f.tasks.size());
            doReturn(Set.of(0L)).when(f.player).getSentChunkKeys();
            f.runTasks();
            f.runTasks();
            assertEquals(1, f.sent.size());
        }
    }

    @Test
    void worldChangeDefersSentChunkAccessAndQuitBeforeRefreshSkipsIt() {
        try (Fixture f = new Fixture()) {
            f.manager.show(f.player, f.pos(0, 0, 0), f.data, true);
            f.sent.clear();
            when(f.player.getSentChunkKeys()).thenThrow(new NullPointerException("chunk loader not initialized"));
            clearInvocations(f.player);
            PlayerChangedWorldEvent event = mock(PlayerChangedWorldEvent.class);
            when(event.getPlayer()).thenReturn(f.player);
            assertDoesNotThrow(() -> f.manager.onWorld(event));
            verify(f.player, never()).getSentChunkKeys();
            when(f.player.isOnline()).thenReturn(false);
            assertDoesNotThrow(f::runTasks);
            verify(f.player, never()).getSentChunkKeys();
            assertTrue(f.sent.isEmpty());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final World world = mock(World.class);
        final Player player = mock(Player.class);
        final UUID viewer = UUID.randomUUID();
        final BlockData data = mock(BlockData.class);
        final List<WrapperPlayServerMultiBlockChange> sent = new ArrayList<>();
        final List<Runnable> tasks = new ArrayList<>();
        final MockedStatic<Bukkit> bukkit;
        final MockedStatic<PacketEvents> packets;
        final MockedStatic<SpigotConversionUtil> conversion;
        final FakeBlockManager manager;

        Fixture() {
            var api = mock(PacketEventsAPI.class, RETURNS_DEEP_STUBS);
            var settings = PacketEvents.getAPI().getSettings();
            var version = PacketEvents.getAPI().getServerManager().getVersion();
            when(api.getSettings()).thenReturn(settings);
            when(api.getServerManager().getVersion()).thenReturn(version);
            var playerManager = api.getPlayerManager();
            doAnswer(call -> { sent.add(call.getArgument(1)); return null; })
                    .when(playerManager).sendPacketSilently(any(Player.class), any(PacketWrapper.class));
            when(world.getUID()).thenReturn(UUID.randomUUID());
            when(world.getMinHeight()).thenReturn(-64);
            when(world.getMaxHeight()).thenReturn(320);
            when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
            when(player.getUniqueId()).thenReturn(viewer);
            when(player.getWorld()).thenReturn(world);
            when(player.isOnline()).thenReturn(true);
            when(player.isChunkSent(anyLong())).thenReturn(true);
            when(player.getSentChunkKeys()).thenReturn(Set.of(0L));
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            doAnswer(call -> { tasks.add(call.getArgument(1)); return null; })
                    .when(scheduler).runTask(any(JavaPlugin.class), any(Runnable.class));
            bukkit = mockStatic(Bukkit.class);
            packets = mockStatic(PacketEvents.class);
            conversion = mockStatic(SpigotConversionUtil.class);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(viewer)).thenReturn(player);
            packets.when(PacketEvents::getAPI).thenReturn(api);
            var state = mock(WrappedBlockState.class);
            when(state.getGlobalId()).thenReturn(42);
            conversion.when(() -> SpigotConversionUtil.fromBukkitBlockData(data)).thenReturn(state);
            manager = new FakeBlockManager(mock(JavaPlugin.class));
        }

        Location pos(int x, int y, int z) { return new Location(world, x, y, z); }

        PacketSendEvent event(PacketType.Play.Server type) {
            var event = mock(PacketSendEvent.class, RETURNS_DEEP_STUBS);
            when(event.getPacketType()).thenReturn(type);
            when(event.getUser().getUUID()).thenReturn(viewer);
            return event;
        }

        void chunk(int x, int z) {
            var event = event(PacketType.Play.Server.CHUNK_DATA);
            List<Runnable> after = new ArrayList<>();
            when(event.getTasksAfterSend()).thenReturn(after);
            try (var ignored = mockConstruction(WrapperPlayServerChunkData.class,
                    withSettings().defaultAnswer(RETURNS_DEEP_STUBS), (wrapper, context) -> {
                        when(wrapper.getColumn().getX()).thenReturn(x);
                        when(wrapper.getColumn().getZ()).thenReturn(z);
                    })) {
                manager.onPacketSend(event);
            }
            after.forEach(Runnable::run);
        }

        boolean blockUpdate(int x, int y, int z) {
            var event = event(PacketType.Play.Server.BLOCK_CHANGE);
            try (var ignored = mockConstruction(WrapperPlayServerBlockChange.class, (wrapper, context) ->
                    when(wrapper.getBlockPosition()).thenReturn(new Vector3i(x, y, z)))) {
                manager.onPacketSend(event);
            }
            return mockingDetails(event).getInvocations().stream()
                    .anyMatch(call -> call.getMethod().getName().equals("setCancelled") && Boolean.TRUE.equals(call.getArgument(0)));
        }

        void runTasks() {
            var batch = List.copyOf(tasks);
            tasks.clear();
            batch.forEach(Runnable::run);
        }

        @Override public void close() {
            conversion.close(); packets.close(); bukkit.close();
        }
    }
}
