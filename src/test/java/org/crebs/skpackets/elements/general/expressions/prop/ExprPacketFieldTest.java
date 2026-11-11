package org.crebs.skpackets.elements.general.expressions.prop;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.crebs.skpackets.api.general.packet.PacketSendOrReceiveEvent;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.elements.general.event.EvtPacketSendOrReceive.PacketSendOrReceiveParserData;
import org.crebs.skpackets.elements.general.field.PacketFieldRegistry;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExprPacketFieldTest {
    @Test void invalidDynamicValueUsesTheExpressionRuntimeErrorHandler() {
        Event event = mock(Event.class);
        PacketWrapper<?> packet = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        FieldAccessor<PacketWrapper<?>, String> accessor = new FieldAccessor<>("review runtime validation", new String[0], String.class,
                false, ignored -> "before", (ignored, next) -> { throw new IllegalArgumentException("Invalid action: update"); });
        Probe expression = spy(expression(accessor, packet, event));
        doNothing().when(expression).error(anyString());
        expression.change(event, new Object[]{"update"}, ChangeMode.SET);
        verify(expression).error("Invalid action: update");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void knownEventPacketKeepsItsArrayFieldTypeDespiteUnrelatedScalarFields() {
        var eventPacket = mock(ch.njol.skript.expressions.base.EventValueExpression.class);
        when(eventPacket.getSource()).thenReturn(eventPacket);
        when(eventPacket.isSingle()).thenReturn(true);
        var data = mock(PacketSendOrReceiveParserData.class);
        when(data.getPacketType()).thenReturn(PacketType.Play.Server.PLUGIN_MESSAGE);
        var parser = mock(ParserInstance.class);
        when(parser.isCurrentEvent(PacketSendOrReceiveEvent.class)).thenReturn(true);
        when(parser.getData(PacketSendOrReceiveParserData.class)).thenReturn(data);
        try (var parsers = mockStatic(ParserInstance.class)) {
            parsers.when(ParserInstance::get).thenReturn(parser);
            var parsed = new ParseResult("packet field data of event-packet", new Expression[0]);
            var match = Pattern.compile("data").matcher("data");
            assertTrue(match.find());
            parsed.regexes.add(match);
            Probe expression = new Probe();
            assertTrue(expression.init(new Expression[]{eventPacket}, 0, Kleenean.FALSE, parsed));
            assertSame(Number.class, expression.getReturnType());
            assertFalse(expression.isSingle());
        }
    }

    @BeforeAll
    static void initializeFields() {
        PacketEventsTestBootstrap.initialize();
        PluginManager plugins = mock(PluginManager.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    @Test
    void readsAndSetsScalarPacketFields() {
        Event event = mock(Event.class);
        PacketWrapper<?> packet = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        AtomicReference<String> value = new AtomicReference<>("before");
        FieldAccessor<PacketWrapper<?>, String> accessor = new FieldAccessor<>("review text", new String[0], String.class, false, ignored -> value.get(), (ignored, next) -> value.set(next));
        Probe expression = expression(accessor, packet, event);
        assertArrayEquals(new Object[]{"before"}, expression.read(event, packet));
        expression.change(event, new Object[]{"after"}, ChangeMode.SET);
        assertEquals("after", value.get());
        expression.change(event, new Object[]{"ignored"}, ChangeMode.ADD);
        expression.change(event, null, ChangeMode.SET);
        expression.change(event, new Object[0], ChangeMode.SET);
        assertEquals("after", value.get());
        assertSame(String.class, expression.getReturnType());
        assertTrue(expression.isSingle());
    }

    @Test
    void flattensArrayFieldsAndReportsPlurality() {
        Event event = mock(Event.class);
        PacketWrapper<?> packet = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        FieldAccessor<PacketWrapper<?>, String[]> accessor = new FieldAccessor<>("review names", new String[0], String[].class, false, ignored -> new String[]{"player1", "player2"}, null);
        Probe expression = expression(accessor, packet, event);
        assertArrayEquals(new Object[]{"player1", "player2"}, expression.read(event, packet));
        assertSame(String.class, expression.getReturnType());
        assertFalse(expression.isSingle());
        try (var skript = mockStatic(Skript.class)) {
            assertNull(expression.acceptChange(ChangeMode.DELETE));
            skript.verify(() -> Skript.error("Cannot set review names because it is a read-only field."));
        }
    }

    @Test
    void nativeScalarFieldsReadTheirOwnValuesWithoutArrayStoreFailure() {
        Event event = mock(Event.class);
        WrapperPlayServerEffect effect = mock(WrapperPlayServerEffect.class, RETURNS_DEEP_STUBS);
        WrapperPlayServerBlockChange block = mock(WrapperPlayServerBlockChange.class, RETURNS_DEEP_STUBS);
        when(effect.getPacketTypeData().getPacketType()).thenReturn(PacketType.Play.Server.EFFECT);
        when(block.getPacketTypeData().getPacketType()).thenReturn(PacketType.Play.Server.BLOCK_CHANGE);
        when(effect.getData()).thenReturn(1);
        BlockData data = mock(BlockData.class);
        try (var conversion = mockStatic(SpigotConversionUtil.class)) {
            conversion.when(() -> SpigotConversionUtil.toBukkitBlockData(block.getBlockState())).thenReturn(data);
            Probe expression = initialized("data", effect, event, null);
            assertSame(Object.class, expression.getReturnType());
            assertArrayEquals(new Object[]{1}, expression.read(event, effect));
            assertArrayEquals(new Object[]{data}, initialized("block state", block, event, null).read(event, block));
            assertTrue(expression.isSingle());
        }
    }

    @Test
    void mixedScalarAndArrayNamesKeepOneValuePerPacketAndRoundTripArrays() {
        Event event = mock(Event.class);
        PacketWrapper<?> packet = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        AtomicReference<Number[]> bytes = new AtomicReference<>(new Number[]{1, 2, 3});
        FieldAccessor<PacketWrapper<?>, Number[]> arrayField = new FieldAccessor<>(
                "review mixed data", new String[0], Number[].class, false,
                ignored -> bytes.get(), (ignored, next) -> bytes.set(next));
        expression(arrayField, packet, event);
        PacketWrapper<?> scalar = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        FieldAccessor<PacketWrapper<?>, Number> scalarField = new FieldAccessor<>(
                "review mixed data", new String[0], Number.class, false, ignored -> 42, null);
        Probe expression = expression(scalarField, scalar, event);
        assertTrue(expression.isSingle());
        assertSame(Object.class, expression.getReturnType());
        assertArrayEquals(new Object[]{42}, expression.read(event, scalar));
        Object[] result = expression.read(event, packet);
        assertEquals(1, result.length);
        assertSame(bytes.get(), result[0]);

        Probe arrayExpression = initialized("review mixed data", packet, event, null);
        arrayExpression.change(event, new Object[]{new Number[]{7, 8}}, ChangeMode.SET);
        assertArrayEquals(new Number[]{7, 8}, bytes.get());
        arrayExpression.change(event, new Object[]{9, 10}, ChangeMode.SET);
        assertArrayEquals(new Number[]{9, 10}, bytes.get());
        bytes.set(new Number[0]);
        assertEquals(1, arrayExpression.read(event, packet).length);
        assertArrayEquals(new Number[0], (Number[]) arrayExpression.read(event, packet)[0]);
    }

    @Test
    void typedMetadataEventStillResolvesTheAccessedVelocityPacketsSchema() {
        Event event = mock(Event.class);
        WrapperPlayServerEntityVelocity velocity = mock(WrapperPlayServerEntityVelocity.class, RETURNS_DEEP_STUBS);
        when(velocity.getPacketTypeData().getPacketType()).thenReturn(PacketType.Play.Server.ENTITY_VELOCITY);
        when(velocity.getEntityId()).thenReturn(123);
        Probe expression = initialized("entity id", velocity, event, PacketType.Play.Server.ENTITY_METADATA);
        assertArrayEquals(new Object[]{123}, expression.read(event, velocity));
    }

    @Test
    void writableSharedAliasAcceptsBothRuntimeValueTypes() {
        Event event = mock(Event.class);
        PacketWrapper<?> packet = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        Probe expression = initialized("data", packet, event, null);
        ParserInstance parser = mock(ParserInstance.class);
        try (var parsers = mockStatic(ParserInstance.class)) {
            parsers.when(ParserInstance::get).thenReturn(parser);
            assertTrue(java.util.Set.of(expression.acceptChange(ChangeMode.SET)).containsAll(java.util.Set.of(Number.class, Number[].class)));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Probe expression(FieldAccessor<PacketWrapper<?>, ?> accessor, PacketWrapper<?> packet, Event event) {
        PacketTypeCommon type = mock(PacketTypeCommon.class);
        when(packet.getPacketTypeData().getPacketType()).thenReturn(type);
        PacketFieldRegistry.INSTANCE.builder(type, (Class) PacketWrapper.class)
                .requiredField((Class) accessor.expectedType(), (Function) accessor.getter(), (BiConsumer) accessor.setter(), accessor.name())
                .build();
        return initialized(accessor.name(), packet, event, null);
    }

    @SuppressWarnings("unchecked")
    private Probe initialized(String field, PacketWrapper<?> packet, Event event, PacketTypeCommon eventType) {
        Expression<PacketWrapper<?>> source = mock(Expression.class);
        when(source.getArray(event)).thenReturn(new PacketWrapper[]{packet});
        when(source.isSingle()).thenReturn(true);
        ParserInstance parser = mock(ParserInstance.class);
        PacketSendOrReceiveParserData data = mock(PacketSendOrReceiveParserData.class);
        when(parser.isCurrentEvent(PacketSendOrReceiveEvent.class)).thenReturn(eventType != null);
        when(parser.getData(PacketSendOrReceiveParserData.class)).thenReturn(data);
        when(data.getPacketType()).thenReturn(eventType);
        Expression<?>[] inputs = {source};
        ParseResult result = new ParseResult("", inputs);
        var matcher = Pattern.compile(Pattern.quote(field)).matcher(field);
        assertTrue(matcher.find());
        result.regexes.add(matcher);
        Probe expression = new Probe();
        try (var parsers = mockStatic(ParserInstance.class)) {
            parsers.when(ParserInstance::get).thenReturn(parser);
            assertTrue(expression.init(inputs, 0, Kleenean.FALSE, result));
        }
        return expression;
    }

    static class Probe extends ExprPacketField {
        Object[] read(Event event, PacketWrapper<?> packet) {
            return get(event, new PacketWrapper[]{packet});
        }
    }
}
