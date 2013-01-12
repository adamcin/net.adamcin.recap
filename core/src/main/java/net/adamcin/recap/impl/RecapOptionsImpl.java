package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapOptions;

/**
 * @author madamcin
 * @version $Id: RecapOptionsImpl.java$
 */
public class RecapOptionsImpl implements RecapOptions {

    private String lastModifiedProperty;
    private int batchSize = 0;
    private long throttle = 0L;
    private boolean onlyNewer;
    private boolean update;

    public String getLastModifiedProperty() {
        return lastModifiedProperty;
    }

    public void setLastModifiedProperty(String lastModifiedProperty) {
        this.lastModifiedProperty = lastModifiedProperty;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getThrottle() {
        return throttle;
    }

    public void setThrottle(long throttle) {
        this.throttle = throttle;
    }

    public boolean isOnlyNewer() {
        return onlyNewer;
    }

    public void setOnlyNewer(boolean onlyNewer) {
        this.onlyNewer = onlyNewer;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }
}

