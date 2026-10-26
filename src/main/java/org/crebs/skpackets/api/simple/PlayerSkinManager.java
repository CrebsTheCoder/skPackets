/*
 * MIT License
 *
 * Copyright (c) 2026 3add
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Difficulty;
import com.github.retrooper.packetevents.protocol.world.WorldBlockPosition;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState.Reason;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import com.google.common.hash.Hashing;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.crebs.skpackets.api.entity.Skin;
import org.crebs.skpackets.api.entity.SkinManager;
import org.crebs.skpackets.api.util.ConversionUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSkinManager {

    private record DisplayedSkin(Skin skin, boolean persistent) {}

    private static final Map<UUID, Map<UUID, DisplayedSkin>> skinMap = new ConcurrentHashMap<>();

    private static final Map<UUID, DisplayedSkin> globalSkinMap = new ConcurrentHashMap<>();

    public static void setGlobalSkin(Player target, Skin skin) {
        setGlobalSkin(target, skin, true);
    }

    public static void setGlobalSkin(Player target, Skin skin, boolean persistent) {
        requireMainThread();
        if (target == null || skin == null) {
            return;
        }

        skinMap.remove(target.getUniqueId());
        if (!persistent && skin.equals(SkinManager.getPlayer(target))) {

            globalSkinMap.remove(target.getUniqueId());
        } else {
            globalSkinMap.put(target.getUniqueId(), new DisplayedSkin(skin, persistent));
        }

        updateSkin(target, Bukkit.getOnlinePlayers());
    }

    public static void setSkinForViewers(
            Player target, Collection<? extends Player> viewers, Skin skin) {
        setSkinForViewers(target, viewers, skin, true);
    }

    public static void setSkinForViewers(
            Player target, Collection<? extends Player> viewers, Skin skin, boolean persistent) {
        requireMainThread();
        if (target == null || viewers == null || skin == null) {
            return;
        }

        Map<UUID, DisplayedSkin> targetMap =
                skinMap.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>());
        for (Player Viewer : viewers) {
            targetMap.put(Viewer.getUniqueId(), new DisplayedSkin(skin, persistent));
        }

        updateSkin(target, viewers);
    }

    public static @Nullable Skin getSkinForViewer(UUID targetId, UUID viewerId) {
        DisplayedSkin displayed = getDisplayedSkin(targetId, viewerId);
        return displayed == null ? null : displayed.skin();
    }

    public static @Nullable Skin getPersistentSkinForViewer(UUID targetId, UUID viewerId) {
        DisplayedSkin displayed = getDisplayedSkin(targetId, viewerId);
        return displayed != null && displayed.persistent() ? displayed.skin() : null;
    }

    private static @Nullable DisplayedSkin getDisplayedSkin(UUID targetId, UUID viewerId) {
        Map<UUID, DisplayedSkin> viewers = skinMap.get(targetId);
        DisplayedSkin override = viewers == null ? null : viewers.get(viewerId);
        return override != null ? override : globalSkinMap.get(targetId);
    }

    public static void clearAllSkins(Player target) {
        requireMainThread();
        skinMap.remove(target.getUniqueId());
        globalSkinMap.remove(target.getUniqueId());
        updateSkin(target, Bukkit.getOnlinePlayers());
    }

    private static void updateSkin(Player target, Collection<? extends Player> viewers) {
        if (!target.isOnline() || PacketEvents.getAPI().getPlayerManager().getUser(target) == null)
            return;
        for (Player viewer : new HashSet<>(viewers)) {
            if (!viewer.isOnline() || !viewer.canSee(target)) continue;
            send(viewer, new WrapperPlayServerPlayerInfoRemove(target.getUniqueId()));
            reRegisterProfile(target, viewer);
            if (viewer.equals(target)) {
                PlayerSkinRefresh.request(target);
            } else if (target.getTrackedBy().contains(viewer)) {
                send(viewer, new WrapperPlayServerDestroyEntities(target.getEntityId()));
                send(
                        viewer,
                        new WrapperPlayServerSpawnEntity(
                                target.getEntityId(),
                                target.getUniqueId(),
                                EntityTypes.PLAYER,
                                ConversionUtil.toPeLocation(target.getLocation()),
                                target.getBodyYaw(),
                                0,
                                ConversionUtil.toPeVectorD(target.getVelocity())));
                restoreEntity(target, viewer);
            }
        }
    }

    private static void send(Player viewer, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    private static void restoreEntity(Player target, Player viewer) {
        send(
                viewer,
                new WrapperPlayServerEntityMetadata(
                        target.getEntityId(), SpigotConversionUtil.getEntityMetadata(target)));
        var equipment = new ArrayList<Equipment>();
        for (var slot :
                List.of(
                        org.bukkit.inventory.EquipmentSlot.HAND,
                        org.bukkit.inventory.EquipmentSlot.OFF_HAND,
                        org.bukkit.inventory.EquipmentSlot.FEET,
                        org.bukkit.inventory.EquipmentSlot.LEGS,
                        org.bukkit.inventory.EquipmentSlot.CHEST,
                        org.bukkit.inventory.EquipmentSlot.HEAD)) {
            var packetSlot =
                    switch (slot) {
                        case HAND -> EquipmentSlot.MAIN_HAND;
                        case OFF_HAND -> EquipmentSlot.OFF_HAND;
                        case FEET -> EquipmentSlot.BOOTS;
                        case LEGS -> EquipmentSlot.LEGGINGS;
                        case CHEST -> EquipmentSlot.CHEST_PLATE;
                        case HEAD -> EquipmentSlot.HELMET;
                        default ->
                                throw new IllegalArgumentException(
                                        "Unsupported player equipment slot: " + slot);
                    };
            equipment.add(
                    new Equipment(
                            packetSlot,
                            SpigotConversionUtil.fromBukkitItemStack(
                                    target.getInventory().getItem(slot))));
        }
        send(viewer, new WrapperPlayServerEntityEquipment(target.getEntityId(), equipment));
        var attributes = new ArrayList<WrapperPlayServerUpdateAttributes.Property>();
        for (var attribute : Registry.ATTRIBUTE) {
            var instance = target.getAttribute(attribute);
            var packetAttribute = Attributes.getByName(attribute.getKey().toString());
            if (instance == null || packetAttribute == null) continue;
            if (viewer.equals(target)
                    && attribute == Attribute.MAX_HEALTH
                    && target.isHealthScaled()) {
                attributes.add(
                        new WrapperPlayServerUpdateAttributes.Property(
                                packetAttribute, target.getHealthScale(), List.of()));
                continue;
            }
            var modifiers = new ArrayList<WrapperPlayServerUpdateAttributes.PropertyModifier>();
            for (var modifier : instance.getModifiers()) {
                modifiers.add(
                        new WrapperPlayServerUpdateAttributes.PropertyModifier(
                                new ResourceLocation(modifier.getKey().toString()),
                                modifier.getAmount(),
                                WrapperPlayServerUpdateAttributes.PropertyModifier.Operation
                                        .values()[modifier.getOperation().ordinal()]));
            }
            attributes.add(
                    new WrapperPlayServerUpdateAttributes.Property(
                            packetAttribute, instance.getBaseValue(), modifiers));
        }
        send(viewer, new WrapperPlayServerUpdateAttributes(target.getEntityId(), attributes));
        send(
                viewer,
                new WrapperPlayServerEntityHeadLook(
                        target.getEntityId(), target.getLocation().getYaw()));
        for (var effect : target.getActivePotionEffects()) {
            byte flags =
                    (byte)
                            ((effect.isAmbient() ? 1 : 0)
                                    | (effect.hasParticles() ? 2 : 0)
                                    | (effect.hasIcon() ? 4 : 0));
            send(
                    viewer,
                    new WrapperPlayServerEntityEffect(
                            target.getEntityId(),
                            SpigotConversionUtil.fromBukkitPotionEffectType(effect.getType()),
                            effect.getAmplifier(),
                            effect.getDuration(),
                            flags));
        }
        Entity root = target;
        while (root.getVehicle() != null) root = root.getVehicle();
        restorePassengers(root, viewer, new HashSet<>());
    }

    private static void restorePassengers(Entity entity, Player viewer, Set<UUID> visited) {
        if (!visited.add(entity.getUniqueId())) return;
        var passengers = entity.getPassengers();
        if (!passengers.isEmpty()) {
            send(
                    viewer,
                    new WrapperPlayServerSetPassengers(
                            entity.getEntityId(),
                            passengers.stream().mapToInt(Entity::getEntityId).toArray()));
            for (var passenger : passengers) restorePassengers(passenger, viewer, visited);
        }
    }

    private static void reRegisterProfile(Player target, Player viewer) {
        User targetUser = PacketEvents.getAPI().getPlayerManager().getUser(target);

        List<TextureProperty> properties;
        Skin customSkin = getSkinForViewer(target.getUniqueId(), viewer.getUniqueId());
        if (customSkin != null) {
            properties = customSkin.properties();
        } else {
            properties = targetUser.getProfile().getTextureProperties();
        }

        UserProfile profile =
                new UserProfile(
                        targetUser.getProfile().getUUID(),
                        targetUser.getProfile().getName(),
                        properties);

        PlayerInfo playerInfo =
                new PlayerInfo(
                        profile,
                        viewer.isListed(target),
                        target.getPing(),
                        GameMode.valueOf(target.getGameMode().name()),
                        target.playerListName(),
                        ChatSessionListener.getChatSession(target.getUniqueId()));

        playerInfo.setListOrder(target.getPlayerListOrder());
        playerInfo.setShowHat(true);

        WrapperPlayServerPlayerInfoUpdate infoUpdate =
                new WrapperPlayServerPlayerInfoUpdate(
                        EnumSet.allOf(WrapperPlayServerPlayerInfoUpdate.Action.class), playerInfo);

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, infoUpdate);
    }

    static void refreshPlayer(Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);

        World world = player.getWorld();
        DimensionType dimensionType = user.getDimensionType();
        String worldName = world.getKey().toString();
        Difficulty difficulty = Difficulty.valueOf(world.getDifficulty().name());
        long hashedSeed = Hashing.sha256().hashLong(world.getSeed()).asLong();
        GameMode gameMode = GameMode.valueOf(player.getGameMode().name());
        @Nullable
        GameMode prevGameMode =
                player.getPreviousGameMode() != null
                        ? GameMode.valueOf(player.getPreviousGameMode().name())
                        : null;
        @Nullable
        WorldBlockPosition position =
                player.getLastDeathLocation() != null
                        ? ConversionUtil.toWorldBlockPosition(player.getLastDeathLocation())
                        : null;

        WrapperPlayServerRespawn respawn =
                new WrapperPlayServerRespawn(
                        dimensionType,
                        worldName,
                        difficulty,
                        hashedSeed,
                        gameMode,
                        prevGameMode,
                        world.getWorldType() == org.bukkit.WorldType.DEBUG_ALL_BLOCK_STATES,
                        world.getWorldType() == org.bukkit.WorldType.FLAT,
                        WrapperPlayServerRespawn.KEEP_ALL_DATA,
                        position,
                        null);

        respawn.setPortalCooldown(player.getPortalCooldown());
        respawn.setSeaLevel(world.getSeaLevel());
        send(player, respawn);
        var at = player.getLocation();
        send(
                player,
                new WrapperPlayServerPlayerPositionAndLook(
                        PlayerSkinRefresh.nextTeleport(player),
                        ConversionUtil.toPeVectorD(at.toVector()),
                        ConversionUtil.toPeVectorD(player.getVelocity()),
                        at.getYaw(),
                        at.getPitch(),
                        (byte) 0));
        send(player, new WrapperPlayServerChangeGameState(Reason.START_LOADING_CHUNKS, 0));
        send(
                player,
                new WrapperPlayServerPlayerAbilities(
                        player.isInvulnerable()
                                || player.getGameMode() == org.bukkit.GameMode.CREATIVE
                                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR,
                        player.isFlying(),
                        player.getAllowFlight(),
                        player.getGameMode() == org.bukkit.GameMode.CREATIVE,
                        player.getFlySpeed() / 2,
                        player.getWalkSpeed() / 2));
        var items = new ArrayList<com.github.retrooper.packetevents.protocol.item.ItemStack>();
        for (int slot = 0; slot < 46; slot++) {
            items.add(
                    SpigotConversionUtil.fromBukkitItemStack(
                            player.getOpenInventory().getItem(slot)));
        }
        send(
                player,
                new WrapperPlayServerWindowItems(
                        0,
                        PlayerSkinRefresh.inventoryState(player),
                        items,
                        SpigotConversionUtil.fromBukkitItemStack(player.getItemOnCursor())));
        send(player, new WrapperPlayServerHeldItemChange(player.getInventory().getHeldItemSlot()));
        send(
                player,
                new WrapperPlayServerSetExperience(
                        player.getExp(), player.getLevel(), player.getTotalExperience()));
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        float health =
                (float)
                        (player.isHealthScaled()
                                ? player.getHealth() * player.getHealthScale() / maxHealth
                                : player.getHealth());
        send(
                player,
                new WrapperPlayServerUpdateHealth(
                        health, player.getFoodLevel(), player.getSaturation()));
        restoreEntity(player, player);
        Entity camera =
                player.getGameMode() == org.bukkit.GameMode.SPECTATOR
                        ? player.getSpectatorTarget()
                        : null;
        if (camera != null) send(player, new WrapperPlayServerCamera(camera.getEntityId()));
    }

    private static void requireMainThread() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Change displayed skins on the server thread");
    }

    static void forget(UUID id) {
        skinMap.remove(id);
        globalSkinMap.remove(id);
        skinMap.values().forEach(viewers -> viewers.remove(id));
    }

    static void clear() {
        skinMap.clear();
        globalSkinMap.clear();
    }
}
