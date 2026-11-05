package org.crebs.skpackets.elements.general.condition;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CondPacketTypeIsBoundTest {
    @ParameterizedTest
    @CsvSource({"client, SERVER, true", "client, CLIENT, false", "server, CLIENT, true", "server, SERVER, false"})
    void checksDestinationRatherThanSendingSide(String tag, PacketSide side, boolean expected) {
        CondPacketTypeIsBound condition = condition(tag, 0);
        PacketTypeCommon packetType = mock(PacketTypeCommon.class);
        when(packetType.getSide()).thenReturn(side);
        assertEquals(expected, condition.check(packetType));
    }

    @ParameterizedTest
    @CsvSource({"client, 0, false", "client, 1, true", "server, 0, false", "server, 1, true"})
    void preservesNegatedPattern(String tag, int pattern, boolean negated) {
        assertEquals(negated, condition(tag, pattern).isNegated());
    }

    private CondPacketTypeIsBound condition(String tag, int pattern) {
        Expression<?>[] expressions = new Expression<?>[]{mock(Expression.class)};
        SkriptParser.ParseResult result = new SkriptParser.ParseResult("", expressions);
        result.tags.add(tag);
        CondPacketTypeIsBound condition = new CondPacketTypeIsBound();
        assertTrue(condition.init(expressions, pattern, Kleenean.FALSE, result));
        return condition;
    }
}
