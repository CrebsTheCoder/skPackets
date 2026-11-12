package org.crebs.skpackets.elements.general.field;

import org.crebs.skpackets.api.general.packet.PacketAccessManager;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.crebs.skpackets.api.util.field.ConstructionContext;

import java.io.IOException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;
import java.util.function.Function;

record PacketConstructor(Constructor<?> method, List<PacketConstructor.Argument> arguments,
                         Function<Object[], Object> factory) {
    private static final ClassValue<PacketConstructor> CONSTRUCTORS = new ClassValue<>() {
        @Override protected PacketConstructor computeValue(Class<?> type) { return discover(type); }
    };

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
        return CONSTRUCTORS.get(wrapper);
    }

    private static PacketConstructor discover(Class<?> wrapper) {
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
            ClassNode classNode = new ClassNode();
            new ClassReader(stream).accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            for (MethodNode model : classNode.methods) {
                if (!model.name.equals("<init>")) continue;
                Constructor<?> constructor = constructors.get(model.desc);
                if (constructor == null || selected != null && constructor.getParameterCount() < selected.arguments().size()) continue;
                var parameters = constructor.getParameters();
                List<Argument> arguments = new ArrayList<>();
                for (int i = 0; i < parameters.length; i++) {
                    if (!parameters[i].isNamePresent()) throw new IllegalStateException("Missing PacketEvents parameter names for " + constructor);
                    arguments.add(new Argument(parameters[i].getName(), parameters[i].getParameterizedType(), nullable(model, i)));
                }
                selected = new PacketConstructor(constructor, List.copyOf(arguments), PacketAccessManager.constructor(constructor));
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
            PacketWrapper<?> packet = (PacketWrapper<?>) factory.apply(args);
            if (packet.getPacketTypeData().getPacketType() == null) {
                packet.getPacketTypeData().setPacketType(values.getKey());
                packet.getPacketTypeData().setNativePacketId(values.getKey().getId(packet.getServerVersion().toClientVersion()));
            }
            return packet;
        } catch (IllegalArgumentException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalArgumentException("Cannot create " + values.getKey().getName() + ": " + cause.getMessage(), cause);
        }
    }

    private static boolean nullable(MethodNode model, int index) {
        return Stream.of(model.invisibleTypeAnnotations, model.visibleTypeAnnotations)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .anyMatch(annotation -> {
                    TypeReference target = new TypeReference(annotation.typeRef);
                    return target.getSort() == TypeReference.METHOD_FORMAL_PARAMETER
                            && target.getFormalParameterIndex() == index
                            && (annotation.typePath == null || annotation.typePath.getLength() == 0)
                            && annotation.desc.endsWith("/Nullable;");
                });
    }
}
