package com.target.devicemanager.common;

import jpos.JposConst;

public enum SimulatorState {
    OFFLINE(JposConst.JPOS_SUE_POWER_OFF_OFFLINE, JposConst.JPOS_E_OFFLINE),
    ONLINE(JposConst.JPOS_SUE_POWER_ONLINE);

    private final int status;
    private final int errorCode;

    SimulatorState(int status) {
        this(status, 0);
    }

    SimulatorState(int status, int errorCode) {
        this.status = status;
        this.errorCode = errorCode;
    }

    public int getStatus(){
        return status;
    }

    public int getErrorCode(){
        return errorCode;
    }
}
