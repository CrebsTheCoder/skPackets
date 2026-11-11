package org.crebs.skpackets.elements.general.field;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketFieldRegistryOptionalPluginTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void missingSkBeeClassesDoNotPreventRegistryLoading(boolean pluginEnabled) throws Exception {
        var plugins = mock(PluginManager.class);
        when(plugins.isPluginEnabled("SkBee")).thenReturn(pluginEnabled);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(Bukkit::getConsoleSender).thenReturn(mock(ConsoleCommandSender.class));
            var loader = new WithoutSkBee();
            Class<?> registryClass = loader.loadClass(PacketFieldRegistry.class.getName());
            Object registry = registryClass.getField("INSTANCE").get(null);
            var registrars = (List<?>) registryClass.getMethod("getRegistrars").invoke(registry);
            assertTrue(registrars.stream().noneMatch(registrar -> registrar instanceof NbtPacketFieldRegistrar));
            assertEquals(pluginEnabled, loader.requestedSkBee);
        }
    }
    private static final class WithoutSkBee extends ClassLoader {
        boolean requestedSkBee;
        WithoutSkBee() { super(PacketFieldRegistryOptionalPluginTest.class.getClassLoader()); }

        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                if (name.startsWith("com.shanebeestudios.skbee.")) {
                    requestedSkBee = true;
                    throw new ClassNotFoundException(name);
                }
                String registry = PacketFieldRegistry.class.getName();
                if (!name.equals(registry) && !name.startsWith(registry + "$")
                        && !name.equals(DirectPacketFields.class.getName())) return super.loadClass(name, resolve);
                Class<?> result = findLoadedClass(name);
                if (result == null) {
                    try (var input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (input == null) throw new ClassNotFoundException(name);
                        byte[] bytes = input.readAllBytes();
                        result = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException exception) {
                        throw new ClassNotFoundException(name, exception);
                    }
                }
                if (resolve) resolveClass(result);
                return result;
            }
        }
    }
}
