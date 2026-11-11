package org.crebs.skpackets.support;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.registrations.Classes;

import static org.mockito.Mockito.*;

/** Supplies core host metadata missing from standalone addon registration tests. */
public final class SkriptRegistrationTestSupport {
    private SkriptRegistrationTestSupport() {}
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static AutoCloseable initialize() throws Exception {
        try (var classes = mockStatic(Classes.class, CALLS_REAL_METHODS)) {
            classes.when(() -> Classes.getExactClassInfo(any(Class.class))).thenAnswer(call -> {
                ClassInfo info = mock(ClassInfo.class);
                when(info.getParser()).thenReturn(mock(Parser.class));
                return info;
            });
            Class.forName("ch.njol.skript.SkriptConfig");
        }
        for (var entry : java.util.Map.ofEntries(
                java.util.Map.entry(Object.class, "object"), java.util.Map.entry(Number.class, "number"),
                java.util.Map.entry(Boolean.class, "boolean"), java.util.Map.entry(String.class, "string"),
                java.util.Map.entry(org.bukkit.entity.Entity.class, "entity"), java.util.Map.entry(org.bukkit.entity.Player.class, "player"),
                java.util.Map.entry(org.bukkit.block.data.BlockData.class, "blockdata"), java.util.Map.entry(org.bukkit.inventory.EquipmentSlot.class, "equipmentslot"),
                java.util.Map.entry(ch.njol.skript.aliases.ItemType.class, "itemtype"), java.util.Map.entry(org.bukkit.Location.class, "location")).entrySet()) {
            if (Classes.getExactClassInfo(entry.getKey()) == null)
                Classes.registerClass(new ClassInfo(entry.getKey(), entry.getValue()).user(entry.getValue() + "s?").parser(new Parser() {
                    @Override public Object parse(String text, ch.njol.skript.lang.ParseContext context) {
                        if (entry.getKey() == Number.class) {
                            try { return new java.math.BigDecimal(text); } catch (NumberFormatException ignored) { return null; }
                        }
                        if (entry.getKey() == Boolean.class) return text.equals("true") ? true : text.equals("false") ? false : null;
                        return null;
                    }
                    @Override public String toString(Object value, int flags) { return String.valueOf(value); }
                    @Override public String toVariableNameString(Object value) { return String.valueOf(value); }
                }));
        }
        var registry = ch.njol.skript.lang.function.FunctionRegistry.class.getDeclaredField("registry");
        registry.setAccessible(true);
        Object previous = registry.get(null);
        registry.set(null, new ch.njol.skript.lang.function.FunctionRegistry());
        var sortedTypes = Classes.class.getDeclaredField("classInfos");
        sortedTypes.setAccessible(true);
        Object previousTypes = sortedTypes.get(null);
        return () -> { registry.set(null, previous); sortedTypes.set(null, previousTypes); };
    }
}
