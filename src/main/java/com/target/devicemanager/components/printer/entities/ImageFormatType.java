package com.target.devicemanager.components.printer.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.target.devicemanager.common.EnumDeserializer;
import jpos.POSPrinterConst;

import java.util.Arrays;

public enum ImageFormatType {
    BMP(POSPrinterConst.PTR_BMT_BMP),
    JPEG(POSPrinterConst.PTR_BMT_JPEG),
    GIF(POSPrinterConst.PTR_BMT_JPEG);

    private final int value;

    ImageFormatType(int value) {
        this.value = value;
    }

    @JsonCreator
    public static ImageFormatType fromValue(String valueToDeserialize) {
        ImageFormatType type = EnumDeserializer.deserialize(Arrays.asList(values()), valueToDeserialize);

        return type == null ? BMP : type;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name();
    }
}
