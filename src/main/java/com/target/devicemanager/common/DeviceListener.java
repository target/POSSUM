package com.target.devicemanager.common;

import jpos.JposConst;
import jpos.JposException;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.JposEvent;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import jpos.services.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceListener implements DataListener, ErrorListener, StatusUpdateListener, OutputCompleteListener {

    private final EventSynchronizer eventSynchronizer;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceListener.class);

    public DeviceListener(EventSynchronizer eventSynchronizer) {
        if (eventSynchronizer == null) {
            throw new IllegalArgumentException("eventSynchronizer cannot be null");
        }
        this.eventSynchronizer = eventSynchronizer;
    }

    @Override
    public void dataOccurred(DataEvent dataEvent) {
        LOGGER.trace("dataOccurred(): " + dataEvent.getStatus());
        eventSynchronizer.triggerEvent(dataEvent);
    }

    @Override
    public void errorOccurred(ErrorEvent errorEvent) {
        LOGGER.error("errorOccurred(): errCode=" + errorEvent.getErrorCode()
                + " errCodeExt=" + errorEvent.getErrorCodeExtended()
                + " errLocus=" + errorEvent.getErrorLocus()
                + " errResponse=" + errorEvent.getErrorResponse());
        int errorCode = errorEvent.getErrorCode();
        if (errorCode == JposConst.JPOS_E_OFFLINE || errorCode == JposConst.JPOS_E_NOHARDWARE) {
            BaseService jposService = (BaseService) errorEvent.getSource();
            try {
                jposService.close();
            } catch (JposException jposException) {
                LOGGER.error("close failed", jposException);
            }
        }
        eventSynchronizer.triggerEvent(errorEvent);
    }

    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        LOGGER.trace("statusUpdateOccurred(): " + statusUpdateEvent.getStatus());
        if (isFailureStatus(statusUpdateEvent.getStatus())) {
            //Don't trigger the event for things we expect, like online statuses
            eventSynchronizer.triggerEvent(statusUpdateEvent);
        }
    }

    @Override
    public void outputCompleteOccurred(OutputCompleteEvent outputCompleteEvent) {
        LOGGER.trace("outputCompleteOccurred(): " + outputCompleteEvent.getOutputID());
        eventSynchronizer.triggerEvent(outputCompleteEvent);
    }

    public void startEventListeners() {
        eventSynchronizer.startEventSynchronizer();
    }

    //Convenience methods to hide the type of event coming back, kinda ugly but makes it easier to handle device specializations
    // currently only used by scanner
    public DataEvent waitForData() throws JposException {
        LOGGER.trace("waitForData(in)");
        JposEvent jposEvent = eventSynchronizer.waitForEvent();
        if (jposEvent instanceof ErrorEvent) {
            LOGGER.trace("waitForData(out)");
            throw jposExceptionFromErrorEvent((ErrorEvent) jposEvent);
        }
        if (jposEvent instanceof StatusUpdateEvent) {
            LOGGER.trace("waitForData(out)");
            throw jposExceptionFromStatusUpdateEvent((StatusUpdateEvent) jposEvent);
        }
        if (!(jposEvent instanceof DataEvent)) {
            LOGGER.trace("waitForData(out)");
            throw new JposException(JposConst.JPOS_E_FAILURE);
        }
        LOGGER.trace("waitForData(out)");
        return (DataEvent) jposEvent;
    }

    // currently only used by scanner
    public void stopWaitingForData() {
        LOGGER.trace("stopWaitingForData(in)");
        eventSynchronizer.stopWaitingForEvent();
        LOGGER.trace("stopWaitingForData(out)");
    }

    // currently this method is only used printer
    public void waitForOutputToComplete() throws JposException {
        LOGGER.trace("waitForOutputToComplete(in)");
        JposEvent jposEvent = eventSynchronizer.waitForEvent();
        if (jposEvent instanceof ErrorEvent) {
            LOGGER.trace("waitForOutputToComplete(out)");
            throw jposExceptionFromErrorEvent((ErrorEvent) jposEvent);
        }
        if (jposEvent instanceof StatusUpdateEvent) {
            LOGGER.trace("waitForOutputToComplete(out)");
            throw jposExceptionFromStatusUpdateEvent((StatusUpdateEvent) jposEvent);
        }
        if (!(jposEvent instanceof OutputCompleteEvent)) {
            LOGGER.trace("waitForOutputToComplete(out)");
            throw new JposException(JposConst.JPOS_E_FAILURE);
        }
    }

    public StatusUpdateEvent waitForStatusUpdate() throws JposException {
        JposEvent jposEvent = eventSynchronizer.waitForEvent();
        if (jposEvent instanceof ErrorEvent) {
            throw jposExceptionFromErrorEvent((ErrorEvent) jposEvent);
        }
        if (!(jposEvent instanceof StatusUpdateEvent)) {
            throw new JposException(JposConst.JPOS_E_FAILURE);
        }
        return (StatusUpdateEvent) jposEvent;
    }

    //This allows us to override status behavior for extended devices like the printer
    protected boolean isFailureStatus(int status) {
        return status != JposConst.JPOS_SUE_POWER_ONLINE;
    }

    private JposException jposExceptionFromErrorEvent(ErrorEvent errorEvent) {
        return new JposException(errorEvent.getErrorCode(), errorEvent.getErrorCodeExtended());
    }

    private JposException jposExceptionFromStatusUpdateEvent(StatusUpdateEvent statusUpdateEvent) {
        return new JposException(JposConst.JPOS_E_EXTENDED, statusUpdateEvent.getStatus());
    }
}
