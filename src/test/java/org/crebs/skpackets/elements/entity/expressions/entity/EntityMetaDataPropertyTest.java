package org.crebs.skpackets.elements.entity.expressions.entity;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.junit.jupiter.api.Test;
import org.bukkit.entity.Entity;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockMakers.INLINE;

class EntityMetaDataPropertyTest {
    @Test
    void realEntitiesReturnMetadataSnapshotsWithTheirOwnIds() {
        org.crebs.skpackets.support.PacketEventsTestBootstrap.initialize();
        var entityType = EntityTypes.ZOMBIE;
        Entity entity = mock(Entity.class);
        when(entity.getEntityId()).thenReturn(71);
        when(entity.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        EntityMeta snapshot = mock(EntityMeta.class, withSettings().mockMaker(INLINE));
        var expression = new ExprFakeEntityEntityMetaData();
        Expression<?>[] inputs = {mock(Expression.class)};
        assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        try (var meta = mockStatic(EntityMeta.class, withSettings().mockMaker(INLINE));
             var conversion = mockStatic(SpigotConversionUtil.class, withSettings().mockMaker(INLINE))) {
            conversion.when(() -> SpigotConversionUtil.fromBukkitEntityType(org.bukkit.entity.EntityType.ZOMBIE)).thenReturn(entityType);
            conversion.when(() -> SpigotConversionUtil.getEntityMetadata(entity)).thenReturn(List.of());
            meta.when(() -> EntityMeta.createMeta(71, entityType)).thenReturn(snapshot);
            assertArrayEquals(new EntityMeta[]{snapshot}, expression.get(null, new Object[]{entity}));
            var packet = org.mockito.ArgumentCaptor.forClass(WrapperPlayServerEntityMetadata.class);
            verify(snapshot).readPacket(packet.capture());
            assertEquals(71, packet.getValue().getEntityId());
            assertTrue(packet.getValue().getEntityMetadata().isEmpty());
            verifyNoMoreInteractions(snapshot);
        }
    }

    @Test
    void returnsLiveMetadataForEachFakeEntityAndIgnoresUnsupportedValues() {
        WrapperEntity first = mock(WrapperEntity.class), second = mock(WrapperEntity.class);
        EntityMeta firstMeta = mock(EntityMeta.class, withSettings().mockMaker(INLINE));
        EntityMeta secondMeta = mock(EntityMeta.class, withSettings().mockMaker(INLINE));
        var expression = new ExprFakeEntityEntityMetaData();
        Expression<?>[] inputs = {mock(Expression.class)};
        assertTrue(expression.init(inputs, 0, Kleenean.FALSE, new ParseResult("", inputs)));
        try (var meta = mockStatic(EntityMeta.class, withSettings().mockMaker(INLINE))) {
            meta.when(() -> EntityMeta.of(first)).thenReturn(firstMeta);
            meta.when(() -> EntityMeta.of(second)).thenReturn(secondMeta);
            assertArrayEquals(new EntityMeta[]{firstMeta, secondMeta}, expression.get(null, new Object[]{first, "ignored", null, second}));
            assertArrayEquals(new EntityMeta[0], expression.get(null, new Object[0]));
        }
        assertEquals(EntityMeta.class, expression.getReturnType());
    }
}
