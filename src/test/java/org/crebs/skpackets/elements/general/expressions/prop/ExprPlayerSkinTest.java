package org.crebs.skpackets.elements.general.expressions.prop;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.crebs.skpackets.api.entity.Skin;
import org.crebs.skpackets.api.general.PlayerSkinRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.MockMakers.INLINE;
import static org.mockito.Mockito.*;

class ExprPlayerSkinTest {
    @Test
    void readsOriginalSkinByPlayerUuid() {
        Server server = mock(Server.class, RETURNS_DEEP_STUBS);
        when(server.getServerConfig().isProxyOnlineMode()).thenReturn(true);
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        Skin skin = new Skin("texture", "signature");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, withSettings().mockMaker(INLINE));
             MockedStatic<PlayerSkinRegistry> registry = mockStatic(PlayerSkinRegistry.class, withSettings().mockMaker(INLINE))) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            registry.when(() -> PlayerSkinRegistry.getSkin(uuid)).thenReturn(skin);
            ExprPlayerSkin expression = new ExprPlayerSkin();
            assertSame(skin, expression.convert(player));
            assertSame(Skin.class, expression.getReturnType());
            registry.verify(() -> PlayerSkinRegistry.getSkin(uuid));
        }
    }
}
