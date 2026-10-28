package io.github.sudabake.appliedagristics.client;

import appeng.client.gui.NumberEntryType;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.widgets.TabButton;
import appeng.client.gui.Icon;
import io.github.sudabake.appliedagristics.menu.MachineMenu;

/** AE's own number entry and priority page layout, hosted by the vault menu. */
public final class VaultPriorityScreen extends AESubScreen<MachineMenu, EssenceVaultScreen> {
    public VaultPriorityScreen(EssenceVaultScreen parent) {
        super(parent,"/screens/appliedagristics_vault_priority.json");
        widgets.add("back",new TabButton(Icon.BACK,menu.machine.getDisplayName(),b->returnToParent()));
        var priority=widgets.addNumberEntryWidget("priority",NumberEntryType.UNITLESS);
        priority.setTextFieldStyle(style.getWidget("priorityInput"));
        priority.setMinValue(Integer.MIN_VALUE);
        priority.setMaxValue(Integer.MAX_VALUE);
        priority.setLongValue(menu.state.getInt("priority"));
        priority.setOnChange(()->priority.getIntValue().ifPresent(value->AltarScreen.send(menu,13,value)));
        priority.setOnConfirm(()->{ priority.getIntValue().ifPresent(value->AltarScreen.send(menu,13,value)); returnToParent(); });
    }
    @Override protected boolean shouldAddToolbar() { return false; }
}
