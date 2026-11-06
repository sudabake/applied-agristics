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
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TestingBootstrap::releaseTestStructures);
        event.register(AgristicsGameTests.class);event.register(BiologyGameTests.class);
        event.register(UniversalSeedGameTests.class);event.register(MultiblockGameTests.class);
        event.register(RemainingWorkGameTests.class);event.register(RevisionGameTests.class);
        event.register(AuditRegressionTests.class);event.register(DeepReviewTests.class);
        event.register(Alpha18Tests.class);event.register(Alpha19Tests.class);event.register(Alpha20Tests.class);
        event.register(ReviewRegressionTests.class);
        event.register(WordUpdateTests.class);
        event.register(InteractionRevisionTests.class);
        event.register(VideoDemoTests.class);
        event.register(CropStorageTests.class);
        event.register(NextPhaseTests.class);
        event.register(ReferenceAdoptionTests.class);
        event.register(MEBiologyTests.class);
        event.register(UIUnificationTests.class);
        event.register(JdteBatchTests.class);
    }

    private static void releaseTestStructures(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        // Release the disposable fixtures before chunk unloading begins.
        for(var level:event.getServer().getAllLevels())
            io.github.sudabake.appliedagristics.multiblock.PlanterStructure.levelUnloaded(
                new net.neoforged.neoforge.event.level.LevelEvent.Unload(level));
    }
}
