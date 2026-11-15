package org.crebs.skpackets.elements.general.field;

import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.lang.reflect.Type;
import static org.mockito.Mockito.*;

class PacketConstructionAuditTest {
    private static final Set<String> REVIEWED_GAPS = Set.of(
            "com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk",
            "com.github.retrooper.packetevents.protocol.world.chunk.palette.Palette",
            "com.github.retrooper.packetevents.protocol.world.chunk.storage.BaseStorage");
    @Test void reportConstructionGaps() throws Exception {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
        org.junit.jupiter.api.Assertions.assertFalse(PacketFieldRegistry.INSTANCE.getAllSchemas().isEmpty());
        var gaps = new TreeMap<String, Set<String>>();
        for (var schema : PacketFieldRegistry.INSTANCE.getAllSchemas())
            for (var field : schema.accessors()) visit(field.nativeType(), schema.type().getName() + "." + field.name(), new HashSet<>(), gaps);
        var lines = new ArrayList<String>();
        gaps.forEach((type, paths) -> lines.add(type + " -> " + String.join(", ", paths)));
        var report = java.nio.file.Path.of("build/construction-gaps.txt");
        java.nio.file.Files.createDirectories(report.getParent());
        java.nio.file.Files.write(report, lines);
        var unreviewed = new TreeSet<>(gaps.keySet());
        unreviewed.removeAll(REVIEWED_GAPS);
        org.junit.jupiter.api.Assertions.assertTrue(unreviewed.isEmpty(),
                () -> "Unreviewed construction gaps (see " + report + "): " + unreviewed);
    }
    private void visit(Type type, String path, Set<Type> visited, Map<String, Set<String>> gaps) {
        if (!visited.add(type)) return;
        var raw = PacketArguments.raw(type);
        var publicType = PacketArguments.expressionType(type);
        if (raw == Optional.class) { visit(PacketArguments.argument(type, 0), path, visited, gaps); return; }
        if (Map.class.isAssignableFrom(raw)) {
            visit(PacketArguments.argument(type, 0), path + ".key", new HashSet<>(visited), gaps);
            visit(PacketArguments.argument(type, 1), path + ".value", new HashSet<>(visited), gaps); return;
        }
        if (raw.isArray() || Collection.class.isAssignableFrom(raw)) {
            visit(PacketRecords.elementType(type), path + "[]", visited, gaps); return;
        }
        if (publicType != PacketValue.class) return;
        var shape = PacketRecords.shape(type);
        if (shape == null) { gaps.computeIfAbsent(type.getTypeName(), k -> new TreeSet<>()).add(path); return; }
        var alternatives = shape.alternatives().isEmpty() ? List.of(shape) : shape.alternatives();
        for (var alternative : alternatives) for (var field : alternative.fields())
            visit(field.nativeType(), path + "." + field.name(), new HashSet<>(visited), gaps);
    }
}
