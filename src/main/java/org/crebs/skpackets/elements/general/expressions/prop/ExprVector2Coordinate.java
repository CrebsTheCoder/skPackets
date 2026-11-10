package org.crebs.skpackets.elements.general.expressions.prop;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.shanebeee.skr.Registration;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.general.Vector2;

public final class ExprVector2Coordinate extends SimpleExpression<Number> {
    public static void register(Registration reg) {
        reg.newSimpleExpression(ExprVector2Coordinate.class, Number.class,
                        "vector2 x of %vector2%", "vector2 (y|z) of %vector2%")
                .name("General - Vector2 Coordinate").description("Reads the first (X) or second (Y, also called Z for packet coordinates) coordinate.")
                .since("1.0.1").register();
    }
    private Expression<Vector2> vector;
    private boolean first;
    @SuppressWarnings("unchecked")
    @Override public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result) {
        vector = (Expression<Vector2>) expressions[0]; first = pattern == 0; return true;
    }
    @Override protected Number[] get(Event event) {
        Vector2 value = vector.getSingle(event);
        return value == null ? new Number[0] : new Number[]{first ? value.x() : value.y()};
    }
    @Override public boolean isSingle() { return true; }
    @Override public Class<Number> getReturnType() { return Number.class; }
    @Override public String toString(Event event, boolean debug) { return "vector2 " + (first ? "x" : "y") + " of " + vector.toString(event, debug); }
}
