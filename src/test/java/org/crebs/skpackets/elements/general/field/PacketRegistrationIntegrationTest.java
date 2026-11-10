package org.crebs.skpackets.elements.general.field;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import com.github.shanebeee.skr.Registration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.crebs.skpackets.elements.general.GeneralModule;
import org.crebs.skpackets.elements.simple.SimpleModule;
import org.crebs.skpackets.elements.team.TeamModule;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.crebs.skpackets.support.SkriptRegistrationTestSupport;
import org.junit.jupiter.api.Test;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketRegistrationIntegrationTest {
    @SuppressWarnings("removal")
    @Test
    void finalizesRealClassInfosAndSyntaxRegistrations() throws Exception {
        PacketEventsTestBootstrap.initialize();
        var core = org.skriptlang.skript.Skript.of(getClass(), "SkriptIntegrationCore");
        core.storeRegistry(EventValueRegistry.class, EventValueRegistry.empty(core));
        try (var snapshots = new RegistrySnapshots(Classes.class, Comparators.class, Converters.class, org.crebs.skpackets.elements.general.section.SecExprNewPacket.class);
             var skript = mockStatic(Skript.class);
             var bukkit = mockStatic(Bukkit.class)) {
            skript.when(Skript::instance).thenReturn(core);
            skript.when(Skript::getVersion).thenReturn(new ch.njol.skript.util.Version("2.17.1"));
            var runtimeErrors = mock(org.skriptlang.skript.log.runtime.RuntimeErrorManager.class);
            when(runtimeErrors.removeAllConsumers()).thenReturn(List.of());
            when(runtimeErrors.removeConsumer(any())).thenReturn(true);
            skript.when(Skript::getRuntimeErrorManager).thenReturn(runtimeErrors);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            var server = mock(org.bukkit.Server.class, RETURNS_DEEP_STUBS);
            when(server.getServerConfig().isProxyOnlineMode()).thenReturn(true);
            bukkit.when(Bukkit::getServer).thenReturn(server);
            Field packetRegistry = org.crebs.skpackets.api.util.field.BaseFieldRegistry.class.getDeclaredField("REGISTRY");
            packetRegistry.setAccessible(true);
            Map<?, ?> packetSchemas = (Map<?, ?>) packetRegistry.get(PacketFieldRegistry.INSTANCE);
            snapshots.capture(packetSchemas);
            packetSchemas.clear();
            try (var functions = SkriptRegistrationTestSupport.initialize()) {
                Registration registration = new Registration("PacketRegistrationIntegration", true);
                registration.getAddon().loadModules(new GeneralModule(registration), new SimpleModule(registration), new TeamModule(registration));
                org.crebs.skpackets.elements.entity.Types.register(registration);
                org.crebs.skpackets.elements.entity.types.PacketEntity.register(registration);
                Field infoField = Registration.TypeRegistrar.class.getDeclaredField("classInfo");
                infoField.setAccessible(true);
                List<ClassInfo<?>> infos = new ArrayList<>();
                Set<Class<?>> classes = new HashSet<>();
                Set<String> codes = new HashSet<>();
                for (var queued : registration.getTypes()) {
                    ClassInfo<?> info = (ClassInfo<?>) infoField.get(queued);
                    assertTrue(classes.add(info.getC()), () -> "Duplicate queued class: " + info.getC());
                    assertTrue(codes.add(info.getCodeName()), () -> "Duplicate queued codename: " + info.getCodeName());
                    infos.add(info);
                }
                assertTrue(infos.size() < 25, "Packet expansion must not register dozens of public types");
                assertTrue(classes.contains(PacketValue.class));
                assertTrue(classes.contains(org.crebs.skpackets.api.general.Vector2.class));
                assertFalse(classes.contains(com.github.retrooper.packetevents.protocol.player.Equipment.class));
                assertFalse(classes.contains(com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar.Action.class));
                assertDoesNotThrow(registration::finalizeRegistration);
                for (ClassInfo<?> info : infos) {
                    assertSame(info, Classes.getExactClassInfo(info.getC()));
                    assertSame(info, Classes.getClassInfoNoError(info.getCodeName()));

                }
                assertNull(Classes.getExactClassInfo(com.github.retrooper.packetevents.protocol.ConnectionState.class));
                assertEquals(1, registration.getFunctions().size());
                Field functionField = Registration.FunctionRegistrar.class.getDeclaredField("function");
                functionField.setAccessible(true);
                var function = (org.skriptlang.skript.common.function.DefaultFunction<?>) functionField.get(registration.getFunctions().getFirst());
                assertArrayEquals(new Object[]{new org.crebs.skpackets.api.general.Vector2(2, 3)},
                        ((ch.njol.skript.lang.function.Function<?>)function).execute(null, new Object[][]{{2L}, {3L}}));
                Classes.onRegistrationsStop();
                for (String syntax : List.of("vec2(2, 3)", "vec2(2,3)", "vector2 2, 3", "vector2 2,3")) {
                    try (var parseLog = new ch.njol.skript.log.ParseLogHandler().start()) {
                        var parsed = new ch.njol.skript.lang.SkriptParser(syntax).parseExpression(org.crebs.skpackets.api.general.Vector2.class);
                        assertNotNull(parsed, "Skript must parse " + syntax + ": " + parseLog.getError());
                        assertEquals(new org.crebs.skpackets.api.general.Vector2(2, 3), parsed.getSingle(mock(org.bukkit.event.Event.class)));
                    }
                }
                var vectorInfo = Classes.getExactClassInfo(org.crebs.skpackets.api.general.Vector2.class);
                assertEquals("x: 1, y: 5", Classes.toString(new org.crebs.skpackets.api.general.Vector2(1, 5)));
                assertEquals("x: 1.25, y: -5.5", Classes.toString(new org.crebs.skpackets.api.general.Vector2(1.25, -5.5)));
                var vector = new org.crebs.skpackets.api.general.Vector2(2.125, -3.5);
                var storage = new ch.njol.yggdrasil.Yggdrasil();
                storage.registerClassResolver(vectorInfo.getSerializer());
                var bytes = new java.io.ByteArrayOutputStream();
                try (var output = storage.newOutputStream(bytes)) { output.writeObject(vector); }
                try (var input = storage.newInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
                    assertEquals(vector, input.readObject(org.crebs.skpackets.api.general.Vector2.class));
                }
                assertNotNull(Classes.getExactClassInfo(org.crebs.skpackets.api.entity.metadata.EntityMeta.class));
                assertFalse(registration.getEffects().isEmpty());
                assertFalse(registration.getExpressions().isEmpty());
            }
        }
    }

    private static final class RegistrySnapshots implements AutoCloseable {
        private final Map<Object, Object> snapshots = new IdentityHashMap<>();
        RegistrySnapshots(Class<?>... owners) throws IllegalAccessException {
            for (Class<?> owner : owners) {
                for (Field field : owner.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) continue;
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value instanceof Map<?, ?> map) snapshots.put(value, new HashMap<>(map));
                    else if (value instanceof Collection<?> collection) snapshots.put(value, new ArrayList<>(collection));
                }
            }
        }
        void capture(Map<?, ?> map) { snapshots.put(map, new HashMap<>(map)); }
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override public void close() {
            snapshots.forEach((target, saved) -> {
                if (target instanceof Map map) {
                    map.clear();
                    map.putAll((Map) saved);
                } else if (target instanceof Collection collection) {
                    collection.clear();
                    collection.addAll((Collection) saved);
                }
            });
        }
    }
}

