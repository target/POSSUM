package com.target.devicemanager.components.check.entities;

import java.util.EventObject;

public class MicrDataEvent extends EventObject {

    private static final long serialVersionUID = 1L;
    private final MicrData micrData;

    public MicrDataEvent(Object source, MicrData micrData) {
        super(source);

        this.micrData = micrData;
    }

    public MicrData getMicrData() {
        return this.micrData;
    }
}
