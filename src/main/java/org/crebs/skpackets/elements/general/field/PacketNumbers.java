package org.crebs.skpackets.elements.general.field;

import java.math.BigDecimal;

final class PacketNumbers {
    private PacketNumbers() {}

    static Object convert(Number value, Class<?> type, String name) {
        try {
            if (type == float.class || type == Float.class) {
                float result = value.floatValue();
                if (!Float.isFinite(result)) throw new ArithmeticException();
                return result;
            }
            if (type == double.class || type == Double.class) {
                double result = value.doubleValue();
                if (!Double.isFinite(result)) throw new ArithmeticException();
                return result;
            }
            long result = new BigDecimal(value.toString()).longValueExact();
            if (type == long.class || type == Long.class) return result;
            if (type == int.class || type == Integer.class) return Math.toIntExact(result);
            if (type == short.class || type == Short.class) {
                if (result < Short.MIN_VALUE || result > Short.MAX_VALUE) throw new ArithmeticException();
                return (short) result;
            }
            if (type == byte.class || type == Byte.class) {
                if (result < -128 || result > 255) throw new ArithmeticException();
                return (byte) result;
            }
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Invalid or out-of-range number for packet field '" + name + "': " + value, exception);
        }
        throw PacketArguments.invalid(name, type, value);
    }
}
