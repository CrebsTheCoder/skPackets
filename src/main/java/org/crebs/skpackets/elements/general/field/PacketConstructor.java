package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.crebs.skpackets.api.util.field.ConstructionContext;

import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeAnnotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

record PacketConstructor(Constructor<?> method, List<PacketConstructor.Argument> arguments) {
    private static final Map<String, List<String>> PREFERRED = Map.of(
            "WrapperPlayClientInteractEntity", List.of("int", "InteractionHand", "Vector3d", "boolean"),
            "WrapperPlayServerBlockChange", List.of("Vector3i", "WrappedBlockState"),
            "WrapperPlayServerDestroyEntities", List.of("int[]"),
            "WrapperPlayServerEntityMetadata", List.of("int", "List"),
            "WrapperPlayClientPlayerDigging", List.of("DiggingAction", "Vector3i", "BlockFace", "int"),
            "WrapperPlayServerPlayerInfoUpdate", List.of("EnumSet", "List"),
            "WrapperPlayServerPlayerInfo", List.of("Action", "List"),
            "WrapperPlayServerSpawnLivingEntity", List.of("int", "UUID", "EntityType", "Vector3d", "float", "float", "float", "Vector3d", "List"),
            "WrapperPlayServerSpawnPlayer", List.of("int", "UUID", "Vector3d", "float", "float", "List"));

    record Argument(String name, Type type, boolean nullable) {
        boolean injected() {
            Class<?> raw = PacketArguments.raw(type);
            return raw == PacketTypeCommon.class || raw == ClientVersion.class;
        }
    }

    static PacketConstructor find(Class<?> wrapper) {
        Map<String, Constructor<?>> constructors = new HashMap<>();
        for (Constructor<?> constructor : wrapper.getConstructors()) {
            if (constructor.isAnnotationPresent(Deprecated.class) || Arrays.stream(constructor.getParameterTypes())
                    .anyMatch(type -> type.getSimpleName().equals("PacketReceiveEvent") || type.getSimpleName().equals("PacketSendEvent"))) continue;
            List<String> preferred = PREFERRED.get(wrapper.getSimpleName());
            if (preferred != null && !Arrays.stream(constructor.getParameterTypes()).map(Class::getSimpleName).toList().equals(preferred)) continue;
            constructors.put(MethodType.methodType(void.class, constructor.getParameterTypes()).descriptorString(), constructor);
        }
        try (var stream = wrapper.getResourceAsStream("/" + wrapper.getName().replace('.', '/') + ".class")) {
            if (stream == null) throw new IllegalStateException("Cannot read PacketEvents class " + wrapper.getName());
            PacketConstructor selected = null;
            for (MethodModel model : ClassFile.of().parse(stream.readAllBytes()).methods()) {
                if (!model.methodName().equalsString("<init>")) continue;
                Constructor<?> constructor = constructors.get(model.methodType().stringValue());
                if (constructor == null || selected != null && constructor.getParameterCount() < selected.arguments().size()) continue;
                var parameters = constructor.getParameters();
                List<Argument> arguments = new ArrayList<>();
                for (int i = 0; i < parameters.length; i++) {
                    if (!parameters[i].isNamePresent()) throw new IllegalStateException("Missing PacketEvents parameter names for " + constructor);
                    arguments.add(new Argument(parameters[i].getName(), parameters[i].getParameterizedType(), nullable(model, i)));
                }
                selected = new PacketConstructor(constructor, List.copyOf(arguments));
            }
            if (selected != null) return selected;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read PacketEvents constructor for " + wrapper.getName(), exception);
        }
        throw new IllegalStateException("No supported PacketEvents constructor for " + wrapper.getName());
    }

    @SuppressWarnings("UnstableApiUsage")
    PacketWrapper<?> create(ConstructionContext<PacketTypeCommon, ?> values, Map<String, PacketFieldBinding> fields) {
        Object[] args = new Object[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            Argument argument = arguments.get(i);
            Class<?> raw = PacketArguments.raw(argument.type());
            if (raw == PacketTypeCommon.class) args[i] = values.getKey();
            else if (raw == ClientVersion.class) args[i] = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
            else args[i] = fields.get(argument.name()).constructorValue(values);
        }
        try {
            PacketWrapper<?> packet = (PacketWrapper<?>) method.newInstance(args);
            if (packet.getPacketTypeData().getPacketType() == null) {
                packet.getPacketTypeData().setPacketType(values.getKey());
                packet.getPacketTypeData().setNativePacketId(values.getKey().getId(packet.getServerVersion().toClientVersion()));
            }
            return packet;
        } catch (InvocationTargetException exception) {
            throw new IllegalArgumentException("Cannot create " + values.getKey().getName() + ": " + exception.getCause().getMessage(), exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke PacketEvents constructor " + method, exception);
        }
    }

    private static boolean nullable(MethodModel model, int index) {
        return Stream.concat(
                model.findAttribute(Attributes.runtimeInvisibleTypeAnnotations()).stream().flatMap(attribute -> attribute.annotations().stream()),
                model.findAttribute(Attributes.runtimeVisibleTypeAnnotations()).stream().flatMap(attribute -> attribute.annotations().stream()))
                .anyMatch(annotation -> annotation.targetInfo() instanceof TypeAnnotation.FormalParameterTarget target
                        && target.formalParameterIndex() == index && annotation.targetPath().isEmpty()
                        && annotation.annotation().className().stringValue().endsWith("/Nullable;"));
    }
}
