package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.EventSynchronizer;
import jpos.CashDrawerConst;
import jpos.JposConst;
import org.springframework.context.annotation.Profile;

@Profile({"local", "dev", "prod"})
public class CashDrawerDeviceListener extends DeviceListener {

    public CashDrawerDeviceListener(EventSynchronizer eventSynchronizer) {
        super(eventSynchronizer);
    }

    @Override
    protected boolean isFailureStatus(int status) {
        switch (status) {
            case CashDrawerConst.CASH_SUE_DRAWEROPEN:
            case JposConst.JPOS_PS_ONLINE:
                return false;
            default:
                return true;
        }
    }
}
