/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapOptions;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;

/**
 * @author madamcin
 * @version $Id: RecapOptionsImpl.java$
 */
public class RecapOptionsImpl implements RecapOptions {

    private String lastModifiedProperty;
    private Integer batchSize;
    private Long throttle;
    private BatchReadConfig batchReadConfig;
    private boolean onlyNewer;
    private boolean update;
    private boolean reverse;
    private boolean noRecurse;

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

    public BatchReadConfig getBatchReadConfig() {
        return batchReadConfig;
    }

    public void setBatchReadConfig(BatchReadConfig batchReadConfig) {
        this.batchReadConfig = batchReadConfig;
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

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public boolean isNoRecurse() {
        return noRecurse;
    }

    public void setNoRecurse(boolean noRecurse) {
        this.noRecurse = noRecurse;
    }

    @Override
    public String toString() {
        return "RecapOptionsImpl{" +
                "lastModifiedProperty='" + lastModifiedProperty + '\'' +
                ", batchSize=" + batchSize +
                ", throttle=" + throttle +
                ", batchReadConfig=" + batchReadConfig +
                ", onlyNewer=" + onlyNewer +
                ", update=" + update +
                ", reverse=" + reverse +
                ", noRecurse=" + noRecurse +
                '}';
    }
}

