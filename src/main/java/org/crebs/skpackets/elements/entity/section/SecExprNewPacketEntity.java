package org.crebs.skpackets.elements.entity.section;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import com.github.shanebeee.skr.Registration;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SecExprNewPacketEntity extends SectionExpression<WrapperEntity> {

    public static void register(Registration registration) {
        registration.newSimpleExpression(SecExprNewPacketEntity.class, WrapperEntity.class, "[a] new packet entity")
                .name("New Packet Entity")
                .description("Creates a new packet entity.")
                .examples("new packet entity:")
                .since("1.0.0")
                .register();
    }

    //todo: work on this tmr rah ;-;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
        return false;
    }

    @Override
    protected WrapperEntity @Nullable [] get(Event event) {
        return new WrapperEntity[0];
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends WrapperEntity> getReturnType() {
        return null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "";
    }
}
