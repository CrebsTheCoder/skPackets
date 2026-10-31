package org.crebs.skpackets.elements.simple.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.crebs.skpackets.api.simple.GlowingEntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockMakers.INLINE;
import static org.mockito.Mockito.*;

class ExprGlowReceiversTest {
    private ExprGlowReceivers expression(Entity... entities) {
        Expression<Entity> source = mock(Expression.class);
        when(source.getAll(null)).thenReturn(entities);
        ExprGlowReceivers expression = new ExprGlowReceivers();
        Expression<?>[] expressions = new Expression<?>[]{source};
        assertTrue(expression.init(expressions, 0, Kleenean.FALSE, new SkriptParser.ParseResult("", expressions)));
        return expression;
    }

    @Test
    void readsOnlineViewersAndSkipsNullEntitiesAndOfflinePlayers() {
        Entity target = mock(Entity.class);
        when(target.getEntityId()).thenReturn(16);
        Player player = mock(Player.class);
        UUID online = UUID.randomUUID();
        UUID offline = UUID.randomUUID();
        try (MockedStatic<GlowingEntityManager> glow = mockStatic(GlowingEntityManager.class, withSettings().mockMaker(INLINE));
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, withSettings().mockMaker(INLINE))) {
            glow.when(() -> GlowingEntityManager.getGlowingReceivers(16)).thenReturn(List.of(online, offline));
            bukkit.when(() -> Bukkit.getPlayer(online)).thenReturn(player);
            assertArrayEquals(new Player[]{player}, expression().get(null, new Entity[]{null, target}));
        }
    }

    @ParameterizedTest
    @EnumSource(Changer.ChangeMode.class)
    void declaresSupportedChangeModes(Changer.ChangeMode mode) {
        Class<?>[] accepted = expression().acceptChange(mode);
        if (Set.of(ChangeMode.SET, ChangeMode.ADD, ChangeMode.REMOVE, ChangeMode.RESET, ChangeMode.REMOVE_ALL).contains(mode)) {
            assertArrayEquals(new Class<?>[]{Player[].class}, accepted);
        } else {
            assertNull(accepted);
        }

        assertEquals(Player.class, expression().getReturnType());
    }

    @ParameterizedTest
    @EnumSource(
        value = Changer.ChangeMode.class,
        names = {"SET", "ADD", "REMOVE", "RESET", "REMOVE_ALL"}
    )
    void changesOnlyNonNullTargetsUsingPlayerDelta(Changer.ChangeMode mode) {
        Entity target = mock(Entity.class);
        when(target.getEntityId()).thenReturn(16);
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        try (MockedStatic<GlowingEntityManager> glow = mockStatic(GlowingEntityManager.class, withSettings().mockMaker(INLINE))) {
            expression(null, target).change(null, new Object[]{player, "ignored", null}, mode);
            switch (mode) {
                case SET:
                    glow.verify(() -> GlowingEntityManager.setGlowingReceivers(16, List.of(uuid)));
                    break;
                case ADD:
                    glow.verify(() -> GlowingEntityManager.addGlowingReceivers(16, List.of(uuid)));
                    break;
                case REMOVE:
                    glow.verify(() -> GlowingEntityManager.removeGlowingReceivers(16, List.of(uuid)));
                    break;
                case RESET:
                case REMOVE_ALL:
                    glow.verify(() -> GlowingEntityManager.clearGlowingReceivers(16));
            }

            glow.verifyNoMoreInteractions();
        }
    }

    @Test
    void emptySourcesDoNotChangeGlowAndNullSetClearsReceivers() {
        Entity target = mock(Entity.class);
        when(target.getEntityId()).thenReturn(16);
        try (MockedStatic<GlowingEntityManager> glow = mockStatic(GlowingEntityManager.class, withSettings().mockMaker(INLINE))) {
            expression().change(null, null, ChangeMode.RESET);
            glow.verifyNoInteractions();
            expression(target).change(null, null, ChangeMode.SET);
            glow.verify(() -> GlowingEntityManager.setGlowingReceivers(16, List.of()));
        }
    }
}
