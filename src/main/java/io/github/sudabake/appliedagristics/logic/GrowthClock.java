package io.github.sudabake.appliedagristics.logic;

/** Both clocks advance only while output is available. A watered harvest still needs water at commit. */
public final class GrowthClock {
    private int dry;
    private int wet;
    public void advance(int ticks, boolean watered) {
        dry = (int) Math.min(Integer.MAX_VALUE, (long) dry + ticks);
        if (watered) wet = (int) Math.min(Integer.MAX_VALUE, (long) wet + ticks);
    }
    public boolean dryReady(int duration) { return dry >= duration; }
    public boolean wetReady(int duration, boolean hasWater) { return hasWater && wet >= duration; }
    public int dry() { return dry; }
    public int wet() { return wet; }
    public void restore(int dry, int wet) { this.dry = Math.max(0, dry); this.wet = Math.max(0, wet); }
    public void reset() { dry = 0; wet = 0; }
}
