package io.github.sudabake.appliedagristics.menu;

import io.github.sudabake.appliedagristics.block.*;
import java.util.ArrayList;
import java.util.List;

/** Slot coordinates and order; shared transport and interaction remain in MachineMenu. */
record MachineMenuLayout(List<MachineSlot> slots, int inventoryX, int inventoryY) {
  record MachineSlot(int index, int x, int y, boolean output) {}

  static MachineMenuLayout forMachine(MachineEntity machine) {
    var slots = new ArrayList<MachineSlot>();
    int inventoryX = 8;
    int inventoryY;
    if (machine instanceof CellCultivator) {
      slots.add(new MachineSlot(0, 44, 29, false));
      slots.add(new MachineSlot(1, 68, 29, false));
      slots.add(new MachineSlot(2, 92, 29, false));
      slots.add(new MachineSlot(3, 116, 48, true));
      slots.add(new MachineSlot(4, 116, 70, true));
      slots.add(new MachineSlot(5, 140, 70, true));
      inventoryY = 112;
    } else if (machine instanceof ItemPort) {
      for (int i = 0; i < 16; i++)
        slots.add(new MachineSlot(i, 52 + i % 4 * 18, 24 + i / 4 * 18, false));
      inventoryY = 116;
    } else if (machine instanceof MEInfusionAltar) {
      slots.add(new MachineSlot(0, 48, 40, false));
      slots.add(new MachineSlot(1, 116, 40, true));
      for (int i = 2; i < 6; i++) slots.add(new MachineSlot(i, 179, 5 + (i - 2) * 18, false));
      inventoryY = 100;
    } else if (machine instanceof EarlyPlanter) {
      slots.add(new MachineSlot(0, 26, 40, false));
      slots.add(new MachineSlot(1, 62, 40, false));
      for (int i = 2; i < 11; i++)
        slots.add(new MachineSlot(i, 108 + (i - 2) % 3 * 18, 34 + (i - 2) / 3 * 18, true));
      inventoryY = 136;
    } else if (machine instanceof EssenceVault) {
      inventoryY = 116;
    } else {
      inventoryX = machine instanceof AltarControls ? 8 : 79;
      inventoryY = 160;
      if (machine instanceof SeedStorage) slots.add(new MachineSlot(0, 24, 29, false));
      // ME planter upgrades are structure modules, not inventory slots.
    }
    return new MachineMenuLayout(List.copyOf(slots), inventoryX, inventoryY);
  }
}
