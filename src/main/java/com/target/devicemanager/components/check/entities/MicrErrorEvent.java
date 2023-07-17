package com.target.devicemanager.components.check.entities;

import jpos.JposException;

import java.util.EventObject;

public class MicrErrorEvent extends EventObject {
    private static final long serialVersionUID = 1L;
    private final JposException error;

    public MicrErrorEvent(Object source, JposException error) {
        super(source);

        this.error = error;
    }

    public JposException getError() {
        return this.error;
    }
}
