package io.github.sudabake.appliedagristics.logic;

/** Pure production and power calculations, shared with the upgrade preview. */
public final class ProductionMath {
    public static int cardLimit(int maximum) { return 32 - Integer.numberOfLeadingZeros(maximum - 1); }
    public static int parallel(int cards, int maximum) { return Math.min(maximum, 1 << Math.min(16, Math.max(0, cards))); }
    public static double speed(int cards, int maximum) {
        double fraction = Math.clamp((double) cards / cardLimit(maximum), 0, 1);
        return Math.exp(Math.log(maximum) * Math.pow(fraction, 1.5));
    }
    /** Four physical blocks span the existing configured upgrade range. */
    public static int moduleParallel(int modules, int maximum) {
        int steps = (int)Math.ceil(cardLimit(maximum) * Math.clamp(modules / 4.0, 0, 1));
        return parallel(steps, maximum);
    }
    public static double moduleSpeed(int modules, int maximum) {
        double fraction = Math.clamp(modules / 4.0, 0, 1);
        return Math.exp(Math.log(maximum) * Math.pow(fraction, 1.5));
    }
    public static double power(long occupied, double speed, double base, double maximumRate, double fullPower) {
        if (occupied <= 0) return 0;
        double rate = occupied * speed;
        double exponent = Math.log(Math.max(base, fullPower) / base) / Math.log(maximumRate);
        double logPower = Math.log(base) + exponent * Math.log(rate);
        return Math.exp(Math.min(Math.log(Double.MAX_VALUE / 20), logPower));
    }
    private ProductionMath() {}
}
