package org.crebs.skpackets.elements.general.effects;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockMakers.INLINE;
import static org.mockito.Mockito.*;

class EffSendOrReceivePacketTest {
    @ParameterizedTest
    @CsvSource({"send,false", "send,true", "receive,false", "receive,true"})
    void dispatchesEveryPacketToEveryConnectedPlayer(String direction, boolean silent) {
        Event event = mock(Event.class);
        Player player1 = mock(Player.class);
        Player disconnectedPlayer = mock(Player.class);
        Player player2 = mock(Player.class);
        User player1User = mock(User.class);
        User player2User = mock(User.class);
        PacketWrapper<?> a = mock(PacketWrapper.class);
        PacketWrapper<?> b = mock(PacketWrapper.class);
        Expression<PacketWrapper<?>> packets = mock(Expression.class);
        Expression<Player> players = mock(Expression.class);
        when(packets.getAll(event)).thenReturn(new PacketWrapper[]{a, b});
        when(players.getAll(event)).thenReturn(new Player[]{player1, disconnectedPlayer, player2});
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class, RETURNS_DEEP_STUBS);
        when(api.getPlayerManager().getUser(player1)).thenReturn(player1User);
        when(api.getPlayerManager().getUser(player2)).thenReturn(player2User);
        try (MockedStatic<PacketEvents> packetEvents = mockStatic(PacketEvents.class, withSettings().mockMaker(INLINE))) {
            packetEvents.when(PacketEvents::getAPI).thenReturn(api);
            EffSendOrReceivePacket effect = new EffSendOrReceivePacket();
            SkriptParser.ParseResult result = new SkriptParser.ParseResult("", new Expression<?>[0]);
            result.tags.add(direction);
            if (silent) {
                result.tags.add("silently");
            }

            assertTrue(effect.init(new Expression<?>[]{packets, players}, 0, Kleenean.FALSE, result));
            effect.execute(event);

            for (User user : new User[]{player1User, player2User}) {
                for (PacketWrapper<?> packet : new PacketWrapper[]{a, b}) {
                    if (direction.equals("send")) {
                        if (silent) {
                            verify(user).sendPacketSilently(packet);
                        } else {
                            verify(user).sendPacket(packet);
                        }
                    } else if (silent) {
                        verify(user).receivePacketSilently(packet);
                    } else {
                        verify(user).receivePacket(packet);
                    }
                }

                verifyNoMoreInteractions(user);
            }
        }
    }

    @Test
    void emptyPacketsNeverAccessPacketEvents() {
        Event event = mock(Event.class);
        Expression<?> packets = mock(Expression.class);
        Expression<?> players = mock(Expression.class);
        EffSendOrReceivePacket effect = new EffSendOrReceivePacket();
        assertTrue(effect.init(new Expression<?>[]{packets, players}, 0, Kleenean.FALSE, new SkriptParser.ParseResult("", new Expression<?>[0])));
        try (MockedStatic<PacketEvents> api = mockStatic(PacketEvents.class, withSettings().mockMaker(INLINE))) {
            effect.execute(event);
            api.verifyNoInteractions();
        }
    }
}
