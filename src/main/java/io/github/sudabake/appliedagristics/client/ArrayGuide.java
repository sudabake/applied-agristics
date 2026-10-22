package io.github.sudabake.appliedagristics.client;

import guideme.Guide;
import guideme.PageAnchor;
import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.resources.ResourceLocation;

/** GuideME owns indexing, GUI hover hotkeys and interactive documentation scenes. */
public final class ArrayGuide {
    public static final ResourceLocation PAGE=ResourceLocation.fromNamespaceAndPath(Agristics.ID,"index.md");
    public static final Guide GUIDE=Guide.builder(ResourceLocation.fromNamespaceAndPath(Agristics.ID,"guide"))
        .folder("guide").defaultLanguage("zh_cn").startPage(PAGE).build();
    public static void init() { }
    public static void open() {
        var mc=net.minecraft.client.Minecraft.getInstance();
        var previous=mc.screen;
        var screen=guideme.internal.screen.GuideScreen.openNew(GUIDE,PageAnchor.page(PAGE));
        screen.setReturnToOnClose(previous);
        mc.setScreen(screen);
    }
    private ArrayGuide() {}
}
