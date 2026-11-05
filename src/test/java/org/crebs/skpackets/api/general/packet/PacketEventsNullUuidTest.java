package org.crebs.skpackets.api.general.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.crebs.skpackets.api.simple.PlayerSkinRefresh;
import org.crebs.skpackets.api.simple.FakeInventoryManager;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.crebs.skpackets.api.general.EntityTracker;
import org.crebs.skpackets.api.simple.ChatSessionListener;
import org.crebs.skpackets.api.simple.FakeBlockManager;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketEventsNullUuidTest {
    @BeforeAll
    static void initializePacketEvents() {
        PacketEventsTestBootstrap.initialize();
    }

    @SuppressWarnings("DataFlowIssue")
    private User connectingUser() throws ReflectiveOperationException {
        User user = mock(User.class, CALLS_REAL_METHODS);
        var profile = User.class.getDeclaredField("profile");
        profile.setAccessible(true);
        profile.set(user, new UserProfile(null, null));
        return user;
    }

    @Test
    void unassignedPacketEventsProfileCanDisconnectWithoutCrashingCaches() throws ReflectiveOperationException {
        User user = connectingUser();
        assertNull(user.getUUID());
        UserDisconnectEvent disconnect = new UserDisconnectEvent(user);
        assertDoesNotThrow(() -> new ChatSessionListener().onUserDisconnect(disconnect));
        assertDoesNotThrow(() -> new EntityTracker().onUserDisconnect(disconnect));
    }

    @Test
    void skinRefreshIgnoresPacketsWithoutAssignedUuid() throws ReflectiveOperationException {
        User user = connectingUser();
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class, RETURNS_DEEP_STUBS);
        try (var packets = mockStatic(PacketEvents.class); var bukkit = mockStatic(Bukkit.class)) {
            packets.when(PacketEvents::getAPI).thenReturn(api);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            FakeInventoryManager inventory = new FakeInventoryManager(mock(JavaPlugin.class));
            PacketSendEvent inventorySent = mock(PacketSendEvent.class);
            when(inventorySent.getUser()).thenReturn(user);
            when(inventorySent.getPacketType()).thenReturn(PacketType.Play.Server.WINDOW_ITEMS);
            assertDoesNotThrow(() -> inventory.onPacketSend(inventorySent));
            PacketReceiveEvent inventoryReceived = mock(PacketReceiveEvent.class);
            when(inventoryReceived.getUser()).thenReturn(user);
            when(inventoryReceived.getPacketType()).thenReturn(PacketType.Play.Client.CLICK_WINDOW);
            assertDoesNotThrow(() -> inventory.onPacketReceive(inventoryReceived));
            inventory.close();
            PlayerSkinRefresh refresh = new PlayerSkinRefresh(mock(JavaPlugin.class));
            try {
                PacketSendEvent sent = mock(PacketSendEvent.class);
                when(sent.getUser()).thenReturn(user);
                when(sent.getPacketType()).thenReturn(PacketType.Play.Server.JOIN_GAME);
                assertDoesNotThrow(() -> refresh.onPacketSend(sent));
                PacketReceiveEvent received = mock(PacketReceiveEvent.class);
                when(received.getUser()).thenReturn(user);
                when(received.getPacketType()).thenReturn(PacketType.Play.Client.TELEPORT_CONFIRM);
                assertDoesNotThrow(() -> refresh.onPacketReceive(received));
            } finally {
                refresh.close();
            }
        }
    }

    @Test
    void packetListenersIgnoreUnassignedProfilesBeforeLookingUpUuidMaps() throws ReflectiveOperationException {
        User user = connectingUser();
        PacketSendEvent event = mock(PacketSendEvent.class);
        when(event.getUser()).thenReturn(user);
        assertDoesNotThrow(() -> new EntityTracker().onPacketSend(event));
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class, RETURNS_DEEP_STUBS);
        PluginManager plugins = mock(PluginManager.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        try (var packets = mockStatic(PacketEvents.class); var bukkit = mockStatic(Bukkit.class)) {
            packets.when(PacketEvents::getAPI).thenReturn(api);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            FakeBlockManager manager = new FakeBlockManager(plugin);
            assertDoesNotThrow(() -> manager.onPacketSend(event));
        }
    }
}
