package org.crebs.skpackets.elements.general.event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.general.packet.PacketSendOrReceiveEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockMakers.INLINE;
import static org.mockito.Mockito.*;

class EvtPacketSendOrReceiveTest {
    @ParameterizedTest
    @CsvSource({"netty,NORMAL", "sync,MONITOR", "async,MONITOR"})
    void defaultsPriorityForEachProcessingThread(String processing, PacketListenerPriority priority) {
        EvtPacketSendOrReceive syntax = syntax(null, processing);
        PacketSendOrReceiveEvent event = event(processing, priority);
        assertTrue(syntax.check(event));
        assertTrue(syntax.canExecuteAsynchronously());
        assertFalse(syntax.check(mock(Event.class)));
        assertFalse(syntax.check(event(processing, PacketListenerPriority.LOWEST)));
        assertTrue(syntax.toString(null, false).contains(priority.name().toLowerCase()));
    }

    @Test
    void filtersPacketTypeAndThread() {
        PacketTypeCommon expected = mock(PacketTypeCommon.class);
        EvtPacketSendOrReceive syntax = syntax(expected, "sync");
        PacketSendOrReceiveEvent matching = event("sync", PacketListenerPriority.MONITOR);
        when(matching.getEvent().getPacketType()).thenReturn(expected);
        assertTrue(syntax.check(matching));
        when(matching.getEvent().getPacketType()).thenReturn(mock(PacketTypeCommon.class));
        assertFalse(syntax.check(matching));
        assertFalse(syntax.check(event("async", PacketListenerPriority.MONITOR)));
    }

    @ParameterizedTest
    @CsvSource({"sync,low", "async,normal", "sync,highest"})
    void rejectsMutablePriorityAfterProcessing(String thread, String priority) {
        try (MockedStatic<Skript> skript = mockStatic(Skript.class, withSettings().mockMaker(INLINE))) {
            EvtPacketSendOrReceive syntax = new EvtPacketSendOrReceive();
            assertFalse(syntax.init(new Literal<?>[0], 0, result(thread, priority)));
            skript.verify(() -> Skript.error(anyString()));
        }
    }

    @Test
    void nettyAliasUsesSameFiltering() {
        EvtNettyPacket syntax = new EvtNettyPacket();
        assertTrue(syntax.init(new Literal<?>[0], 0, result("high")));
        assertTrue(syntax.check(event("netty", PacketListenerPriority.HIGH)));
        assertFalse(syntax.check(event("sync", PacketListenerPriority.HIGH)));
    }

    private EvtPacketSendOrReceive syntax(PacketTypeCommon type, String... tags) {
        EvtPacketSendOrReceive syntax = new EvtPacketSendOrReceive();
        Literal<?>[] literals = new Literal<?>[0];
        if (type != null) {
            Literal<PacketTypeCommon> literal = mock(Literal.class);
            when(literal.getSingle()).thenReturn(type);
            literals = new Literal<?>[]{literal};
        }

        assertTrue(syntax.init(literals, 0, result(tags)));
        return syntax;
    }

    private SkriptParser.ParseResult result(String... tags) {
        SkriptParser.ParseResult result = new SkriptParser.ParseResult("", new Expression<?>[0]);
        result.tags.addAll(List.of(tags));
        return result;
    }

    private PacketSendOrReceiveEvent event(String thread, PacketListenerPriority priority) {
        Class<? extends PacketSendOrReceiveEvent> type = switch (thread) {
            case "sync" -> PacketSendOrReceiveEvent.SyncPacketEvent.class;
            case "async" -> PacketSendOrReceiveEvent.AsyncPacketEvent.class;
            default -> PacketSendOrReceiveEvent.NettyPacketEvent.class;
        };
        PacketSendOrReceiveEvent event = mock(type);
        when(event.getPriority()).thenReturn(priority);
        when(event.getEvent()).thenReturn(mock(ProtocolPacketEvent.class));
        return event;
    }
}
