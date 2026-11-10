package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Type;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PacketArgumentsTest {
    List<UUID> ids;
    Optional<List<UUID>> optionalIds;
    List<Vector3d> vectors;
    EnumSet<InteractionHand> hands;

    private static Type type(String field) throws Exception { return PacketArgumentsTest.class.getDeclaredField(field).getGenericType(); }

    @Test void usesProperScalarAndCollectionTypes() throws Exception {
        assertSame(Number.class, PacketArguments.expressionType(int.class));
        assertSame(Boolean.class, PacketArguments.expressionType(boolean.class));
        assertSame(Number[].class, PacketArguments.expressionType(byte[].class));
        assertSame(UUID[].class, PacketArguments.expressionType(type("ids")));
        assertSame(UUID[].class, PacketArguments.expressionType(type("optionalIds")));
        assertSame(Vector[].class, PacketArguments.expressionType(type("vectors")));
        assertSame(InteractionHand[].class, PacketArguments.expressionType(type("hands")));
        assertSame(org.bukkit.inventory.EquipmentSlot.class, PacketArguments.expressionType(EquipmentSlot.class));
        assertSame(org.bukkit.inventory.ItemStack.class, PacketArguments.expressionType(com.github.retrooper.packetevents.protocol.item.ItemStack.class));
    }

    @ParameterizedTest @EnumSource(org.bukkit.inventory.EquipmentSlot.class)
    void equipmentUsesNativeSlotsInBothDirections(org.bukkit.inventory.EquipmentSlot slot) {
        Object packetSlot = PacketArguments.convert(slot, EquipmentSlot.class, "slot");
        assertInstanceOf(EquipmentSlot.class, packetSlot);
        assertSame(slot, PacketArguments.toSkript(packetSlot));
    }

    @ParameterizedTest @EnumSource(org.bukkit.SoundCategory.class)
    void soundCategoriesUseNativeValuesIncludingPluralNames(org.bukkit.SoundCategory category) {
        assertEquals(category, PacketArguments.toSkript(PacketArguments.convert(category,
                com.github.retrooper.packetevents.protocol.sound.SoundCategory.class, "sound category")));
    }

    @ParameterizedTest @EnumSource(org.bukkit.GameMode.class)
    void gameModesUseNativeValues(org.bukkit.GameMode mode) {
        assertEquals(mode, PacketArguments.toSkript(PacketArguments.convert(mode,
                com.github.retrooper.packetevents.protocol.player.GameMode.class, "game mode")));
    }

    @ParameterizedTest @EnumSource(org.bukkit.Difficulty.class)
    void difficultyUsesNativeValues(org.bukkit.Difficulty difficulty) {
        assertEquals(difficulty, PacketArguments.toSkript(PacketArguments.convert(difficulty,
                com.github.retrooper.packetevents.protocol.world.Difficulty.class, "difficulty")));
    }

    @ParameterizedTest @EnumSource(org.bukkit.inventory.MainHand.class)
    void mainHandUsesNativeValues(org.bukkit.inventory.MainHand hand) {
        assertEquals(hand, PacketArguments.toSkript(PacketArguments.convert(hand,
                com.github.retrooper.packetevents.protocol.player.HumanoidArm.class, "main hand")));
    }

    @Test void convertsListsOptionalsAndEnumSetsWithoutStringCoercion() throws Exception {
        UUID uuid = UUID.randomUUID();
        assertEquals(List.of(uuid), PacketArguments.convert(new UUID[]{uuid}, type("ids"), "ids"));
        assertEquals(Optional.empty(), PacketArguments.convert(null, type("optionalIds"), "ids"));
        assertEquals(Optional.of(List.of(uuid)), PacketArguments.convert(new UUID[]{uuid}, type("optionalIds"), "ids"));
        assertEquals(EnumSet.of(InteractionHand.MAIN_HAND), PacketArguments.convert(new InteractionHand[]{InteractionHand.MAIN_HAND}, type("hands"), "hands"));
        assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert("main hand", InteractionHand.class, "hand"));
        assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert("true", boolean.class, "enabled"));
        assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert("12", int.class, "count"));
    }

    @Test void convertsVectorsWithoutLosingDoublePrecision() throws Exception {
        Vector vector = new Vector(1.1234567890123, 2, -4);
        var result = (List<?>) PacketArguments.convert(new Vector[]{vector}, type("vectors"), "vectors");
        assertEquals(vector.getX(), ((Vector3d) result.getFirst()).getX());
        assertEquals(vector, PacketArguments.toSkript(result.getFirst()));
    }

    @Test void validatesNumbersAndByteRanges() {
        assertArrayEquals(new byte[]{0, -1, -128}, (byte[]) PacketArguments.convert(new Number[]{0L, 255L, -128L}, byte[].class, "data"));
        for (Number bad : new Number[]{256, -129, 1.5, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert(bad, byte.class, "data"));
        assertThrows(IllegalArgumentException.class, () -> PacketArguments.convert(Long.MAX_VALUE, int.class, "count"));
        assertEquals(Long.MAX_VALUE, PacketArguments.convert(Long.MAX_VALUE, long.class, "id"));
    }

    @Test void leavesQueuedModuleTypesToTheirExistingRegistrations() {
        assertTrue(PacketValueTypes.managedByModule(InteractionHand.class));
        assertTrue(PacketValueTypes.managedByModule(org.crebs.skpackets.api.entity.metadata.EntityMeta.class));
        assertFalse(PacketValueTypes.managedByModule(com.github.retrooper.packetevents.protocol.player.Equipment.class));
    }








    @Test void twoDimensionalVectorsUseNativeXAndZCoordinates() {
        assertSame(org.crebs.skpackets.api.general.Vector2.class, PacketArguments.expressionType(com.github.retrooper.packetevents.util.Vector2i.class));
        var value = (com.github.retrooper.packetevents.util.Vector2i) PacketArguments.convert(
                new Vector(3, 99, -2), com.github.retrooper.packetevents.util.Vector2i.class, "chunk position");
        assertEquals(3, value.getX());
        assertEquals(-2, value.getZ());
        assertEquals(new org.crebs.skpackets.api.general.Vector2(3, -2), PacketArguments.toSkript(value));
    }




}
