package io.github.sudabake.appliedagristics.logic;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SeedProbabilityTest {
    @Test void disabledAndGuaranteedModesHaveExactCounts() {
        var random=new Random(17);
        assertEquals(0,SeedReproduction.roll(100000,0,random::nextDouble,random::nextGaussian));
        assertEquals(100000,SeedReproduction.roll(100000,1,random::nextDouble,random::nextGaussian));
    }
    @Test void customProbabilityChangesTheDistribution() {
        var random=new Random(23);long total=0;
        for(int i=0;i<100;i++) total+=SeedReproduction.roll(10000,.35,random::nextDouble,random::nextGaussian);
        assertTrue(Math.abs(total-350000)<3000);
        assertThrows(IllegalArgumentException.class,()->SeedReproduction.roll(1,Double.NaN,random::nextDouble,random::nextGaussian));
    }
}
