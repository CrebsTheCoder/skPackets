package org.crebs.skpackets.support;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class PacketEventsTestBootstrap {
    private PacketEventsTestBootstrap() {}

    public static synchronized void initialize() {
        if (PacketEvents.getAPI() != null && PacketEvents.getAPI().getSettings() != null) return;
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class);
        PacketEventsSettings settings = new PacketEventsSettings();
        ServerManager server = mock(ServerManager.class);
        when(api.getSettings()).thenReturn(settings);
        when(api.getServerManager()).thenReturn(server);
        when(server.getVersion()).thenReturn(ServerVersion.V_26_3);
        PacketEvents.setAPI(api);
    }
}
