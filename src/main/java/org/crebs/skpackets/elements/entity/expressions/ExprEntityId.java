package org.crebs.skpackets.elements.entity.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

import com.github.shanebeee.skr.Registration;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprEntityId extends SimpleExpression<Number> {

    public static void register(Registration registration) {
        registration
                .newSimpleExpression(ExprEntityId.class, Number.class, "entity id of %entity%")
                .name("Entity ID")
                .description("Returns the entity ID of a given entity.")
                .examples("set {_id} to entity id of player")
                .since("1.0.0")
                .register();
    }

    private Expression<?> entityExpression;

    @Override
    public boolean init(
            Expression<?>[] expressions,
            int matchedPattern,
            Kleenean isDelayed,
            ParseResult parseResult) {
        entityExpression = expressions[0];
        return true;
    }

    @Override
    protected Number[] get(Event event) {
        Object entity = entityExpression.getSingle(event);
        if (entity instanceof Entity bukkitEntity) {
            return new Number[] {bukkitEntity.getEntityId()};
        }
        return new Number[0];
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "entity id of " + entityExpression.toString(event, debug);
    }
}
