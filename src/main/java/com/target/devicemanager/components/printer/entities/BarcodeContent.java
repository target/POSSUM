package com.target.devicemanager.components.printer.entities;

public class BarcodeContent extends PrinterContent {
    public BarcodeType barcodeType = BarcodeType.OTHER;
    public BarcodeAlignment barcodeAlign = BarcodeAlignment.CENTER;
    public BarcodeTextLocation textLocation = BarcodeTextLocation.BELOW;
    public int height;
    public int width;
}
