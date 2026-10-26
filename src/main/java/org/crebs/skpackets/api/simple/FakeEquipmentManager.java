package org.crebs.skpackets.api.simple;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class FakeEquipmentManager extends PacketListenerAbstract
        implements Listener, AutoCloseable {
    private record Key(UUID viewer, UUID target) {}

    private record Appearance(UUID target, Map<EquipmentSlot, ItemStack> slots) {}

    private final Map<Key, Appearance> overrides = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> entityIds = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;
    private volatile boolean closed;

    public FakeEquipmentManager(JavaPlugin plugin) {
        super(PacketListenerPriority.HIGHEST);
        this.plugin = plugin;
        PacketEvents.getAPI().getEventManager().registerListener(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void show(
            Player viewer,
            LivingEntity target,
            org.bukkit.inventory.EquipmentSlot slot,
            org.bukkit.inventory.ItemStack item,
            boolean persistent) {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Fake equipment must be changed on the server thread.");
        if (!viewer.isOnline() || !target.isValid() || !viewer.getWorld().equals(target.getWorld()))
            return;
        EquipmentSlot packetSlot = packetSlot(slot);
        ItemStack encoded = SpigotConversionUtil.fromBukkitItemStack(item.clone());
        Key key = new Key(viewer.getUniqueId(), target.getUniqueId());
        entityIds.put(target.getEntityId(), target.getUniqueId());
        overrides.compute(
                key,
                (ignored, previous) -> {
                    var slots = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
                    if (previous != null && previous.target().equals(target.getUniqueId()))
                        slots.putAll(previous.slots());
                    if (persistent) slots.put(packetSlot, encoded.copy());
                    else slots.remove(packetSlot);
                    return slots.isEmpty()
                            ? null
                            : new Appearance(
                                    target.getUniqueId(),
                                    Map.copyOf(slots));
                });
        if (!persistent) pruneEntityIds();
        send(viewer, target.getEntityId(), Map.of(packetSlot, encoded));
    }

    public void reset(Player viewer, LivingEntity target, org.bukkit.inventory.EquipmentSlot slot) {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Fake equipment must be changed on the server thread.");
        EquipmentSlot packetSlot = packetSlot(slot);
        overrides.computeIfPresent(new Key(viewer.getUniqueId(), target.getUniqueId()), (key, previous) -> {
            var slots = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
            slots.putAll(previous.slots());
            slots.remove(packetSlot);
            return slots.isEmpty() ? null : new Appearance(target.getUniqueId(), Map.copyOf(slots));
        });
        pruneEntityIds();
        if (viewer.isOnline() && target.isValid() && viewer.getWorld().equals(target.getWorld())
                && target.getEquipment() != null) {
            send(viewer, target.getEntityId(), Map.of(packetSlot,
                    SpigotConversionUtil.fromBukkitItemStack(target.getEquipment().getItem(slot))));
        }
    }

    public void reset(Player viewer, LivingEntity target) {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Fake equipment must be changed on the server thread.");
        overrides.remove(new Key(viewer.getUniqueId(), target.getUniqueId()));
        pruneEntityIds();
        if (!viewer.isOnline() || !target.isValid() || !viewer.getWorld().equals(target.getWorld())
                || target.getEquipment() == null) return;
        var slots = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
        for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
            if ((slot == org.bukkit.inventory.EquipmentSlot.BODY
                    || slot == org.bukkit.inventory.EquipmentSlot.SADDLE)
                    && !target.canUseEquipmentSlot(slot)) continue;
            slots.put(packetSlot(slot),
                    SpigotConversionUtil.fromBukkitItemStack(target.getEquipment().getItem(slot)));
        }
        send(viewer, target.getEntityId(), slots);
    }

    public static EquipmentSlot packetSlot(org.bukkit.inventory.EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> EquipmentSlot.MAIN_HAND;
            case OFF_HAND -> EquipmentSlot.OFF_HAND;
            case HEAD -> EquipmentSlot.HELMET;
            case CHEST -> EquipmentSlot.CHEST_PLATE;
            case LEGS -> EquipmentSlot.LEGGINGS;
            case FEET -> EquipmentSlot.BOOTS;
            case BODY -> EquipmentSlot.BODY;
            case SADDLE -> EquipmentSlot.SADDLE;
        };
    }

    @Override
    public void onPacketSend(@org.jetbrains.annotations.NotNull PacketSendEvent event) {
        if (closed || event.isCancelled()) return;
        UUID viewer = event.getUser().getUUID();
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            var packet = new WrapperPlayServerEntityEquipment(event);
            Appearance appearance = overrides.get(new Key(viewer, entityIds.get(packet.getEntityId())));
            if (appearance == null) return;
            var slots = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
            packet.getEquipment().forEach(entry -> slots.put(entry.getSlot(), entry.getItem()));
            appearance.slots().forEach((slot, item) -> slots.put(slot, item.copy()));
            packet.setEquipment(
                    slots.entrySet().stream()
                            .map(entry -> new Equipment(entry.getKey(), entry.getValue()))
                            .toList());
            event.markForReEncode(true);
        } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            var packet = new WrapperPlayServerSpawnEntity(event);
            UUID target = packet.getUUID().orElse(null);
            if (target == null) return;
            Key key = new Key(viewer, target);
            Appearance appearance = overrides.get(key);
            if (appearance == null) return;
            entityIds.put(packet.getEntityId(), target);
            event.getTasksAfterSend()
                    .add(
                            () -> {
                                if (!closed)
                                    Bukkit.getScheduler().runTask(plugin, () -> reapply(key));
                            });
        }
    }

    private void reapply(Key key) {
        if (closed) return;
        Appearance appearance = overrides.get(key);
        Player viewer = Bukkit.getPlayer(key.viewer());
        if (appearance == null || viewer == null) return;
        var target = Bukkit.getEntity(appearance.target());
        if (target == null || !target.isValid()) return;
        entityIds.put(target.getEntityId(), target.getUniqueId());
        if (viewer.getWorld().equals(target.getWorld()))
            send(viewer, target.getEntityId(), appearance.slots());
    }

    private void send(Player viewer, int entity, Map<EquipmentSlot, ItemStack> slots) {
        var entries =
                slots.entrySet().stream()
                        .map(entry -> new Equipment(entry.getKey(), entry.getValue().copy()))
                        .toList();
        PacketEvents.getAPI()
                .getPlayerManager()
                .sendPacketSilently(viewer, new WrapperPlayServerEntityEquipment(entity, entries));
    }

    private void forgetTarget(UUID target) {
        overrides.values().removeIf(value -> value.target().equals(target));
        pruneEntityIds();
    }

    private void pruneEntityIds() {
        Set<UUID> targets = new HashSet<>();
        overrides.keySet().forEach(key -> targets.add(key.target()));
        entityIds.values().removeIf(id -> !targets.contains(id));
    }

    private void refresh(Player player) {
        UUID id = player.getUniqueId();
        entityIds.entrySet().removeIf(entry -> entry.getValue().equals(id)
                && entry.getKey() != player.getEntityId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (closed) return;
            overrides.keySet().stream()
                    .filter(key -> key.viewer().equals(id) || key.target().equals(id))
                    .forEach(this::reapply);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        entityIds.values().removeIf(id -> id.equals(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { refresh(event.getPlayer()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) { refresh(event.getPlayer()); }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) { refresh(event.getPlayer()); }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) forgetTarget(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onUnload(EntitiesUnloadEvent event) {
        event.getEntities().stream().filter(entity -> !(entity instanceof Player))
                .forEach(entity -> forgetTarget(entity.getUniqueId()));
    }

    @Override
    public void close() {
        closed = true;
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        HandlerList.unregisterAll(this);
        overrides.forEach(
                (key, appearance) -> {
                    Player viewer = Bukkit.getPlayer(key.viewer());
                    var entity = Bukkit.getEntity(appearance.target());
                    if (viewer == null
                            || !(entity instanceof LivingEntity target)
                            || target.getEquipment() == null
                            || !target.getWorld().equals(viewer.getWorld())) return;
                    for (org.bukkit.inventory.EquipmentSlot slot :
                            org.bukkit.inventory.EquipmentSlot.values()) {
                        if (appearance.slots().containsKey(packetSlot(slot)))
                            viewer.sendEquipmentChange(
                                    target, slot, target.getEquipment().getItem(slot));
                    }
                });
        overrides.clear();
        entityIds.clear();
    }
}
