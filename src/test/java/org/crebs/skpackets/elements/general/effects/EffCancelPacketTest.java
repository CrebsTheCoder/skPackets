package org.crebs.skpackets.elements.general.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.general.packet.PacketSendOrReceiveEvent;
import org.crebs.skpackets.elements.general.event.EvtPacketSendOrReceive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.MockMakers.INLINE;
import static org.mockito.Mockito.*;

class EffCancelPacketTest {
    @ParameterizedTest
    @CsvSource({"true,NETTY,NORMAL,true", "false,NETTY,NORMAL,false", "true,SYNC,MONITOR,false", "true,NETTY,MONITOR,false"})
    void onlyInitializesInMutableNettyPacketEvents(boolean packetEvent, EvtPacketSendOrReceive.ProcessType process, PacketListenerPriority priority, boolean expected) {
        ParserInstance parser = mock(ParserInstance.class, withSettings().mockMaker(INLINE));
        EvtPacketSendOrReceive.PacketSendOrReceiveParserData data = mock(EvtPacketSendOrReceive.PacketSendOrReceiveParserData.class);
        when(parser.isCurrentEvent(PacketSendOrReceiveEvent.class)).thenReturn(packetEvent);
        when(parser.getData(EvtPacketSendOrReceive.PacketSendOrReceiveParserData.class)).thenReturn(data);
        when(data.getProcessType()).thenReturn(process);
        when(data.getPriority()).thenReturn(priority);
        try (MockedStatic<ParserInstance> instances = mockStatic(ParserInstance.class, withSettings().mockMaker(INLINE));
             MockedStatic<Skript> skript = mockStatic(Skript.class, withSettings().mockMaker(INLINE))) {
            instances.when(ParserInstance::get).thenReturn(parser);
            Expression<?>[] expressions = new Expression<?>[0];
            assertEquals(expected, new EffCancelPacket().init(expressions, 0, Kleenean.FALSE, new SkriptParser.ParseResult("", expressions)));
        }
    }

    @Test
    void cancelsOnlyNettyEvents() {
        EffCancelPacket effect = new EffCancelPacket();
        PacketSendOrReceiveEvent.NettyPacketEvent event = mock(PacketSendOrReceiveEvent.NettyPacketEvent.class);
        effect.execute(event);
        verify(event).setCancelled(true);
        Event unrelated = mock(Event.class);
        effect.execute(unrelated);
        verifyNoInteractions(unrelated);
        PacketSendOrReceiveEvent.SyncPacketEvent sync = mock(PacketSendOrReceiveEvent.SyncPacketEvent.class);
        effect.execute(sync);
        verifyNoInteractions(sync);
    }
}
