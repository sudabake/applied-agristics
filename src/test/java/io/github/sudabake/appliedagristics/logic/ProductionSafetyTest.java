package io.github.sudabake.appliedagristics.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductionSafetyTest {
    @Test void fourPhysicalModulesRetainConfiguredMaximumAndPower() {
        for(int maximum:new int[]{256,65536}) {
            assertEquals(1,ProductionMath.moduleParallel(0,maximum));
            assertEquals(1,ProductionMath.moduleSpeed(0,maximum),1e-10);
            assertEquals(maximum,ProductionMath.moduleParallel(4,maximum));
            assertEquals(maximum,ProductionMath.moduleSpeed(4,maximum),1e-8);
            assertEquals(5000,ProductionMath.power(maximum,ProductionMath.moduleSpeed(4,maximum),4,(double)maximum*maximum,5000),1e-7);
        }
        assertEquals(4,ProductionMath.moduleParallel(1,256));
        assertEquals(16,ProductionMath.moduleParallel(2,256));
        assertEquals(64,ProductionMath.moduleParallel(3,256));
    }
    @Test void upgradeCurveAndPowerMatchTheAgreedExamples() {
        assertEquals(256, ProductionMath.parallel(8, 256));
        assertEquals(65536, ProductionMath.parallel(16, 65536));
        assertEquals(2, ProductionMath.speed(2, 256), 1e-12);
        assertEquals(36.660445, ProductionMath.speed(6, 256), 1e-5);
        assertEquals(4, ProductionMath.power(1, 1, 4, 65536, 5000), 1e-12);
        assertEquals(216.24, ProductionMath.power(32, ProductionMath.speed(5, 256), 4, 65536, 5000), .01);
        assertEquals(5000, ProductionMath.power(256, 256, 4, 65536, 5000), 1e-8);
        assertEquals(0, ProductionMath.power(0, 256, 4, 65536, 5000));
        assertTrue(ProductionMath.power(1, 256, 4, 65536, 5000) < 150);
        assertEquals(5000, ProductionMath.power(65536, 65536, 4, 4294967296.0, 5000), 1e-8);
    }
    @Test void amountsBeyondOrdinaryItemStacksRemainExact() {
        long count = 10_000_000_000L;
        assertEquals(64, Amounts.extractable(count, 1, 64));
        assertEquals(10_000_000_001L, count + Amounts.insertable(count, 1));
    }
    @Test void numericOverflowIsRejectedRatherThanVoided() {
        assertEquals(2, Amounts.insertable(Long.MAX_VALUE - 2, 10));
        assertEquals(0, Amounts.insertable(Long.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> Amounts.insertable(0, -1));
    }
    @Test void occupiedSeedsCannotBeWithdrawn() {
        assertEquals(0, Amounts.extractable(1, 1, 64));
        assertEquals(2, Amounts.extractable(3, 1, 64));
        assertEquals(0, Amounts.extractable(0, 1, 64));
    }
    @Test void networkTargetUsesCurrentStockAndResumesAfterConsumption() {
        assertEquals(0, Amounts.harvestSize(1000, 1000, 1));
        assertEquals(0, Amounts.harvestSize(1200, 1000, 1));
        assertEquals(1, Amounts.harvestSize(999, 1000, 1));
        assertEquals(4, Amounts.harvestSize(998, 1000, 4));
        assertEquals(4, Amounts.harvestSize(100000, -1, 4));
        assertEquals(0, Amounts.harvestSize(0, 0, 1));
    }
    @Test void waterIsOptionalAndWateredHarvestCannotCommitAfterWaterRemoval() {
        var clock = new GrowthClock();
        for (int i = 0; i < 30; i++) clock.advance(20, true);
        assertTrue(clock.wetReady(600, true));
        assertFalse(clock.wetReady(600, false));
        assertFalse(clock.dryReady(1200));
        for (int i = 0; i < 30; i++) clock.advance(20, false);
        assertTrue(clock.dryReady(1200));
    }
    @Test void restartPreservesProgressWithoutOfflineProduction() {
        var original = new GrowthClock(); original.advance(200, true);
        var restored = new GrowthClock(); restored.restore(original.dry(), original.wet());
        assertEquals(200, restored.dry()); assertEquals(200, restored.wet());
        assertFalse(restored.dryReady(1200)); restored.reset(); assertEquals(0, restored.wet());
    }
}
