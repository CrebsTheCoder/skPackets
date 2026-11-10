package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.crebs.skpackets.api.general.packet.PacketTypeRegistry;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.*;

import java.lang.reflect.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketWrapperConstructionTest {
    @BeforeAll static void initialize() {
        PacketEventsTestBootstrap.initialize();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            PacketFieldRegistry.INSTANCE.registerAll();
        }
    }

    @TestFactory List<DynamicTest> constructsEveryDiscoveredWrapper() {
        List<DynamicTest> tests = new ArrayList<>();
        for (PacketTypeCommon type : PacketTypeRegistry.getAllPackets()) {
            var wrapper = PacketFieldRegistrar.wrapperClass(type);
            if (wrapper == null) continue;
            var schema = PacketFieldRegistry.INSTANCE.getSchema(type);
            if (schema.getAccessor("raw data") != null) continue;
            if (schema.constructor().getClass().getNestHost() != PacketFieldRegistrar.class) continue;
            var constructor = PacketConstructor.find(wrapper);
            var methods = PacketReflection.methods(wrapper);
            tests.add(DynamicTest.dynamicTest(wrapper.getSimpleName(), () -> {
                Map<String, Object> values = new HashMap<>();
                for (var argument : constructor.arguments()) {
                    if (argument.injected() || argument.nullable()) continue;
                    String name = PacketFieldBinding.parameter(wrapper, argument, methods).name();
                    if (schema.getAccessor(name).isOptional()) continue;
                    values.put(name, sample(argument.type()));
                }
                PacketWrapper<?> packet = PacketCreationCoverageTest.create(type, values);
                assertInstanceOf(wrapper, packet);
                assertEquals(type, packet.getPacketTypeData().getPacketType(), "Constructor must preserve direction/state/type");
            }));
        }
        assertTrue(tests.size() > 250, "Real construction coverage unexpectedly shrank: " + tests.size());
        return tests;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object sample(Type type) {
        Class<?> raw = PacketArguments.raw(type);
        if (raw == boolean.class || raw == Boolean.class) return true;
        if (raw == int.class || raw == Integer.class) return 1;
        if (raw == byte.class || raw == Byte.class) return (byte) 1;
        if (raw == short.class || raw == Short.class) return (short) 1;
        if (raw == long.class || raw == Long.class) return 1L;
        if (raw == float.class || raw == Float.class) return 1f;
        if (raw == double.class || raw == Double.class) return 1d;
        if (raw == String.class) return "test";
        if (raw == UUID.class) return new UUID(0, 1);
        if (raw.isEnum()) return raw.getEnumConstants()[0];
        if (raw == Optional.class) return Optional.empty();
        if (raw == EnumSet.class) return EnumSet.noneOf((Class) PacketArguments.raw(((ParameterizedType) type).getActualTypeArguments()[0]));
        if (Set.class.isAssignableFrom(raw)) return Set.of();
        if (Collection.class.isAssignableFrom(raw)) return List.of();
        if (Map.class.isAssignableFrom(raw)) return Map.of();
        if (raw.isArray()) return Array.newInstance(raw.getComponentType(), 0);
        if (raw == net.kyori.adventure.text.Component.class) return net.kyori.adventure.text.Component.text("Test");
        if (raw == com.github.retrooper.packetevents.resources.ResourceLocation.class) return new com.github.retrooper.packetevents.resources.ResourceLocation("minecraft:test");
        if (raw == com.github.retrooper.packetevents.protocol.nbt.NBTCompound.class) return new com.github.retrooper.packetevents.protocol.nbt.NBTCompound();
        if (raw == com.github.retrooper.packetevents.protocol.item.ItemStack.class) return com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY;
        if (raw == com.github.retrooper.packetevents.util.Vector3i.class) return new com.github.retrooper.packetevents.util.Vector3i(1, 2, 3);
        if (raw == com.github.retrooper.packetevents.util.Vector3f.class) return new com.github.retrooper.packetevents.util.Vector3f(1, 2, 3);
        if (raw == com.github.retrooper.packetevents.util.Vector3d.class) return new com.github.retrooper.packetevents.util.Vector3d(1, 2, 3);
        return mock(raw, RETURNS_DEEP_STUBS);
    }
}

