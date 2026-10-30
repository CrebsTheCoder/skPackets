package org.crebs.skpackets.elements.entity.expressions.prop.living;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExprFakeLivingEntityAttributeTest {
    @org.junit.jupiter.api.BeforeAll
    static void initializePackets() { org.crebs.skpackets.support.PacketEventsTestBootstrap.initialize(); }
    private final Event event = mock(Event.class);

    @ParameterizedTest
    @ValueSource(ints={0,1})
    void readsTheRequestedAttributeInBothPropertyOrders(int pattern) {
        var attribute = mock(Attribute.class, withSettings().mockMaker(org.mockito.MockMakers.PROXY));
        when(attribute.getKey()).thenReturn(NamespacedKey.minecraft("scale"));
        var protocol = mock(com.github.retrooper.packetevents.protocol.attribute.Attribute.class);
        when(protocol.getDefaultValue()).thenReturn(1.0);
        var other = mock(com.github.retrooper.packetevents.protocol.attribute.Attribute.class);
        var property = mock(Property.class);
        when(property.getAttribute()).thenReturn(protocol);
        when(property.getValue()).thenReturn(2.0);
        var unrelated = mock(Property.class);
        when(unrelated.getAttribute()).thenReturn(other);
        var living = living();
        when(living.getAttributes().getProperties()).thenReturn(List.of(unrelated,property));
        var defaults = living();
        when(defaults.getAttributes().getProperties()).thenReturn(List.of());
        Probe expression = expression(attribute,pattern,living,defaults,mock(WrapperEntity.class),null);
        try (var attributes = mockStatic(Attributes.class,withSettings().mockMaker(org.mockito.MockMakers.INLINE))) {
            attributes.when(() -> Attributes.getByName("minecraft:scale")).thenReturn(protocol);
            assertArrayEquals(new Number[]{2.0,1.0},expression.read(event,new WrapperEntity[]{living,defaults,mock(WrapperEntity.class),null}));
        }
    }

    @ParameterizedTest
    @CsvSource({"SET,3.0", "ADD,5.0", "REMOVE,-1.0", "RESET,1.0"})
    void changesAttributeUsingTheCurrentValue(ChangeMode mode,double expected) {
        var attribute = mock(Attribute.class, withSettings().mockMaker(org.mockito.MockMakers.PROXY));
        when(attribute.getKey()).thenReturn(NamespacedKey.minecraft("scale"));
        var protocol = mock(com.github.retrooper.packetevents.protocol.attribute.Attribute.class);
        when(protocol.getDefaultValue()).thenReturn(1.0);
        var property = mock(Property.class);
        when(property.getAttribute()).thenReturn(protocol);
        when(property.getValue()).thenReturn(2.0);
        var living = living();
        when(living.getAttributes().getProperties()).thenReturn(List.of(property));
        var nonliving = mock(WrapperEntity.class);
        Probe expression = expression(attribute,0,living,nonliving,null);
        try (var attributes = mockStatic(Attributes.class,withSettings().mockMaker(org.mockito.MockMakers.INLINE))) {
            attributes.when(() -> Attributes.getByName("minecraft:scale")).thenReturn(protocol);
            expression.change(event,mode==ChangeMode.RESET?null:new Object[]{3},mode);
            verify(living.getAttributes()).setAttribute(protocol,expected);
            verifyNoInteractions(nonliving);
        }
    }

    @Test
    void missingAttributeAndInvalidDeltasDoNothing() {
        var living = mock(WrapperLivingEntity.class);
        Probe missing = expression(null,0,living);
        assertEquals(0,missing.read(event,new WrapperEntity[]{living}).length);
        missing.change(event,new Object[]{3},ChangeMode.SET);
        var attribute = mock(Attribute.class, withSettings().mockMaker(org.mockito.MockMakers.PROXY));
        Probe expression = expression(attribute,0,living);
        expression.change(event,null,ChangeMode.SET);
        expression.change(event,new Object[0],ChangeMode.ADD);
        expression.change(event,new Object[]{"wrong"},ChangeMode.REMOVE);
        verifyNoInteractions(living,attribute);
    }

    @Test
    void exposesOnlySupportedChangeModes() {
        Probe expression = new Probe();
        for (ChangeMode mode:ChangeMode.values()) {
            boolean supported=mode==ChangeMode.SET||mode==ChangeMode.ADD||mode==ChangeMode.REMOVE||mode==ChangeMode.RESET;
            if(supported) assertArrayEquals(new Class[]{Number.class},expression.acceptChange(mode));
            else assertNull(expression.acceptChange(mode));
        }
        assertSame(Number.class,expression.getReturnType());
    }

    @Test
    void addingToAnUnsetAttributeStartsAtItsProtocolDefault() {
        var attribute=mock(Attribute.class, withSettings().mockMaker(org.mockito.MockMakers.PROXY));
        when(attribute.getKey()).thenReturn(NamespacedKey.minecraft("scale"));
        var protocol=mock(com.github.retrooper.packetevents.protocol.attribute.Attribute.class);
        when(protocol.getDefaultValue()).thenReturn(1.0);
        var living=living();
        when(living.getAttributes().getProperties()).thenReturn(List.of());
        Probe expression=expression(attribute,1,living);
        try(var attributes=mockStatic(Attributes.class,withSettings().mockMaker(org.mockito.MockMakers.INLINE))) {
            attributes.when(() -> Attributes.getByName("minecraft:scale")).thenReturn(protocol);
            expression.change(event,new Object[]{3},ChangeMode.ADD);
            verify(living.getAttributes()).setAttribute(protocol,4.0);
        }
    }

    @Test
    void emptyEntityListsDoNotResolveProtocolAttributes() {
        var attribute=mock(Attribute.class, withSettings().mockMaker(org.mockito.MockMakers.PROXY));
        Probe expression=expression(attribute,0);
        expression.change(event,null,ChangeMode.RESET);
        verifyNoInteractions(attribute);
    }
    private WrapperLivingEntity living() {
        var living=mock(WrapperLivingEntity.class);
        var attributes=mock(me.tofaa.entitylib.wrapper.WrapperEntityAttributes.class,withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        when(living.getAttributes()).thenReturn(attributes);
        return living;
    }

    private Probe expression(Attribute attribute,int pattern,WrapperEntity... entities) {
        Expression<Attribute> attributes = mock(Expression.class);
        when(attributes.getSingle(event)).thenReturn(attribute);
        Expression<WrapperEntity> source = mock(Expression.class);
        when(source.getAll(event)).thenReturn(entities);
        Probe expression = new Probe();
        Expression<?>[] expressions=pattern==0?new Expression[]{attributes,source}:new Expression[]{source,attributes};
        assertTrue(expression.init(expressions,pattern,Kleenean.FALSE,new ParseResult("",expressions)));
        return expression;
    }

    static class Probe extends ExprFakeLivingEntityAttribute {
        Number[] read(Event event,WrapperEntity[] source) { return get(event,source); }
    }
}





