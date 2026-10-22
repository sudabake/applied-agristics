package io.github.sudabake.appliedagristics.client;

import appeng.api.config.*;
import appeng.client.gui.*;
import appeng.client.gui.widgets.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Compact AE sub-screen: six faces around the machine, auto-export inside this page. */
public final class AltarIOScreen<P extends AEBaseScreen<MachineMenu>> extends AESubScreen<MachineMenu, P> {
    private final SettingToggleButton<YesNo> autoOutput;
    private final ToggleButton autoInput;
    public AltarIOScreen(P parent) {
        super(parent, parent.getMenu().machine instanceof io.github.sudabake.appliedagristics.block.ItemPort ? "/screens/appliedagristics_port_io.json" : "/screens/appliedagristics_machine_io.json");
        widgets.add("back", new TabButton(Icon.BACK, menu.machine.getDisplayName(), b -> returnToParent()));
        for (int i = 0; i < 6; i++) {
            final int side = i;
            widgets.add("side" + i, new IconButton(b -> AltarScreen.send(menu, 7, side)) {
                private int mode() { var modes = menu.state.getIntArray("sides"); return modes.length == 6 ? modes[side] : 0; }
                @Override protected Icon getIcon() { return switch (mode()) { case 1 -> Icon.ACCESS_WRITE; case 2 -> Icon.ACCESS_READ; case 3 -> Icon.ACCESS_READ_WRITE; default -> Icon.INVALID; }; }
                @Override public List<Component> getTooltipMessage() { return List.of(Component.translatable("gui.appliedagristics.side_" + side).append(": ").append(Component.translatable("gui.appliedagristics.io_" + mode()))); }
            });
        }
        autoOutput = new SettingToggleButton<>(Settings.AUTO_EXPORT, YesNo.NO, (button, backwards) -> AltarScreen.send(menu, 8, 0));
        widgets.add("autoOutput", autoOutput);
        if (menu.machine instanceof io.github.sudabake.appliedagristics.block.ItemPort) {
            autoInput=new ToggleButton(Icon.ACCESS_WRITE,Icon.INVALID,b -> AltarScreen.send(menu,10,0));
            autoInput.setTooltipOn(List.of(Component.translatable("gui.appliedagristics.auto_input_on")));
            autoInput.setTooltipOff(List.of(Component.translatable("gui.appliedagristics.auto_input_off")));
            widgets.add("autoInput",autoInput);
        } else autoInput=null;
    }
    @Override protected boolean shouldAddToolbar() { return false; }
    @Override protected void updateBeforeRender() {
        super.updateBeforeRender(); autoOutput.set(menu.state.getBoolean("autoOutput") ? YesNo.YES : YesNo.NO);
        if (autoInput!=null) autoInput.setState(menu.state.getBoolean("autoInput"));
    }
}
