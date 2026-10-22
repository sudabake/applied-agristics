package io.github.sudabake.appliedagristics.multiblock;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.block.NetworkPlanter;
import java.util.Set;

/** Both the finite early input port and the later bulk seed bay can supply a planter. */
public interface SeedBank {
    Set<AEItemKey> keys();
    long count(AEItemKey key);
    long used(AEItemKey key);
    long claim(AEItemKey key, NetworkPlanter owner, long requested);
    void unclaim(AEItemKey key, NetworkPlanter owner);
    long add(AEItemKey key, long requested);
    boolean belongsTo(NetworkPlanter owner);
}
