package org.biterp.admin1c.cmd;

import com._1c.v8.ibis.admin.IInfoBaseInfoShort;

import java.util.*;

public final class IBResult {
    private final Map<IInfoBaseInfoShort, UUID> allBases;
    private final Map<IInfoBaseInfoShort, UUID> basesForUnlocks;
    private final Map<IInfoBaseInfoShort, UUID> basesForLocks;

    public IBResult(Map<IInfoBaseInfoShort, UUID> allBases, Map<IInfoBaseInfoShort, UUID> basesForUnlocks, Map<IInfoBaseInfoShort, UUID> basesForLocks) {
        this.allBases = allBases;
        this.basesForUnlocks = basesForUnlocks;
        this.basesForLocks = basesForLocks;
    }

    public synchronized Map<IInfoBaseInfoShort, UUID> getAllBases() {
        return allBases;
    }

    public synchronized Map<IInfoBaseInfoShort, UUID> getBasesForUnlocks() {
        return basesForUnlocks;
    }

    public synchronized Map<IInfoBaseInfoShort, UUID> getBasesForLocks() {
        return basesForLocks;
    }
}
