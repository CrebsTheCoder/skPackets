package org.crebs.skpackets.api.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.util.MojangAPIUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.crebs.skpackets.api.general.PlayerSkinRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkinManagerTest {
    @Test
    void missingPlayerOrTexturesReturnNoSkin() {
        try (var bukkit = mockStatic(Bukkit.class); var mojang = mockStatic(MojangAPIUtil.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            assertNull(SkinManager.getOfflinePlayer(null));
            assertNull(SkinManager.getOfflinePlayer(" "));
            assertNull(SkinManager.getOfflinePlayer("missingPlayer"));
            mojang.verify(() -> MojangAPIUtil.requestPlayerTextureProperties(null), never());
            UUID id = UUID.randomUUID();
            mojang.when(() -> MojangAPIUtil.requestPlayerUUID("noTextures")).thenReturn(id);
            mojang.when(() -> MojangAPIUtil.requestPlayerTextureProperties(id)).thenReturn(List.of());
            assertNull(SkinManager.getOfflinePlayer("noTextures"));
        }
    }

    @Test
    void onlinePlayerWithoutTexturesAndUncachedOriginalSkinReturnNoSkin() {
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class, RETURNS_DEEP_STUBS);
        Player player = mock(Player.class);
        when(api.getPlayerManager().getUser(player).getProfile().getTextureProperties()).thenReturn(List.of());
        try (var packets = mockStatic(PacketEvents.class)) {
            packets.when(PacketEvents::getAPI).thenReturn(api);
            assertNull(SkinManager.getPlayer(player));
        }
        assertNull(PlayerSkinRegistry.getSkin(UUID.randomUUID()));
    }
}
