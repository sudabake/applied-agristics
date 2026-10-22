package io.github.sudabake.appliedagristics.logic;

/** AE2's transport uses signed longs: never wrap, truncate or silently void overflow. */
public final class Amounts {
    private Amounts() {}
    public static long insertable(long stored, long requested) {
        if (stored < 0 || requested < 0) throw new IllegalArgumentException("Negative amount");
        return Math.min(requested, Long.MAX_VALUE - stored);
    }
    public static long extractable(long stored, long reserved, long requested) {
        if (stored < 0 || reserved < 0 || requested < 0) throw new IllegalArgumentException("Negative amount");
        return Math.min(requested, Math.max(0, stored - Math.min(stored, reserved)));
    }
    public static int harvestSize(long stock, long target, int yield) {
        if (stock < 0 || target < -1 || yield < 0) throw new IllegalArgumentException("Invalid amount");
        return target >= 0 && stock >= target ? 0 : yield;
    }
}
