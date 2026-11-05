package org.crebs.skpackets.elements.simple.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.crebs.skpackets.SkPackets;
import org.crebs.skpackets.api.entity.Skin;
import org.crebs.skpackets.api.simple.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockMakers.INLINE;
import static org.mockito.Mockito.*;

class SimpleEffectsTest {
    @BeforeAll
    static void initializeItemTypesWithoutServerVersionLookup() throws ClassNotFoundException {
        try (MockedStatic<Skript> ignored = statics(Skript.class)) {
            Class.forName(ItemType.class.getName());
        }
    }

    private static <T> MockedStatic<T> statics(Class<T> type) {
        return mockStatic(type, withSettings().mockMaker(INLINE));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private static <T> Expression<T> values(T... values) {
        Expression<T> expression = mock(Expression.class);
        when(expression.getArray(null)).thenReturn(values);
        when(expression.getAll(null)).thenReturn(values);
        when(expression.getSingle(null)).thenReturn(values.length == 0 ? null : values[0]);
        return expression;
    }

    private static void init(SyntaxElement element, int pattern, boolean persistent, Expression<?>... values) {
        SkriptParser.ParseResult result = new SkriptParser.ParseResult("", values);
        if (persistent) {
            result.tags.add("persistent");
        }

        assertTrue(element.init(values, pattern, Kleenean.FALSE, result));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fakeBlocksDispatchEveryLocationAndViewer(boolean persistent) {
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(INLINE));
        FakeBlockManager manager = mock(FakeBlockManager.class, withSettings().mockMaker(INLINE));
        when(plugin.getFakeBlocks()).thenReturn(manager);
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        Location a = new Location(null, 1.0, 2.0, 3.0);
        Location b = new Location(null, 4.0, 5.0, 6.0);
        BlockData data = mock(BlockData.class);
        try (MockedStatic<SkPackets> ignored = statics(SkPackets.class)) {
            ignored.when(SkPackets::getInstance).thenReturn(plugin);
            EffFakeBlock effect = new EffFakeBlock();
            init(effect, 0, persistent, values(a, b), values(data), values(player1, player2));
            effect.execute(null);

            for (Player viewer : new Player[]{player1, player2}) {
                verify(manager).show(viewer, a, data, persistent);
                verify(manager).show(viewer, b, data, persistent);
            }

            verifyNoMoreInteractions(manager);
        }
    }

    @Test
    void fakeBlocksResetClearAndMissingData() {
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(INLINE));
        FakeBlockManager manager = mock(FakeBlockManager.class, withSettings().mockMaker(INLINE));
        when(plugin.getFakeBlocks()).thenReturn(manager);
        Player player = mock(Player.class);
        Location location = new Location(null, 1.0, 2.0, 3.0);
        try (MockedStatic<SkPackets> ignored = statics(SkPackets.class)) {
            ignored.when(SkPackets::getInstance).thenReturn(plugin);
            EffFakeBlock reset = new EffFakeBlock();
            init(reset, 1, false, values(location), values(player));
            reset.execute(null);
            verify(manager).reset(player, location);
            EffFakeBlock clear = new EffFakeBlock();
            init(clear, 2, false, values(player));
            clear.execute(null);
            verify(manager).clear(player);
            EffFakeBlock missing = new EffFakeBlock();
            init(missing, 0, false, values(location), values(), values(player));
            missing.execute(null);
            verifyNoMoreInteractions(manager);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fakeInventoryShowsAllSelectedSlots(boolean persistent) {
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(INLINE));
        FakeInventoryManager manager = mock(FakeInventoryManager.class, withSettings().mockMaker(INLINE));
        when(plugin.getFakeInventory()).thenReturn(manager);
        Player player = mock(Player.class);
        ItemStack stack = mock(ItemStack.class);
        ItemType type = mock(ItemType.class);
        when(type.getRandom()).thenReturn(stack);
        try (MockedStatic<SkPackets> ignored = statics(SkPackets.class)) {
            ignored.when(SkPackets::getInstance).thenReturn(plugin);
            EffFakeInventorySlot effect = new EffFakeInventorySlot();
            init(effect, 0, persistent, values(0, 1, 45), values(type), values(player));
            effect.execute(null);

            for (int slot : new int[]{0, 1, 45}) {
                verify(manager).show(player, slot, stack, persistent);
            }

            verifyNoMoreInteractions(manager);
        }
    }

    @Test
    void fakeInventoryRejectsInvalidSlotsButContinuesWithValidSlots() {
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(INLINE));
        FakeInventoryManager manager = mock(FakeInventoryManager.class, withSettings().mockMaker(INLINE));
        when(plugin.getFakeInventory()).thenReturn(manager);
        Player player = mock(Player.class);
        try (MockedStatic<SkPackets> ignored = statics(SkPackets.class);
             MockedStatic<Skript> skript = statics(Skript.class)) {
            ignored.when(SkPackets::getInstance).thenReturn(plugin);
            EffFakeInventorySlot effect = new EffFakeInventorySlot();
            init(effect, 1, false, values(-1, 0.5, Double.NaN, Double.POSITIVE_INFINITY, (long) Integer.MAX_VALUE + 1, 4), values(player));
            effect.execute(null);
            verify(manager).reset(player, 4);
            verifyNoMoreInteractions(manager);
            skript.verify(() -> Skript.error("Fake inventory slots require nonnegative whole numbers."), times(5));
            EffFakeInventorySlot clear = new EffFakeInventorySlot();
            init(clear, 2, false, values(player));
            clear.execute(null);
            verify(manager).clear(player);
        }
    }

    @Test
    void fakeInventoryMissingItemDoesNotSendPackets() {
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(INLINE));
        FakeInventoryManager manager = mock(FakeInventoryManager.class, withSettings().mockMaker(INLINE));
        when(plugin.getFakeInventory()).thenReturn(manager);
        try (MockedStatic<SkPackets> ignored = statics(SkPackets.class)) {
            ignored.when(SkPackets::getInstance).thenReturn(plugin);
            EffFakeInventorySlot effect = new EffFakeInventorySlot();
            init(effect, 0, false, values(1), values(), values(mock(Player.class)));
            effect.execute(null);
            verifyNoInteractions(manager);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void equipmentShowResetAndClear(boolean persistent) {
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(INLINE));
        FakeEquipmentManager manager = mock(FakeEquipmentManager.class, withSettings().mockMaker(INLINE));
        when(plugin.getFakeEquipment()).thenReturn(manager);
        LivingEntity target = mock(LivingEntity.class);
        when(target.getEquipment()).thenReturn(mock(EntityEquipment.class));
        Player viewer = mock(Player.class);
        ItemType type = mock(ItemType.class);
        ItemStack stack = mock(ItemStack.class);
        when(type.getRandom()).thenReturn(stack);
        try (MockedStatic<SkPackets> ignored = statics(SkPackets.class)) {
            ignored.when(SkPackets::getInstance).thenReturn(plugin);
            EffFakeEquipment show = new EffFakeEquipment();
            init(show, 0, persistent, values(EquipmentSlot.HAND, EquipmentSlot.HEAD), values(target), values(type), values(viewer));
            show.execute(null);
            verify(manager).show(viewer, target, EquipmentSlot.HAND, stack, persistent);
            verify(manager).show(viewer, target, EquipmentSlot.HEAD, stack, persistent);
            EffFakeEquipment reset = new EffFakeEquipment();
            init(reset, 1, false, values(EquipmentSlot.HEAD), values(target), values(viewer));
            reset.execute(null);
            verify(manager).reset(viewer, target, EquipmentSlot.HEAD);
            EffFakeEquipment clear = new EffFakeEquipment();
            init(clear, 2, false, values(target), values(viewer));
            clear.execute(null);
            verify(manager).reset(viewer, target);
            verifyNoMoreInteractions(manager);
        }
    }

    @Test
    void equipmentRejectsEntitiesWithoutEquipment() {
        SkPackets plugin = mock(SkPackets.class, withSettings().mockMaker(INLINE));
        FakeEquipmentManager manager = mock(FakeEquipmentManager.class, withSettings().mockMaker(INLINE));
        when(plugin.getFakeEquipment()).thenReturn(manager);
        try (MockedStatic<SkPackets> ignored = statics(SkPackets.class);
             MockedStatic<Skript> skript = statics(Skript.class)) {
            ignored.when(SkPackets::getInstance).thenReturn(plugin);
            EffFakeEquipment effect = new EffFakeEquipment();
            init(effect, 2, false, values(mock(Entity.class), mock(LivingEntity.class)), values(mock(Player.class)));
            effect.execute(null);
            verifyNoInteractions(manager);
            skript.verify(() -> Skript.error("Displayed equipment requires a living entity with equipment."), times(2));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void displayedSkinUsesGlobalOrSpecificViewers(boolean persistent) {
        Player target = mock(Player.class);
        Player viewer = mock(Player.class);
        Skin skin = new Skin("texture", "signature");
        try (MockedStatic<PlayerSkinManager> skins = statics(PlayerSkinManager.class)) {
            EffDisplayedSkin global = new EffDisplayedSkin();
            init(global, 0, persistent, values(target), values(skin), null);
            global.execute(null);
            skins.verify(() -> PlayerSkinManager.setGlobalSkin(target, skin, persistent));
            EffDisplayedSkin specific = new EffDisplayedSkin();
            init(specific, 0, persistent, values(target), values(skin), values(viewer));
            specific.execute(null);
            skins.verify(() -> PlayerSkinManager.setSkinForViewers(target, List.of(viewer), skin, persistent));
            skins.verifyNoMoreInteractions();
        }
    }

    @Test
    void displayedSkinMissingSkinOrTargetsIsNoOp() {
        try (MockedStatic<PlayerSkinManager> skins = statics(PlayerSkinManager.class)) {
            EffDisplayedSkin effect = new EffDisplayedSkin();
            init(effect, 0, false, values(mock(Player.class)), values(), null);
            effect.execute(null);
            init(effect, 0, false, values(new Player[0]), values(new Skin("texture", "signature")), null);
            effect.execute(null);
            skins.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void glowDispatchesStateToViewerIds(boolean state) {
        Entity target = mock(Entity.class);
        when(target.getEntityId()).thenReturn(37);
        Player viewer = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(viewer.getUniqueId()).thenReturn(uuid);
        try (MockedStatic<GlowingEntityManager> glow = statics(GlowingEntityManager.class)) {
            EffGlow effect = new EffGlow();
            init(effect, 0, false, values(target), values(state), values(viewer, viewer));
            effect.execute(null);
            if (state) {
                glow.verify(() -> GlowingEntityManager.addGlowingReceivers(37, Set.of(uuid)));
            } else {
                glow.verify(() -> GlowingEntityManager.removeGlowingReceivers(37, Set.of(uuid)));
            }

            glow.verifyNoMoreInteractions();
        }
    }

    @Test
    void glowOmittedViewersUsesOnlinePlayersAndMissingStateIsNoOp() {
        Entity target = mock(Entity.class);
        when(target.getEntityId()).thenReturn(37);
        Player viewer = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(viewer.getUniqueId()).thenReturn(uuid);
        try (MockedStatic<GlowingEntityManager> glow = statics(GlowingEntityManager.class);
             MockedStatic<Bukkit> bukkit = statics(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(viewer));
            EffGlow effect = new EffGlow();
            init(effect, 0, false, values(target), values(true), null);
            effect.execute(null);
            glow.verify(() -> GlowingEntityManager.addGlowingReceivers(37, Set.of(uuid)));
            init(effect, 0, false, values(target), values(), null);
            effect.execute(null);
            glow.verifyNoMoreInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void glowExplicitEmptyViewersDoesNotAffectOnlinePlayers(boolean state) {
        Entity target = mock(Entity.class);
        Player player1 = mock(Player.class);
        try (MockedStatic<GlowingEntityManager> glow = statics(GlowingEntityManager.class);
             MockedStatic<Bukkit> bukkit = statics(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player1));
            EffGlow effect = new EffGlow();
            init(effect, 0, false, values(target), values(state), values(new Player[0]));
            effect.execute(null);
            glow.verifyNoInteractions();
            bukkit.verifyNoInteractions();
        }
    }
}
