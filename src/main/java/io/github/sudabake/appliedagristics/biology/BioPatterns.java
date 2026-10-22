package io.github.sudabake.appliedagristics.biology;

import java.util.*;

/** Translation and horizontal mirroring have the same meaning as vanilla crafting. */
public final class BioPatterns {
    private BioPatterns() {}
    private static int mask(String digits) { int mask = 0; for (char c : digits.toCharArray()) mask |= 1 << (c - '1'); return mask; }
    private static int normalize(int mask) {
        int minX = 3, minY = 3;
        for (int i = 0; i < 9; i++) if ((mask & 1 << i) != 0) { minX = Math.min(minX, i % 3); minY = Math.min(minY, i / 3); }
        int result = 0; for (int i = 0; i < 9; i++) if ((mask & 1 << i) != 0) result |= 1 << ((i / 3 - minY) * 3 + i % 3 - minX);
        return result;
    }
    public static int canonical(int mask) {
        if (mask == 0) return 0;
        int mirror = 0; for (int i = 0; i < 9; i++) if ((mask & 1 << i) != 0) mirror |= 1 << (i / 3 * 3 + 2 - i % 3);
        return Math.min(normalize(mask), normalize(mirror));
    }
    private static final List<Integer> PATTERNS = create();
    private static List<Integer> create() {
        var result = new LinkedHashSet<Integer>();
        // Eleven distinct shapes: row, column, diagonal, square, T, inverted T,
        // L, four corners, cross, hollow square, full square.
        for (var digits : List.of("123", "147", "159", "1245", "1235", "2456", "145", "1379", "24568", "12346789", "123456789")) result.add(canonical(mask(digits)));
        var extra = new ArrayList<Integer>(); for (int m = 1; m < 512; m++) extra.add(m);
        extra.sort(Comparator.comparingInt(Integer::bitCount).thenComparingInt(Integer::intValue));
        for (int m : extra) result.add(canonical(m)); return List.copyOf(result);
    }
    public static int index(int mask) { return PATTERNS.indexOf(canonical(mask)); }
    public static int at(int index) { return PATTERNS.get(index); }
    public static int capacity() { return PATTERNS.size(); }
    public static String digits(int index) { var s = new StringBuilder(); int m = at(index); for (int i = 0; i < 9; i++) if ((m & 1 << i) != 0) s.append(i + 1); return s.toString(); }
}
