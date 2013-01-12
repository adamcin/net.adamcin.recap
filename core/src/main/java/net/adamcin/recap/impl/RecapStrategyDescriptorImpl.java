package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapStrategyDescriptor;

/**
 * @author madamcin
 * @version $Id: RecapStrategyDescriptorImpl.java$
 */
public class RecapStrategyDescriptorImpl implements RecapStrategyDescriptor {
    private String type;
    private String label;
    private String description;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
