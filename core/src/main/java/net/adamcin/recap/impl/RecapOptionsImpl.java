package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapOptions;

/**
 * @author madamcin
 * @version $Id: RecapOptionsImpl.java$
 */
public class RecapOptionsImpl implements RecapOptions {

    private String lastModifiedProperty;
    private Integer batchSize;
    private Long throttle;
    private boolean onlyNewer;
    private boolean update;

    public String getLastModifiedProperty() {
        return lastModifiedProperty;
    }

    public void setLastModifiedProperty(String lastModifiedProperty) {
        this.lastModifiedProperty = lastModifiedProperty;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Long getThrottle() {
        return throttle;
    }

    public void setThrottle(Long throttle) {
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

