package com.target.devicemanager.components.linedisplay.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.linedisplay.entities.LineDisplayData;
import jpos.JposConst;
import jpos.LineDisplay;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

public class SimulatedJposLineDisplay extends LineDisplay  {

    private SimulatorState simulatorState;
    private int statusUpdateStatus;

    private final LineDisplayData displayLines = new LineDisplayData();

    public SimulatedJposLineDisplay() {
        statusUpdateStatus = JposConst.JPOS_SUE_POWER_OFF_OFFLINE;
        simulatorState = SimulatorState.ONLINE;
    }

    void setState(SimulatorState simulatorState) {
        statusUpdateStatus = simulatorState.getStatus();
        this.simulatorState = simulatorState;

        triggerStatusUpdateEvent();
    }

    @Override
    public int getState() {
        return simulatorState == SimulatorState.ONLINE ? JposConst.JPOS_S_IDLE : JposConst.JPOS_S_CLOSED;
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
    public int getCapPowerReporting() {
        return JPOS_PR_STANDARD;
    }

    @Override
    public int getPowerState() {
        return JPOS_PS_ONLINE;
    }

    @Override
    public void displayTextAt(int lineIndex, int columnIndex, String lineDisplayText, int textType) {
        if(lineIndex == 0) {
            displayLines.line1 = lineDisplayText;
        } else if (lineIndex == 1) {
            displayLines.line2 = lineDisplayText;
        }

        //Do nothing if the index is OOB
    }

    public LineDisplayData getDisplayText() {
        return displayLines;
    }

    @Override
    public void close(){
        //do nothing
    }

    @Override
    public void clearText() {
        displayLines.line1 = null;
        displayLines.line2 = null;
    }

    private void triggerStatusUpdateEvent() {

        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, statusUpdateStatus);

        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
    }
}
