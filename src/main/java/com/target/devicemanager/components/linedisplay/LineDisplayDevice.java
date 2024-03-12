package com.target.devicemanager.components.linedisplay;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.common.events.ConnectionEventListener;
import jpos.JposConst;
import jpos.JposException;
import jpos.LineDisplay;
import jpos.LineDisplayConst;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Profile({"local","dev","prod"})
public class LineDisplayDevice implements StatusUpdateListener {
    private final DynamicDevice<LineDisplay> dynamicLineDisplay;
    private final List<ConnectionEventListener> connectionEventListeners;
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(LineDisplayDevice.class);
    private static final Marker MARKER = MarkerFactory.getMarker("FATAL");

    /**
     * Initializes LineDisplayDevice and gets it ready for use.
     * @param dynamicLineDisplay The dynamic device is the line display.
     */
    public LineDisplayDevice(DynamicDevice<LineDisplay> dynamicLineDisplay) {
        this(dynamicLineDisplay, new CopyOnWriteArrayList<>(), new ReentrantLock(true));
    }

    /**
     * This Constructor is Mainly for unit tests
     * Initializes LineDisplayDevice and gets it ready for use.
     * @param dynamicLineDisplay The dynamic device is the line display.
     */
    public LineDisplayDevice(DynamicDevice<LineDisplay> dynamicLineDisplay, List<ConnectionEventListener> connectionEventListenerList, ReentrantLock connectLock) {
        if (dynamicLineDisplay == null) {
            LOGGER.error(MARKER, "Line Display Failed in Constructor: dynamicLineDisplay cannot be null");
            throw new IllegalArgumentException("dynamicLineDisplay cannot be null");
        }
        this.dynamicLineDisplay = dynamicLineDisplay;
        this.connectionEventListeners = connectionEventListenerList;
        LineDisplay lineDisplay = dynamicLineDisplay.getDevice();
        lineDisplay.addStatusUpdateListener(this);
        this.connectLock = connectLock;
    }

    /**
     * linking manager to device and adding a new instance to List of connectionEventListeners.
     * @param connectionEventListener is an instance of an event.
     */
    void addConnectionEventListener(ConnectionEventListener connectionEventListener) {
        this.connectionEventListeners.add(connectionEventListener);
    }

    /**
     * Sends event after connection.
     * @param connectStatus Stating that an event happened.
     */
    private void fireConnectionEvent(boolean connectStatus) {
        this.connectionEventListeners.forEach(listener -> listener.connectionEventOccurred(new ConnectionEvent(this, connectStatus)));
    }

    /**
     * Makes sure a connection occurs.
     * @return Returns the connection status.
     */
    public boolean connect() {
        try {
            DynamicDevice.ConnectionResult connectionResult = dynamicLineDisplay.connect();
            if (connectionResult == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
                return false;
            }
            //Make sure the device is enabled even if we were already connected
            LineDisplay lineDisplay = dynamicLineDisplay.getDevice();
            if (!lineDisplay.getDeviceEnabled()) {
                lineDisplay.setDeviceEnabled(true);
            }
            //First connection, fire the event and clear the screen
            if(connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                lineDisplay.clearText();
                fireConnectionEvent(true);
            }
        } catch (JposException jposException) {
            return false;
        }
        return true;
    }

    //This separates the low level disconnect from the high level disconnect
    // the idea is to prevent deadlocks in device code that has the device locked
    // when it sends things like status update events

    /**
     * Disconnects device .
     */
    void disconnect() {
        if (dynamicLineDisplay.isConnected()) {
            dynamicLineDisplay.disconnect();
            fireConnectionEvent(false);
        }
    }

    /**
     * Shows if device is connected
     * @return Device is connected.
     */
    public boolean isConnected() {
        return dynamicLineDisplay.isConnected();
    }

    /**
     * Makes sure it displays the lines on device.
     * @param line1Text displays the first line text.
     * @param line2Text displays second line text.
     * @throws JposException
     */
    public void displayLine(String line1Text, String line2Text) throws JposException {
        /*
        We are relying on the disconnect call during status notify events to shutdown the device.
        This will cause us to throw the proper exception when trying to display text to a closed device
        */
        try {
            LineDisplay lineDisplay;
            synchronized (lineDisplay = dynamicLineDisplay.getDevice()) {
                lineDisplay.displayTextAt(0, 0, line1Text, LineDisplayConst.DISP_DT_NORMAL);
                lineDisplay.displayTextAt(1, 0, line2Text, LineDisplayConst.DISP_DT_NORMAL);
            }
        } catch (JposException jposException) {
            if(isConnected()) {
                LOGGER.error(MARKER, "Line Display Failed to Display: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
            } else {
                LOGGER.trace("Line Display Failed to Display: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
            }
            throw jposException;
        }
    }

    /**
     * Gets device name.
     * @return returns device name.
     */
    public String getDeviceName() {
        return dynamicLineDisplay.getDeviceName();
    }

    /**
     * Gets the StatusUpdate of the line displays whether its Power online/offline and connects or disconnects accordingly.
     * @param statusUpdateEvent
     */
    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        LOGGER.debug("statusUpdateOccurred(): " + statusUpdateEvent.getStatus());
        int status = statusUpdateEvent.getStatus();
        switch (status) {
            case JposConst.JPOS_SUE_POWER_OFF:
            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
            case JposConst.JPOS_SUE_POWER_OFFLINE:
                LOGGER.error(MARKER, "Line Display Status Update: Power offline");
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
}
