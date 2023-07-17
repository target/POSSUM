package com.target.devicemanager.components.printer.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.target.devicemanager.common.EnumDeserializer;
import jpos.POSPrinterConst;

import java.util.Arrays;

public enum BarcodeTextLocation {
    NONE(POSPrinterConst.PTR_BC_TEXT_NONE),
    ABOVE(POSPrinterConst.PTR_BC_TEXT_ABOVE),
    BELOW(POSPrinterConst.PTR_BC_TEXT_BELOW);

    private final int textLocation;

    BarcodeTextLocation(int location) {
        this.textLocation = location;
    }

    @JsonCreator
    public static BarcodeTextLocation fromValue(String valueToDeserialize) {
        BarcodeTextLocation location = EnumDeserializer.deserialize(Arrays.asList(values()), valueToDeserialize);

        return location == null ? BELOW : location;
    }

    public int getValue() {
        return textLocation;
    }

    @Override
    public String toString() {
        return name();
    }
}
