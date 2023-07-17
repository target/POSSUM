package com.target.devicemanager.components.scale.entities;

import java.util.EventObject;

public class WeightEvent extends EventObject{

    private static final long serialVersionUID = 1L;
    private final FormattedWeight weight;

    public WeightEvent(Object source, FormattedWeight weight) {
        super(source);

        this.weight = weight;
    }

    public FormattedWeight getWeight() {
        return this.weight;
    }
}

