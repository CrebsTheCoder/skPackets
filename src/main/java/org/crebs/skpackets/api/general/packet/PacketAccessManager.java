package org.crebs.skpackets.api.general.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import org.jspecify.annotations.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class PacketAccessManager {
    private PacketAccessManager() {}

    private static final class Access {
        final List<Method> methods;
        final Map<Method, Function<Object, Object>> readers = new ConcurrentHashMap<>();
        final Map<Method, BiConsumer<Object, Object>> writers = new ConcurrentHashMap<>();
        final Map<Constructor<?>, Function<Object[], Object>> constructors = new ConcurrentHashMap<>();

        Access(Class<?> type) {
            methods = Arrays.stream(type.getMethods())
                    .filter(method -> !method.isBridge() && !method.isSynthetic())
                    .filter(method -> !Modifier.isStatic(method.getModifiers()))
                    .sorted(Comparator.comparing(Method::toGenericString)).toList();
        }
    }

    private static final ClassValue<Access> ACCESS = new ClassValue<>() {
        @Override protected Access computeValue(@NonNull Class<?> type) { return new Access(type); }
    };
    private static final Map<PacketTypeCommon, Function<ProtocolPacketEvent, PacketWrapper<?>>> EVENT_FACTORIES =
            new ConcurrentHashMap<>();

    public static List<Method> methods(Class<?> type) {
        return ACCESS.get(type).methods;
    }

    public static Function<Object, Object> reader(Method method) {
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0
                || method.getReturnType() == void.class) {
            throw new IllegalArgumentException("Expected an instance getter: " + method);
        }
        return ACCESS.get(method.getDeclaringClass()).readers.computeIfAbsent(method, key -> {
            MethodHandle handle = unreflect(key).asType(MethodType.methodType(Object.class, Object.class));
            return target -> {
                try { return (Object) handle.invokeExact(target); }
                catch (Throwable failure) { throw failure("PacketEvents field access failed: " + key.getName(), failure); }
            };
        });
    }

    public static BiConsumer<Object, Object> writer(Method method) {
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) {
            throw new IllegalArgumentException("Expected an instance setter: " + method);
        }
        return ACCESS.get(method.getDeclaringClass()).writers.computeIfAbsent(method, key -> {
            MethodHandle handle = unreflect(key).asType(MethodType.methodType(void.class, Object.class, Object.class));
            return (target, value) -> {
                try { handle.invokeExact(target, value); }
                catch (Throwable failure) { throw failure("PacketEvents field access failed: " + key.getName(), failure); }
            };
        });
    }

    public static Function<Object[], Object> constructor(Constructor<?> constructor) {
        return ACCESS.get(constructor.getDeclaringClass()).constructors.computeIfAbsent(constructor, key -> {
            final MethodHandle handle;
            try {
                handle = MethodHandles.publicLookup().unreflectConstructor(key).asFixedArity()
                        .asSpreader(Object[].class, key.getParameterCount())
                        .asType(MethodType.methodType(Object.class, Object[].class));
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot access PacketEvents constructor " + key, exception);
            }
            return arguments -> {
                try { return (Object) handle.invokeExact(arguments); }
                catch (Throwable failure) { throw failure("PacketEvents construction failed: " + key, failure); }
            };
        });
    }

    public static void registerEventFactory(PacketTypeCommon type,
            Function<ProtocolPacketEvent, PacketWrapper<?>> factory) {
        EVENT_FACTORIES.put(Objects.requireNonNull(type), Objects.requireNonNull(factory));
    }

    public static PacketWrapper<?> wrap(PacketTypeCommon type, ProtocolPacketEvent event) {
        return EVENT_FACTORIES.computeIfAbsent(type, key -> {
            if (key == PacketType.Play.Server.KEEP_ALIVE) {
                return packetEvent -> new WrapperPlayServerKeepAlive((PacketSendEvent) packetEvent);
            }
            if (key == PacketType.Play.Client.KEEP_ALIVE) {
                return packetEvent -> new WrapperPlayClientKeepAlive((PacketReceiveEvent) packetEvent);
            }
            Class<? extends PacketWrapper<?>> wrapper = key.getWrapperClass();
            if (wrapper == null) return ignored -> null;
            try {
                var factory = constructor(wrapper.getConstructor(key.getSide() == PacketSide.SERVER
                        ? PacketSendEvent.class : PacketReceiveEvent.class));
                return packetEvent -> (PacketWrapper<?>) factory.apply(new Object[] {packetEvent});
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Cannot find event constructor for " + key, exception);
            }
        }).apply(event);
    }

    public static void clearEventFactories() {
        EVENT_FACTORIES.clear();
    }

    private static MethodHandle unreflect(Method method) {
        try { return MethodHandles.publicLookup().unreflect(method).asFixedArity(); }
        catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access PacketEvents method " + method, exception);
        }
    }

    private static IllegalArgumentException failure(String message, Throwable cause) {
        if (cause instanceof Error error) throw error;
        return new IllegalArgumentException(message, cause);
    }
}
