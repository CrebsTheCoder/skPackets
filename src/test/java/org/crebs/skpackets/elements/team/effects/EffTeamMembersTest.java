package org.crebs.skpackets.elements.team.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Player;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.api.team.PacketTeamManager;
import org.crebs.skpackets.elements.team.PacketTeam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockMakers;

class EffTeamMembersTest {
    @Test
    void addAndRemoveResolvePlayersAndStrings() {
        var plugin = mock(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE));
        var manager = mock(PacketTeamManager.class, withSettings().mockMaker(MockMakers.INLINE));
        var team = mock(PacketTeam.class, withSettings().mockMaker(MockMakers.INLINE));
        var player = mock(Player.class);
        when(player.getName()).thenReturn("player1");
        when(plugin.getPacketTeams()).thenReturn(manager);
        try (var plugins = mockStatic(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE))) {
            plugins.when(SkPackets::getInstance).thenReturn(plugin);
            effect(0, team, player, "player2").execute(null);
            verify(manager).assign("player1", team);
            verify(manager).assign("player2", team);
            effect(1, team, player, "player2").execute(null);
            verify(manager).remove("player1", team);
            verify(manager).remove("player2", team);
        }
    }

    @Test
    void missingTeamDoesNothing() {
        try (var plugins = mockStatic(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE))) {
            effect(0, null, "player1").execute(null);
            plugins.verifyNoInteractions();
        }
    }

    @Test
    void rejectsNonTeamPackets() {
        try (var skript = mockStatic(Skript.class, withSettings().mockMaker(MockMakers.INLINE)); var plugins = mockStatic(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE))) {
            effect(0, mock(PacketWrapper.class), "player1").execute(null);
            skript.verify(() -> Skript.error("Use a team made with 'a new fake team' or 'a new packet team'."));
            plugins.verifyNoInteractions();
        }
    }

    @Test
    void invalidEntriesDoNotPreventLaterValidEntries() {
        var plugin = mock(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE));
        var manager = mock(PacketTeamManager.class, withSettings().mockMaker(MockMakers.INLINE));
        var team = mock(PacketTeam.class, withSettings().mockMaker(MockMakers.INLINE));
        when(plugin.getPacketTeams()).thenReturn(manager);
        try (var plugins = mockStatic(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE)); var skript = mockStatic(Skript.class, withSettings().mockMaker(MockMakers.INLINE))) {
            plugins.when(SkPackets::getInstance).thenReturn(plugin);
            effect(0, team, "", "x".repeat(41), "player1").execute(null);
            verify(manager).assign("player1", team);
            verifyNoMoreInteractions(manager);
            skript.verify(() -> Skript.error("Team entries must contain 1 to 40 characters."), times(2));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EffTeamMembers effect(int pattern, PacketWrapper<?> packet, Object... entries) {
        Expression members = mock(Expression.class);
        Expression team = mock(Expression.class);
        when(members.getArray(null)).thenReturn(entries);
        when(team.getSingle(null)).thenReturn(packet);
        var effect = new EffTeamMembers();
        var expressions = new Expression<?>[]{members, team};
        assertTrue(effect.init(expressions, pattern, Kleenean.FALSE, new ParseResult("", expressions)));
        return effect;
    }
}

