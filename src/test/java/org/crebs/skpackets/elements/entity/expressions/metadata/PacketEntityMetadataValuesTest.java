package org.crebs.skpackets.elements.entity.expressions.metadata;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import me.tofaa.entitylib.meta.MetaFields;
import me.tofaa.entitylib.meta.Metadata;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.util.Vector;
import org.crebs.skpackets.api.entity.metadata.PacketEntityField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketEntityMetadataValuesTest {
    private WrapperEntity entity;
    private Metadata metadata;

    @BeforeEach
    void createEntity() {
        entity = mock(WrapperEntity.class);
        metadata = mock(Metadata.class, withSettings().mockMaker(org.mockito.MockMakers.INLINE));
        when(entity.getMetadata()).thenReturn(metadata);
    }

    @Test
    void glowUsesTheGlowingBitRatherThanReplacingEntityFlags() {
        when(entity.getEntityType()).thenReturn(EntityTypes.ZOMBIE);
        when(metadata.getFlag(MetaFields.Entity.GLOWING)).thenReturn(true);
        assertEquals(true, PacketEntityField.GLOWING.get(entity));
        PacketEntityField.GLOWING.set(entity, false);
        verify(entity).setFlag(MetaFields.Entity.GLOWING, false);
        verify(entity, never()).setMetadata(any(), any());
    }

    @Test
    void gravityInvertsTheWireNoGravityValue() {
        when(entity.getEntityType()).thenReturn(EntityTypes.ZOMBIE);
        when(metadata.get(MetaFields.Entity.NO_GRAVITY)).thenReturn(true);
        assertEquals(false, PacketEntityField.GRAVITY.get(entity));
        PacketEntityField.GRAVITY.set(entity, true);
        verify(entity).setMetadata(MetaFields.Entity.NO_GRAVITY, false);
        PacketEntityField.GRAVITY.reset(entity);
        verify(entity, times(2)).setMetadata(MetaFields.Entity.NO_GRAVITY, false);
    }

    @Test
    void customNameConvertsTextAndEmptyStringToOptionalComponent() {
        when(entity.getEntityType()).thenReturn(EntityTypes.ZOMBIE);
        when(metadata.get(MetaFields.Entity.CUSTOM_NAME))
                .thenReturn(Optional.of(Component.text("Crebs")));
        assertEquals("Crebs", PacketEntityField.CUSTOM_NAME.get(entity));
        PacketEntityField.CUSTOM_NAME.set(entity, "Cat");
        verify(entity).setMetadata(MetaFields.Entity.CUSTOM_NAME, Optional.of(Component.text("Cat")));
        PacketEntityField.CUSTOM_NAME.set(entity, "");
        verify(entity).setMetadata(MetaFields.Entity.CUSTOM_NAME, Optional.empty());
    }

    @Test
    void healthConvertsNumbersAndRejectsNonFiniteOrUnsupportedValues() {
        when(entity.getEntityType()).thenReturn(EntityTypes.ZOMBIE);
        when(metadata.get(MetaFields.LivingEntity.HEALTH)).thenReturn(7.5f);
        assertEquals(7.5f, PacketEntityField.HEALTH.get(entity));
        PacketEntityField.HEALTH.set(entity, 12);
        verify(entity).setMetadata(MetaFields.LivingEntity.HEALTH, 12f);
        assertThrows(IllegalArgumentException.class, () -> PacketEntityField.HEALTH.set(entity, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> PacketEntityField.HEALTH.set(entity, "12"));
        when(entity.getEntityType()).thenReturn(EntityTypes.TEXT_DISPLAY);
        assertThrows(IllegalArgumentException.class, () -> PacketEntityField.HEALTH.get(entity));
        assertThrows(IllegalArgumentException.class, () -> PacketEntityField.HEALTH.set(entity, 12));
    }

    @Test
    void displayVectorsConvertBothDirectionsAndDefaultsAreIndependent() {
        when(entity.getEntityType()).thenReturn(EntityTypes.TEXT_DISPLAY);
        when(metadata.get(MetaFields.Display.TRANSLATION)).thenReturn(new Vector3f(1, 2, 3));
        assertEquals(new Vector(1, 2, 3), PacketEntityField.TRANSLATION.get(entity));
        PacketEntityField.TRANSLATION.set(entity, new Vector(4, 5, 6));
        verify(entity).setMetadata(MetaFields.Display.TRANSLATION, new Vector3f(4, 5, 6));
        Vector first = (Vector) PacketEntityField.SCALE.get(entity);
        first.setX(99);
        assertEquals(new Vector(1, 1, 1), PacketEntityField.SCALE.get(entity));
    }

    @Test
    void teleportInterpolationRejectsOutOfRangeOrFractionalTicks() {
        when(entity.getEntityType()).thenReturn(EntityTypes.TEXT_DISPLAY);
        PacketEntityField.TELEPORT_DURATION.set(entity, 59);
        verify(entity).setMetadata(MetaFields.Display.POS_ROT_INTERPOLATION_DURATION, 59);
        for (Number value : new Number[]{-1, 60, 1.5, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,
                    () -> PacketEntityField.TELEPORT_DURATION.set(entity, value));
    }

    @Test
    void entityIdIsReadableButCannotBeChanged() {
        when(entity.getEntityType()).thenReturn(EntityTypes.ZOMBIE);
        when(entity.getEntityId()).thenReturn(42);
        assertEquals(42, PacketEntityField.ENTITY_ID.get(entity));
        assertThrows(IllegalArgumentException.class, () -> PacketEntityField.ENTITY_ID.set(entity, 7));
        verify(entity, never()).setMetadata(any(), any());
    }
}
