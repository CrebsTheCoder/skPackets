package org.crebs.skpackets.elements.team;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;

public enum TeamField {
    NAME("name", String.class),
    ENTRIES("entries", String[].class),
    DISPLAY_NAME("display name", String.class),
    PREFIX("prefix", String.class),
    SUFFIX("suffix", String.class),
    COLOR("color", String.class),
    NAME_TAG_VISIBILITY("name tag visibility", String.class),
    COLLISION_RULE("collision rule", String.class),
    FRIENDLY_FIRE("friendly fire", Boolean.class),
    SEE_INVISIBLE("see invisible teammates", Boolean.class);

    private final String key;
    private final Class<?> type;

    TeamField(String key, Class<?> type) {
        this.key = key;
        this.type = type;
    }

    public String key() {
        return key;
    }

    public Class<?> type() {
        return type;
    }

    public Class<?> elementType() {
        return type.isArray() ? type.getComponentType() : type;
    }

    private static final LegacyComponentSerializer TEXT = LegacyComponentSerializer.legacySection();

    public static ScoreBoardTeamInfo defaults(String name) {
        return new ScoreBoardTeamInfo(
                Component.text(name),
                Component.empty(),
                Component.empty(),
                NameTagVisibility.ALWAYS,
                CollisionRule.ALWAYS,
                NamedTextColor.WHITE,
                OptionData.NONE);
    }

    public static WrapperPlayServerTeams create(String name) {
        validateName(name);
        return new PacketTeam(name);
    }

    private static void validateName(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) {
            throw new IllegalArgumentException("Team name must contain 1 to 16 characters.");
        }
    }

    private static String normalize(String text) {
        return text.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private static <T extends Enum<T>> T parse(Class<T> type, String text) {
        try {
            return Enum.valueOf(type, normalize(text));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid "
                            + type.getSimpleName()
                            + " '"
                            + text
                            + "'. Expected: "
                            + Arrays.toString(type.getEnumConstants()));
        }
    }

    public Object get(WrapperPlayServerTeams packet) {
        if (this == NAME) {
            return packet.getTeamName();
        }
        if (this == ENTRIES) {
            return packet.getPlayers().toArray(String[]::new);
        }
        var info = packet.getTeamInfo().orElse(null);
        if (info == null) {
            return null;
        }
        return switch (this) {
            case DISPLAY_NAME -> TEXT.serialize(info.getDisplayName());
            case PREFIX -> TEXT.serialize(info.getPrefix());
            case SUFFIX -> TEXT.serialize(info.getSuffix());
            case COLOR -> NamedTextColor.NAMES.key(info.getColor());
            case NAME_TAG_VISIBILITY ->
                    info.getTagVisibility().name().toLowerCase(Locale.ROOT).replace('_', ' ');
            case COLLISION_RULE ->
                    info.getCollisionRule().name().toLowerCase(Locale.ROOT).replace('_', ' ');
            case FRIENDLY_FIRE -> (info.getOptionData().getByteValue() & 1) != 0;
            case SEE_INVISIBLE -> (info.getOptionData().getByteValue() & 2) != 0;
            default -> throw new IllegalStateException();
        };
    }

    public void set(WrapperPlayServerTeams packet, Object value) {
        if (this == NAME) {
            validateName((String) value);
            packet.setTeamName((String) value);
            return;
        }
        if (this == ENTRIES) {
            String[] entries = (String[]) value;
            for (String entry : entries) {
                if (entry == null || entry.isEmpty() || entry.length() > 40) {
                    throw new IllegalArgumentException(
                            "Team entries must contain 1 to 40 characters (player names or entity"
                                    + " UUIDs).");
                }
            }
            packet.setPlayers(new ArrayList<>(new LinkedHashSet<>(Arrays.asList(entries))));
            return;
        }
        var info = packet.getTeamInfo().orElseGet(() -> defaults(packet.getTeamName()));
        switch (this) {
            case DISPLAY_NAME -> info.setDisplayName(TEXT.deserialize((String) value));
            case PREFIX -> info.setPrefix(TEXT.deserialize((String) value));
            case SUFFIX -> info.setSuffix(TEXT.deserialize((String) value));
            case COLOR -> {
                NamedTextColor color =
                        NamedTextColor.NAMES.value(
                                normalize((String) value).toLowerCase(Locale.ROOT));
                if (color == null) {
                    throw new IllegalArgumentException("Unknown team color: " + value);
                }
                info.setColor(color);
            }
            case NAME_TAG_VISIBILITY ->
                    info.setTagVisibility(parse(NameTagVisibility.class, (String) value));
            case COLLISION_RULE ->
                    info.setCollisionRule(parse(CollisionRule.class, (String) value));
            case FRIENDLY_FIRE, SEE_INVISIBLE -> {
                int bit = this == FRIENDLY_FIRE ? 1 : 2;
                int flags = info.getOptionData().getByteValue();
                info.setOptionData(
                        OptionData.fromValue(
                                (byte) ((Boolean) value ? flags | bit : flags & ~bit)));
            }
            default -> throw new IllegalStateException();
        }
        packet.setTeamInfo(info);
    }
}
