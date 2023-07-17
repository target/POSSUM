package com.target.devicemanager.common.entities;

public class DeviceErrorStatus {
    public String deviceName;
    public Boolean faultPresent;
    public DeviceError deviceError;

    public DeviceErrorStatus(String deviceName, Boolean faultPresent, DeviceError deviceError) {
        this.deviceName = deviceName;
        this.faultPresent = faultPresent;
        this.deviceError = deviceError;
    }
}
