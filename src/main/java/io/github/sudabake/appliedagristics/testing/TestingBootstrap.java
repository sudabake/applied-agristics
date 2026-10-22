package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.Agristics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Development classpath only: this package is excluded from the distributed JAR. */
@EventBusSubscriber(modid=Agristics.ID,bus=EventBusSubscriber.Bus.MOD)
public final class TestingBootstrap {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if(!Boolean.getBoolean("appliedagristics.tests")) return;
        event.register(AgristicsGameTests.class);event.register(BiologyGameTests.class);
        event.register(UniversalSeedGameTests.class);event.register(MultiblockGameTests.class);
        event.register(RemainingWorkGameTests.class);event.register(RevisionGameTests.class);
        event.register(AuditRegressionTests.class);event.register(DeepReviewTests.class);
    }
}
