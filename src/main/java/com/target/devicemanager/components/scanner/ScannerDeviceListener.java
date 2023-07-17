package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.EventSynchronizer;
import jpos.JposConst;

public class ScannerDeviceListener extends DeviceListener {

    public ScannerDeviceListener(EventSynchronizer eventSynchronizer) {
        super(eventSynchronizer);
    }

    @Override
    protected boolean isFailureStatus(int status) {
        switch (status) {
            case JposConst.JPOS_PS_UNKNOWN:
            case JposConst.JPOS_PS_ONLINE:
                return false;
            default:
                return true;
        }
    }
}
