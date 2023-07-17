package com.target.devicemanager.components.scale.entities;

import jpos.JposException;

import java.util.EventObject;

public class WeightErrorEvent extends EventObject {

    private static final long serialVersionUID = 1L;
    private final JposException error;

    public WeightErrorEvent(Object source, JposException error) {
        super(source);
        
        this.error = error;
    }

    public JposException getError() {
        return this.error;
    }
}
