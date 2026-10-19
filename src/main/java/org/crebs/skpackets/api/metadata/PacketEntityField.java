package org.crebs.skpackets.api.metadata;

import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3f;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import me.tofaa.entitylib.meta.*;
import me.tofaa.entitylib.meta.enums.BillboardConstraints;
import me.tofaa.entitylib.meta.enums.ItemDisplayContext;
import me.tofaa.entitylib.wrapper.WrapperEntity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.*;

public enum PacketEntityField {
    ENTITY_ID(
            Number.class,
            "entity id"
            , "id"),
    ON_FIRE(
            MetaFields.Entity.ON_FIRE,
            "on fire state",
            "on fire"),
    SNEAKING(
            MetaFields.Entity.CROUCHING,
            "sneaking state",
            "sneaking"),
    SPRINTING(
            MetaFields.Entity.SPRINTING,
            "sprinting state",
            "sprinting"),
    SWIMMING(
            MetaFields.Entity.SWIMMING,
            "swimming state",
            "swimming"),
    INVISIBLE(
            MetaFields.Entity.INVISIBLE,
            "invisible state",
            "invisible"),
    GLOWING(
            MetaFields.Entity.GLOWING,
            "glowing state",
            "glowing"),
    ELYTRA_FLYING(
            MetaFields.Entity.FALL_FLYING,
            "elytra flying state",
            "elytra flying"),
    CUSTOM_NAME(
            Kind.OPTIONAL_TEXT,
            MetaFields.Entity.CUSTOM_NAME,
            "",
            "custom name"),
    CUSTOM_NAME_VISIBLE(
            Kind.BOOLEAN,
            MetaFields.Entity.CUSTOM_NAME_VISIBLE,
            false,
            "custom name visible state",
            "custom name visible"),
    SILENT(
            Kind.BOOLEAN,
            MetaFields.Entity.SILENT,
            false,
            "silent state",
            "silent"),
    GRAVITY(
            Kind.INVERTED_BOOLEAN,
            MetaFields.Entity.NO_GRAVITY,
            true,
            "gravity state",
            "gravity"),
    FROZEN_TIME(
            Kind.INTEGER,
            MetaFields.Entity.TICKS_FROZEN,
            0,
            "frozen time",
            "frozen ticks"),
    ITEM(
            Kind.ITEM,
            MetaFields.ItemEntity.ITEM,
            null,
            "item"),
    HEALTH(
            Kind.FLOAT,
            MetaFields.LivingEntity.HEALTH,
            1f,
            "health",
            "hp"),
    INTERACTION_WIDTH(
            Kind.FLOAT,
            MetaFields.Interaction.WIDTH,
            1f,
            "interaction width"),
    INTERACTION_HEIGHT(
            Kind.FLOAT,
            MetaFields.Interaction.HEIGHT,
            1f,
            "interaction height"),
    INTERACTION_RESPONSIVE(
            Kind.BOOLEAN,
            MetaFields.Interaction.RESPONSE,
            false,
            "interaction responsive state",
            "interaction responsive"),
    INTERPOLATION_DELAY(
            Kind.INTEGER,
            MetaFields.Display.TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS,
            0,
            "display interpolation delay"),
    TRANSFORM_DURATION(
            Kind.INTEGER,
            MetaFields.Display.TRANSFORMATION_INTERPOLATION_DURATION,
            0,
            "display transform interpolation duration"),
    TELEPORT_DURATION(
            Kind.INTEGER,
            MetaFields.Display.POS_ROT_INTERPOLATION_DURATION,
            0,
            "display teleport interpolation duration"),
    TRANSLATION(
            Kind.VECTOR,
            MetaFields.Display.TRANSLATION,
            new Vector(),
            "display translation"),
    SCALE(
            Kind.VECTOR,
            MetaFields.Display.SCALE,
            new Vector(1, 1, 1),
            "display scale"),
    LEFT_ROTATION(
            Kind.QUATERNION,
            MetaFields.Display.LEFT_ROTATION,
            new Quaternionf(),
            "display left rotation"),
    RIGHT_ROTATION(
            Kind.QUATERNION,
            MetaFields.Display.RIGHT_ROTATION,
            new Quaternionf(),
            "display right rotation"),
    BILLBOARD(
            Kind.BILLBOARD,
            MetaFields.Display.BILLBOARD_RENDER_CONSTRAINTS,
            "fixed",
            "display billboard constraints",
            "display billboard"),
    BRIGHTNESS(
            Kind.INTEGER,
            MetaFields.Display.BRIGHTNESS_OVERRIDE,
            -1,
            "display brightness override",
            "display brightness"),
    VIEW_RANGE(
            Kind.FLOAT,
            MetaFields.Display.VIEW_RANGE,
            1f,
            "display view range"),
    SHADOW_RADIUS(Kind.FLOAT,
            MetaFields.Display.SHADOW_RADIUS,
            0f,
            "display shadow radius"),
    SHADOW_STRENGTH(
            Kind.FLOAT,
            MetaFields.Display.SHADOW_STRENGTH,
            1f,
            "display shadow strength"),
    DISPLAY_WIDTH(
            Kind.FLOAT,
            MetaFields.Display.WIDTH,
            0f,
            "display width"),
    DISPLAY_HEIGHT(
            Kind.FLOAT,
            MetaFields.Display.HEIGHT,
            0f,
            "display height"),
    GLOW_COLOR(
            Kind.INTEGER,
            MetaFields.Display.GLOW_COLOR_OVERRIDE,
            -1,
            "display glow color override",
            "display glow color"),
    BLOCK_DATA(
            Kind.BLOCK,
            MetaFields.BlockDisplay.BLOCK_STATE,
            null,
            "display block data",
            "display block state"),
    DISPLAY_ITEM(
            Kind.ITEM,
            MetaFields.ItemDisplay.ITEM_STACK,
            null,
            "display item",
            "display item stack"),
    ITEM_TRANSFORM(
            Kind.ITEM_TRANSFORM,
            MetaFields.ItemDisplay.ITEM_DISPLAY,
            "none",
            "display item transform"),
    TEXT(
            Kind.TEXT,
            MetaFields.TextDisplay.TEXT,
            "",
            "display text",
            "display content"),
    LINE_WIDTH(
            Kind.INTEGER,
            MetaFields.TextDisplay.LINE_WIDTH,
            200,
            "display line width"),
    BACKGROUND_COLOR(
            Kind.INTEGER,
            MetaFields.TextDisplay.BACKGROUND_COLOR,
            0x40000000,
            "display background color"),
    TEXT_SHADOW(
            MetaFields.TextDisplay.SHADOW,
            "display text shadowed state",
            "display shadowed state",
            "display shadowed"),
    SEE_THROUGH(
            MetaFields.TextDisplay.SEE_THROUGH, "display see through state", "display see through"),
    DEFAULT_BACKGROUND(
            MetaFields.TextDisplay.USE_DEFAULT_BACKGROUND,
            "display use default background state",
            "display use default background"),
    ALIGNMENT(
            Kind.ALIGNMENT,
            MetaFields.TextDisplay.STYLE_FLAGS,
            "center",
            "display text alignment",
            "display alignment");

    private enum Kind {
        BOOLEAN(Boolean.class),
        INVERTED_BOOLEAN(Boolean.class),
        INTEGER(Number.class),
        FLOAT(Number.class),
        VECTOR(Vector.class),
        QUATERNION(Quaternionf.class),
        ITEM(ItemStack.class),
        BLOCK(BlockData.class),
        TEXT(String.class),
        OPTIONAL_TEXT(String.class),
        BILLBOARD(String.class),
        ITEM_TRANSFORM(String.class),
        ALIGNMENT(String.class);
        final Class<?> type;

        Kind(Class<?> type) {
            this.type = type;
        }
    }

    private final Class<?> valueType;
    private final Kind kind;
    private final MetaField<?> field;
    private final MetaFlag flag;
    private final Object defaultValue;
    private final List<String> aliases;

    PacketEntityField(Class<?> type, String... aliases) {
        this.valueType = type;
        this.kind = null;
        this.field = null;
        this.flag = null;
        this.defaultValue = null;
        this.aliases = List.of(aliases);
    }

    PacketEntityField(MetaFlag flag, String... aliases) {
        this.valueType = Boolean.class;
        this.kind = Kind.BOOLEAN;
        this.field = flag.field();
        this.flag = flag;
        this.defaultValue = false;
        this.aliases = List.of(aliases);
    }

    PacketEntityField(Kind kind, MetaField<?> field, Object defaultValue, String... aliases) {
        this.valueType = kind.type;
        this.kind = kind;
        this.field = field;
        this.flag = null;
        this.defaultValue = defaultValue;
        this.aliases = List.of(aliases);
    }

    public Class<?> valueType() {
        return valueType;
    }

    public List<String> aliases() {
        return aliases;
    }

    public boolean isWritable() {
        return this != ENTITY_ID;
    }

    public static Optional<PacketEntityField> named(String name) {
        String key = name.toLowerCase(Locale.ROOT).trim();
        return Arrays.stream(values()).filter(field -> field.aliases.contains(key)).findFirst();
    }

    public boolean supports(WrapperEntity entity) {
        return field == null || MetaFields.owns(entity.getEntityType(), field);
    }

    private void requireSupported(WrapperEntity entity) {
        if (!supports(entity)) {
            throw new IllegalArgumentException(
                    aliases.getFirst()
                            + " is not available for "
                            + entity.getEntityType().getName());
        }
    }

    public Object get(WrapperEntity entity) {
        requireSupported(entity);
        if (this == ENTITY_ID) {
            return entity.getEntityId();
        }
        if (flag != null) {
            return entity.getMetadata().getFlag(flag);
        }
        if (kind == Kind.ALIGNMENT) {
            if (entity.getMetadata().getFlag(MetaFields.TextDisplay.ALIGN_LEFT)) {
                return "left";
            }
            return entity.getMetadata().getFlag(MetaFields.TextDisplay.ALIGN_RIGHT)
                    ? "right"
                    : "center";
        }
        Object raw = entity.getMetadata().get(field);
        if (raw == null) {
            return defaultValue();
        }
        return switch (kind) {
            case INVERTED_BOOLEAN -> !(Boolean) raw;
            case TEXT -> PlainTextComponentSerializer.plainText().serialize((Component) raw);
            case OPTIONAL_TEXT ->
                    ((Optional<?>) raw)
                            .map(
                                    value ->
                                            PlainTextComponentSerializer.plainText()
                                                    .serialize((Component) value))
                            .orElse("");
            case VECTOR -> {
                Vector3f v = (Vector3f) raw;
                yield new Vector(v.getX(), v.getY(), v.getZ());
            }
            case QUATERNION -> {
                Quaternion4f q = (Quaternion4f) raw;
                yield new Quaternionf(q.getX(), q.getY(), q.getZ(), q.getW());
            }
            case ITEM ->
                    SpigotConversionUtil.toBukkitItemStack(
                            (com.github.retrooper.packetevents.protocol.item.ItemStack) raw);
            case BLOCK ->
                    SpigotConversionUtil.toBukkitBlockData(
                            (com.github.retrooper.packetevents.protocol.world.states
                                            .WrappedBlockState)
                                    raw);
            case BILLBOARD, ITEM_TRANSFORM ->
                    ((Enum<?>) raw).name().toLowerCase(Locale.ROOT).replace('_', ' ');
            default -> raw;
        };
    }

    private Object defaultValue() {
        if (kind == Kind.ITEM) {
            return new ItemStack(Material.AIR);
        }
        if (kind == Kind.BLOCK) {
            return Material.AIR.createBlockData();
        }
        if (defaultValue instanceof Vector v) {
            return v.clone();
        }
        if (defaultValue instanceof Quaternionf q) {
            return new Quaternionf(q);
        }
        return defaultValue;
    }

    public void reset(WrapperEntity entity) {
        set(entity, defaultValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void set(WrapperEntity entity, Object value) {
        requireSupported(entity);
        if (!isWritable()) {
            throw new IllegalArgumentException("Entity IDs are read-only");
        }
        if (!valueType.isInstance(value)) {
            throw new IllegalArgumentException(
                    aliases.getFirst() + " requires " + valueType.getSimpleName());
        }
        if (flag != null) {
            entity.setFlag(flag, (Boolean) value);
            return;
        }
        Object converted =
                switch (kind) {
                    case INVERTED_BOOLEAN -> !(Boolean) value;
                    case INTEGER -> integer((Number) value);
                    case FLOAT -> finiteFloat((Number) value);
                    case TEXT -> Component.text((String) value);
                    case OPTIONAL_TEXT ->
                            ((String) value).isEmpty()
                                    ? Optional.empty()
                                    : Optional.of(Component.text((String) value));
                    case VECTOR -> {
                        Vector v = (Vector) value;
                        yield new Vector3f(
                                finiteFloat(v.getX()),
                                finiteFloat(v.getY()),
                                finiteFloat(v.getZ()));
                    }
                    case QUATERNION -> {
                        Quaternionf q = (Quaternionf) value;
                        if (!q.isFinite() || q.lengthSquared() == 0) {
                            throw new IllegalArgumentException(
                                    "Rotation must be a finite nonzero quaternion");
                        }
                        Quaternionf normalized = new Quaternionf(q).normalize();
                        yield new Quaternion4f(
                                normalized.x, normalized.y, normalized.z, normalized.w);
                    }
                    case ITEM -> SpigotConversionUtil.fromBukkitItemStack((ItemStack) value);
                    case BLOCK -> SpigotConversionUtil.fromBukkitBlockData((BlockData) value);
                    case BILLBOARD -> enumValue(BillboardConstraints.class, (String) value);
                    case ITEM_TRANSFORM -> enumValue(ItemDisplayContext.class, (String) value);
                    case ALIGNMENT -> {
                        String alignment = ((String) value).toLowerCase(Locale.ROOT);
                        if (!List.of("left", "right", "center").contains(alignment)) {
                            throw new IllegalArgumentException(
                                    "Alignment must be left, right or center");
                        }
                        boolean notify = entity.isNotifyingChanges();
                        entity.setNotifyAboutChanges(false);
                        try {
                            entity.setFlag(
                                    MetaFields.TextDisplay.ALIGN_LEFT, alignment.equals("left"));
                            entity.setFlag(
                                    MetaFields.TextDisplay.ALIGN_RIGHT, alignment.equals("right"));
                        } finally {
                            entity.setNotifyAboutChanges(notify);
                        }
                        yield null;
                    }
                    default -> value;
                };
        if (kind != Kind.ALIGNMENT) {
            entity.setMetadata((MetaField) field, converted);
        }
    }

    private int integer(Number value) {
        double number = value.doubleValue();
        boolean color = this == BACKGROUND_COLOR || this == GLOW_COLOR;
        long max = color ? 0xffffffffL : Integer.MAX_VALUE;
        if (!Double.isFinite(number)
                || number != Math.rint(number)
                || number < Integer.MIN_VALUE
                || number > max) {
            throw new IllegalArgumentException(aliases.getFirst() + " requires a 32-bit integer");
        }
        int result = (int) value.longValue();
        if (this == TELEPORT_DURATION && (result < 0 || result > 59)) {
            throw new IllegalArgumentException("Teleport duration must be 0 to 59 ticks");
        }
        if ((this == FROZEN_TIME || this == TRANSFORM_DURATION || this == LINE_WIDTH)
                && result < 0) {
            throw new IllegalArgumentException(aliases.getFirst() + " cannot be negative");
        }
        if (this == BRIGHTNESS && result != -1 && (result < 0 || (result & ~0xF000F0) != 0)) {
            throw new IllegalArgumentException(
                    "Brightness must be -1 or packed block/sky light: block * 16 + sky * 1048576"
                            + " (both 0 to 15)");
        }
        return result;
    }

    private static float finiteFloat(Number value) {
        float number = value.floatValue();
        if (!Float.isFinite(number)) {
            throw new IllegalArgumentException("Value must be a finite number");
        }
        return number;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        try {
            return Enum.valueOf(
                    type, value.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Expected one of " + Arrays.toString(type.getEnumConstants()));
        }
    }
}
