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
    void sharedPacketEntityIdSupportsFakeEntitiesAndRealPlayers() {
        var fake = mock(me.tofaa.entitylib.wrapper.WrapperEntity.class);
        Player player1 = mock(Player.class);
        when(fake.getEntityId()).thenReturn(50);
        when(player1.getEntityId()).thenReturn(32);
        ExprEntityId expression = new ExprEntityId();
        assertEquals(32, expression.convert(player1));
        assertEquals(50, expression.convert(fake));
        assertNull(expression.convert("player1"));
    }

    @Test
    void packetEntityIdHasOneRegistrationForBothSourceTypes() {
        org.crebs.skpackets.support.PacketEventsTestBootstrap.initialize();
        org.crebs.skpackets.elements.entity.field.entity.FakeEntityFieldRegistry.INSTANCE.registerAll();
        java.util.List<String> patterns = new java.util.ArrayList<>();
        java.util.List<String> sourceTypes = new java.util.ArrayList<>();
        var registration = mock(com.github.shanebeee.skr.Registration.class, call -> {
            if (!call.getMethod().getName().equals("newPropertyExpression")) {
                return RETURNS_DEFAULTS.answer(call);
            }
            Object[] arguments = call.getRawArguments();
            sourceTypes.add((String) arguments[3]);
            patterns.addAll(java.util.List.of(ch.njol.skript.expressions.base.SimplePropertyExpression.getPatterns((String) arguments[2], (String) arguments[3])));
            return mock(call.getMethod().getReturnType(), RETURNS_SELF);
        });
        ExprEntityId.register(registration);
        org.crebs.skpackets.elements.entity.expressions.entity.ExprFakeEntityEntityId.register(registration);
        assertEquals("entity/packetentity", sourceTypes.getFirst());
        long sharedMatches = patterns.stream()
                .map(pattern -> ch.njol.skript.patterns.PatternCompiler.compile(pattern.replaceAll("%[^%]+%", "<value>")))
                .filter(pattern -> pattern.match("packet entity id of value") != null)
                .count();
        assertEquals(1, sharedMatches);
        assertTrue(patterns.stream()
                .map(pattern -> ch.njol.skript.patterns.PatternCompiler.compile(pattern.replaceAll("%[^%]+%", "<value>")))
                .anyMatch(pattern -> pattern.match("packet entity field id of value") != null));
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
