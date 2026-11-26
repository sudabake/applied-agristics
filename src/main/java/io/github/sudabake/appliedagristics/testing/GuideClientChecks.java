package io.github.sudabake.appliedagristics.testing;

import guideme.Guide;
import guideme.internal.screen.GuideScreen;
import guideme.scene.LytGuidebookScene;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.client.ArrayGuide;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;

final class GuideClientChecks {
    private static final List<String> PAGES = List.of("index","getting-started","array","array-layers",
            "storage","crop-output","array-upgrades","bio-cultivator","infusion-altar");
    private static final List<String> SCREENSHOTS = List.of("index","array","array-layers","bio-cultivator");
    private int ticks;
    private int next;
    private boolean passed;

    GuideClientChecks(Minecraft minecraft) {
        var guide=ArrayGuide.guide();
        var root=guide.getNavigationTree().getNodeById(page("index"));
        var array=guide.getNavigationTree().getNodeById(page("array"));
        boolean chinese=minecraft.getLanguageManager().getSelected().equals("zh_cn");
        passed=root!=null && root.children().size()==7 && root.title().equals(chinese ? "应用农业" : "Applied Agristics")
                && array!=null && array.children().size()==1
                && array.children().getFirst().pageId().equals(page("array-layers"))
                && array.title().equals(chinese ? "搭建 ME 农业阵列" : "Build the ME Agricultural Array")
                && PAGES.stream().allMatch(name -> {
                    var document=guide.getPage(page(name));
                    return document!=null && !document.document().getBlocks().isEmpty();
                })
                && sceneCounts(guide,"array").equals(List.of(36L))
                && sceneCounts(guide,"array-layers").equals(List.of(9L,18L,27L,36L))
                && sceneCounts(guide,"array-upgrades").equals(List.of(36L))
                && page("storage").equals(ArrayGuide.pageForPart("seed_storage"))
                && page("crop-output").equals(ArrayGuide.pageForPart("essence_vault"))
                && page("array-upgrades").equals(ArrayGuide.pageForPart("planter_speed_module"));
    }

    boolean tick(Minecraft minecraft) {
        var name=SCREENSHOTS.get(next);
        if (ticks++==0) ArrayGuide.open(page(name));
        if (ticks<8) return false;
        if (minecraft.screen instanceof GuideScreen screen && screen.getCurrentPageId().equals(page(name))) {
            if (ticks==8) {
                Screenshot.grab(minecraft.gameDirectory,"agr-guide-"+name+".png",minecraft.getMainRenderTarget(),message->{});
                if (name.equals("array-layers")) {
                    screen.mouseScrolled(screen.width/2.0,screen.height/2.0,0,-5);
                    return false;
                }
            } else {
                Screenshot.grab(minecraft.gameDirectory,"agr-guide-array-layers-interior.png",minecraft.getMainRenderTarget(),message->{});
            }
        } else {
            passed=false;
        }
        ticks=0;
        return ++next==SCREENSHOTS.size();
    }

    boolean passed() { return passed; }

    private static ResourceLocation page(String name) {
        return ResourceLocation.fromNamespaceAndPath(Agristics.ID,name+".md");
    }

    private static List<Long> sceneCounts(Guide guide,String name) {
        var document=guide.getPage(page(name));
        if (document==null) return List.of();
        return document.document().getBlocks().stream().filter(LytGuidebookScene.class::isInstance)
                .map(LytGuidebookScene.class::cast)
                .map(scene -> scene.isInteractive() ? scene.getScene().getFilledBlocks().count() : -1L)
                .toList();
    }
}
