package org.crebs.skpackets.api.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RealEntityPassengerManagerTest {
    private record Sent(UUID viewer, int vehicle, int[] passengers) {}

    @BeforeAll
    static void initializePacketEvents() {
        PacketEventsTestBootstrap.initialize();
    }

    @Test
    void sequentialMountsAndDismountKeepOtherFakeAndRealPassengers() {
        try (Fixture fixture = new Fixture()) {
            WrapperEntity first = fixture.passenger(11, fixture.player1Id);
            WrapperEntity second = fixture.passenger(12, fixture.player1Id);
            fixture.manager.update(fixture.vehicle, List.of(first), true);
            fixture.assertLast(fixture.player1Id, 1, 99, 11);
            fixture.manager.update(fixture.vehicle, List.of(second), true);
            fixture.assertLast(fixture.player1Id, 1, 99, 11, 12);
            fixture.manager.update(fixture.vehicle, List.of(first), false);
            fixture.assertLast(fixture.player1Id, 1, 99, 12);
            fixture.manager.update(fixture.vehicle, List.of(second), false);
            fixture.assertLast(fixture.player1Id, 1, 99);
        }
    }

    @Test
    void eachViewerReceivesOnlyItsVisiblePassengersWithoutDuplicatePackets() {
        try (Fixture fixture = new Fixture()) {
            WrapperEntity shared = fixture.passenger(11, fixture.player1Id, fixture.player2Id);
            WrapperEntity privatePassenger = fixture.passenger(12, fixture.player1Id);
            fixture.manager.update(fixture.vehicle, List.of(shared, privatePassenger), true);
            assertEquals(2, fixture.sent.size());
            fixture.assertPacket(fixture.player1Id, 1, 99, 11, 12);
            fixture.assertPacket(fixture.player2Id, 1, 99, 11);
            fixture.sent.clear();
            fixture.manager.update(fixture.vehicle, List.of(shared), false);
            assertEquals(2, fixture.sent.size());
            fixture.assertPacket(fixture.player1Id, 1, 99, 12);
            fixture.assertPacket(fixture.player2Id, 1, 99);
        }
    }

    @Test
    void repeatedMountDoesNotDuplicatePassenger() {
        try (Fixture fixture = new Fixture()) {
            WrapperEntity passenger = fixture.passenger(11, fixture.player1Id);
            fixture.manager.update(fixture.vehicle, List.of(passenger), true);
            fixture.manager.update(fixture.vehicle, List.of(passenger), true);
            fixture.assertLast(fixture.player1Id, 1, 99, 11);
        }
    }

    @Test
    void switchingVehiclesRestoresOldVehicleAndKeepsItsOtherRiders() {
        try (Fixture fixture = new Fixture()) {
            WrapperEntity first = fixture.passenger(11, fixture.player1Id);
            WrapperEntity second = fixture.passenger(12, fixture.player1Id);
            Entity otherVehicle = fixture.vehicle(2);
            fixture.manager.update(fixture.vehicle, List.of(first, second), true);
            fixture.sent.clear();
            fixture.manager.update(otherVehicle, List.of(first), true);
            assertEquals(2, fixture.sent.size());
            fixture.assertPacket(fixture.player1Id, 1, 99, 12);
            fixture.assertPacket(fixture.player1Id, 2, 11);
        }
    }

    @Test
    void removingFakeEntityKeepsTheRemainingRider() {
        try (Fixture fixture = new Fixture()) {
            WrapperEntity first = fixture.passenger(11, fixture.player1Id);
            WrapperEntity second = fixture.passenger(12, fixture.player1Id);
            fixture.manager.update(fixture.vehicle, List.of(first, second), true);
            fixture.manager.remove(first);
            fixture.assertLast(fixture.player1Id, 1, 99, 12);
            fixture.manager.clear();
            fixture.assertLast(fixture.player1Id, 1, 99);
        }
    }

    @Test
    void removingViewerDoesNotChangeOtherViewersMounts() {
        try (Fixture fixture = new Fixture()) {
            WrapperEntity shared = fixture.passenger(11, fixture.player1Id, fixture.player2Id);
            fixture.manager.update(fixture.vehicle, List.of(shared), true);
            fixture.sent.clear();
            fixture.manager.removeViewer(shared, fixture.player1Id);
            assertEquals(1, fixture.sent.size());
            fixture.assertLast(fixture.player1Id, 1, 99);
            fixture.manager.clear();
            fixture.assertLast(fixture.player2Id, 1, 99);
        }
    }

    @Test
    void discardedViewerAndVehicleStateDoesNotBringBackOldRiders() {
        try (Fixture fixture = new Fixture()) {
            WrapperEntity first = fixture.passenger(11, fixture.player1Id);
            WrapperEntity second = fixture.passenger(12, fixture.player1Id);
            fixture.manager.update(fixture.vehicle, List.of(first), true);
            fixture.manager.forgetViewer(fixture.player1Id);
            fixture.manager.update(fixture.vehicle, List.of(second), true);
            fixture.assertLast(fixture.player1Id, 1, 99, 12);
            fixture.manager.forgetVehicle(fixture.vehicle);
            fixture.manager.update(fixture.vehicle, List.of(first), true);
            fixture.assertLast(fixture.player1Id, 1, 99, 11);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final RealEntityPassengerManager manager = new RealEntityPassengerManager();
        final UUID player1Id = UUID.randomUUID();
        final UUID player2Id = UUID.randomUUID();
        final World world = mock(World.class);
        final Entity vehicle = vehicle(1);
        final List<Sent> sent = new ArrayList<>();
        final MockedStatic<Bukkit> bukkit;
        final MockedStatic<PacketEvents> packets;

        Fixture() {
            Player player1 = player(player1Id);
            Player player2 = player(player2Id);
            Entity realPassenger = mock(Entity.class);
            when(realPassenger.getEntityId()).thenReturn(99);
            when(vehicle.getPassengers()).thenReturn(List.of(realPassenger));
            PacketEventsAPI<?> api = mock(PacketEventsAPI.class, RETURNS_DEEP_STUBS);
            var settings = PacketEvents.getAPI().getSettings();
            var version = PacketEvents.getAPI().getServerManager().getVersion();
            when(api.getSettings()).thenReturn(settings);
            when(api.getServerManager().getVersion()).thenReturn(version);
            var playerManager = api.getPlayerManager();
            doAnswer(call -> {
                Player viewer = call.getArgument(0);
                WrapperPlayServerSetPassengers packet = call.getArgument(1);
                sent.add(new Sent(viewer.getUniqueId(), packet.getEntityId(), packet.getPassengers()));
                return null;
            }).when(playerManager).sendPacket(any(Player.class), any(PacketWrapper.class));
            bukkit = mockStatic(Bukkit.class);
            packets = mockStatic(PacketEvents.class);
            bukkit.when(() -> Bukkit.getPlayer(player1Id)).thenReturn(player1);
            bukkit.when(() -> Bukkit.getPlayer(player2Id)).thenReturn(player2);
            packets.when(PacketEvents::getAPI).thenReturn(api);
        }

        Player player(UUID id) {
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(id);
            when(player.isOnline()).thenReturn(true);
            when(player.getWorld()).thenReturn(world);
            return player;
        }

        Entity vehicle(int id) {
            Entity vehicle = mock(Entity.class);
            when(vehicle.getEntityId()).thenReturn(id);
            when(vehicle.getUniqueId()).thenReturn(UUID.randomUUID());
            when(vehicle.getWorld()).thenReturn(world);
            return vehicle;
        }

        WrapperEntity passenger(int id, UUID... viewers) {
            WrapperEntity passenger = mock(WrapperEntity.class);
            when(passenger.getEntityId()).thenReturn(id);
            when(passenger.getViewers()).thenReturn(Set.of(viewers));
            when(passenger.isSpawned()).thenReturn(true);
            return passenger;
        }

        void assertLast(UUID viewer, int vehicle, int... passengers) {
            Sent packet = sent.getLast();
            assertEquals(viewer, packet.viewer());
            assertEquals(vehicle, packet.vehicle());
            assertArrayEquals(passengers, packet.passengers());
        }

        void assertPacket(UUID viewer, int vehicle, int... passengers) {
            Sent packet = sent.stream().filter(value -> value.viewer().equals(viewer) && value.vehicle() == vehicle).findFirst().orElseThrow();
            assertArrayEquals(passengers, packet.passengers());
        }

        @Override
        public void close() {
            packets.close();
            bukkit.close();
        }
    }
}
