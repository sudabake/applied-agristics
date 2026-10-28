package io.github.sudabake.appliedagristics.client;

import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import io.github.sudabake.appliedagristics.network.Messages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final List<Button> checks = new ArrayList<>();
    private final List<EditBox> targets = new ArrayList<>();
    private final List<Button> applies = new ArrayList<>();
    private boolean draggingScroll;
    private Button cropFilter;
    private String rowSignature = "";
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = menu.machine instanceof EarlyPlanter ? 176 : 320;
        imageHeight = menu.machine instanceof EarlyPlanter ? 218 : 240;
        inventoryLabelX = menu.machine instanceof EarlyPlanter ? 8 : 79;
        inventoryLabelY = menu.machine instanceof EarlyPlanter ? 126 : 150;
    }
    private void send(int action, String id, long value) {
        var seed = action == 3 && row((int) value) != null ? ItemStack.parseOptional(minecraft.level.registryAccess(), row((int) value).getCompound("seed")) : ItemStack.EMPTY;
        PacketDistributor.sendToServer(new Messages.Control(menu.machine.getBlockPos(), action, id, value, seed));
    }
    @Override protected void init() {
        super.init(); checks.clear(); targets.clear(); applies.clear(); rowSignature = "";
        if (!menu.machine.networkMachine()) return;
        if (menu.machine instanceof NetworkPlanter) {
            cropFilter=addRenderableWidget(Button.builder(Component.empty(),b -> send(12,"",0)).bounds(leftPos+174,topPos+5,104,18).build(AeButton::new));
            addRenderableWidget(Button.builder(Component.literal("IO"),b -> minecraft.setScreen(new NetworkIOScreen(this))).bounds(leftPos+282,topPos+5,29,18).build(AeButton::new));
            addRenderableWidget(Button.builder(Component.translatable("gui.appliedagristics.enable_all"), b -> send(5, "", 0)).bounds(leftPos + 240, topPos + 26, 34, 18).build(AeButton::new));
            addRenderableWidget(Button.builder(Component.translatable("gui.appliedagristics.disable_all"), b -> send(6, "", 0)).bounds(leftPos + 277, topPos + 26, 34, 18).build(AeButton::new));
        }
        if (menu.machine instanceof SeedStorage) addRenderableWidget(Button.builder(Component.translatable("gui.appliedagristics.collect"), b -> send(0, "", 0)).bounds(leftPos + 48, topPos + 26, 48, 18).build(AeButton::new));
        for (int i = 0; i < (menu.machine instanceof SeedStorage ? 12 : 4); i++) {
            final int index = i; int y = topPos + rowY(i);
            checks.add(addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                var row = row(index); if (row == null) return;
                if (menu.machine instanceof SeedStorage) send(3, row.getString("id"), index);
                else send(row.getBoolean("enabled") ? 2 : 1, row.getString("id"), target(index, row));
            }).bounds(leftPos + (menu.machine instanceof SeedStorage ? 86+i%3*100 : 8), y, menu.machine instanceof SeedStorage ? 16 : 20, 18).build(AeButton::new)));
            if (menu.machine instanceof NetworkPlanter) {
                var target = addRenderableWidget(new EditBox(font, leftPos + 196, y, 64, 18, Component.translatable("gui.appliedagristics.target")));
                target.setMaxLength(19); target.setFilter(s -> s.matches("[0-9]*|-1|-")); targets.add(target);
                applies.add(addRenderableWidget(Button.builder(Component.literal("✓"), b -> { var row = row(index); if (row != null) send(row.getBoolean("enabled") ? 1 : 2, row.getString("id"), target(index, row)); }).bounds(leftPos + 263, y, 20, 18).build(AeButton::new)));
            }
        }
    }
    private int rowY(int index) { return 60+(menu.machine instanceof SeedStorage ? index/3 : index)*22; }
    private CompoundTag row(int index) { var rows = menu.state.getList("rows", Tag.TAG_COMPOUND); return index < rows.size() ? rows.getCompound(index) : null; }
    private long target(int index, CompoundTag row) { try { return Long.parseLong(targets.get(index).getValue()); } catch (NumberFormatException e) { return row.getLong("target"); } }
    @Override protected void containerTick() {
        super.containerTick();
        if (menu.machine.networkMachine()) menu.page = Math.clamp(menu.page, 0, Math.max(0, menu.state.getInt("pages") - 1));
        if(cropFilter!=null) cropFilter.setMessage(Component.translatable(menu.state.getBoolean("hideOrdinary")?"gui.appliedagristics.show_ordinary":"gui.appliedagristics.hide_ordinary"));
        var signature = new StringBuilder(); for (int i = 0; i < checks.size(); i++) { var row = row(i); if (row != null) signature.append(row.getString("id")).append(';'); }
        boolean changed = !signature.toString().equals(rowSignature); rowSignature = signature.toString();
        for (int i = 0; i < checks.size(); i++) {
            var row = row(i); checks.get(i).visible = row != null;
            if (row != null) checks.get(i).setMessage(Component.literal(menu.machine instanceof SeedStorage ? "↓" : row.getBoolean("enabled") ? "✓" : "□"));
            if (i < targets.size()) {
                var box = targets.get(i); box.visible = row != null; applies.get(i).visible = row != null;
                if (row != null && (changed || !box.isFocused())) box.setValue(Long.toString(row.getLong("target")));
            }
        }
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x - 1, y = topPos + slot.y - 1;
            if (menu.machine.networkMachine()) { appeng.client.gui.Icon.SLOT_BACKGROUND.getBlitter().dest(x,y).blit(g); continue; }
            boolean basic = true;
            g.fill(x, y, x + 18, y + 18, basic ? 0xFF373737 : 0xFF88899E);
            g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFF1F1F5);
            g.fill(x + 1, y + 1, x + 17, y + 17, basic ? 0xFF8B8B8B : 0xFFA8A9BE);
        }
        if (menu.machine instanceof EarlyPlanter) {
            drawWaterTank(g,leftPos+7,topPos+26,16,60);
            label(g, "seed", 26, 26); label(g, "farmland", 62, 26); label(g, "output", 108, 20);
            g.drawString(font, Component.translatable("gui.appliedagristics.water", menu.state.getInt("water")), leftPos + 12, topPos + 96, 0x404054, false);
            progress(g, 12, 112, 152);
        } else {
            if (menu.machine instanceof NetworkPlanter) g.drawString(font, font.plainSubstrByWidth("×" + compact(menu.state.getDouble("speed")) + "  " + compact(menu.state.getDouble("power")) + " AE/t", 220), leftPos + 16, topPos + 31, 0x404054, false);
            label(g, menu.machine instanceof NetworkPlanter ? "seed_parallel" : "seed_count", 54, 48);
            if (menu.machine instanceof NetworkPlanter) { label(g, "stock", 145, 48); label(g, "target", 196, 48); label(g, "state", 288, 48); }
            for (int i = 0; i < checks.size(); i++) {
                var row = row(i); if (row == null) continue; int y = topPos + rowY(i);
                boolean storage=menu.machine instanceof SeedStorage;
                int x=leftPos+(storage ? 8+i%3*100 : 32);
                g.fill(x, y - 1, storage?x+96:leftPos+310, y + 21, 0xFFBFC0CE);
                var seed = ItemStack.parseOptional(minecraft.level.registryAccess(), row.getCompound("seed")); g.renderItem(seed, x+2, y);
                var crop = com.blakebr0.mysticalagriculture.registry.CropRegistry.getInstance().getCropById(net.minecraft.resources.ResourceLocation.tryParse(row.getString("id")));
                String name = io.github.sudabake.appliedagristics.biology.BioData.seed(seed) ? net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(io.github.sudabake.appliedagristics.biology.BioData.identity(seed)).getDescription().getString() : crop == null ? seed.getHoverName().getString() : crop.getDisplayName().getString();
                boolean hover=mouseX>=x && mouseX<x+96 && mouseY>=y && mouseY<y+22;
                rowText(g,name,x+22,y+1,storage?52:70,0x404054,storage&&hover);
                rowText(g,row.getLong("used") + "/" + row.getLong("count"),x+22,y+11,storage?52:70,0x66667A,hover);
                if (menu.machine instanceof NetworkPlanter) {
                    var output = ItemStack.parseOptional(minecraft.level.registryAccess(), row.getCompound("output")); g.renderItem(output, leftPos + 126, y);
                    g.drawString(font, font.plainSubstrByWidth(Long.toString(row.getLong("stock")), 48), leftPos + 145, y + 4, 0x404054, false);
                    g.drawString(font, font.plainSubstrByWidth(Component.translatable("status.appliedagristics." + row.getString("status")).getString(), 28), leftPos + 288, y + 4, 0x404054, false);
                }
            }
            int positions = Math.max(1, menu.state.getInt("pages"));
            if (positions > 1) {
                g.fill(leftPos + 313, topPos + 60, leftPos + 316, topPos + 146, 0xFF88899E);
                int thumbY = topPos + 60 + (int)(70L * menu.page / (positions - 1));
                g.fill(leftPos + 313, thumbY, leftPos + 316, thumbY + 16, 0xFF7476AC);
            }
        }
    }
    private void rowText(GuiGraphics g,String value,int x,int y,int width,int color,boolean scroll) {
        if(scroll && font.width(value)>width) RowText.draw(g,font,Component.literal(value),x,y,x+width,y+9,color);
        else g.drawString(font,font.plainSubstrByWidth(value,width),x,y,color,false);
    }
    /** Reuse Minecraft's own clipped marquee text. */
    private abstract static class RowText extends AbstractWidget {
        private RowText() { super(0,0,0,0,Component.empty()); }
        static void draw(GuiGraphics g,net.minecraft.client.gui.Font font,Component text,int x,int y,int right,int bottom,int color) {
            renderScrollingString(g,font,text,x,y,right,bottom,color);
        }
    }
    private void drawWaterTank(GuiGraphics graphics,int x,int y,int width,int height) {
        WaterGauge.draw(graphics,x,y,width,height,menu.state.getInt("water"));
    }
    private static final class AeButton extends appeng.client.gui.widgets.AE2Button {
        private AeButton(Button.Builder builder) { this(builder.build()); }
        private AeButton(Button button) { super(button.getX(), button.getY(), button.getWidth(), button.getHeight(), button.getMessage(), b -> button.onPress()); }
    }
    private void panel(GuiGraphics g, int x, int y, int width, int height) {
        if (menu.machine instanceof EarlyPlanter) {
            g.fill(x, y, x + width, y + height, 0xFF181818);
            g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFF0F0F0);
            g.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xFFC6C6C6);
            return;
        }
        g.fill(x, y, x + width, y + height, 0xFF3D3E52);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFDCDDE5);
        g.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFF8D8EA5);
        g.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xFFCDCED8);
    }
    private void label(GuiGraphics g, String key, int x, int y) { g.drawString(font, Component.translatable("gui.appliedagristics." + key), leftPos + x, topPos + y, 0x404054, false); }
    private void progress(GuiGraphics g, int x, int y, int width) {
        g.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 8, menu.machine instanceof EarlyPlanter ? 0xFF8B8B8B : 0xFF88899E);
        int filled = (int) Math.min(width, (long) menu.state.getInt("progress") * width / Math.max(1, menu.state.getInt("duration")));
        g.fill(leftPos + x, topPos + y, leftPos + x + filled, topPos + y + 8, menu.machine instanceof EarlyPlanter ? 0xFF699548 : 0xFF7476AC);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        var status = Component.translatable("status.appliedagristics." + menu.state.getString("status"));
        int x = menu.machine instanceof EarlyPlanter ? 30 : 8;
        int y = menu.machine instanceof EarlyPlanter ? 78 : 146;
        int width = menu.machine instanceof EarlyPlanter ? 90 : 65;
        g.drawString(font, font.plainSubstrByWidth(status.getString(), width), x, y, 0x404054, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); renderTooltip(g, mouseX, mouseY);
        if (menu.machine instanceof NetworkPlanter && mouseY >= topPos + 26 && mouseY <= topPos + 46) {
            if (mouseX >= leftPos + 16 && mouseX < leftPos + 236) g.renderComponentTooltip(font, List.of(
                Component.translatable("gui.appliedagristics.module_summary", menu.state.getInt("speedModules"), menu.state.getInt("parallelModules"), menu.state.getBoolean("conversion") ? "✓" : "×"),
                Component.translatable("gui.appliedagristics.network_speed", String.format(Locale.ROOT, "%.3f", menu.state.getDouble("speed"))),
                Component.translatable("gui.appliedagristics.network_power", compact(menu.state.getDouble("power")))), mouseX, mouseY);
        }
        if (menu.machine instanceof EarlyPlanter && mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 94 && mouseY < topPos + 122) {
            var tips = new ArrayList<Component>();
            tips.add(Component.translatable("gui.appliedagristics.harvest_rules"));
            tips.add(Component.translatable("gui.appliedagristics.cycle_help", menu.state.getInt("baseTicks") / 20, menu.state.getInt("waterTicks") / 20));
            if (!menu.state.getString("crux").isEmpty()) tips.add(Component.translatable("gui.appliedagristics.crux_help", menu.state.getString("crux")));
            if (menu.state.getBoolean("mystical")) {
                tips.add(Component.translatable("gui.appliedagristics.seed_chance", String.format(Locale.ROOT, "%.1f", menu.state.getDouble("seedChance"))));
                tips.add(Component.translatable("gui.appliedagristics.fertilizer_chance", String.format(Locale.ROOT, "%.1f", menu.state.getDouble("fertilizerChance"))));
            }
            g.renderComponentTooltip(font, tips, mouseX, mouseY);
        }
        for (int i = 0; i < checks.size(); i++) {
            var row = row(i); if (row == null) continue; int y = topPos + rowY(i);
            if (mouseY >= y && mouseY < y + 20) {
                if(menu.machine instanceof SeedStorage) {
                    int x=leftPos+8+i%3*100;
                    if(mouseX>=x && mouseX<x+76) g.renderComponentTooltip(font,List.of(
                        ItemStack.parseOptional(minecraft.level.registryAccess(),row.getCompound("seed")).getHoverName(),
                        Component.literal(row.getLong("used")+" / "+row.getLong("count"))),mouseX,mouseY);
                    continue;
                }
                if (menu.machine instanceof NetworkPlanter && mouseX >= leftPos + 196 && mouseX < leftPos + 260) g.renderTooltip(font, Component.translatable("gui.appliedagristics.target_help"), mouseX, mouseY);
                if (mouseX >= leftPos + 34 && mouseX < leftPos + 50) g.renderTooltip(font, ItemStack.parseOptional(minecraft.level.registryAccess(), row.getCompound("seed")), mouseX, mouseY);
                else if (menu.machine instanceof NetworkPlanter && mouseX >= leftPos + 145 && mouseX < leftPos + 192) g.renderTooltip(font, Component.literal(Long.toString(row.getLong("stock"))), mouseX, mouseY);
                else if (menu.machine instanceof NetworkPlanter && mouseX >= leftPos + 288 && mouseX < leftPos + 318) g.renderTooltip(font, Component.translatable("status.appliedagristics." + row.getString("status")), mouseX, mouseY);
                else if (menu.machine instanceof NetworkPlanter && mouseX >= leftPos + 54 && mouseX < leftPos + 124) g.renderComponentTooltip(font, List.of(
                    Component.literal(row.getLong("used") + " / " + row.getLong("count")),
                    Component.translatable("gui.appliedagristics.network_power", compact(row.getDouble("power"))),
                    Component.translatable("gui.appliedagristics.required_power", compact(row.getDouble("requiredPower"))),
                    Component.translatable("gui.appliedagristics.next_speed_power", compact(row.getDouble("nextSpeedPower"))),
                    Component.translatable("gui.appliedagristics.next_parallel_power", compact(row.getDouble("nextParallelPower")))), mouseX, mouseY);
            }
        }
    }
    private void scrollAt(double y) {
        int last = Math.max(0, menu.state.getInt("pages") - 1);
        int next = Math.clamp((int)Math.round((y - topPos - 68) * last / 70.0), 0, last);
        if (next != menu.page) { menu.page = next; send(4, "", next); }
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && menu.machine.networkMachine() && x >= leftPos + 311 && x <= leftPos + 318 && y >= topPos + 60 && y <= topPos + 146) {
            draggingScroll = true; scrollAt(y); return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (draggingScroll && button == 0) { scrollAt(y); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (draggingScroll && button == 0) { draggingScroll = false; return true; }
        return super.mouseReleased(x, y, button);
    }
    private static String compact(double value) {
        if(value>0 && value<.01) return String.format(Locale.ROOT,"%.3g",value);
        if (value >= 1e9) return String.format(Locale.ROOT, "%.2fG", value / 1e9);
        if (value >= 1e6) return String.format(Locale.ROOT, "%.2fM", value / 1e6);
        if (value >= 1e3) return String.format(Locale.ROOT, "%.1fk", value / 1e3);
        return value == Math.rint(value) ? Long.toString((long)value) : String.format(Locale.ROOT, "%.2f", value);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (menu.machine.networkMachine() && x >= leftPos && x <= leftPos + imageWidth && y >= topPos + 48 && y <= topPos + 148) {
            int next = Math.clamp(menu.page - (int)Math.signum(vertical), 0, Math.max(0, menu.state.getInt("pages") - 1));
            if (next != menu.page) { menu.page = next; send(4, menu.search, next); } return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key != 256 && getFocused() instanceof EditBox box) {
            if (key == 257 || key == 335) {
                int index = targets.indexOf(box); var row = row(index); if (row != null) send(row.getBoolean("enabled") ? 1 : 2, row.getString("id"), target(index, row));
                return true;
            }
            box.keyPressed(key, scanCode, modifiers); return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
