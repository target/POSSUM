package com.target.devicemanager.common;

import jpos.BaseJposControl;
import jpos.JposConst;

public class SimulatedDynamicDevice<T extends BaseJposControl> extends DynamicDevice<T> {
    private final T simulatedDevice;

    public SimulatedDynamicDevice(T baseJposControl, DevicePower devicePower, DeviceConnector<T> deviceConnector) {
        super(baseJposControl, devicePower, deviceConnector);
        simulatedDevice = baseJposControl;
    }

    @Override
    public ConnectionResult connect() {
        if (isConnected()) {
            return ConnectionResult.CONNECTED;
        }
        else {
            return ConnectionResult.NOT_CONNECTED;
        }
    }

    @Override
    public boolean isConnected() {
        return simulatedDevice.getState() == JposConst.JPOS_S_IDLE;
    }

    /**
     * A simulated device does not go through the real open/claim handshake, so
     * opened/claimed track its simulated connection state instead of the
     * underlying JavaPOS control (whose getClaimed() would otherwise report not
     * claimed).
     */
    @Override
    public boolean isOpened() {
        return isConnected();
    }

    @Override
    public boolean isClaimed() {
        return isConnected();
    }
}