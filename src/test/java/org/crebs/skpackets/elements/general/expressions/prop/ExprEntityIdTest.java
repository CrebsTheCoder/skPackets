package org.crebs.skpackets.elements.general.expressions.prop;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExprEntityIdTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 42, Integer.MAX_VALUE})
    void returnsPlayerEntityId(int id) {
        Player player = mock(Player.class);
        when(player.getEntityId()).thenReturn(id);
        assertEquals(id, new ExprEntityId().convert(player));
    }

    @Test
    void supportsNonPlayerEntities() {
        Entity entity = mock(Entity.class);
        when(entity.getEntityId()).thenReturn(73);
        assertEquals(73, new ExprEntityId().convert(entity));
    }

    @Test
    void readsEachPlayersOwnId() {
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        when(player1.getEntityId()).thenReturn(12);
        when(player2.getEntityId()).thenReturn(34);
        ExprEntityId expression = new ExprEntityId();
        assertEquals(12, expression.convert(player1));
        assertEquals(34, expression.convert(player2));
        assertEquals(12, expression.convert(player1));
    }
}
