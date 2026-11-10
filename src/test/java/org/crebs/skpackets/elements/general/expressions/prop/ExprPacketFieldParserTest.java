package org.crebs.skpackets.elements.general.expressions.prop;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.effects.EffChange;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.crebs.skpackets.elements.general.field.PacketFieldRegistry;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExprPacketFieldParserTest {
    @BeforeAll
    static void initializeFields() {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void skriptAcceptsExistingScalarDataAssignment() {
        Event event = mock(Event.class);
        WrapperPlayServerEffect packet = mock(WrapperPlayServerEffect.class, RETURNS_DEEP_STUBS);
        when(packet.getPacketTypeData().getPacketType()).thenReturn(PacketType.Play.Server.EFFECT);
        when(packet.getData()).thenReturn(7);
        ParserInstance parser = mock(ParserInstance.class);
        try (var parsers = mockStatic(ParserInstance.class);
             var skript = mockStatic(Skript.class);
             var classes = mockStatic(Classes.class);
             var logs = mockStatic(SkriptLogger.class)) {
            parsers.when(ParserInstance::get).thenReturn(parser);
            classes.when(() -> Classes.getSuperClassInfo(any(Class.class))).thenReturn(mock(ClassInfo.class));
            Expression<Object> variable = mock(Expression.class);
            when(variable.acceptChange(ChangeMode.SET)).thenReturn(new Class[]{Object.class});
            doReturn(Object.class).when(variable).getReturnType();
            when(variable.toString(null, false)).thenReturn("{_effectData}");
            ExprPacketField field = field("data", packet, event);
            EffChange assignment = new EffChange();
            assertTrue(assignment.init(new Expression[]{variable, field}, 3, Kleenean.FALSE,
                    new ParseResult("set {_effectData} to packet field data of {_effect}", new Expression[0])));
            assertEquals(7, field.getSingle(event));
            skript.verify(() -> Skript.error(anyString()), never());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void skriptAcceptsMultipleValuesForAnArrayPacketField() {
        Event event = mock(Event.class);
        WrapperPlayServerDestroyEntities packet = mock(WrapperPlayServerDestroyEntities.class, RETURNS_DEEP_STUBS);
        when(packet.getPacketTypeData().getPacketType()).thenReturn(PacketType.Play.Server.DESTROY_ENTITIES);
        ParserInstance parser = mock(ParserInstance.class);
        try (var parsers = mockStatic(ParserInstance.class);
             var skript = mockStatic(Skript.class);
             var classes = mockStatic(Classes.class);
             var logs = mockStatic(SkriptLogger.class)) {
            parsers.when(ParserInstance::get).thenReturn(parser);
            classes.when(() -> Classes.getSuperClassInfo(any(Class.class))).thenReturn(mock(ClassInfo.class));
            classes.when(() -> Classes.toString(any(Object[].class), eq(false))).thenReturn("numbers");
            Expression<Number> values = mock(Expression.class);
            doReturn(Number.class).when(values).getReturnType();
            when(values.canReturn(Number.class)).thenReturn(true);
            when(values.isSingle()).thenReturn(false);
            when(values.canBeSingle()).thenReturn(false);
            when(values.getArray(event)).thenReturn(new Number[]{3, 4});
            ExprPacketField field = field("entity ids", packet, event);
            EffChange assignment = new EffChange();
            assertTrue(assignment.init(new Expression[]{field, values}, 3, Kleenean.FALSE,
                    new ParseResult("set packet field entity ids of {_packet} to 3 and 4", new Expression[0])));
            field.change(event, values.getArray(event), ChangeMode.SET);
            verify(packet).setEntityIds(new int[]{3, 4});
            skript.verify(() -> Skript.error(anyString()), never());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void skriptCopiesBoxedMixedArrayFieldsThroughItsActualConversionPipeline() {
        Event event = mock(Event.class);
        PacketWrapper<?> sourcePacket = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        PacketWrapper<?> targetPacket = mock(PacketWrapper.class, RETURNS_DEEP_STUBS);
        var sourceType = mock(com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon.class);
        var targetType = mock(com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon.class);
        var scalarType = mock(com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon.class);
        when(sourcePacket.getPacketTypeData().getPacketType()).thenReturn(sourceType);
        when(targetPacket.getPacketTypeData().getPacketType()).thenReturn(targetType);
        var target = new java.util.concurrent.atomic.AtomicReference<Number[]>(new Number[]{99});
        PacketFieldRegistry.INSTANCE.builder(sourceType, (Class) PacketWrapper.class)
                .requiredField(Number[].class, ignored -> new Number[]{3, 4}, null, "parser mixed array").build();
        PacketFieldRegistry.INSTANCE.builder(targetType, (Class) PacketWrapper.class)
                .requiredField(Number[].class, ignored -> target.get(),
                        (ignored, value) -> target.set((Number[]) value), "parser mixed array").build();
        PacketFieldRegistry.INSTANCE.builder(scalarType, (Class) PacketWrapper.class)
                .requiredField(Number.class, ignored -> 1, null, "parser mixed array").build();
        ParserInstance parser = mock(ParserInstance.class);
        try (var parsers = mockStatic(ParserInstance.class);
             var skript = mockStatic(Skript.class);
             var classes = mockStatic(Classes.class);
             var logs = mockStatic(SkriptLogger.class)) {
            parsers.when(ParserInstance::get).thenReturn(parser);
            ClassInfo info = mock(ClassInfo.class);
            doReturn(Object.class).when(info).getC();
            classes.when(() -> Classes.getSuperClassInfo(any(Class.class))).thenReturn(info);
            classes.when(() -> Classes.getSuperClassInfo(any(Class[].class))).thenReturn(info);
            ExprPacketField source = field("parser mixed array", sourcePacket, event);
            ExprPacketField destination = field("parser mixed array", targetPacket, event);
            RunChange assignment = new RunChange();
            assertTrue(assignment.init(new Expression[]{destination, source}, 3, Kleenean.FALSE,
                    new ParseResult("set packet field parser mixed array of {_target} to packet field parser mixed array of {_source}", new Expression[0])));
            assignment.executeAssignment(event);
            assertArrayEquals(new Number[]{3, 4}, target.get());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void skriptCopiesRealPluginMessageDataUsingTypedArrayValues() {
        Event event = mock(Event.class);
        var sourcePacket = mock(com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage.class, RETURNS_DEEP_STUBS);
        var targetPacket = mock(com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage.class, RETURNS_DEEP_STUBS);
        when(sourcePacket.getPacketTypeData().getPacketType()).thenReturn(PacketType.Play.Client.PLUGIN_MESSAGE);
        when(targetPacket.getPacketTypeData().getPacketType()).thenReturn(PacketType.Play.Client.PLUGIN_MESSAGE);
        when(sourcePacket.getData()).thenReturn(new byte[]{3, 4});
        ParserInstance parser = mock(ParserInstance.class);
        try (var parsers = mockStatic(ParserInstance.class);
             var skript = mockStatic(Skript.class);
             var classes = mockStatic(Classes.class);
             var logs = mockStatic(SkriptLogger.class)) {
            parsers.when(ParserInstance::get).thenReturn(parser);
            ClassInfo info = mock(ClassInfo.class);
            doReturn(Object.class).when(info).getC();
            classes.when(() -> Classes.getSuperClassInfo(any(Class.class))).thenReturn(info);
            classes.when(() -> Classes.getSuperClassInfo(any(Class[].class))).thenReturn(info);
            ExprPacketField source = field("data", sourcePacket, event);
            ExprPacketField destination = field("data", targetPacket, event);
            assertInstanceOf(Number[].class, source.getSingle(event));
            RunChange assignment = new RunChange();
            assertTrue(assignment.init(new Expression[]{destination, source}, 3, Kleenean.FALSE,
                    new ParseResult("set packet field data of {_target} to packet field data of {_source}", new Expression[0])));
            assignment.executeAssignment(event);
            verify(targetPacket).setData(new byte[]{3, 4});
        }
    }

    @Test
    void invalidArrayElementsAreReportedWithoutEscapingTheChangeEffect() {
        Event event = mock(Event.class);
        var packet = mock(WrapperPlayServerDestroyEntities.class, RETURNS_DEEP_STUBS);
        when(packet.getPacketTypeData().getPacketType()).thenReturn(PacketType.Play.Server.DESTROY_ENTITIES);
        ParserInstance parser = mock(ParserInstance.class);
        try (var parsers = mockStatic(ParserInstance.class);
             var skript = mockStatic(Skript.class)) {
            parsers.when(ParserInstance::get).thenReturn(parser);
            ExprPacketField expression = spy(field("entity ids", packet, event));
            doNothing().when(expression).error(anyString());
            assertDoesNotThrow(() -> expression.change(event, new Object[]{"wrong"}, ChangeMode.SET));
            verify(packet, never()).setEntityIds(any(int[].class));
            verify(expression).error(anyString());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void packetCreationSectionCopiesBoxedArrayDataThroughSkriptConversion() {
        Event event = mock(Event.class);
        var packet = mock(com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage.class, RETURNS_DEEP_STUBS);
        when(packet.getPacketTypeData().getPacketType()).thenReturn(PacketType.Play.Client.PLUGIN_MESSAGE);
        when(packet.getData()).thenReturn(new byte[]{3, 4});
        ParserInstance parser = mock(ParserInstance.class);
        try (var parsers = mockStatic(ParserInstance.class);
             var skript = mockStatic(Skript.class);
             var classes = mockStatic(Classes.class);
             var logs = mockStatic(SkriptLogger.class)) {
            parsers.when(ParserInstance::get).thenReturn(parser);
            ClassInfo info = mock(ClassInfo.class);
            doReturn(Object.class).when(info).getC();
            classes.when(() -> Classes.getSuperClassInfo(any(Class.class))).thenReturn(info);
            classes.when(() -> Classes.getSuperClassInfo(any(Class[].class))).thenReturn(info);
            ExprPacketField source = field("data", packet, event);
            var schema = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Client.PLUGIN_MESSAGE);
            var entries = mock(org.skriptlang.skript.lang.entry.EntryContainer.class);
            doReturn(source).when(entries).getOptional("data", Object.class, false);
            java.util.Map<String, Expression<?>> expressions = new java.util.HashMap<>();
            assertTrue(org.crebs.skpackets.api.util.field.FieldSections.parse(
                    java.util.List.of(schema.getAccessor("data")), entries, expressions, "plugin message", true, java.util.Set.of()));
            var created = org.crebs.skpackets.api.util.field.FieldSections.create(schema,
                    PacketType.Play.Client.PLUGIN_MESSAGE, expressions, event,
                    java.util.Map.of("channel name", "minecraft:brand"));
            assertInstanceOf(com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage.class, created);
            assertArrayEquals(new byte[]{3, 4}, ((com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage) created).getData());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void numericListLiteralsAreConvertedBeforePacketCreation() {
        Event event = mock(Event.class);
        var unparsed = mock(ch.njol.skript.lang.UnparsedLiteral.class);
        var numbers = mock(ch.njol.skript.lang.Literal.class);
        doReturn(Object.class).when(unparsed).getReturnType();
        doReturn(numbers).when(unparsed).getConvertedExpression(Number.class);
        when(numbers.getArray(event)).thenReturn(new Number[]{1L, 2L, 3L});
        var schema = PacketFieldRegistry.INSTANCE.getSchema(PacketType.Play.Server.DESTROY_ENTITIES);
        var entries = mock(org.skriptlang.skript.lang.entry.EntryContainer.class);
        doReturn(unparsed).when(entries).getOptional("entity ids", Object.class, false);
        java.util.Map<String, Expression<?>> expressions = new java.util.HashMap<>();
        assertTrue(org.crebs.skpackets.api.util.field.FieldSections.parse(schema.accessors(), entries,
                expressions, "destroy entities", true, java.util.Set.of()));
        verify(unparsed).getConvertedExpression(Number.class);
        var packet = org.crebs.skpackets.api.util.field.FieldSections.create(schema,
                PacketType.Play.Server.DESTROY_ENTITIES, expressions, event, java.util.Map.of());
        assertArrayEquals(new int[]{1, 2, 3}, ((WrapperPlayServerDestroyEntities) packet).getEntityIds());
    }

    private static class RunChange extends EffChange {
        void executeAssignment(Event event) { execute(event); }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ExprPacketField field(String name, PacketWrapper<?> packet, Event event) {
        Expression<PacketWrapper<?>> source = mock(Expression.class);
        when(source.getArray(event)).thenReturn(new PacketWrapper[]{packet});
        when(source.isSingle()).thenReturn(true);
        Expression<?>[] inputs = {source};
        ParseResult result = new ParseResult("", inputs);
        var matcher = Pattern.compile(Pattern.quote(name)).matcher(name);
        assertTrue(matcher.find());
        result.regexes.add(matcher);
        ExprPacketField field = new ExprPacketField();
        assertTrue(field.init(inputs, 0, Kleenean.FALSE, result));
        return field;
    }
}


