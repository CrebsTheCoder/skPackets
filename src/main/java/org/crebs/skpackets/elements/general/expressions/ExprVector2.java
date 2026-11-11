package org.crebs.skpackets.elements.general.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.github.shanebeee.skr.Registration;
import org.bukkit.event.Event;
import org.crebs.skpackets.api.general.Vector2;
import org.crebs.skpackets.elements.general.Functions;

public final class ExprVector2 extends SimpleExpression<Vector2> {

    public static void register(Registration reg) {
        reg.newSimpleExpression(ExprVector2.class, Vector2.class, "[a] [new] vector2 %number%,[ ]%number%")
                .name("General - Vector2").description("Creates a two-dimensional vector, like vec2(x, y).")
                .examples("set {_v} to vector2 2, 3").since("1.0.1").register();
    }

    private Expression<Number> x, y;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result) {
        x = (Expression<Number>) expressions[0];
        y = (Expression<Number>) expressions[1];
        return true;
    }

    @Override
    protected Vector2[] get(Event event) {
        Vector2 value = Functions.createVector2(x.getSingle(event), y.getSingle(event));
        return value == null ? new Vector2[0] : new Vector2[]{value};
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<Vector2> getReturnType() { return Vector2.class; }

    @Override
    public String toString(Event event, boolean debug) {
        return "vector2 " + x.toString(event, debug) + ", " + y.toString(event, debug);
    }
}
