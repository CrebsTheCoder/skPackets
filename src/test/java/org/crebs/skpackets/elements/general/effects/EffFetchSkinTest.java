package org.crebs.skpackets.elements.general.effects;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import java.lang.reflect.Field;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.entity.Skin;
import org.crebs.skpackets.api.entity.SkinManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockMakers.INLINE;
import static org.mockito.Mockito.*;

class EffFetchSkinTest {
    @Test
    void writesFetchedSkinToTheDestination() throws Exception {
        Event event = mock(Event.class);
        Expression<String> name = mock(Expression.class);
        Expression<?> destination = mock(Expression.class);
        when(name.getSingle(event)).thenReturn("player1");
        Skin skin = new Skin("value", "signature");
        EffFetchSkin effect = effect(name, destination);
        try (MockedStatic<SkinManager> manager = mockStatic(SkinManager.class, withSettings().mockMaker(INLINE))) {
            manager.when(() -> SkinManager.getOfflinePlayer("player1")).thenReturn(skin);
            effect.execute(event);
            verify(destination).change(event, new Skin[]{skin}, ChangeMode.SET);
        }
    }

    @Test
    void failedFetchLeavesDestinationUnchanged() throws Exception {
        Event event = mock(Event.class);
        Expression<String> name = mock(Expression.class);
        Expression<?> destination = mock(Expression.class);
        when(name.getSingle(event)).thenReturn("player2");
        try (MockedStatic<SkinManager> manager = mockStatic(SkinManager.class, withSettings().mockMaker(INLINE))) {
            manager.when(() -> SkinManager.getOfflinePlayer("player2")).thenThrow(new IllegalStateException("offline"));
            effect(name, destination).execute(event);
            verifyNoInteractions(destination);
        }
    }

    private EffFetchSkin effect(Expression<?> name, Expression<?> destination) throws Exception {
        EffFetchSkin effect = new EffFetchSkin();
        Field nameField = EffFetchSkin.class.getDeclaredField("nameExpr");
        nameField.setAccessible(true);
        nameField.set(effect, name);
        Field destinationField = EffFetchSkin.class.getDeclaredField("expression");
        destinationField.setAccessible(true);
        destinationField.set(effect, destination);
        return effect;
    }
}
