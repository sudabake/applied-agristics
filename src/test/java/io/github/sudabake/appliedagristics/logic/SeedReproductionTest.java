package io.github.sudabake.appliedagristics.logic;

import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class SeedReproductionTest {
    @Test void smallCyclesHaveTenPercentChance() {
        var random=new Random(1729); long successes=0;
        for (int i=0;i<100000;i++) successes+=SeedReproduction.roll(1,random::nextDouble,random::nextGaussian);
        assertTrue(successes>9700 && successes<10300);
        assertEquals(0,SeedReproduction.roll(0,random::nextDouble,random::nextGaussian));
    }
    @Test void batchesKeepBinomialMeanAndVariance() {
        var random=new Random(2048); double sum=0,squared=0; int samples=20000;
        for (int i=0;i<samples;i++) { long x=SeedReproduction.roll(1000,random::nextDouble,random::nextGaussian); assertTrue(x>=0 && x<=1000); sum+=x; squared+=(double)x*x; }
        double mean=sum/samples,variance=squared/samples-mean*mean;
        assertEquals(100,mean,.5); assertEquals(90,variance,4.5);
    }
    @Test void billionsOfCyclesDoNotLoopPerSeed() {
        var random=new Random(4096); var draws=new AtomicInteger();
        long value=SeedReproduction.roll(4_000_000_000L,()->{draws.incrementAndGet();return random.nextDouble();},random::nextGaussian);
        assertTrue(Math.abs(value-400_000_000L)<200_000); assertTrue(draws.get()<300);
    }
}
