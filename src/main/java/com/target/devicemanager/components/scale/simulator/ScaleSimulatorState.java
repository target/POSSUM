package com.target.devicemanager.components.scale.simulator;

import jpos.JposConst;
import jpos.ScaleConst;

public enum ScaleSimulatorState {
    OFFLINE(JposConst.JPOS_SUE_POWER_OFF_OFFLINE, JposConst.JPOS_PS_OFF_OFFLINE),
    ONLINE(JposConst.JPOS_SUE_POWER_ONLINE),
    UNSTABLE(ScaleConst.SCAL_SUE_WEIGHT_UNSTABLE, JposConst.JPOS_E_FAILURE),
    OVERWEIGHT(ScaleConst.SCAL_SUE_WEIGHT_OVERWEIGHT, ScaleConst.JPOS_ESCAL_OVERWEIGHT),
    UNDER_ZERO(ScaleConst.SCAL_SUE_WEIGHT_UNDER_ZERO, ScaleConst.JPOS_ESCAL_UNDER_ZERO),
    NEEDS_ZEROING(JposConst.JPOS_E_FAILURE, JposConst.JPOS_E_BUSY);

    private final int status;
    private final int errorCode;

    ScaleSimulatorState(int status) {
        this(status, 0);
    }

    ScaleSimulatorState(int status, int errorCode) {
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
