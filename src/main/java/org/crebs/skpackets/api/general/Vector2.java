package org.crebs.skpackets.api.general;

import org.jspecify.annotations.NonNull;

public record Vector2(double x, double y) {
    public Vector2 {
        if (!Double.isFinite(x) || !Double.isFinite(y))
            throw new IllegalArgumentException("Vector2 coordinates must be finite numbers.");
    }

    @Override public @NonNull String toString() { return "x: " + format(x) + ", y: " + format(y); }

    private static String format(double coordinate) {
        return java.math.BigDecimal.valueOf(coordinate).stripTrailingZeros().toPlainString();
    }
}
