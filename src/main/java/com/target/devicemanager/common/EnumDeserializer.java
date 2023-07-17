package com.target.devicemanager.common;

import java.util.List;

public final class EnumDeserializer {
    private EnumDeserializer() {
    }

    public static <T extends Enum> T deserialize(List<T> possibleValues, String valueToDeserialize) {
        for (T value : possibleValues) {
            if (value.name().equalsIgnoreCase(valueToDeserialize)) {
                return value;
            }
        }
        return null;
    }
}
