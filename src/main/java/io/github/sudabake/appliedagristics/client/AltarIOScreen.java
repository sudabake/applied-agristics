package io.github.sudabake.appliedagristics.client;

import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.gui.*;
import appeng.client.gui.widgets.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** AE-native relative-side selector, with the neighbor actually touching each face. */
public final class AltarIOScreen<P extends AEBaseScreen<MachineMenu>> extends AESubScreen<MachineMenu, P> {
    public AltarIOScreen(P parent) {
        super(parent, "/screens/appliedagristics_machine_io.json");
        widgets.add("back", new TabButton(Icon.BACK, menu.machine.getDisplayName(), b -> returnToParent()));
        for (var side : RelativeSide.values()) {
            var name = side == RelativeSide.BACK ? "backSide" : side.name().toLowerCase(Locale.ROOT);
            widgets.add(name, new SideButton(side));
        }
    }

    @Override protected boolean shouldAddToolbar() { return false; }

    private Direction absoluteSide(RelativeSide side) {
        // The client menu can own a placeholder entity whose default facing is unrelated to the host.
        var front = Direction.byName(menu.state.getString("facing"));
        return BlockOrientation.get(front == null ? Direction.NORTH : front).getSide(side);
    }

    private final class SideButton extends IconButton {
        private final RelativeSide relative;

        SideButton(RelativeSide relative) {
            super(button -> AltarScreen.send(menu, 7, absoluteSide(relative).ordinal()));
            this.relative = relative;
        }

        private int mode() {
            var modes = menu.state.getIntArray("sides");
            return modes.length == 6 ? modes[absoluteSide(relative).ordinal()] : 0;
        }

        private ItemStack neighbor() {
            var side = absoluteSide(relative);
            var pos = menu.machine.getBlockPos().relative(side);
            if (minecraft.level == null || !minecraft.level.hasChunkAt(pos)) return ItemStack.EMPTY;
            if (minecraft.level.getBlockEntity(pos) instanceof CableBusBlockEntity cable) {
                var part = cable.getPart(side.getOpposite());
                if (part != null) return new ItemStack(part.getPartItem());
            }
            return new ItemStack(minecraft.level.getBlockState(pos).getBlock());
        }

        @Override protected Icon getIcon() {
            return (mode() & 2) != 0 ? Icon.ACCESS_READ : Icon.INVALID;
        }

        @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int yOffset = isHovered() ? 1 : 0;
            var background = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                : (mode() & 2) != 0 ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
            background.getBlitter().dest(getX()-1, getY()+yOffset, 18, 20).zOffset(2).blit(graphics);
            var stack = neighbor();
            if (!stack.isEmpty()) graphics.renderItem(stack, getX(), getY()+1+yOffset, 0, 3);
        }

        @Override public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.appliedagristics.output_side_"
                + ((mode() & 2) != 0 ? "on" : "off")));
        }
    }
}
