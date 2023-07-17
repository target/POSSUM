package com.target.devicemanager.components.check.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.check.entities.MicrData;
import jpos.JposConst;
import jpos.JposException;
import jpos.MICR;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

public class SimulatedJposMicr extends MICR {

    private SimulatorState simulatorState;
    private int statusUpdateStatus;
    private boolean isCheckInserted;
    private MicrData micrData;

    public SimulatedJposMicr() {
        simulatorState = SimulatorState.ONLINE;
        isCheckInserted = false;
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
    public boolean getClaimed() {
        return true;
    }

    @Override
    public int getState() {
        return simulatorState == SimulatorState.ONLINE ? JposConst.JPOS_S_IDLE : JposConst.JPOS_S_CLOSED;
    }

    @Override
    public void close() {
        //do nothing
    }

    @Override
    public boolean getDataEventEnabled() {
        return true;
    }

    @Override
    public String getAccountNumber() {
        if(micrData == null) {
            return "";
        }

        return micrData.account_number;
    }

    @Override
    public String getBankNumber() {
        if(micrData == null) {
            return "";
        }

        return micrData.bank_number;
    }

    @Override
    public String getTransitNumber() {
        if(micrData == null) {
            return "";
        }

        return micrData.transit_number;
    }

    @Override
    public String getRawData() {
        if(micrData == null) {
            return "";
        }

        return micrData.raw_data;
    }

    @Override
    public String getSerialNumber() {
        if(micrData == null) {
            return "";
        }

        return micrData.sequence_number;
    }

    @Override
    public int getCapPowerReporting() {
        return JPOS_PR_STANDARD;
    }

    @Override
    public int getPowerState() {
        return JPOS_PS_ONLINE;
    }

    @Override
    public void beginInsertion(int timeoutMsec) throws JposException {
        if(simulatorState != SimulatorState.ONLINE){
            throw new JposException(simulatorState.getErrorCode());
        }

        if(!isCheckInserted) {
            try {
                Thread.sleep(timeoutMsec);
            } catch(Exception exception) {
                //This is fine
            }

            throw new JposException(JPOS_E_TIMEOUT);
        }

        isCheckInserted = false;
    }

    @Override
    public void endInsertion() {
        //Do nothing
    }
    public boolean isCheckInserted() {
        return isCheckInserted;
    }

    void setState(SimulatorState simulatorState) {
        this.simulatorState = simulatorState;

        statusUpdateStatus = simulatorState.getStatus();

        triggerStatusUpdateEvent();
    }

    private void triggerStatusUpdateEvent() {
        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, statusUpdateStatus);

        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
    }

    private void triggerDataEvent() {
        DataEvent dataEvent = new DataEvent(this, simulatorState.getStatus());
        for (Object object : dataListeners) {
            DataListener dataListener = (DataListener) object;
            dataListener.dataOccurred(dataEvent);
        }
    }

    public void setCheckInserted(MicrData micrData) {
        this.isCheckInserted = true;
        this.micrData = micrData;

        triggerDataEvent();
    }

    public MicrData getMicrData() {
        return micrData;
    }
}
