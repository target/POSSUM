package com.target.devicemanager.common.entities;

public class DeviceHealthResponse {

    private final String deviceName;
    private DeviceHealth health;

    public DeviceHealthResponse(String deviceName, DeviceHealth health) {
        this.deviceName = deviceName == null ? "" : deviceName;
        this.health = health == null ? DeviceHealth.NOTREADY : health;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public DeviceHealth getHealthStatus() {
        return health;
    }

    public void updateHealthStatus(DeviceHealth health) {
        this.health = health;
    }

    public String toString() {
        return "DeviceHealthResponse{" +
                "deviceName='" + deviceName + '\'' +
                ", health=" + health +
                '}';
    }
}

