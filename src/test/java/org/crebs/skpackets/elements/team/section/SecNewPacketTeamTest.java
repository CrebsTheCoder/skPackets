package org.crebs.skpackets.elements.team.section;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.crebs.skpackets.elements.team.TeamField;
import org.crebs.skpackets.elements.team.PacketTeam;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.api.team.PacketTeamManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecNewPacketTeamTest {
    @Test
    void createsAndTracksATeamFromEvaluatedEntries() throws Exception {
        var section = new SecNewPacketTeam();
        fields(section).put(TeamField.NAME, value("red"));
        var team = mock(PacketTeam.class, withSettings().mockMaker(MockMakers.INLINE));
        var plugin = mock(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE));
        var manager = mock(PacketTeamManager.class, withSettings().mockMaker(MockMakers.INLINE));
        when(plugin.getPacketTeams()).thenReturn(manager);
        try (var teams = mockStatic(TeamField.class, withSettings().mockMaker(MockMakers.INLINE));
             var plugins = mockStatic(SkPackets.class, withSettings().mockMaker(MockMakers.INLINE))) {
            teams.when(() -> TeamField.create("red")).thenReturn(team);
            plugins.when(SkPackets::getInstance).thenReturn(plugin);
            assertArrayEquals(new PacketTeam[]{team}, section.get(null));
            verify(team).setTeamName("red");
            verify(manager).track(team);
        }
    }

    @Test
    void requiresASection() {
        try (var skript = mockStatic(Skript.class, withSettings().mockMaker(MockMakers.INLINE))) {
            assertFalse(new SecNewPacketTeam().init(new Expression<?>[0], 0, Kleenean.FALSE,
                    new ParseResult("", new Expression<?>[0]), null, List.of()));
            skript.verify(() -> Skript.error("A new packet team requires a section with a name entry."));
        }
    }

    @Test
    void missingRuntimeNameProducesNoPacket() throws Exception {
        var section = new SecNewPacketTeam();
        fields(section).put(TeamField.NAME, value(null));
        assertEquals(0, section.get(null).length);
        assertTrue(section.isSingle());
        assertEquals("new packet team", section.toString(null, false));
    }

    @Test
    void invalidRuntimeNameIsReported() throws Exception {
        var section = new SecNewPacketTeam();
        fields(section).put(TeamField.NAME, value("x".repeat(17)));
        try (var skript = mockStatic(Skript.class, withSettings().mockMaker(MockMakers.INLINE))) {
            assertEquals(0, section.get(null).length);
            skript.verify(() -> Skript.error("Team name must contain 1 to 16 characters."));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<TeamField, Expression<?>> fields(SecNewPacketTeam section) throws Exception {
        var field = SecNewPacketTeam.class.getDeclaredField("fields");
        field.setAccessible(true);
        return (Map<TeamField, Expression<?>>) field.get(section);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Expression<?> value(Object value) {
        Expression expression = mock(Expression.class);
        when(expression.getSingle(null)).thenReturn(value);
        return expression;
    }
}
