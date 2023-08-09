package com.target.devicemanager.components.scanner.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.target.devicemanager.common.EnumDeserializer;

import java.util.Arrays;

public enum ScannerType {
    HANDHELD("HANDHELD"),
    FLATBED("FLATBED"),
    BOTH("BOTH");

    private final String scannerTypeString;

    ScannerType(String scannerTypeString) {
        this.scannerTypeString = scannerTypeString;
    }

    @JsonCreator
    public static ScannerType fromValue(String valueToDeserialize) {
        ScannerType type = EnumDeserializer.deserialize(Arrays.asList(values()), valueToDeserialize);
        return type;
    }

    public String toString() {
        return scannerTypeString;
    }
}
