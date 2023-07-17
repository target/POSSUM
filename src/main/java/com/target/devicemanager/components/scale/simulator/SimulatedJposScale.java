package com.target.devicemanager.components.scale.simulator;

import jpos.JposConst;
import jpos.JposException;
import jpos.Scale;
import jpos.ScaleConst;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

import java.math.BigDecimal;

public class SimulatedJposScale extends Scale {
    private ScaleSimulatorState simulatorState;
    private int statusUpdateStatus;
    private int liveWeight;
    private int stableWeight;

    public SimulatedJposScale() {
        simulatorState = ScaleSimulatorState.ONLINE;
    }

    @Override
    public void setDeviceEnabled(boolean value) {
        //doNothing
    }

    @Override
    public boolean getDeviceEnabled() {
        return true;
    }

    @Override
    public int getState() {
        return simulatorState == ScaleSimulatorState.ONLINE ? JposConst.JPOS_S_IDLE : JposConst.JPOS_S_CLOSED;
    }

    @Override
    public void close() {
        //do nothing
    }

    @Override
    public int getStatusNotify() {
        return ScaleConst.SCAL_SN_ENABLED;
    }

    @Override
    public void clearInput() {
        liveWeight = 0;
    }

    @Override
    public boolean getDataEventEnabled() {
        return true;
    }

    @Override
    public int getScaleLiveWeight() {
        return liveWeight;
    }

    @Override
    public void readWeight(int[] weight, int timeout) throws JposException {
        if(simulatorState != ScaleSimulatorState.ONLINE){
            throw new JposException(simulatorState.getErrorCode());
        }

        if (stableWeight != 0) {
            weight[0] = stableWeight;
            stableWeight = 0;
            return;
        }

        try {
            Thread.sleep(timeout);
        } catch (InterruptedException interruptedException) {
            throw new JposException(JposConst.JPOS_E_FAILURE);
        }

        throw new JposException(JposConst.JPOS_E_TIMEOUT);
    }

    void setState(ScaleSimulatorState scaleSimulatorState) {
        this.simulatorState = scaleSimulatorState;

        statusUpdateStatus = scaleSimulatorState.getStatus();

        triggerStatusUpdateEvent(statusUpdateStatus);
    }

    private void triggerStatusUpdateEvent(int status) {
        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, status);
        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
    }

    void setWeight(BigDecimal weight) {
        statusUpdateStatus = ScaleConst.SCAL_SUE_STABLE_WEIGHT;
        simulatorState = ScaleSimulatorState.ONLINE;

        int newWeight = (weight.multiply(new BigDecimal(1000))).intValue();
        liveWeight = newWeight;
        stableWeight = newWeight;

        triggerStatusUpdateEvent(statusUpdateStatus);
    }
}
