package com.target.devicemanager.components.printer.entities;

public enum ContentType {

    TEXT(1),
    BARCODE(2),
    IMAGE(3);

    private final int type;

    ContentType(int type) {
        this.type = type;
    }

    public int getValue() {
        return type;
    }

    @Override
    public String toString() {
        return name();
    }
}

