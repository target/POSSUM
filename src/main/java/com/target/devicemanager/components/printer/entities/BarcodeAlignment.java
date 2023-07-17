package com.target.devicemanager.components.printer.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.target.devicemanager.common.EnumDeserializer;
import jpos.POSPrinterConst;

import java.util.Arrays;

public enum BarcodeAlignment {
    LEFT(POSPrinterConst.PTR_BC_LEFT),
    CENTER(POSPrinterConst.PTR_BC_CENTER),
    RIGHT(POSPrinterConst.PTR_BC_RIGHT);

    private final int alignment;

    BarcodeAlignment(int alignment) {
        this.alignment = alignment;
    }

    @JsonCreator
    public static BarcodeAlignment fromValue(String valueToDeserialize) {
        BarcodeAlignment alignment = EnumDeserializer.deserialize(Arrays.asList(values()), valueToDeserialize);

        return alignment == null ? CENTER : alignment;
    }

    public int getValue() {
        return alignment;
    }

    @Override
    public String toString(){
        return name();
    }
}
