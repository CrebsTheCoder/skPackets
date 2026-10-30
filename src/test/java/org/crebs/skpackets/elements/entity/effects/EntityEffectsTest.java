package org.crebs.skpackets.elements.entity.effects;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.api.entity.PacketEntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EntityEffectsTest {
    private final Event event = mock(Event.class);

    @org.junit.jupiter.api.BeforeAll
    static void initializePacketEvents() {
        org.crebs.skpackets.support.PacketEventsTestBootstrap.initialize();
    }

    @SuppressWarnings("unchecked")
    private <T> Expression<T> input(T... values) {
        Expression<T> expression = mock(Expression.class);
        when(expression.getArray(event)).thenReturn(values);
        when(expression.getAll(event)).thenReturn(values);
        when(expression.getSingle(event)).thenReturn(values.length == 0 ? null : values[0]);
        return expression;
    }

    @Test
    void killingRemovesEveryEntityFromPersistenceManager() {
        WrapperEntity first = mock(WrapperEntity.class), second = mock(WrapperEntity.class);
        var effect = new EffKillPacketEntity();
        Expression<?>[] inputs = {input(first, second)};
        assertTrue(effect.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        PacketEntityManager manager = mock(PacketEntityManager.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        when(plugin.getPacketEntities()).thenReturn(manager);
        try (var staticPlugin = mockStatic(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE))) {
            staticPlugin.when(SkPackets::getInstance).thenReturn(plugin);
            effect.execute(event);
            verify(manager).remove(first);
            verify(manager).remove(second);
            verifyNoMoreInteractions(manager);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void viewerPatternsUpdateEveryEntity(int pattern) {
        WrapperEntity first = mock(WrapperEntity.class), second = mock(WrapperEntity.class);
        Player viewer = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(viewer.getUniqueId()).thenReturn(id);
        Expression<?>[] inputs = {input(viewer), input(first, second)};
        var effect = new EffPacketEntityViewers();
        assertTrue(effect.init(inputs, pattern, Kleenean.FALSE, new ParseResult("", inputs)));
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        PacketEntityManager manager = mock(PacketEntityManager.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        when(plugin.getPacketEntities()).thenReturn(manager);
        try (var staticPlugin = mockStatic(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE))) {
            staticPlugin.when(SkPackets::getInstance).thenReturn(plugin);
            effect.execute(event);
            for (WrapperEntity entity : new WrapperEntity[]{first, second}) {
                if (pattern == 0) verify(manager).addViewer(entity, viewer);
                else verify(manager).removeViewer(entity, id);
            }
            verifyNoMoreInteractions(manager);
        }
    }

    @Test
    void removingViewerSupportsOfflinePlayers() {
        OfflinePlayer viewer = mock(OfflinePlayer.class);
        UUID id = UUID.randomUUID();
        when(viewer.getUniqueId()).thenReturn(id);
        WrapperEntity entity = mock(WrapperEntity.class);
        Expression<?>[] inputs = {input(viewer), input(entity)};
        var effect = new EffPacketEntityViewers();
        assertTrue(effect.init(inputs, 1, Kleenean.FALSE, new ParseResult("", inputs)));
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        PacketEntityManager manager = mock(PacketEntityManager.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        when(plugin.getPacketEntities()).thenReturn(manager);
        try (var staticPlugin = mockStatic(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE))) {
            staticPlugin.when(SkPackets::getInstance).thenReturn(plugin);
            effect.execute(event);
            verify(manager).removeViewer(entity, id);
        }
    }

    @Test
    void teleportMovesEveryEntityToTheEvaluatedLocation() {
        WrapperEntity first = mock(WrapperEntity.class), second = mock(WrapperEntity.class);
        Location target = new Location(null, 1, 20, -3);
        Expression<?>[] inputs = {input(first, second), input(target)};
        var effect = new EffTeleportPacketEntity();
        assertTrue(effect.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        PacketEntityManager manager = mock(PacketEntityManager.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        when(plugin.getPacketEntities()).thenReturn(manager);
        try (var staticPlugin = mockStatic(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE))) {
            staticPlugin.when(SkPackets::getInstance).thenReturn(plugin);
            effect.execute(event);
            verify(manager).teleport(first, target);
            verify(manager).teleport(second, target);
        }
    }

    @Test
    void missingTeleportLocationDoesNotAccessPlugin() {
        Expression<?>[] inputs = {input(mock(WrapperEntity.class)), input(new Location[0])};
        var effect = new EffTeleportPacketEntity();
        assertTrue(effect.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        try (var staticPlugin = mockStatic(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE))) {
            effect.execute(event);
            staticPlugin.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void ridingFakeVehicleUpdatesEachPassenger(int pattern) {
        WrapperEntity vehicle = mock(WrapperEntity.class);
        WrapperEntity first = mock(WrapperEntity.class), second = mock(WrapperEntity.class);
        Expression<?>[] inputs = {input(first, second), input(vehicle)};
        var effect = new EffRideFakeEntity();
        assertTrue(effect.init(inputs, pattern, Kleenean.FALSE, new ParseResult("", inputs)));
        effect.execute(event);
        for (WrapperEntity passenger : new WrapperEntity[]{first, second}) {
            if (pattern == 0) verify(vehicle).addPassenger(passenger);
            else verify(vehicle).removePassenger(passenger);
        }
        verifyNoMoreInteractions(vehicle);
    }

    @Test
    void missingVehicleDoesNotChangePassengers() {
        WrapperEntity passenger = mock(WrapperEntity.class);
        Expression<?>[] inputs = {input(passenger), input(new Object[0])};
        var effect = new EffRideFakeEntity();
        assertTrue(effect.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        effect.execute(event);
        verifyNoInteractions(passenger);
    }

    @Test
    void spawnWithMissingLocationDoesNothing() {
        WrapperEntity entity = mock(WrapperEntity.class);
        Expression<?>[] inputs = {input(entity), input(new Location[0])};
        var effect = new EffSpawnFakeEntity();
        assertTrue(effect.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        effect.execute(event);
        verifyNoInteractions(entity);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void spawningTeleportsManagedEntitiesAndRestoresOnlyOnlineViewers(boolean managed) {
        WrapperEntity entity = mock(WrapperEntity.class);
        when(entity.getEntityId()).thenReturn(15);
        UUID online = UUID.randomUUID(), offline = UUID.randomUUID();
        when(entity.getViewers()).thenReturn(Set.of(online, offline));
        Player viewer = mock(Player.class);
        Location target = new Location(null, 4, 50, 6);
        Expression<?>[] inputs = {input(entity), input(target)};
        var effect = new EffSpawnFakeEntity();
        assertTrue(effect.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        PacketEntityManager manager = mock(PacketEntityManager.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        when(plugin.getPacketEntities()).thenReturn(manager);
        when(manager.getEntity(15)).thenReturn(managed ? Optional.of(entity) : Optional.empty());
        try (var staticPlugin = mockStatic(SkPackets.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
             var conversion = mockStatic(SpigotConversionUtil.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
             var bukkit = mockStatic(Bukkit.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE))) {
            staticPlugin.when(SkPackets::getInstance).thenReturn(plugin);
            bukkit.when(() -> Bukkit.getPlayer(online)).thenReturn(viewer);
            effect.execute(event);
            verify(manager).getEntity(15);
            if (managed) verify(manager).teleport(entity, target);
            else verify(manager).spawn(entity, target, List.of(viewer), true);
            verifyNoMoreInteractions(manager);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void ridingRealVehiclePreservesItsServerPassengers(int pattern) {
        Entity vehicle = mock(Entity.class), realPassenger = mock(Entity.class);
        WrapperEntity fakePassenger = mock(WrapperEntity.class);
        when(vehicle.getEntityId()).thenReturn(1);
        when(realPassenger.getEntityId()).thenReturn(2);
        when(fakePassenger.getEntityId()).thenReturn(3);
        when(vehicle.getPassengers()).thenReturn(List.of(realPassenger));
        Expression<?>[] inputs = {input(fakePassenger), input(vehicle)};
        var effect = new EffRideFakeEntity();
        assertTrue(effect.init(inputs, pattern, Kleenean.FALSE, new ParseResult("", inputs)));
        effect.execute(event);
        var packet = ArgumentCaptor.forClass(WrapperPlayServerSetPassengers.class);
        verify(fakePassenger).sendPacketToViewers(packet.capture());
        assertEquals(1, packet.getValue().getEntityId());
        assertArrayEquals(pattern == 0 ? new int[]{2, 3} : new int[]{2}, packet.getValue().getPassengers());
        verify(vehicle, never()).addPassenger(any());
        verify(vehicle, never()).removePassenger(any());
    }
}


