package org.crebs.skpackets.elements.entity.expressions.prop;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockMakers.INLINE;

class VisibleFakeEntitiesTest {
    @Test
    @SuppressWarnings("unchecked")
    void returnsUnionOfEntitiesVisibleToAnySourcePlayerWithoutDuplicates() {
        Player first = mock(Player.class), second = mock(Player.class);
        UUID firstId = UUID.randomUUID(), secondId = UUID.randomUUID();
        when(first.getUniqueId()).thenReturn(firstId);
        when(second.getUniqueId()).thenReturn(secondId);
        WrapperEntity shared = mock(WrapperEntity.class), privateEntity = mock(WrapperEntity.class), hidden = mock(WrapperEntity.class);
        when(shared.getViewers()).thenReturn(Set.of(firstId, secondId));
        when(privateEntity.getViewers()).thenReturn(Set.of(secondId));
        when(hidden.getViewers()).thenReturn(Set.of(UUID.randomUUID()));
        EntityLib lib = mock(EntityLib.class, withSettings().mockMaker(INLINE));
        when(lib.getEntities()).thenReturn(List.of(shared, privateEntity, hidden));
        Expression<Player> source = mock(Expression.class);
        Expression<?>[] inputs = {source};
        var expression = new ExprVisibleFakeEntities();
        assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        try (var library = mockStatic(EntityLib.class, withSettings().mockMaker(INLINE))) {
            library.when(EntityLib::get).thenReturn(lib);
            assertArrayEquals(new WrapperEntity[]{shared, privateEntity}, expression.get(null, new Player[]{first, second}));
            assertArrayEquals(new WrapperEntity[]{shared}, expression.get(null, new Player[]{first}));
            assertArrayEquals(new WrapperEntity[0], expression.get(null, new Player[0]));
        }
        assertFalse(expression.isSingle());
        assertEquals(WrapperEntity.class, expression.getReturnType());
    }
}
