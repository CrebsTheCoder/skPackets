package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTitle;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.crebs.skpackets.api.general.packet.PacketTypeRegistry;
import org.crebs.skpackets.api.util.field.FieldRegistrar;

import java.lang.reflect.Method;
import java.util.*;

public final class PacketFieldRegistrar implements FieldRegistrar {
    @Override
    public void register() {
        for (PacketTypeCommon type : PacketTypeRegistry.getAllPackets()) {
            if (PacketFieldRegistry.INSTANCE.getSchema(type) != null) continue;
            Class<? extends PacketWrapper<?>> wrapper = wrapperClass(type);
            if (wrapper == null || wrapper.getSimpleName().equals("WrapperPlayServerChunkDataBulk")) registerRaw(type);
            else register(type, wrapper);
        }
    }

    static Class<? extends PacketWrapper<?>> wrapperClass(PacketTypeCommon type) {
        return type == PacketType.Play.Server.TITLE ? WrapperPlayServerTitle.class : type.getWrapperClass();
    }

    private static <T extends PacketWrapper<?>> void register(PacketTypeCommon type, Class<T> wrapper) {
        PacketConstructor constructor = PacketConstructor.find(wrapper);
        List<Method> methods = PacketReflection.methods(wrapper);
        var builder = PacketFieldRegistry.INSTANCE.builder(type, wrapper);
        Map<String, PacketFieldBinding> parameters = new LinkedHashMap<>();
        Set<String> names = new HashSet<>();

        for (var argument : constructor.arguments()) {
            if (argument.injected()) continue;
            var field = PacketFieldBinding.parameter(wrapper, argument, methods);
            parameters.put(argument.name(), field);
            field.names().forEach(name -> names.add(PacketReflection.normalized(name)));
            field.register(builder);
        }

        List<PacketFieldBinding> extras = extraFields(methods, names);
        extras.forEach(field -> field.register(builder));
        builder.constructor(values -> {
            T packet = wrapper.cast(constructor.create(values, parameters));
            initializeDefaults(packet);
            for (var field : extras) {
                Object value = values.getOptional(field.name(), Object.class);
                if (value != null) field.write(packet, value);
            }
            return packet;
        });
        builder.build();
    }

    private static List<PacketFieldBinding> extraFields(List<Method> methods, Set<String> names) {
        List<PacketFieldBinding> fields = new ArrayList<>();
        for (Method setter : methods) {
            if (!setter.getName().startsWith("set") || setter.getParameterCount() != 1
                    || setter.getDeclaringClass() == PacketWrapper.class
                    || !setter.getDeclaringClass().getName().startsWith("com.github.retrooper.packetevents.wrapper.")) continue;
            String property = setter.getName().substring(3);
            Method getter = PacketReflection.getter(methods, property, setter.getGenericParameterTypes()[0]);
            if (getter == null || getter.getReturnType() != setter.getParameterTypes()[0]
                    || !names.add(PacketReflection.normalized(property))) continue;
            fields.add(PacketFieldBinding.extra(getter, setter));
        }
        return List.copyOf(fields);
    }

    private static void initializeDefaults(PacketWrapper<?> packet) {
        if (packet instanceof WrapperPlayServerBossBar bar) {
            bar.setTitle(Component.empty());
            bar.setHealth(1f);
            bar.setColor(BossBar.Color.WHITE);
            bar.setOverlay(BossBar.Overlay.PROGRESS);
            bar.setFlags(EnumSet.noneOf(BossBar.Flag.class));
        }
    }

    private static void registerRaw(PacketTypeCommon type) {
        PacketFieldRegistry.INSTANCE.builder(type, RawPacket.class)
                .requiredField(Number[].class, RawPacket::numbers, RawPacket::setData, "raw data")
                .constructor(values -> new RawPacket(type, values.getRequired("raw data", Number[].class)))
                .build();
    }

    public static final class RawPacket extends PacketWrapper<RawPacket> {
        private byte[] data;

        RawPacket(PacketTypeCommon type, Number[] data) {
            super(type);
            setData(data);
        }

        void setData(Number[] values) {
            data = (byte[]) PacketArguments.convert(values, byte[].class, "raw data");
        }

        Number[] numbers() {
            Number[] result = new Number[data.length];
            for (int i = 0; i < data.length; i++) result[i] = Byte.toUnsignedInt(data[i]);
            return result;
        }

        @Override public void write() { writeBytes(data); }
        @Override public void copy(RawPacket other) { data = other.data.clone(); }
    }
}
