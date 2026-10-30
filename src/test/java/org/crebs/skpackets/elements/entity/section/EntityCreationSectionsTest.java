package org.crebs.skpackets.elements.entity.section;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.util.field.BaseFieldRegistry;
import org.crebs.skpackets.api.util.field.FieldAccessor;
import org.crebs.skpackets.api.util.field.FieldSchema;
import org.crebs.skpackets.api.util.field.FieldSections;
import org.crebs.skpackets.elements.entity.field.entity.FakeEntityFieldRegistry;
import org.crebs.skpackets.elements.entity.field.meta.MetaFieldRegistry;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockMakers.INLINE;

class EntityCreationSectionsTest {
    @BeforeAll
    static void bootstrap() { PacketEventsTestBootstrap.initialize(); }

    @SuppressWarnings("rawtypes")
    private Map registry(Object owner) throws Exception {
        Field field = BaseFieldRegistry.class.getDeclaredField("REGISTRY");
        field.setAccessible(true);
        return (Map) field.get(owner);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void typedEntityCreationPassesPersistenceAndReturnsConstructedEntity(boolean persistent) throws Exception {
        Map registry = registry(FakeEntityFieldRegistry.INSTANCE);
        Map original = new HashMap(registry);
        WrapperEntity created = mock(WrapperEntity.class);
        EntityType type = EntityTypes.ZOMBIE;
        FieldAccessor<WrapperEntity, Boolean> flag = new FieldAccessor<>("persistent", new String[0], Boolean.class, true, ignored -> false, null);
        FieldSchema<EntityType, WrapperEntity> schema = new FieldSchema<>(type, List.of(flag), context -> {
            assertSame(type, context.getKey());
            assertEquals(persistent, context.getOptional("persistent", Boolean.class));
            return created;
        });
        registry.put(type, schema);
        Expression<?>[] inputs = {mock(Expression.class)};
        ParseResult result = new ParseResult("", inputs);
        if (persistent) result.tags.add("persistent");
        try (var fields = mockStatic(FieldSections.class, withSettings().mockMaker(INLINE).defaultAnswer(CALLS_REAL_METHODS));
             var conversion = mockStatic(SpigotConversionUtil.class, withSettings().mockMaker(INLINE))) {
            fields.when(() -> FieldSections.entityType(inputs[0])).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
            conversion.when(() -> SpigotConversionUtil.fromBukkitEntityType(org.bukkit.entity.EntityType.ZOMBIE)).thenReturn(type);
            var expression = new SecExprNewFakeEntity();
            assertTrue(expression.init(inputs, 0, Kleenean.FALSE, result, null, null));
            assertArrayEquals(new WrapperEntity[]{created}, expression.get(null));
            assertTrue(expression.isSingle());
            assertEquals(WrapperEntity.class, expression.getReturnType());
        } finally {
            registry.clear(); registry.putAll(original);
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void metaCreationUsesResolvedSchemaAndHandlesNullConstructorResults() throws Exception {
        Map registry = registry(MetaFieldRegistry.INSTANCE);
        Map original = new HashMap(registry);
        EntityType type = EntityTypes.ZOMBIE;
        EntityMeta meta = mock(EntityMeta.class, withSettings().mockMaker(INLINE));
        var results = new java.util.concurrent.atomic.AtomicReference<>(meta);
        FieldSchema<EntityType, EntityMeta> schema = new FieldSchema<>(type, List.of(), ignored -> results.get());
        registry.put(type, schema);
        Expression<?>[] inputs = {mock(Expression.class)};
        try (var fields = mockStatic(FieldSections.class, withSettings().mockMaker(INLINE).defaultAnswer(CALLS_REAL_METHODS));
             var conversion = mockStatic(SpigotConversionUtil.class, withSettings().mockMaker(INLINE))) {
            fields.when(() -> FieldSections.entityType(inputs[0])).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
            conversion.when(() -> SpigotConversionUtil.fromBukkitEntityType(org.bukkit.entity.EntityType.ZOMBIE)).thenReturn(type);
            var expression = new SecExprNewMeta();
            assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs), null, null));
            assertArrayEquals(new EntityMeta[]{meta}, expression.get(null));
            results.set(null);
            assertArrayEquals(new EntityMeta[0], expression.get(null));
            assertTrue(expression.isSingle());
            assertEquals(EntityMeta.class, expression.getReturnType());
        } finally {
            registry.clear(); registry.putAll(original);
        }
    }

    @Test
    void genericEntityCreationRequiresSection() {
        try (var logging = mockStatic(Skript.class, withSettings().mockMaker(INLINE))) {
            var expression = new SecExprNewFakeEntity();
            Expression<?>[] inputs = new Expression<?>[0];
            assertFalse(expression.init(inputs, 1, Kleenean.FALSE, new ParseResult("", inputs), null, null));
            logging.verify(() -> Skript.error(contains("requires a section")));
        }
    }

    @Test
    void invalidEntityTypeRejectsBothSections() {
        try (var logging = mockStatic(Skript.class, withSettings().mockMaker(INLINE))) {
            Expression<?>[] inputs = {mock(Expression.class)};
            var result = new ParseResult("", inputs);
            assertFalse(new SecExprNewFakeEntity().init(inputs, 0, Kleenean.FALSE, result, null, null));
            assertFalse(new SecExprNewMeta().init(inputs, 0, Kleenean.FALSE, result, null, null));
        }
    }
}
