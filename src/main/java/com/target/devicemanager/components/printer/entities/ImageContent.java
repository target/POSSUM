package com.target.devicemanager.components.printer.entities;

public class ImageContent extends PrinterContent {
    public ImageFormatType imageFormatType = ImageFormatType.BMP;

    public void setImageFormatType(ImageFormatType imageFormatType) {
        this.imageFormatType = imageFormatType == null ? ImageFormatType.BMP : imageFormatType;
    }
}
