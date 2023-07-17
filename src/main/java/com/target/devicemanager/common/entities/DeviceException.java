package com.target.devicemanager.common.entities;

import jpos.JposConst;
import jpos.JposException;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeviceException extends Exception {

    private static final long serialVersionUID = 1L;

    protected DeviceError deviceError;
    protected Exception causedBy;

    protected final Map<Integer, DeviceError> errorCodeMap = Stream.of(
            new SimpleEntry<>(JposConst.JPOS_E_BUSY, DeviceError.DEVICE_BUSY),
            new SimpleEntry<>(JposConst.JPOS_PS_OFF, DeviceError.DEVICE_OFFLINE),
            new SimpleEntry<>(JposConst.JPOS_E_CLOSED, DeviceError.DEVICE_OFFLINE),
            new SimpleEntry<>(JposConst.JPOS_E_OFFLINE, DeviceError.DEVICE_OFFLINE),
            new SimpleEntry<>(JposConst.JPOS_E_FAILURE, DeviceError.UNEXPECTED_ERROR),
            new SimpleEntry<>(JposConst.JPOS_PS_OFFLINE, DeviceError.DEVICE_OFFLINE),
            new SimpleEntry<>(JposConst.JPOS_E_NOTCLAIMED, DeviceError.DEVICE_OFFLINE),
            new SimpleEntry<>(JposConst.JPOS_E_NOHARDWARE, DeviceError.DEVICE_OFFLINE),
            new SimpleEntry<>(JposConst.JPOS_PS_OFF_OFFLINE, DeviceError.DEVICE_OFFLINE),
            new SimpleEntry<>(JposConst.JPOS_E_DISABLED, DeviceError.DEVICE_OFFLINE))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    protected DeviceException() {

    }

    public DeviceException(DeviceError deviceError) {
        this.deviceError = deviceError;
    }

    public DeviceException(JposException originalException) {
        causedBy = originalException;
        this.deviceError = errorCodeMap.getOrDefault(originalException.getErrorCode(),
                errorCodeMap.getOrDefault(originalException.getErrorCodeExtended(), DeviceError.UNEXPECTED_ERROR));
    }

    public DeviceError getDeviceError() {
        return deviceError;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String toString() {
        return "DeviceException{" +
                "deviceError=" + deviceError +
                ", causedBy=" + causedBy +
                '}';
    }
}