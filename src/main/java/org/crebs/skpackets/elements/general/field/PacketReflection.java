package org.crebs.skpackets.elements.general.field;

import org.crebs.skpackets.api.general.packet.PacketAccessManager;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

final class PacketReflection {
    private PacketReflection() {}

    static List<Method> methods(Class<?> wrapper) {
        return PacketAccessManager.methods(wrapper);
    }

    static String fieldName(String name) {
        return name.replaceAll("([A-Z]{2,})s$", "$1S").replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
    }

    static String normalized(String name) {
        return name.replace("_", "").replace(" ", "").toLowerCase(Locale.ROOT);
    }

    static String property(Class<?> wrapper, String parameter) {
        return switch (wrapper.getSimpleName() + ":" + parameter) {
            case "WrapperPlayClientAnimation:interactionHand", "WrapperPlayClientPlayerBlockPlacement:interactionHand" -> "hand";
            case "WrapperPlayClientGenerateStructure:keepJigsaws" -> "keepingJigsaws";
            case "WrapperPlayClientPlayerAbilities:godMode", "WrapperPlayServerPlayerAbilities:godMode" -> "inGodMode";
            case "WrapperPlayClientPlayerAbilities:creativeMode", "WrapperPlayServerPlayerAbilities:creativeMode" -> "inCreativeMode";
            case "WrapperPlayClientSettings:visibility" -> "chatVisibility";
            case "WrapperPlayClientSettings:hand" -> "mainHand";
            case "WrapperPlayClientSettings:allowServerListings", "WrapperConfigClientSettings:allowServerListings" -> "serverListingAllowed";
            case "WrapperPlayClientSpectate:uuid" -> "targetUUID";
            case "WrapperPlayServerBlockChange:state" -> "blockState";
            case "WrapperPlayServerOpenSignEditor:isFrontText", "WrapperPlayClientUpdateSign:isFrontText" -> "frontText";
            case "WrapperPlayServerBlockAction:actionParam" -> "actionData";
            case "WrapperPlayServerBlockEntityData:type" -> "blockEntityType";
            case "WrapperPlayServerParticle:maxSpeed" -> "maxSpeedVec";
            case "WrapperPlayServerCloseWindow:id" -> "windowId";
            case "WrapperPlayServerEntityEffect:amplifier" -> "effectAmplifier";
            case "WrapperPlayServerEntityEffect:duration" -> "effectDurationTicks";
            case "WrapperPlayServerExplosion:smallParticle" -> "smallExplosionParticles";
            case "WrapperPlayServerExplosion:playerMotion" -> "knockback";
            case "WrapperPlayServerJoinGame:enableRespawnScreen" -> "respawnScreenEnabled";
            case "WrapperPlayServerMultiBlockChange:blockData" -> "blocks";
            case "WrapperPlayServerPlayerInfo:playerData" -> "playerDataList";
            case "WrapperPlayServerPlayerPositionAndLook:flags" -> "relativeMask";
            case "WrapperPlayServerTimeUpdate:gameTime" -> "worldAge";
            case "WrapperPlayServerWindowProperty:windowId" -> "containerId";
            default -> parameter;
        };
    }

    static Method getter(List<Method> methods, String name, Type expectedType) {
        Class<?> expected = PacketArguments.expressionType(expectedType);
        return methods.stream().filter(m -> m.getParameterCount() == 0)
                .filter(m -> m.getName().startsWith("get") || m.getName().startsWith("is") || m.getName().startsWith("has"))
                .filter(m -> normalized(m.getName().replaceFirst("^(get|is|has)", "")).equals(normalized(name))
                        || normalized(m.getName()).equals(normalized(name)))
                .filter(m -> PacketArguments.expressionType(m.getGenericReturnType()) == expected)
                .findFirst().orElse(null);
    }

    static Method setter(List<Method> methods, String name, Class<?> parameter) {
        return methods.stream().filter(m -> m.getParameterCount() == 1)
                .filter(m -> m.getName().startsWith("set"))
                .filter(m -> normalized(m.getName().substring(3)).equals(normalized(name)))
                .filter(m -> com.google.common.primitives.Primitives.wrap(m.getParameterTypes()[0])
                        == com.google.common.primitives.Primitives.wrap(parameter))
                .findFirst().orElse(null);
    }

}
