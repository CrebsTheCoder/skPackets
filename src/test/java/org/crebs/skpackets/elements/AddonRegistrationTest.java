package org.crebs.skpackets.elements;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import com.github.shanebeee.skr.Registration;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.crebs.skpackets.api.entity.metadata.EntityMeta;
import org.crebs.skpackets.api.util.field.BaseFieldRegistry;
import org.crebs.skpackets.elements.general.field.PacketFieldRegistry;
import org.crebs.skpackets.elements.general.section.SecExprNewPacket;
import org.crebs.skpackets.elements.entity.section.SecExprNewFakeEntity;
import org.crebs.skpackets.elements.entity.section.SecExprNewMeta;
import org.skriptlang.skript.Skript;
import org.bukkit.plugin.PluginManager;
import org.crebs.skpackets.elements.general.GeneralModule;
import org.crebs.skpackets.elements.entity.EntityModule;
import org.crebs.skpackets.elements.entity.field.entity.FakeEntityFieldRegistry;
import org.crebs.skpackets.elements.entity.field.meta.MetaFieldRegistry;
import org.crebs.skpackets.elements.simple.SimpleModule;
import org.crebs.skpackets.elements.team.TeamModule;
import org.crebs.skpackets.support.PacketEventsTestBootstrap;
import org.junit.jupiter.api.Test;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddonRegistrationTest {
    @Test
    void registersAllModulesWithoutDuplicateTypes() throws Exception {
        PacketEventsTestBootstrap.initialize();
        Skript skript = Skript.of(AddonRegistrationTest.class, "skPackets");
        skript.storeRegistry(EventValueRegistry.class, EventValueRegistry.empty(skript));
        try (var snapshots = new RegistrySnapshots(
                Classes.class, Comparators.class, Converters.class,
                SecExprNewPacket.class, SecExprNewFakeEntity.class, SecExprNewMeta.class);
             var skriptPlugin = mockStatic(ch.njol.skript.Skript.class);
             var bukkit = mockStatic(Bukkit.class)) {
            skriptPlugin.when(ch.njol.skript.Skript::instance).thenReturn(skript);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            var server = mock(Server.class, RETURNS_DEEP_STUBS);
            when(server.getServerConfig().isProxyOnlineMode()).thenReturn(true);
            bukkit.when(Bukkit::getServer).thenReturn(server);
            Field packetRegistry = BaseFieldRegistry.class.getDeclaredField("REGISTRY");
            packetRegistry.setAccessible(true);
            Map<?, ?> packetSchemas = (Map<?, ?>) packetRegistry.get(PacketFieldRegistry.INSTANCE);
            snapshots.capture(packetSchemas);
            packetSchemas.clear();
            snapshots.captureRegistry(FakeEntityFieldRegistry.INSTANCE);
            snapshots.captureRegistry(MetaFieldRegistry.INSTANCE);
            Registration registration = new Registration("skPackets", true);
            registration.getAddon().loadModules(
                    new GeneralModule(registration),
                    new SimpleModule(registration),
                    new EntityModule(registration),
                    new TeamModule(registration));
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
            assertFalse(infos.isEmpty());
            assertDoesNotThrow(registration::finalizeRegistration);
            for (ClassInfo<?> info : infos) {
                assertSame(info, Classes.getExactClassInfo(info.getC()));
                assertSame(info, Classes.getClassInfoNoError(info.getCodeName()));
            }
            assertNotNull(Classes.getExactClassInfo(PacketTypeCommon.class));
            assertNotNull(Classes.getExactClassInfo(EntityMeta.class));
            assertFalse(registration.getEffects().isEmpty());
            assertFalse(registration.getExpressions().isEmpty());
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
        void captureRegistry(Object registry) throws IllegalAccessException {
            for (Class<?> owner = registry.getClass(); owner != Object.class; owner = owner.getSuperclass()) {
                for (Field field : owner.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    field.setAccessible(true);
                    if (field.get(registry) instanceof Map<?, ?> map) capture(map);
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

