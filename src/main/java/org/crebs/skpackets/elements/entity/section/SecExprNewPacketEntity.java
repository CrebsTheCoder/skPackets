package org.crebs.skpackets.elements.entity.section;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import me.tofaa.entitylib.wrapper.WrapperEntity;

import org.bukkit.event.Event;

import java.util.List;

public class SecExprNewPacketEntity extends SectionExpression<WrapperEntity> {
    private final PacketEntitySpawn spawn = new PacketEntitySpawn();

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(
                        SecExprNewPacketEntity.class, WrapperEntity.class, "[a] new packet entity")
                .name("New Packet Entity")
                .description(
                        "Spawns and returns a packet entity. Requires entity, viewers and location"
                                + " entries; persistent defaults to true. Metadata belongs in the"
                                + " optional meta field section.")
                .examples(
                        "set {_mob} to a new packet entity:",
                        "    entity: zombie",
                        "    viewers: player",
                        "    location: location of player",
                        "    persistent: true",
                        "    meta field:",
                        "        sneaking: true")
                .since("1.0.0")
                .register();
    }

    @Override
    public boolean init(
            Expression<?>[] expressions,
            int pattern,
            Kleenean delayed,
            SkriptParser.ParseResult result,
            SectionNode node,
            List<TriggerItem> items) {
        return spawn.init(node);
    }

    @Override
    protected WrapperEntity[] get(Event event) {
        WrapperEntity entity = spawn.spawn(event);
        return entity == null ? new WrapperEntity[0] : new WrapperEntity[] {entity};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends WrapperEntity> getReturnType() {
        return WrapperEntity.class;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "new packet entity";
    }
}
