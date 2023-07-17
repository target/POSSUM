package com.target.devicemanager.components.cashdrawer.simulator;

import java.util.Vector;

import com.target.devicemanager.common.SimulatorState;
import jpos.CashDrawer;
import jpos.CashDrawerConst;
import jpos.JposConst;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.springframework.context.annotation.Profile;

@Profile("local")
public class SimulatedJposCashDrawer extends CashDrawer {
    private CashDrawerStatus cashDrawerStatus;
    private SimulatorState simulatorState;
    private int statusUpdateStatus = JposConst.JPOS_SUE_POWER_OFF_OFFLINE;

    public SimulatedJposCashDrawer() {
        simulatorState = SimulatorState.ONLINE;
        this.cashDrawerStatus = CashDrawerStatus.DRAWER_CLOSED;
    }

    void setState(SimulatorState simulatorState) {
        statusUpdateStatus = simulatorState.getStatus();
        this.simulatorState = simulatorState;

        triggerStatusUpdateEvent();
    }

    void setStatus(CashDrawerStatus cashDrawerStatus) {
        this.cashDrawerStatus = cashDrawerStatus;
        if(cashDrawerStatus == CashDrawerStatus.DRAWER_CLOSED) {
            triggerDrawerClosedEvent();
        } else {
            triggerDrawerFailedToOpenEvent();
        }
    }

    private void triggerDrawerClosedEvent() {
        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, CashDrawerConst.CASH_SUE_DRAWERCLOSED);

        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
    }

    /**
     * As Cash drawer device doesn't listen for error events. We are setting offline event when it is failed to open.
     */
    private void triggerDrawerFailedToOpenEvent() {
        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, CashDrawerConst.CASH_SUE_DRAWERCLOSED);
        this.cashDrawerStatus = CashDrawerStatus.FAILED_TO_OPEN;
        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
        statusUpdateEvent = new StatusUpdateEvent(this, JposConst.JPOS_SUE_POWER_OFFLINE);
        this.cashDrawerStatus = CashDrawerStatus.FAILED_TO_OPEN;
        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
    }

    private void triggerDrawerOpenedEvent() {
        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, CashDrawerConst.CASH_SUE_DRAWEROPEN);

        ((Vector<StatusUpdateListener>) statusUpdateListeners).forEach(listener ->
                listener.statusUpdateOccurred(statusUpdateEvent)
        );
    }

    @Override
    public boolean getDrawerOpened(){
        return (this.cashDrawerStatus != CashDrawerStatus.DRAWER_CLOSED) && (this.cashDrawerStatus != CashDrawerStatus.FAILED_TO_OPEN);
    }

    @Override
    public void openDrawer() {
        triggerDrawerOpenedEvent();
    }

    @Override
    public int getState() {
        return simulatorState == SimulatorState.ONLINE ? JposConst.JPOS_S_IDLE : JposConst.JPOS_S_CLOSED;
    }

    @Override
    public boolean getDeviceEnabled() {
        return false;
    }

    @Override
    public void setDeviceEnabled(boolean value) {
        //doNothing
    }

    private void triggerStatusUpdateEvent() {

        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, statusUpdateStatus);

        ((Vector<StatusUpdateListener>) statusUpdateListeners).forEach(listener ->
                listener.statusUpdateOccurred(statusUpdateEvent)
        );
    }
}
