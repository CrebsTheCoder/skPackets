package org.crebs.skpackets.elements.general.field;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.ColorRGB;
import ch.njol.skript.util.Direction;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.Difficulty;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.Vector2i;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientResourcePackStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientResourcePackStatus;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.bukkit.util.Vector;
import org.crebs.skpackets.api.general.Vector2;
import org.crebs.skpackets.api.simple.FakeEquipmentManager;
import org.crebs.skpackets.api.util.ConversionUtil;
import org.skriptlang.skript.bukkit.bossbar.BossBarUtils;
import org.skriptlang.skript.bukkit.entity.EntityData;
import org.skriptlang.skript.bukkit.entity.EntityUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.crebs.skpackets.elements.general.field.PacketValueCodec.of;

final class PacketValueCodecs {
    private static final Map<Class<?>, PacketValueCodec<?, ?>> CODECS = codecs();

    private PacketValueCodecs() {}

    private static Map<Class<?>, PacketValueCodec<?, ?>> codecs() {
        List<PacketValueCodec<?, ?>> values = List.of(
                new PacketValueCodec<>(Vector2i.class, Vector2.class,
                        value -> new Vector2(value.getX(), value.getZ()),
                        (value, name) -> new Vector2i((int) PacketNumbers.convert(value.x(), int.class, name + " x"),
                                (int) PacketNumbers.convert(value.y(), int.class, name + " z"))),
                of(Vector3i.class, Vector.class,
                        value -> new Vector(value.getX(), value.getY(), value.getZ()), ConversionUtil::toPeVectorI),
                of(Vector3f.class, Vector.class,
                        value -> new Vector(value.getX(), value.getY(), value.getZ()), ConversionUtil::toPeVectorF),
                of(Vector3d.class, Vector.class,
                        value -> new Vector(value.getX(), value.getY(), value.getZ()),
                        value -> new Vector3d(value.getX(), value.getY(), value.getZ())),
                of(com.github.retrooper.packetevents.protocol.world.Location.class, org.bukkit.Location.class,
                        ConversionUtil::toBukkitLocation, ConversionUtil::toPeLocation),
                of(ItemStack.class, org.bukkit.inventory.ItemStack.class,
                        SpigotConversionUtil::toBukkitItemStack, SpigotConversionUtil::fromBukkitItemStack),
                of(WrappedBlockState.class, org.bukkit.block.data.BlockData.class,
                        SpigotConversionUtil::toBukkitBlockData, SpigotConversionUtil::fromBukkitBlockData),
                of(EquipmentSlot.class, org.bukkit.inventory.EquipmentSlot.class,
                        PacketValueCodecs::equipmentSlot, FakeEquipmentManager::packetSlot),
                of(GameMode.class, org.bukkit.GameMode.class,
                        SpigotConversionUtil::toBukkitGameMode, SpigotConversionUtil::fromBukkitGameMode),
                of(Difficulty.class, org.bukkit.Difficulty.class,
                        value -> org.bukkit.Difficulty.valueOf(value.name()), value -> Difficulty.valueOf(value.name())),
                of(BlockFace.class, Direction.class,
                        value -> new Direction(value == BlockFace.OTHER ? org.bukkit.block.BlockFace.SELF
                                : org.bukkit.block.BlockFace.valueOf(value.name()), 1),
                        value -> blockFace(value)),
                of(com.github.retrooper.packetevents.protocol.world.Direction.class, Direction.class,
                        value -> new Direction(org.bukkit.block.BlockFace.valueOf(value.name()), 1),
                        value -> com.github.retrooper.packetevents.protocol.world.Direction.valueOf(absoluteFace(value).name())),
                of(com.github.retrooper.packetevents.protocol.entity.type.EntityType.class, EntityData.class,
                        value -> EntityUtils.toSkriptEntityData(SpigotConversionUtil.toBukkitEntityType(value)),
                        value -> SpigotConversionUtil.fromBukkitEntityType(EntityUtils.toBukkitEntityType(value))),
                of(PotionType.class, org.bukkit.potion.PotionEffectType.class,
                        SpigotConversionUtil::toBukkitPotionEffectType, SpigotConversionUtil::fromBukkitPotionEffectType),
                of(com.github.retrooper.packetevents.protocol.item.type.ItemType.class, ItemType.class,
                        value -> new ItemType(SpigotConversionUtil.toBukkitItemMaterial(value)),
                        value -> SpigotConversionUtil.fromBukkitItemMaterial(value.getRandom().getType())),
                of(SoundCategory.class, org.bukkit.SoundCategory.class,
                        PacketValueCodecs::soundCategory, PacketValueCodecs::soundCategory),
                of(HumanoidArm.class, org.bukkit.inventory.MainHand.class,
                        SpigotConversionUtil::toBukkitHand, value -> HumanoidArm.valueOf(value.name())),
                of(EntityPose.class, org.bukkit.entity.Pose.class,
                        SpigotConversionUtil::toBukkitPose, SpigotConversionUtil::fromBukkitPose),
                of(ResourceLocation.class, NamespacedKey.class,
                        value -> NamespacedKey.fromString(value.toString()), value -> new ResourceLocation(value.toString())),
                of(BossBar.Color.class, Color.class,
                        value -> BossBarUtils.rgbFromBarColor(BarColor.valueOf(value.name())), PacketValueCodecs::bossBarColor),
                of(BossBar.Overlay.class, BarStyle.class,
                        value -> BarStyle.valueOf(value == BossBar.Overlay.PROGRESS ? "SOLID" : value.name().replace("NOTCHED_", "SEGMENTED_")),
                        value -> BossBar.Overlay.valueOf(value == BarStyle.SOLID ? "PROGRESS" : value.name().replace("SEGMENTED_", "NOTCHED_"))),
                of(WrapperPlayClientResourcePackStatus.Result.class, Status.class,
                        value -> Status.valueOf(value.name()), value -> WrapperPlayClientResourcePackStatus.Result.valueOf(value.name())),
                of(WrapperConfigClientResourcePackStatus.Result.class, Status.class,
                        value -> Status.valueOf(value.name()), value -> WrapperConfigClientResourcePackStatus.Result.valueOf(value.name())),
                of(TextColor.class, Color.class,
                        value -> ColorRGB.fromRGB(value.red(), value.green(), value.blue()), PacketValueCodecs::textColor),
                of(NamedTextColor.class, Color.class,
                        value -> ColorRGB.fromRGB(value.red(), value.green(), value.blue()), value -> NamedTextColor.nearestTo(textColor(value))),
                of(Component.class, Component.class, value -> value, value -> value),
                of(Sound.class, String.class, value -> value.getSoundId().toString(), PacketValueCodecs::sound)
        );
        Map<Class<?>, PacketValueCodec<?, ?>> result = new LinkedHashMap<>();
        for (var codec : values) result.put(codec.packetType(), codec);
        return java.util.Collections.unmodifiableMap(result);
    }

    static PacketValueCodec<?, ?> find(Class<?> type) {
        var exact = CODECS.get(type);
        if (exact != null) return exact;
        for (var codec : CODECS.values()) {
            if (codec.packetType().isAssignableFrom(type)) return codec;
        }
        return null;
    }

    private static org.bukkit.inventory.EquipmentSlot equipmentSlot(EquipmentSlot value) {
        return switch (value) {
            case MAIN_HAND -> org.bukkit.inventory.EquipmentSlot.HAND;
            case OFF_HAND -> org.bukkit.inventory.EquipmentSlot.OFF_HAND;
            case BOOTS -> org.bukkit.inventory.EquipmentSlot.FEET;
            case LEGGINGS -> org.bukkit.inventory.EquipmentSlot.LEGS;
            case CHEST_PLATE -> org.bukkit.inventory.EquipmentSlot.CHEST;
            case HELMET -> org.bukkit.inventory.EquipmentSlot.HEAD;
            case BODY -> org.bukkit.inventory.EquipmentSlot.BODY;
            case SADDLE -> org.bukkit.inventory.EquipmentSlot.SADDLE;
        };
    }

    private static org.bukkit.SoundCategory soundCategory(SoundCategory value) {
        return org.bukkit.SoundCategory.valueOf(switch (value) {
            case RECORD -> "RECORDS";
            case BLOCK -> "BLOCKS";
            case PLAYER -> "PLAYERS";
            default -> value.name();
        });
    }

    private static SoundCategory soundCategory(org.bukkit.SoundCategory value) {
        return SoundCategory.valueOf(switch (value) {
            case RECORDS -> "RECORD";
            case BLOCKS -> "BLOCK";
            case PLAYERS -> "PLAYER";
            default -> value.name();
        });
    }

    private static org.bukkit.block.BlockFace absoluteFace(Direction value) {
        if (value.isRelative()) throw new IllegalArgumentException("Packet directions must be absolute, such as north or up.");
        Vector vector = value.getDirection();
        if (vector.lengthSquared() == 0) throw new IllegalArgumentException("A packet direction cannot be zero.");
        return Direction.toNearestCartesianBlockFace(vector);
    }

    private static BlockFace blockFace(Direction value) {
        if (!value.isRelative() && value.getDirection().lengthSquared() == 0) return BlockFace.OTHER;
        return BlockFace.valueOf(absoluteFace(value).name());
    }

    private static BossBar.Color bossBarColor(Color value) {
        BarColor nearest = BossBarUtils.nearest(value);
        return BossBar.Color.valueOf(nearest == null ? "WHITE" : nearest.name());
    }

    private static TextColor textColor(Color value) {
        return TextColor.color(value.getRed(), value.getGreen(), value.getBlue());
    }

    private static Sound sound(String value) {
        if (NamespacedKey.fromString(value) == null) throw new IllegalArgumentException("Invalid sound key: " + value);
        return Sounds.getByNameOrCreate(value);
    }
}
