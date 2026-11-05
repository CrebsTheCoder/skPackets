package org.crebs.skpackets.elements.team.expressions;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.api.team.PacketTeamManager;
import org.crebs.skpackets.elements.team.PacketTeam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockMakers;

class ExprPacketTeamTest {
    @Test
    void looksUpTeamsAndOmitsUnassignedEntities() {
        var plugin = mock(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE));
        var manager = mock(PacketTeamManager.class, withSettings().mockMaker(MockMakers.INLINE));
        var team = mock(PacketTeam.class, withSettings().mockMaker(MockMakers.INLINE));
        var player1 = player("player1");
        var player2 = player("player2");
        when(plugin.getPacketTeams()).thenReturn(manager);
        when(manager.get("player1")).thenReturn(team);
        try (var plugins = mockStatic(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE))) {
            plugins.when(SkPackets::getInstance).thenReturn(plugin);
            assertArrayEquals(new PacketTeam[]{team}, expression(player1, player2).get(null, new Object[]{player1, player2}));
        }
    }

    @Test
    void deleteAndResetClearAssignmentsButSetIsUnsupported() {
        var plugin = mock(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE));
        var manager = mock(PacketTeamManager.class, withSettings().mockMaker(MockMakers.INLINE));
        when(plugin.getPacketTeams()).thenReturn(manager);
        try (var plugins = mockStatic(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE))) {
            plugins.when(SkPackets::getInstance).thenReturn(plugin);
            var expression = expression(player("player1"));
            expression.change(null, null, ChangeMode.DELETE);
            expression.change(null, null, ChangeMode.RESET);
            expression.change(null, new Object[]{mock(PacketTeam.class, withSettings().mockMaker(MockMakers.INLINE))}, ChangeMode.SET);
            verify(manager, times(2)).assign("player1", null);
            verifyNoMoreInteractions(manager);
        }
    }

    @Test
    void advertisesOnlyDeletionAndReset() {
        var expression = new ExprPacketTeam();
        for (var mode : ChangeMode.values()) {
            if (mode == ChangeMode.DELETE || mode == ChangeMode.RESET) assertArrayEquals(new Class<?>[]{PacketTeam.class}, expression.acceptChange(mode));
            else assertNull(expression.acceptChange(mode));
        }
        assertEquals(PacketTeam.class, expression.getReturnType());
    }

    private static Player player(String name) {
        var player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        return player;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ExprPacketTeam expression(Object... entities) {
        Expression source = mock(Expression.class);
        when(source.getArray(null)).thenReturn(entities);
        when(source.isSingle()).thenReturn(entities.length == 1);
        var expression = new ExprPacketTeam();
        var expressions = new Expression<?>[]{source};
        assertTrue(expression.init(expressions, 0, Kleenean.FALSE, new ParseResult("", expressions)));
        assertEquals(entities.length == 1, expression.isSingle());
        return expression;
    }
}

