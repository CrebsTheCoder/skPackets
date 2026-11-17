package org.crebs.skpackets.elements.general.field;

import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.recipe.data.MerchantOffer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import org.crebs.skpackets.api.util.field.FieldAccessor;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

public final class PacketRecords {
    private PacketRecords() {}
    public record Shape(List<FieldAccessor<Object, ?>> fields, Map<String, Object> defaults,
                        Function<Map<String, Object>, Object> factory, List<Shape> alternatives) {
        public Shape(List<FieldAccessor<Object, ?>> fields, Map<String, Object> defaults,
                     Function<Map<String, Object>, Object> factory) {
            this(fields, defaults, factory, List.of());
        }
        Shape select(Set<String> supplied, String name) {
            return alternatives.isEmpty() ? this : PacketValueDiscovery.select(alternatives, supplied, name);
        }
    }
    private static final Map<Class<?>, Shape> SHAPES = new HashMap<>();
    static {
        define(com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property.class,
                List.of(field("attribute", com.github.retrooper.packetevents.protocol.attribute.Attribute.class, false),
                        field("value", double.class, false),
                        field("modifiers", com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.PropertyModifier[].class, true)),
                Map.of("modifiers", new PacketValue[0]),
                v -> new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property(
                        (com.github.retrooper.packetevents.protocol.attribute.Attribute) v.get("attribute"), (double) v.get("value"),
                        new ArrayList<>(Arrays.asList((com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.PropertyModifier[]) v.get("modifiers")))));
        defineAlternatives(com.github.retrooper.packetevents.protocol.world.positionsource.PositionSource.class,
                discovered(com.github.retrooper.packetevents.protocol.world.positionsource.builtin.BlockPositionSource.class),
                discovered(com.github.retrooper.packetevents.protocol.world.positionsource.builtin.EntityPositionSource.class));
        defineAlternatives(com.github.retrooper.packetevents.protocol.recipe.data.RecipeData.class,
                variant("shaped", com.github.retrooper.packetevents.protocol.recipe.data.ShapedRecipeData.class),
                variant("shapeless", com.github.retrooper.packetevents.protocol.recipe.data.ShapelessRecipeData.class),
                variant("cooked", com.github.retrooper.packetevents.protocol.recipe.data.CookedRecipeData.class),
                variant("simple", com.github.retrooper.packetevents.protocol.recipe.data.SimpleRecipeData.class),
                variant("smithing", com.github.retrooper.packetevents.protocol.recipe.data.SmithingRecipeData.class),
                variant("smithing trim", com.github.retrooper.packetevents.protocol.recipe.data.SmithingTrimRecipeData.class),
                variant("stonecutting", com.github.retrooper.packetevents.protocol.recipe.data.StoneCuttingRecipeData.class));
        defineAlternatives(com.github.retrooper.packetevents.protocol.particle.data.ParticleData.class,
                constant("empty", com.github.retrooper.packetevents.protocol.particle.data.ParticleData::emptyData),
                variant("block", com.github.retrooper.packetevents.protocol.particle.data.ParticleBlockStateData.class),
                variant("item", com.github.retrooper.packetevents.protocol.particle.data.ParticleItemStackData.class),
                variant("power", com.github.retrooper.packetevents.protocol.particle.data.ParticlePowerData.class),
                variant("sculk charge", com.github.retrooper.packetevents.protocol.particle.data.ParticleSculkChargeData.class),
                variant("shriek", com.github.retrooper.packetevents.protocol.particle.data.ParticleShriekData.class),
                variant("vibration", com.github.retrooper.packetevents.protocol.particle.data.ParticleVibrationData.class),
                variant("geyser", com.github.retrooper.packetevents.protocol.particle.data.ParticleGeyserData.class),
                variant("geyser base", com.github.retrooper.packetevents.protocol.particle.data.ParticleGeyserBaseData.class));
        defineAlternatives(com.github.retrooper.packetevents.protocol.chat.message.ChatMessage.class,
                variant("legacy", com.github.retrooper.packetevents.protocol.chat.message.ChatMessageLegacy.class),
                variant("v1 16", com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_16.class),
                variant("v1 19", com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19.class),
                variant("v1 19 1", com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19_1.class),
                variant("v1 19 3", com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19_3.class),
                variant("v1 21 5", com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_21_5.class));
        defineAlternatives(com.github.retrooper.packetevents.protocol.recipe.display.RecipeDisplay.class,
                variant("shaped", com.github.retrooper.packetevents.protocol.recipe.display.ShapedCraftingRecipeDisplay.class),
                variant("shapeless", com.github.retrooper.packetevents.protocol.recipe.display.ShapelessCraftingRecipeDisplay.class),
                variant("furnace", com.github.retrooper.packetevents.protocol.recipe.display.FurnaceRecipeDisplay.class),
                variant("smithing", com.github.retrooper.packetevents.protocol.recipe.display.SmithingRecipeDisplay.class),
                variant("stonecutter", com.github.retrooper.packetevents.protocol.recipe.display.StonecutterRecipeDisplay.class));
        defineAlternatives(com.github.retrooper.packetevents.protocol.recipe.display.slot.SlotDisplay.class,
                constant("empty", () -> com.github.retrooper.packetevents.protocol.recipe.display.slot.EmptySlotDisplay.INSTANCE),
                constant("any fuel", () -> com.github.retrooper.packetevents.protocol.recipe.display.slot.AnyFuelSlotDisplay.INSTANCE),
                variant("item", com.github.retrooper.packetevents.protocol.recipe.display.slot.ItemSlotDisplay.class),
                variant("item stack", com.github.retrooper.packetevents.protocol.recipe.display.slot.ItemStackSlotDisplay.class),
                variant("tag", com.github.retrooper.packetevents.protocol.recipe.display.slot.TagSlotDisplay.class),
                variant("composite", com.github.retrooper.packetevents.protocol.recipe.display.slot.CompositeSlotDisplay.class),
                variant("smithing trim", com.github.retrooper.packetevents.protocol.recipe.display.slot.SmithingTrimSlotDisplay.class),
                variant("with remainder", com.github.retrooper.packetevents.protocol.recipe.display.slot.WithRemainderSlotDisplay.class),
                variant("with any potion", com.github.retrooper.packetevents.protocol.recipe.display.slot.WithAnyPotionSlotDisplay.class),
                variant("only with component", com.github.retrooper.packetevents.protocol.recipe.display.slot.OnlyWithComponentSlotDisplay.class),
                variant("dyed", com.github.retrooper.packetevents.protocol.recipe.display.slot.DyedSlotDisplay.class));
        define(com.github.retrooper.packetevents.util.VersionRange.class,
                List.of(field("minimum", com.github.retrooper.packetevents.protocol.player.ClientVersion.class, true),
                        field("maximum", com.github.retrooper.packetevents.protocol.player.ClientVersion.class, true)), Map.of(),
                v -> new com.github.retrooper.packetevents.util.VersionRange(
                        (com.github.retrooper.packetevents.protocol.player.ClientVersion) v.get("minimum"),
                        (com.github.retrooper.packetevents.protocol.player.ClientVersion) v.get("maximum")));
        defineAlternatives(com.github.retrooper.packetevents.protocol.score.ScoreFormat.class,
                new Shape(List.of(), Map.of(), v -> com.github.retrooper.packetevents.protocol.score.ScoreFormat.blankScore()),
                new Shape(List.of(field("value", net.kyori.adventure.text.Component.class, false)), Map.of(),
                        v -> com.github.retrooper.packetevents.protocol.score.ScoreFormat.fixedScore((net.kyori.adventure.text.Component) v.get("value"))));
        defineAlternatives(com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef.class,
                new Shape(List.of(field("id", int.class, false)), Map.of(),
                        v -> new com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef.IdRef((int) v.get("id"))),
                new Shape(List.of(field("name", com.github.retrooper.packetevents.resources.ResourceLocation.class, false)), Map.of(),
                        v -> new com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef.NameRef(
                                (com.github.retrooper.packetevents.resources.ResourceLocation) v.get("name"))));
        defineAlternatives(com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath.class,
                discovered(com.github.retrooper.packetevents.protocol.vector.positionpath.LinearPositionPath.class),
                discovered(com.github.retrooper.packetevents.protocol.vector.positionpath.SteppedPositionPath.class));
        defineAlternatives(com.github.retrooper.packetevents.protocol.vector.vecdelta.VecDelta.class,
                new Shape(List.of(field("dx", double.class, false), field("dy", double.class, false), field("dz", double.class, false)),
                        Map.of(), v -> new com.github.retrooper.packetevents.protocol.vector.vecdelta.LinearVecDelta(
                                (double) v.get("dx"), (double) v.get("dy"), (double) v.get("dz"))),
                discovered(com.github.retrooper.packetevents.protocol.vector.vecdelta.SteppedVecDelta.class));
        defineAlternatives(com.github.retrooper.packetevents.protocol.world.waypoint.WaypointInfo.class,
                new Shape(List.of(), Map.of(), v -> com.github.retrooper.packetevents.protocol.world.waypoint.EmptyWaypointInfo.EMPTY),
                discovered(com.github.retrooper.packetevents.protocol.world.waypoint.Vec3iWaypointInfo.class),
                discovered(com.github.retrooper.packetevents.protocol.world.waypoint.ChunkWaypointInfo.class),
                discovered(com.github.retrooper.packetevents.protocol.world.waypoint.AzimuthWaypointInfo.class));
        define(Equipment.class, List.of(
                field("slot", com.github.retrooper.packetevents.protocol.player.EquipmentSlot.class, false),
                field("item", com.github.retrooper.packetevents.protocol.item.ItemStack.class, false)), Map.of(),
                v -> new Equipment((com.github.retrooper.packetevents.protocol.player.EquipmentSlot)v.get("slot"),
                        (com.github.retrooper.packetevents.protocol.item.ItemStack)v.get("item")));
        define(UserProfile.class, List.of(field("uuid", UUID.class, false), field("name", String.class, false)), Map.of(),
                v -> new UserProfile((UUID)v.get("uuid"), (String)v.get("name")));
        define(PlayerInfo.class, List.of(
                field("profile", UserProfile.class, false), field("listed", boolean.class, true),
                field("latency", int.class, true), field("game mode", com.github.retrooper.packetevents.protocol.player.GameMode.class, true),
                field("display name", net.kyori.adventure.text.Component.class, true), field("list order", int.class, true),
                field("show hat", boolean.class, true)),
                Map.of("listed", true, "latency", 0, "game mode", org.bukkit.GameMode.SURVIVAL, "list order", 0, "show hat", true),
                v -> new PlayerInfo((UserProfile)v.get("profile"), (boolean)v.get("listed"), (int)v.get("latency"),
                        (com.github.retrooper.packetevents.protocol.player.GameMode)v.get("game mode"),
                        (net.kyori.adventure.text.Component)v.get("display name"), null, (int)v.get("list order"), (boolean)v.get("show hat")));
        define(MerchantOffer.class, List.of(
                field("input", com.github.retrooper.packetevents.protocol.item.ItemStack.class, false),
                field("second input", com.github.retrooper.packetevents.protocol.item.ItemStack.class, true),
                field("output", com.github.retrooper.packetevents.protocol.item.ItemStack.class, false),
                field("uses", int.class, true), field("max uses", int.class, true), field("xp", int.class, true),
                field("special price", int.class, true), field("price multiplier", float.class, true), field("demand", int.class, true)),
                Map.of("uses", 0, "max uses", 12, "xp", 0, "special price", 0, "price multiplier", 0f, "demand", 0),
                v -> MerchantOffer.of((com.github.retrooper.packetevents.protocol.item.ItemStack)v.get("input"),
                        (com.github.retrooper.packetevents.protocol.item.ItemStack)v.get("second input"),
                        (com.github.retrooper.packetevents.protocol.item.ItemStack)v.get("output"),
                        (int)v.get("uses"), (int)v.get("max uses"), (int)v.get("xp"), (int)v.get("special price"),
                        (float)v.get("price multiplier"), (int)v.get("demand")));
    }
    private static final ClassValue<Optional<Shape>> DISCOVERED = new ClassValue<>() {
        @Override protected Optional<Shape> computeValue(Class<?> type) {
            return Optional.ofNullable(PacketValueDiscovery.discover(type));
        }
    };

    private static Shape variant(String name, Class<?> type) {
        return new Shape(List.of(field(name, type, false)), Map.of(), values -> values.get(name));
    }
    private static Shape constant(String name, java.util.function.Supplier<?> value) {
        return new Shape(List.of(field(name, boolean.class, false)), Map.of(), values -> {
            if (!Boolean.TRUE.equals(values.get(name)))
                throw new IllegalArgumentException("Nested builder '" + name + "' must be true.");
            return value.get();
        });
    }
    private static Shape discovered(Class<?> type) {
        return Objects.requireNonNull(PacketValueDiscovery.discover(type));
    }
    private static void defineAlternatives(Class<?> type, Shape... candidates) {
        SHAPES.put(type, alternatives(type, candidates));
    }
    private static Shape alternatives(Class<?> type, Shape... candidates) {
        List<Shape> alternatives = Arrays.stream(candidates)
                .flatMap(shape -> (shape.alternatives().isEmpty() ? List.of(shape) : shape.alternatives()).stream()).toList();
        Map<String, FieldAccessor<Object, ?>> fields = new LinkedHashMap<>();
        for (var shape : alternatives) for (var field : shape.fields()) fields.putIfAbsent(field.name(), field);
        return new Shape(List.copyOf(fields.values()), Map.of(),
                v -> construct(v, PacketValueDiscovery.select(alternatives, v.keySet(), type.getSimpleName()), type.getSimpleName()), alternatives);
    }

    private static void define(Class<?> type, List<FieldAccessor<Object, ?>> fields, Map<String, Object> defaults,
                               Function<Map<String, Object>, Object> factory) {
        SHAPES.put(type, new Shape(List.copyOf(fields), Map.copyOf(defaults), factory));
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static FieldAccessor<Object, ?> field(String name, Type nativeType, boolean optional) {
        return new FieldAccessor(name, new String[0], PacketArguments.expressionType(nativeType), optional, null, null, nativeType);
    }
    public static Type elementType(Type type) {
        Class<?> raw = PacketArguments.raw(type);
        if (raw == Optional.class) return elementType(PacketArguments.argument(type, 0));
        if (raw.isArray()) return raw.getComponentType();
        if (Collection.class.isAssignableFrom(raw)) return PacketArguments.argument(type, 0);
        return type;
    }
    public static boolean isList(Type type) {
        Class<?> raw = PacketArguments.raw(type);
        if (raw == Optional.class) return isList(PacketArguments.argument(type, 0));
        return raw.isArray() || Collection.class.isAssignableFrom(raw) || Map.class.isAssignableFrom(raw);
    }
    public static Shape shape(Type type) {
        if (PacketArguments.raw(type) == Optional.class) return shape(PacketArguments.argument(type, 0));
        if (Map.class.isAssignableFrom(PacketArguments.raw(type)))
            return new Shape(List.of(field("key", PacketArguments.argument(type, 0), false),
                    field("value", PacketArguments.argument(type, 1),
                            PacketArguments.raw(PacketArguments.argument(type, 1)) == Optional.class)), Map.of(), v -> v);
        Type element = elementType(type);
        Class<?> raw = PacketArguments.raw(element);
        if (raw == com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet.class) {
            Class<?> entity = PacketArguments.raw(PacketArguments.argument(element, 0));
            if (entity == Object.class) entity = com.github.retrooper.packetevents.protocol.mapper.MappedEntity.class;
            Class<?> array = java.lang.reflect.Array.newInstance(entity, 0).getClass();
            return alternatives(raw,
                    new Shape(List.of(field("tag key", com.github.retrooper.packetevents.resources.ResourceLocation.class, false)), Map.of(),
                            v -> new com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet<>(
                                    (com.github.retrooper.packetevents.resources.ResourceLocation) v.get("tag key"))),
                    new Shape(List.of(field("entities", array, false)), Map.of(),
                            v -> new com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet<>(Arrays.asList(
                                    (com.github.retrooper.packetevents.protocol.mapper.MappedEntity[]) v.get("entities")))));
        }
        Shape known = SHAPES.get(raw);
        return known != null ? known : DISCOVERED.get(raw).orElse(null);
    }
    public static Object construct(PacketRecord record, Type type, String name) {
        Shape shape = shape(type);
        if (shape == null) throw new IllegalArgumentException("Packet field '" + name + "' does not support nested entries.");
        return construct(record.fields(), shape.select(record.fields().keySet(), name), name);
    }
    static Object construct(Map<String, Object> supplied, Shape shape, String name) {
        Map<String, Object> values = new HashMap<>(shape.defaults());
        for (String key : supplied.keySet()) {
            if (shape.fields().stream().noneMatch(f -> f.name().equals(key)))
                throw new IllegalArgumentException("Unknown nested packet field '" + name + "." + key + "'.");
        }
        values.putAll(supplied);
        Map<String, Object> converted = new HashMap<>();
        for (var field : shape.fields()) {
            Object value = values.get(field.name());
            if (value == null && !field.isOptional()) throw new IllegalArgumentException("Missing packet field '" + name + "." + field.name() + "'.");
            if (value == null && PacketArguments.raw(field.nativeType()) == Optional.class)
                converted.put(field.name(), Optional.empty());
            if (value != null) converted.put(field.name(), PacketArguments.convert(value, field.nativeType(), name + "." + field.name()));
        }
        return shape.factory().apply(converted);
    }
}
