package org.crebs.skpackets.elements.general;

import ch.njol.skript.Skript;
import com.github.shanebeee.skr.Registration;
import org.crebs.skpackets.api.general.Vector2;
import org.skriptlang.skript.common.function.DefaultFunction;

public final class Functions {

    private Functions() {}

    public static void register(Registration reg) {
        reg.newFunction(DefaultFunction.builder(reg.getAddon(), "vec2", Vector2.class)
                .parameter("x", Number.class).parameter("y", Number.class)
                .description("Creates a persistable two-dimensional vector.")
                .since("1.0.1").examples("set {saved::offset} to vec2(2, 3)")
                .build(args -> createVector2(args.get("x"), args.get("y"))))
                .name("General - Vec2").since("1.0.1").register();
    }

    public static Vector2 createVector2(Number x, Number y) {
        if (x == null || y == null) return null;
        try {
            return new Vector2(x.doubleValue(), y.doubleValue());
        } catch (IllegalArgumentException ex) {
            Skript.error(ex.getMessage());
            return null;
        }
    }
}
