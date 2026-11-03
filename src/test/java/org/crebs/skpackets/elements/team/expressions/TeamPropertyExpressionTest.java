package org.crebs.skpackets.elements.team.expressions;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.SkriptColor;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import org.crebs.skpackets.elements.team.TeamField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockMakers;

class TeamPropertyExpressionTest {
    record Property(TeamPropertyExpression<?> expression, TeamField field, Object value) {}

    static Stream<Property> properties() {
        return Stream.of(
                new Property(new ExprTeamName(), TeamField.NAME, "red"),
                new Property(new ExprTeamEntries(), TeamField.ENTRIES, new String[]{"player1", "player2"}),
                new Property(new ExprTeamDisplayName(), TeamField.DISPLAY_NAME, "Red Team"),
                new Property(new ExprTeamPrefix(), TeamField.PREFIX, "[Red] "),
                new Property(new ExprTeamSuffix(), TeamField.SUFFIX, "!"),
                new Property(new ExprTeamColor(), TeamField.COLOR, SkriptColor.LIGHT_RED),
                new Property(new ExprTeamCollisionRule(), TeamField.COLLISION_RULE, "never"),
                new Property(new ExprTeamNameTagVisibility(), TeamField.NAME_TAG_VISIBILITY, "never"),
                new Property(new ExprTeamFriendlyFire(), TeamField.FRIENDLY_FIRE, true),
                new Property(new ExprTeamSeeInvisible(), TeamField.SEE_INVISIBLE, true));
    }

    @ParameterizedTest
    @MethodSource("properties")
    void readsAndWritesTheCorrectField(Property property) {
        WrapperPlayServerTeams packet = packet();
        var expression = initialize(property.expression(), packet);
        Object[] delta = property.value() instanceof String[] entries ? entries : new Object[]{property.value()};
        expression.change(null, delta, ChangeMode.SET);

        Object[] expected = property.value() instanceof String[] entries ? entries : new Object[]{property.value()};
        assertArrayEquals(expected, expression.get(null, new PacketWrapper<?>[]{packet}));
        assertEquals(property.field().elementType(), expression.getReturnType());
        assertEquals(property.field() != TeamField.ENTRIES, expression.isSingle());
        assertTrue(expression.toString(null, false).contains(property.field().key()));
    }

    @ParameterizedTest
    @MethodSource("properties")
    void advertisesOnlySupportedChanges(Property property) {
        var expression = property.expression();
        for (ChangeMode mode : ChangeMode.values()) {
            boolean entriesChange = property.field() == TeamField.ENTRIES
                    && (mode == ChangeMode.ADD || mode == ChangeMode.REMOVE || mode == ChangeMode.DELETE || mode == ChangeMode.RESET);
            if (mode == ChangeMode.SET) assertArrayEquals(new Class<?>[]{property.field().type()}, expression.acceptChange(mode));
            else if (entriesChange) assertArrayEquals(new Class<?>[]{String[].class}, expression.acceptChange(mode));
            else assertNull(expression.acceptChange(mode));
        }
    }

    @ParameterizedTest
    @MethodSource("properties")
    void ignoresOtherPacketTypesAndHandlesEmptySources(Property property) {
        PacketWrapper<?> other = mock(PacketWrapper.class);
        var expression = initialize(property.expression(), other);
        assertEquals(0, expression.get(null, new PacketWrapper<?>[]{other}).length);
        assertEquals(0, expression.get(null, new PacketWrapper<?>[0]).length);
        expression.change(null, new Object[]{property.value()}, ChangeMode.SET);
        verifyNoInteractions(other);
    }

    @Test
    void entriesChangesPreserveOrderAndRemoveDuplicates() {
        var packet = packet();
        var expression = initialize(new ExprTeamEntries(), packet);
        expression.change(null, new Object[]{"player1", "player2", "player1"}, ChangeMode.SET);
        expression.change(null, new Object[]{"player2", "player3"}, ChangeMode.ADD);
        assertArrayEquals(new String[]{"player1", "player2", "player3"}, expression.get(null, new PacketWrapper<?>[]{packet}));
        expression.change(null, new Object[]{"player2", "player4"}, ChangeMode.REMOVE);
        assertArrayEquals(new String[]{"player1", "player3"}, expression.get(null, new PacketWrapper<?>[]{packet}));
        expression.change(null, null, ChangeMode.RESET);
        assertEquals(0, expression.get(null, new PacketWrapper<?>[]{packet}).length);
        expression.change(null, new Object[]{"player1"}, ChangeMode.ADD);
        expression.change(null, null, ChangeMode.DELETE);
        assertEquals(0, expression.get(null, new PacketWrapper<?>[]{packet}).length);
    }

    @Test
    void optionFlagsDoNotOverwriteEachOther() {
        var packet = packet();
        var friendly = initialize(new ExprTeamFriendlyFire(), packet);
        var invisible = initialize(new ExprTeamSeeInvisible(), packet);
        friendly.change(null, new Object[]{true}, ChangeMode.SET);
        invisible.change(null, new Object[]{true}, ChangeMode.SET);
        friendly.change(null, new Object[]{false}, ChangeMode.SET);
        assertArrayEquals(new Boolean[]{false}, friendly.get(null, new PacketWrapper<?>[]{packet}));
        assertArrayEquals(new Boolean[]{true}, invisible.get(null, new PacketWrapper<?>[]{packet}));
    }

    @Test
    void missingTeamInfoProducesNoValuesAndCanBeInitializedByAChange() {
        var packet = packet();
        when(packet.getTeamInfo()).thenReturn(Optional.empty());
        var expression = initialize(new ExprTeamPrefix(), packet);
        assertEquals(0, expression.get(null, new PacketWrapper<?>[]{packet}).length);
        var info = teamInfo();
        try (var defaults = mockStatic(TeamField.class)) {
            defaults.when(() -> TeamField.defaults("original")).thenReturn(info);
            expression.change(null, new Object[]{"prefix"}, ChangeMode.SET);
            defaults.verify(() -> TeamField.defaults("original"));
        }
        assertArrayEquals(new String[]{"prefix"}, expression.get(null, new PacketWrapper<?>[]{packet}));
    }

    @Test
    void scalarPropertiesIgnoreMissingDeltaAndUnsupportedModes() {
        var packet = packet();
        var expression = initialize(new ExprTeamName(), packet);
        expression.change(null, null, ChangeMode.SET);
        expression.change(null, new Object[0], ChangeMode.SET);
        expression.change(null, new Object[]{"new"}, ChangeMode.ADD);
        assertArrayEquals(new String[]{"original"}, expression.get(null, new PacketWrapper<?>[]{packet}));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends TeamPropertyExpression<?>> T initialize(T expression, PacketWrapper<?> packet) {
        Expression source = mock(Expression.class);
        when(source.getArray(null)).thenReturn(new PacketWrapper<?>[]{packet});
        when(source.isSingle()).thenReturn(true);
        when(source.toString(null, false)).thenReturn("source");
        assertTrue(expression.init(new Expression<?>[]{source}, 0, Kleenean.FALSE, new ParseResult("", new Expression<?>[]{source})));
        return expression;
    }

    private static WrapperPlayServerTeams packet() {
        var packet = mock(WrapperPlayServerTeams.class);
        when(packet.getTeamName()).thenReturn("original");
        when(packet.getPlayers()).thenReturn(List.of());
        var info = teamInfo();
        when(packet.getTeamInfo()).thenReturn(Optional.of(info));
        doAnswer(call -> { when(packet.getTeamName()).thenReturn(call.getArgument(0)); return null; }).when(packet).setTeamName(anyString());
        doAnswer(call -> { when(packet.getPlayers()).thenReturn(call.getArgument(0)); return null; }).when(packet).setPlayers(any());
        doAnswer(call -> { when(packet.getTeamInfo()).thenReturn(Optional.of(call.getArgument(0))); return null; }).when(packet).setTeamInfo(any());
        return packet;
    }

    private static WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo() {
        Map<String, Object> state = new HashMap<>();
        state.put("DisplayName", Component.text("original"));
        state.put("Prefix", Component.empty());
        state.put("Suffix", Component.empty());
        state.put("Color", NamedTextColor.WHITE);
        state.put("TagVisibility", WrapperPlayServerTeams.NameTagVisibility.ALWAYS);
        state.put("CollisionRule", WrapperPlayServerTeams.CollisionRule.ALWAYS);
        state.put("OptionData", WrapperPlayServerTeams.OptionData.NONE);
        return mock(WrapperPlayServerTeams.ScoreBoardTeamInfo.class, invocation -> {
            String method = invocation.getMethod().getName();
            if (method.startsWith("get") && invocation.getArguments().length == 0) {
                return state.get(method.substring(3));
            }
            if (method.startsWith("set") && invocation.getArguments().length == 1) {
                state.put(method.substring(3), invocation.getArgument(0));
                return null;
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });
    }
}


