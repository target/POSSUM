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
    private static final StructuredEventLogger log = StructuredEventLogger.of("common", "DeviceListener", LOGGER);

    public DeviceListener(EventSynchronizer eventSynchronizer) {
        if (eventSynchronizer == null) {
            throw new IllegalArgumentException("eventSynchronizer cannot be null");
        }
        this.eventSynchronizer = eventSynchronizer;
    }

    @Override
    public void dataOccurred(DataEvent dataEvent) {
        log.success("dataOccurred(): " + dataEvent.getStatus(), 1);
        eventSynchronizer.triggerEvent(dataEvent);
    }

    @Override
    public void errorOccurred(ErrorEvent errorEvent) {
        log.failure("errorOccurred(): errCode=" + errorEvent.getErrorCode()
                + " errCodeExt=" + errorEvent.getErrorCodeExtended()
                + " errLocus=" + errorEvent.getErrorLocus()
                + " errResponse=" + errorEvent.getErrorResponse(), 17, null);
        int errorCode = errorEvent.getErrorCode();
        if (errorCode == JposConst.JPOS_E_OFFLINE || errorCode == JposConst.JPOS_E_NOHARDWARE) {
            BaseService jposService = (BaseService) errorEvent.getSource();
            try {
                jposService.close();
            } catch (JposException jposException) {
                log.failure("close failed", 17, jposException);
            }
        }
        eventSynchronizer.triggerEvent(errorEvent);
    }

    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        log.success("statusUpdateOccurred(): " + statusUpdateEvent.getStatus(), 1);
        if (isFailureStatus(statusUpdateEvent.getStatus())) {
            //Don't trigger the event for things we expect, like online statuses
            eventSynchronizer.triggerEvent(statusUpdateEvent);
        }
    }

    @Override
    public void outputCompleteOccurred(OutputCompleteEvent outputCompleteEvent) {
        log.success("outputCompleteOccurred(): " + outputCompleteEvent.getOutputID(), 1);
        eventSynchronizer.triggerEvent(outputCompleteEvent);
    }

    public void startEventListeners() {
        eventSynchronizer.startEventSynchronizer();
    }

    //Convenience methods to hide the type of event coming back, kinda ugly but makes it easier to handle device specializations
    // currently only used by scanner
    public DataEvent waitForData() throws JposException {
        log.success("waitForData(in)", 1);
        JposEvent jposEvent = eventSynchronizer.waitForEvent();
        if (jposEvent instanceof ErrorEvent) {
            throw jposExceptionFromErrorEvent((ErrorEvent) jposEvent);
        }
        if (jposEvent instanceof StatusUpdateEvent) {
            throw jposExceptionFromStatusUpdateEvent((StatusUpdateEvent) jposEvent);
        }
        if (!(jposEvent instanceof DataEvent)) {
            throw new JposException(JposConst.JPOS_E_FAILURE);
        }
        log.success("waitForData(out)", 1);
        return (DataEvent) jposEvent;
    }

    // currently only used by scanner
    public void stopWaitingForData() {
        log.success("stopWaitingForData(in)", 1);
        eventSynchronizer.stopWaitingForEvent();
        log.success("stopWaitingForData(out)", 1);
    }

    // currently this method is only used printer
    public void waitForOutputToComplete() throws JposException {
        log.success("waitForOutputToComplete(in)", 1);
        JposEvent jposEvent = eventSynchronizer.waitForEvent();
        if (jposEvent instanceof ErrorEvent) {
            throw jposExceptionFromErrorEvent((ErrorEvent) jposEvent);
        }
        if (jposEvent instanceof StatusUpdateEvent) {
            throw jposExceptionFromStatusUpdateEvent((StatusUpdateEvent) jposEvent);
        }
        if (!(jposEvent instanceof OutputCompleteEvent)) {
            throw new JposException(JposConst.JPOS_E_FAILURE);
        }

        log.success("waitForOutputToComplete(out)", 1);
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
