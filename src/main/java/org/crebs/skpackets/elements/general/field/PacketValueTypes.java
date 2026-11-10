package org.crebs.skpackets.elements.general.field;

import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import com.github.shanebeee.skr.Registration;

import java.util.*;

public final class PacketValueTypes {
    private PacketValueTypes() {}

    public static void register(Registration registration) {
        registration.newType(PacketValue.class, "packetvalue")
                .user("packet ?values?").name("General - Packet Value")
                .description("A complex packet value copied from an intercepted packet or built with a nested packet field section. Runtime-only; not saved across restarts.")
                .since("1.0.1")
                .parser(new Parser<PacketValue>() {
                    @Override public boolean canParse(ParseContext context) { return false; }
                    @Override public String toString(PacketValue value, int flags) {
                        return "packet value (" + value.value().getClass().getSimpleName() + ")";
                    }
                    @Override public String toVariableNameString(PacketValue value) {
                        return "packetvalue:" + value.value().getClass().getName() + ":" + System.identityHashCode(value.value());
                    }
                }).register();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<String> literalNames(Class<?> type) {
        if (!type.isEnum()) return List.of();
        var info = Classes.getExactClassInfo(type);
        Parser parser = info == null ? null : info.getParser();
        List<String> names = new ArrayList<>();
        for (Object constant : type.getEnumConstants()) {
            if (constant instanceof org.bukkit.block.BlockFace face && !Set.of(org.bukkit.block.BlockFace.DOWN,
                    org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH,
                    org.bukkit.block.BlockFace.WEST, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.SELF).contains(face)) continue;
            String name;
            if (parser != null) name = parser.toString(constant, 0).replace('_', ' ');
            else {
                name = ((Enum<?>) constant).name().toLowerCase(Locale.ROOT).replace('_', ' ');
                if (!managedByModule(type)) name += " " + label(type);
            }
            names.add(name);
        }
        return List.copyOf(names);
    }

    static boolean managedByModule(Class<?> type) {

        return type == Object.class || type.getName().startsWith("org.crebs.skpackets.")
                || type == com.github.retrooper.packetevents.wrapper.PacketWrapper.class
                || type == com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon.class
                || type == com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityType.class
                || type == com.github.retrooper.packetevents.protocol.player.InteractionHand.class
                || type == org.bukkit.block.sign.Side.class
                || type == me.tofaa.entitylib.wrapper.WrapperEntity.class;
    }

    public static String label(Class<?> type) {
        if (type == com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.CollisionRule.class)
            return "packet team collision rule";
        if (type == com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.NameTagVisibility.class)
            return "packet team name tag visibility";
        
        String name = type.getName();
        name = name.substring(name.lastIndexOf('.') + 1).replace('$', ' ');
        return "packet " + PacketReflection.fieldName(name.replace("Wrapper", ""));
    }
}
