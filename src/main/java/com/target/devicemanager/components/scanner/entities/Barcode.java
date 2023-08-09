package com.target.devicemanager.components.scanner.entities;

public class Barcode {
    public String data;
    public BarcodeType type;
    public ScannerType source;

    public Barcode() {
    }

    public Barcode(String data, BarcodeType type, ScannerType source) {
        this.data = data;
        this.type = type;
        this.source = source;
    }

    public Barcode(String data, int type, ScannerType source) {
        this.data = data;
        this.type = BarcodeType.fromInt(type);
        this.source = source;
    }
}
