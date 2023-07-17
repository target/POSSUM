package com.target.devicemanager.components.check;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.common.events.ConnectionEventListener;
import com.target.devicemanager.components.check.entities.MicrData;
import com.target.devicemanager.components.check.entities.MicrDataEvent;
import com.target.devicemanager.components.check.entities.MicrErrorEvent;
import com.target.devicemanager.components.check.entities.MicrException;
import jpos.JposConst;
import jpos.JposException;
import jpos.MICR;
import jpos.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class MicrDevice implements StatusUpdateListener, ErrorListener, DataListener {

    private final DynamicDevice<? extends MICR> dynamicMicr;
    private final List<ConnectionEventListener> connectionEventListeners;
    private final List<MicrEventListener> micrEventListeners;
    private boolean isCheckCancelReceived;
    private static final int RETURN_IMMEDIATE = 0;
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrDevice.class);
    private static final Marker MARKER = MarkerFactory.getMarker("FATAL");

    /**
     * Initializes the MicrDevice.
     * @param dynamicMICR The dynamic device is Micr.
     * @param connectionEventListeners is an instance of an event.
     * @param micrEventListeners Listens for the micr events.
     */
    public MicrDevice(DynamicDevice<? extends MICR> dynamicMICR, List<ConnectionEventListener> connectionEventListeners, List<MicrEventListener> micrEventListeners) {
        this(dynamicMICR, connectionEventListeners, micrEventListeners, new ReentrantLock(true));
    }

    public MicrDevice(DynamicDevice<? extends MICR> dynamicMICR, List<ConnectionEventListener> connectionEventListeners, List<MicrEventListener> micrEventListeners, ReentrantLock connectLock) {
        if (dynamicMICR == null) {
            LOGGER.error(MARKER, "Check Reader Failed in Constructor: dynamicMICR cannot be null");
            throw new IllegalArgumentException("dynamicMICR cannot be null");
        }
        if (connectionEventListeners == null) {
            LOGGER.error(MARKER, "Check Reader Failed in Constructor: connectionEventListeners cannot be null");
            throw new IllegalArgumentException("connectionEventListeners cannot be null");
        }
        if (micrEventListeners == null) {
            LOGGER.error(MARKER, "Check Reader Failed in Constructor: micrEventListeners cannot be null");
            throw new IllegalArgumentException("micrEventListeners cannot be null");
        }
        this.dynamicMicr = dynamicMICR;
        this.connectionEventListeners = connectionEventListeners;
        this.micrEventListeners = micrEventListeners;
        MICR micr = dynamicMICR.getDevice();
        micr.addStatusUpdateListener(this);
        micr.addDataListener(this);
        micr.addErrorListener(this);
        this.connectLock = connectLock;
    }

    /**
     * adding a new instance to List of micrEventListeners.
     * @param micrEventListener an instance of a micr event.
     */
    void addMicrEventListener(MicrEventListener micrEventListener) {
        this.micrEventListeners.add(micrEventListener);
    }

    /**
     *linking manager to device and adding a new instance to List of connectionEventListeners.
     * @param connectionEventListener is an instance of an event.
     */
    void addConnectionEventListener(ConnectionEventListener connectionEventListener) {
        this.connectionEventListeners.add(connectionEventListener);
    }

    /**
     *Sends event after connection.
     * @param connectStatus Stating event happened.
     */
    private void fireConnectionEvent(boolean connectStatus) {
        this.connectionEventListeners.forEach(listener -> listener.connectionEventOccurred(new ConnectionEvent(this, connectStatus)));
    }

    /**
     * Sends Micr Data event.
     * @param micrData check data.
     */
    private void fireMicrDataEvent(MicrData micrData) {
        this.micrEventListeners.forEach(listener -> listener
                .micrDataEventOccurred(new MicrDataEvent(this, micrData)));
    }

    /**
     * Sends micr error event with jpos exception.
     * @param micrError stating an error event happened.
     */
    private void fireMicrErrorEvent(JposException micrError) {
        this.micrEventListeners.forEach(listener -> listener
                .micrErrorEventOccurred(new MicrErrorEvent(this, micrError)));
    }

    /**
     * Makes sure a connection occurs.
     * @return
     */
    public boolean connect() {
        MICR micr = dynamicMicr.getDevice();
        try {
            DynamicDevice.ConnectionResult connectionResult = dynamicMicr.connect();
            if (connectionResult == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
                return false;
            }
            if (!micr.getDataEventEnabled()) {
                micr.setDataEventEnabled(true);
            }
            if (!micr.getDeviceEnabled()) {
                micr.setDeviceEnabled(true);
            }
            //Only fire the connection even when first connected
            if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                fireConnectionEvent(true);
            }
        } catch (JposException jposException) {
            return false;
        }
        return true;
    }

    /**
     * Disconnects Micr and starts event.
     */
    public void disconnect() {
        if (dynamicMicr.isConnected()) {
            MICR micr = dynamicMicr.getDevice();
            try {
                if (micr.getDataEventEnabled()) {
                    micr.setDataEventEnabled(false);
                }
                if (micr.getDeviceEnabled()) {
                    micr.setDeviceEnabled(false);
                }
                dynamicMicr.disconnect();
                fireConnectionEvent(false);
            } catch (JposException jposException) {
                LOGGER.error("Check Reader Failed to Disconnect Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
            }
        }
    }

    /**
     * Shows if device is connected.
     * @return Device is connected.
     */
    public boolean isConnected() {
        return dynamicMicr.isConnected();
    }

    /**
     * begins check insertion process.
     */
    void insertCheck() throws MicrException {
        LOGGER.debug("waiting for check to be inserted...");
        /*
        We are waiting for the check to be inserted. There are only 3 ways to get out of this black hole
        1. CHECK is inserted
        2. CLIENT calls a cancel
        3. Device error occurs and a JPOS exception (other than timeout) is raised
        */
        MICR micr = dynamicMicr.getDevice();
        synchronized (micr) {
            while (!isCheckCancelReceived()) {

                try {
                    micr.beginInsertion(250);
                    micr.endInsertion();
                    return;

                } catch (JposException jposException) {
                    if (jposException.getErrorCode() != JposConst.JPOS_E_TIMEOUT) {
                        LOGGER.error(MARKER, "Check Reader Failed to Insert Check: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
                        this.micrEventListeners.forEach(listener -> listener
                                .micrErrorEventOccurred(new MicrErrorEvent(this, jposException)));
                        setCheckCancelReceived(true);
                        throw new MicrException(jposException);
                    }
                }
            }
        }
        //If we are here, CLIENT called a cancel.
        fireMicrErrorEvent(new JposException(JposConst.JPOS_E_TIMEOUT));
    }

    /**
     * Begins check removal process.
     * @throws JposException
     */
    public void withdrawCheck() throws JposException {
        LOGGER.debug("withdraw check");
        try {
            MICR micr;
            synchronized (micr = dynamicMicr.getDevice()) {
                micr.beginRemoval(RETURN_IMMEDIATE);
                micr.endRemoval();
            }
        } catch (JposException jposException) {
            LOGGER.error(MARKER, "Check Reader Failed to Remove Check: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
            throw jposException;
        }
    }

    /**
     * Checks to see if check cancellation was received.
     * @return
     */
    public boolean isCheckCancelReceived() {
        return isCheckCancelReceived;
    }

    /**
     * a request to cancel the check is received.
     * @param checkCancelReceived check cancel request.
     */
    public void setCheckCancelReceived(boolean checkCancelReceived) {
        isCheckCancelReceived = checkCancelReceived;
    }

    /**
     * Gets the StatusUpdate of the MCR whether its Power online/offline.
     * @param statusUpdateEvent
     */
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        LOGGER.debug("statusUpdateOccurred(): " + statusUpdateEvent.getStatus());
        int status = statusUpdateEvent.getStatus();
        switch (status) {
            case JposConst.JPOS_SUE_POWER_OFF:
            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
            case JposConst.JPOS_SUE_POWER_OFFLINE:
                LOGGER.error(MARKER, "Check Reader Status Update: Power offline");
                disconnect();
                break;
            case JposConst.JPOS_SUE_POWER_ONLINE:
                connect();
                break;
            default:
                break;
        }
    }

    /**
     * Creates error responses for when error occurs.
     * @param errorEvent event error.
     */
    public void errorOccurred(ErrorEvent errorEvent) {
        JposException jposException = new JposException(errorEvent.getErrorCode(), errorEvent.getErrorCodeExtended());
        LOGGER.error(MARKER, "Check Reader Received an Error: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
        LOGGER.debug("errorOccurred(): errCode=" + errorEvent.getErrorCode()
                + " errCodeExt=" + errorEvent.getErrorCodeExtended()
                + " errLocus=" + errorEvent.getErrorLocus()
                + " errResponse=" + errorEvent.getErrorResponse());
        fireMicrErrorEvent(jposException);
    }

    /**
     * Makes sure data is received from check properly.
     * @param dataEvent
     */
    public void dataOccurred(DataEvent dataEvent) {
        LOGGER.debug("dataOccurred(): " + dataEvent.getStatus());
        try {
            MICR micr = (MICR) dataEvent.getSource();
            MicrData micrData = new MicrData(
                    micr.getAccountNumber(),
                    micr.getBankNumber(),
                    micr.getTransitNumber(),
                    micr.getRawData(),
                    micr.getSerialNumber());
            fireMicrDataEvent(micrData);
            micr.clearInput();

        } catch (JposException jposException) {
            LOGGER.error(MARKER, "Check Reader Received an Error in Data: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
            fireMicrErrorEvent(jposException);
        }

    }

    /**
     * Lock the current resource.
     * @return
     */
    public boolean tryLock() {
        try {
            isLocked = connectLock.tryLock(10, TimeUnit.SECONDS);
            LOGGER.trace("Lock: " + isLocked);
        } catch(InterruptedException interruptedException) {
            LOGGER.error("Lock Failed: " + interruptedException.getMessage());
        }
        return isLocked;
    }

    /**
     * unlock the current resource.
     */
    public void unlock() {
        connectLock.unlock();
        isLocked = false;
    }

    /**
     * This method is only used to get "isLocked" for unit testing
     * @return
     */
    public boolean getIsLocked() {
        return isLocked;
    }

    /**
     * Gets device name.
     * @return returns device name.
     */
    public String getDeviceName() {
        return dynamicMicr.getDeviceName();
    }
}
