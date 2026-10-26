package org.crebs.skpackets.elements.entity.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.shanebeee.skr.Registration;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import me.tofaa.entitylib.wrapper.WrapperEntity;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.crebs.skpackets.api.simple.FakeEquipmentManager;

import java.util.*;

public final class ExprFakeEquipment extends SimpleExpression<ItemType> {
    private Expression<org.bukkit.inventory.EquipmentSlot> slot;
    private Expression<WrapperEntity> entities;

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        ExprFakeEquipment.class,
                        ItemType.class,
                        "(fake|packet) equipment %equipmentslot% of %packetentities%")
                .name("Fake Entity Equipment")
                .description(
                        "Gets or sets equipment on fake living entities, including players. Uses Skript"
                            + " equipment slots: main hand, off hand, helmet slot, chestplate slot,"
                            + " leggings slot and boots slot. Body and saddle are not supported by EntityLib."
                            + " Delete/reset clears the slot. Equipment is retained for new"
                            + " viewers and persistent entity respawns.")
                .examples(
                        """
                        command /dress-up:
                            trigger:
                                set {_npc} to a new fake entity:
                                    entity: player
                                    username: "Knight"
                                    skin: skin of player
                                    location: location of player
                                    viewers: player
                                set fake equipment helmet slot of {_npc} to player's helmet
                                set fake equipment main hand of {_npc} to player's held item
                                wait 20 seconds
                                kill fake entity {_npc}
                        """)
                .since("1.0.0")
                .register();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(
            Expression<?>[] expressions,
            int pattern,
            Kleenean delayed,
            SkriptParser.ParseResult result) {
        slot = (Expression<org.bukkit.inventory.EquipmentSlot>) expressions[0];
        entities = (Expression<WrapperEntity>) expressions[1];
        return true;
    }

    private EquipmentSlot slot(Event event) {
        var value = slot.getSingle(event);
        if (value == null) return null;
        if (value == org.bukkit.inventory.EquipmentSlot.BODY
                || value == org.bukkit.inventory.EquipmentSlot.SADDLE) {
            Skript.error("Fake entity equipment supports hand and armor slots; EntityLib does not support body or saddle slots.");
            return null;
        }
        return FakeEquipmentManager.packetSlot(value);
    }

    @Override
    protected ItemType[] get(Event event) {
        EquipmentSlot selected = slot(event);
        if (selected == null) return new ItemType[0];
        List<ItemType> items = new ArrayList<>();
        for (WrapperEntity entity : entities.getArray(event)) {
            if (entity instanceof WrapperLivingEntity living) {
                var item = living.getEquipment().getItem(selected);
                items.add(
                        new ItemType(
                                item == null
                                        ? new ItemStack(Material.AIR)
                                        : SpigotConversionUtil.toBukkitItemStack(item)));
            } else Skript.error("Equipment requires a living fake entity.");
        }
        return items.toArray(ItemType[]::new);
    }

    @Override
    public Class<?>[] acceptChange(ChangeMode mode) {
        return mode == ChangeMode.SET || mode == ChangeMode.DELETE || mode == ChangeMode.RESET
                ? new Class<?>[] {ItemType.class}
                : null;
    }

    @Override
    public void change(Event event, Object[] delta, ChangeMode mode) {
        EquipmentSlot selected = slot(event);
        if (selected == null || (mode == ChangeMode.SET && (delta == null || delta.length == 0)))
            return;
        for (WrapperEntity entity : entities.getArray(event)) {
            if (!(entity instanceof WrapperLivingEntity living)) {
                Skript.error("Equipment requires a living fake entity.");
                continue;
            }
            if (mode == ChangeMode.SET)
                living.getEquipment()
                        .setItem(
                                selected,
                                SpigotConversionUtil.fromBukkitItemStack(
                                        ((ItemType) delta[0]).getRandom()));
            else living.getEquipment().clearSlot(selected);
        }
    }

    @Override
    public boolean isSingle() {
        return entities.isSingle();
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "fake equipment "
                + slot.toString(event, debug)
                + " of "
                + entities.toString(event, debug);
    }
}
