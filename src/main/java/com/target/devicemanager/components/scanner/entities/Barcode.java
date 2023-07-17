package com.target.devicemanager.components.scanner.entities;

public class Barcode {
    public String data;
    public BarcodeType type;

    public Barcode() {
    }

    public Barcode(String data, BarcodeType type) {
        this.data = data;
        this.type = type;
    }

    public Barcode(String data, int type) {
        this.data = data;
        this.type = BarcodeType.fromInt(type);
    }
}
