package com.target.devicemanager.common.entities;

public enum DeviceHealth {
    READY(0),
    NOTREADY(1);

    private final int healthStatus;

    DeviceHealth(int healthStatus) { this.healthStatus = healthStatus; }

    public int getValue() {
        return healthStatus;
    }
}