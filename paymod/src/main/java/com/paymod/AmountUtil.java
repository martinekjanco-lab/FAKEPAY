package com.paymod;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountUtil {

    /** Parses things like 35774, 1k, 2.5m, 1b, 3t. Throws NumberFormatException if invalid. */
    public static long parse(String input) {
        String s = input.trim().toLowerCase().replace(",", "").replace("_", "");
        if (s.isEmpty()) throw new NumberFormatException("empty");
        BigDecimal mult = BigDecimal.ONE;
        char last = s.charAt(s.length() - 1);
        switch (last) {
            case 'k' -> mult = BigDecimal.valueOf(1_000L);
            case 'm' -> mult = BigDecimal.valueOf(1_000_000L);
            case 'b' -> mult = BigDecimal.valueOf(1_000_000_000L);
            case 't' -> mult = BigDecimal.valueOf(1_000_000_000_000L);
            default -> { }
        }
        if (!mult.equals(BigDecimal.ONE)) s = s.substring(0, s.length() - 1);
        try {
            BigDecimal v = new BigDecimal(s).multiply(mult).setScale(0, RoundingMode.DOWN);
            return v.longValueExact();
        } catch (ArithmeticException e) {
            throw new NumberFormatException("too large");
        }
    }

    /** 296000000 -> 296M, 1500 -> 1.5K */
    public static String format(long v) {
        long abs = Math.abs(v);
        if (abs >= 1_000_000_000_000L) return fmt(v, 1_000_000_000_000L, "T");
        if (abs >= 1_000_000_000L) return fmt(v, 1_000_000_000L, "B");
        if (abs >= 1_000_000L) return fmt(v, 1_000_000L, "M");
        if (abs >= 1_000L) return fmt(v, 1_000L, "K");
        return Long.toString(v);
    }

    private static String fmt(long v, long unit, String suffix) {
        return BigDecimal.valueOf(v).divide(BigDecimal.valueOf(unit), 2, RoundingMode.DOWN)
                .stripTrailingZeros().toPlainString() + suffix;
    }
}
