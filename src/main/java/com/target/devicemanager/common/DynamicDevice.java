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
    private static final StructuredEventLogger log = StructuredEventLogger.of(StructuredEventLogger.getCommonServiceName(), "DynamicDevice", LOGGER);


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
                log.failure(getDeviceName() + " Connect Failed: " + connectCount, 1 , null);
                return ConnectionResult.NOT_CONNECTED;
            }

            devicePower.enablePowerNotification(device);
        }
        log.success(getDeviceName() + " Connect Succeeded: " + connectCount, 9);
        connectCount = 0;
        return ConnectionResult.CONNECTED;
    }

    public void disconnect() {
        synchronized (device) {
            try {
                device.release();
                log.success(getDeviceName() + " Released", 5);
            } catch (JposException jposException) {
                log.failure(getDeviceName() + " Release failed " + jposException.getMessage(), 5, jposException);
            }
            try {
                device.close();
                log.success(getDeviceName() + " Closed", 5);
            } catch (JposException jposException) {
                log.failure(getDeviceName() + " Close failed : " + jposException.getMessage(), 17, jposException);
            }
        }
    }

    /**
     * Whether the underlying device has been opened (its JavaPOS state is no
     * longer CLOSED). A claimed device is always opened, but an opened device
     * is not necessarily claimed.
     */
    public boolean isOpened() {
        synchronized (device) {
            int deviceState = device.getState();
            return deviceState == JposConst.JPOS_S_IDLE || deviceState == JposConst.JPOS_S_BUSY;
        }
    }

    /**
     * Whether this process has claimed (taken exclusive ownership of) the
     * device. Returns false if the device is not opened or the claim cannot be
     * read.
     */
    public boolean isClaimed() {
        synchronized (device) {
            try {
                return device.getClaimed();
            } catch (JposException jposException) {
                return false;
            }
        }
    }

    public boolean isConnected() {
        synchronized (device) {
            if (!isOpened() || !isClaimed()) {
                return false;
            }
            int powerState = devicePower.getPowerState(device);
            if (powerState != JposConst.JPOS_PS_ONLINE && powerState != JposConst.JPOS_PS_UNKNOWN) {
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
