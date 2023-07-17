package com.target.devicemanager.common;

import jpos.BaseJposControl;
import jpos.JposConst;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicDevice<DEVICE extends BaseJposControl> {
    private final DEVICE device;
    private final DeviceConnector<DEVICE> deviceConnector;
    private final DevicePower devicePower;
    private int connectCount = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDevice.class);

    public enum ConnectionResult {
        CONNECTED,
        NOT_CONNECTED,
        ALREADY_CONNECTED
    }

    public DynamicDevice(DEVICE device, DevicePower devicePower, DeviceConnector<DEVICE> deviceConnector) {
        if (device == null) {
            throw new IllegalArgumentException("device cannot be null");
        }
        if (devicePower == null) {
            throw new IllegalArgumentException("devicePower cannot be null");
        }
        if (deviceConnector == null) {
            throw new IllegalArgumentException("deviceConnector cannot be null");
        }

        this.device = device;
        this.devicePower = devicePower;
        this.deviceConnector = deviceConnector;
    }

    public ConnectionResult connect() {
        connectCount++;
        synchronized (device) {
            if (isConnected()) {
                connectCount = 0;
                return ConnectionResult.ALREADY_CONNECTED;
            }
            boolean deviceFound = deviceConnector.discoverConnectedDevice();
            if (!deviceFound) {
                LOGGER.trace(getDeviceName() + " Connect Failed: " + connectCount);
                return ConnectionResult.NOT_CONNECTED;
            }

            devicePower.enablePowerNotification(device);
        }
        LOGGER.info(getDeviceName() + " Connect Succeeded: " + connectCount);
        connectCount = 0;
        return ConnectionResult.CONNECTED;
    }

    public void disconnect() {
        synchronized (device) {
            try {
                device.release();
                LOGGER.debug(getDeviceName() + " Released");
            } catch (JposException jposException) {
                LOGGER.debug(getDeviceName() + " Release failed " + jposException.getMessage());
            }
            try {
                device.close();
                LOGGER.debug(getDeviceName() + " Closed");
            } catch (JposException jposException) {
                LOGGER.error(getDeviceName() + " Close failed : " + jposException.getMessage());
            }
        }
    }

    public boolean isConnected() {
        synchronized (device) {
            int deviceState = device.getState();
            if (deviceState != JposConst.JPOS_S_IDLE && deviceState != JposConst.JPOS_S_BUSY) {
                return false;
            }
            try {
                if (!device.getClaimed()) {
                    return false;
                }
                int powerState = devicePower.getPowerState(device);
                if (powerState != JposConst.JPOS_PS_ONLINE && powerState != JposConst.JPOS_PS_UNKNOWN) {
                    return false;
                }
            } catch (JposException jposException) {
                return false;
            }
        }
        return true;
    }

    public DEVICE getDevice() {
        return device;
    }

    public String getDeviceName() {
        return deviceConnector.getConnectedDeviceName();
    }
}
