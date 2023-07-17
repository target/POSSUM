package com.target.devicemanager.components.scanner.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.scanner.entities.Barcode;
import com.target.devicemanager.components.scanner.entities.BarcodeType;
import jpos.JposConst;
import jpos.Scanner;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

import java.nio.charset.Charset;

public class SimulatedJposScanner extends Scanner  {
    private Barcode barcode;
    private SimulatorState simulatorState;

    public SimulatedJposScanner() {
        barcode = new Barcode("POST desired data to scanner simulator", BarcodeType.UNKNOWN);
        simulatorState = SimulatorState.ONLINE;
    }

    void setBarcode(Barcode barcode) {
        this.barcode = barcode;
        triggerDataEvent();
    }

    void setState(SimulatorState simulatorState) {
        this.simulatorState = simulatorState;
        triggerStatusUpdateEvent();
    }

    private void triggerDataEvent() {
        DataEvent dataEvent = new DataEvent(this, JposConst.JPOS_SUCCESS);

        for (Object object : dataListeners) {
            DataListener dataListener = (DataListener) object;
            dataListener.dataOccurred(dataEvent);
        }
    }

    private void triggerStatusUpdateEvent() {
        int status = JposConst.JPOS_SUE_POWER_OFF_OFFLINE;

        if (simulatorState == SimulatorState.ONLINE) {
            status = JposConst.JPOS_SUE_POWER_ONLINE;
        }

        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, status);

        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
    }

    @Override
    public byte[] getScanDataLabel() {
        return barcode.data.getBytes(Charset.defaultCharset());
    }

    @Override
    public int getScanDataType() {
        return barcode.type.getValue();
    }

    @Override
    public int getState() {
        return simulatorState == SimulatorState.ONLINE ? JposConst.JPOS_S_IDLE : JposConst.JPOS_S_CLOSED;
    }

    @Override
    public void setAutoDisable(boolean value) {
        //doNothing
    }

    @Override
    public void setDecodeData(boolean value) {
        //doNothing
    }

    @Override
    public void setDataEventEnabled(boolean value) {
        //doNothing
    }

    @Override
    public void setDeviceEnabled(boolean value) {
        //doNothing
    }

    @Override
    public void close(){
        //do nothing
    }
}
