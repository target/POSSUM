package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.components.cashdrawer.entities.CashDrawerError;
import jpos.CashDrawer;
import jpos.CashDrawerConst;
import jpos.JposConst;
import jpos.JposException;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import com.target.devicemanager.common.LogPayloadBuilder;
import com.target.devicemanager.common.entities.LogField;

@Profile({"local", "dev", "prod"})
public class CashDrawerDevice implements StatusUpdateListener{
    private final DynamicDevice<? extends CashDrawer> dynamicCashDrawer;
    private final DeviceListener deviceListener;
    private boolean deviceConnected = false;
    private boolean cashDrawerOpen = false;
    private boolean areListenersAttached;
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    private static final int DRAWER_STATUS_CHECK_INTERVAL = 500;
    private static final Logger LOGGER = LoggerFactory.getLogger(CashDrawerDevice.class);

    /**
     * Makes sure everything is connected and online.
     * @param dynamicCashDrawer
     * @param deviceListener
     */
    public CashDrawerDevice(DynamicDevice<? extends CashDrawer> dynamicCashDrawer, DeviceListener deviceListener) {
        this(dynamicCashDrawer, deviceListener, new ReentrantLock(true));
    }

    public CashDrawerDevice(DynamicDevice<? extends CashDrawer> dynamicCashDrawer, DeviceListener deviceListener, ReentrantLock connectLock) {
        if (dynamicCashDrawer == null) {
            new LogPayloadBuilder()
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 18)
                .add(LogField.COMPONENT, "CashDrawerDevice")
                .add(LogField.EVENT_ACTION, "Constructor")
                .add(LogField.EVENT_OUTCOME, "failure")
                .add(LogField.ERROR_MESSAGE, "Cash Drawer Failed in Constructor: simpleCashDrawer cannot be null")
                .add(LogField.ERROR_TYPE, "IllegalArgumentException")
                .logError(LOGGER);
            throw new IllegalArgumentException("simpleCashDrawer cannot be null");
        }
        if (deviceListener == null) {
            new LogPayloadBuilder()
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 18)
                .add(LogField.COMPONENT, "CashDrawerDevice")
                .add(LogField.EVENT_ACTION, "Constructor")
                .add(LogField.EVENT_OUTCOME, "failure")
                .add(LogField.ERROR_MESSAGE, "Cash Drawer Failed in Constructor: deviceListener cannot be null")
                .add(LogField.ERROR_TYPE, "IllegalArgumentException")
                .logError(LOGGER);
            throw new IllegalArgumentException("deviceListener cannot be null");
        }
        this.dynamicCashDrawer = dynamicCashDrawer;
        this.deviceListener = deviceListener;
        this.connectLock = connectLock;
    }

    /**
     * Makes sure cash drawer is enabled and connected.
     */
    public boolean connect() {
        if (dynamicCashDrawer.connect() == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
            return false;
        }
        if (!areListenersAttached) {
            attachEventListeners();
            areListenersAttached = true;
        }
        
        /*
        NCR Devices throws exception when setDeviceEnabled is called when device is not connected.
        Enable the device when device is connected so that we can get status update events.
        When device is disabled we will not get the status events
        */
        CashDrawer cashDrawer;
        synchronized (cashDrawer = dynamicCashDrawer.getDevice()) {
            try {
                if (!cashDrawer.getDeviceEnabled()) {
                    cashDrawer.setDeviceEnabled(true);
                    if (cashDrawer.getDrawerOpened()) {
                        cashDrawerOpen = true;
                    }
                    deviceConnected = true;
                }
            } catch (JposException jposException) {
                deviceConnected = false;
                return false;
            }
        }
        return true;
    }

    /**
     * This method is only used to set 'areListenersAttached' for unit testing
     * @param areListenersAttached
     */
    public void setAreListenersAttached(boolean areListenersAttached) {
        this.areListenersAttached = areListenersAttached;
    }

    /**
     * This method is only used to get 'areListenersAttached' for unit testing
     * @return
     */
    public boolean getAreListenersAttached() {
        return areListenersAttached;
    }

    /**
     * This method is only used to set 'deviceConnected' for unit testing
     * @param deviceConnected
     */
    public void setDeviceConnected(boolean deviceConnected) {
        this.deviceConnected = deviceConnected;
    }

    /**
     * This method is only used to set 'cashDrawerOpen' for unit testing
     * @param cashDrawerOpen
     */
    public void setCashDrawerOpen(boolean cashDrawerOpen) {
        this.cashDrawerOpen = cashDrawerOpen;
    }

    /**
     * This method is only used to get 'cashDrawerOpen' for unit testing
     * @return
     */
    public boolean getCashDrawerOpen() {
        return cashDrawerOpen;
    }

    /**
     * Disconnects the cash drawer device
     */
    public void disconnect() {
        if (dynamicCashDrawer.isConnected()) {
            if (areListenersAttached) {
                detachEventListeners();
                areListenersAttached = false;
            }
            CashDrawer cashDrawer;
            synchronized (cashDrawer = dynamicCashDrawer.getDevice()) {
                try {
                    if (cashDrawer.getDeviceEnabled()) {
                        cashDrawer.setDeviceEnabled(false);
                        dynamicCashDrawer.disconnect();
                        deviceConnected = false;
                    }
                } catch (JposException jposException) {
                    new LogPayloadBuilder()
                        .add(LogField.SERVICE_NAME, "CashDrawer")
                        .add(LogField.EVENT_SEVERITY, 18)
                        .add(LogField.COMPONENT, "CashDrawerDevice")
                        .add(LogField.EVENT_ACTION, "disable")
                        .add(LogField.EVENT_OUTCOME, "failure")
                        .add(LogField.ERROR_TYPE, "JposException")
                        .add(LogField.ERROR_CODE, jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended())
                        .add(LogField.ERROR_MESSAGE, jposException.getMessage())
                        .add(LogField.MESSAGE, "Cash Drawer Failed to Disconnect: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended())
                        .logError(LOGGER);
                }
            }
        }
        /*
        NCR Devices throws exception when setDeviceEnabled is called when device is not connected.
        Enable the device when device is connected so that we can get status update events.
        When device is disabled we will not get the status events
        */
        CashDrawer cashDrawer;
        synchronized (cashDrawer = dynamicCashDrawer.getDevice()) {
            try {
                if (!cashDrawer.getDeviceEnabled()) {
                    cashDrawer.setDeviceEnabled(true);
                    if (cashDrawer.getDrawerOpened()) {
                        cashDrawerOpen = true;
                    }
                    deviceConnected = true;
                }
            } catch (JposException jposException) {
                new LogPayloadBuilder()
                    .add(LogField.SERVICE_NAME, "CashDrawer")
                    .add(LogField.EVENT_SEVERITY, 18)
                    .add(LogField.COMPONENT, "CashDrawerDevice")
                    .add(LogField.EVENT_ACTION, "enable")
                    .add(LogField.EVENT_OUTCOME, "failure")
                    .add(LogField.ERROR_TYPE, "JposException")
                    .add(LogField.ERROR_CODE, jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended())
                    .add(LogField.ERROR_MESSAGE, jposException.getMessage())
                    .add(LogField.MESSAGE, "Cash Drawer Failed to Enable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended())
                    .logError(LOGGER);
                deviceConnected = false;
            }
        }
    }


    /**
     * Opens the Cash drawer and goes offline after closing.
     * @return
     * @throws JposException
     * @throws DeviceException
     */
    public void openCashDrawer() throws JposException, DeviceException {
        enable();
        CashDrawer cashDrawer;
        synchronized (cashDrawer = dynamicCashDrawer.getDevice()) {
            if (cashDrawerOpen) {
                new LogPayloadBuilder()
                    .add(LogField.SERVICE_NAME, "CashDrawer")
                    .add(LogField.EVENT_SEVERITY, 18)
                    .add(LogField.COMPONENT, "CashDrawerDevice")
                    .add(LogField.EVENT_ACTION, "openCashDrawer")
                    .add(LogField.EVENT_OUTCOME, "failure")
                    .add(LogField.ERROR_TYPE, "DeviceException")
                    .add(LogField.ERROR_MESSAGE, CashDrawerError.ALREADY_OPEN.getDescription())
                    .add(LogField.ERROR_CODE, CashDrawerError.ALREADY_OPEN.getCode())
                    .add(LogField.MESSAGE,"Cash Drawer is already open: " + CashDrawerError.ALREADY_OPEN.getDescription())
                    .logError(LOGGER);
                throw new DeviceException(CashDrawerError.ALREADY_OPEN);
            }
            new LogPayloadBuilder()
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 9)
                .add(LogField.COMPONENT, "CashDrawerDevice")
                .add(LogField.EVENT_ACTION, "openCashDrawer")
                .add(LogField.MESSAGE, "Opening cash drawer...")
                .logInfo(LOGGER);
            cashDrawer.openDrawer();
            waitForCashDrawerClose();
            if(!deviceConnected) {
                new LogPayloadBuilder()
                    .add(LogField.SERVICE_NAME, "CashDrawer")
                    .add(LogField.EVENT_SEVERITY, 18)
                    .add(LogField.COMPONENT, "CashDrawerDevice")
                    .add(LogField.EVENT_ACTION, "openCashDrawer")
                    .add(LogField.EVENT_OUTCOME, "failure")
                    .add(LogField.ERROR_TYPE, "DeviceException")
                    .add(LogField.ERROR_MESSAGE, CashDrawerError.DEVICE_OFFLINE.getDescription())
                    .add(LogField.ERROR_CODE, CashDrawerError.DEVICE_OFFLINE.getCode())
                    .add(LogField.MESSAGE,  "Cash Drawer is offline after closing: " + CashDrawerError.DEVICE_OFFLINE.getDescription())
                    .logError(LOGGER);
                throw new DeviceException(CashDrawerError.DEVICE_OFFLINE);
            }
        }
    }

    /**
     * Checks if it's disconnected it throws an error, if it's connected it starts the device listener.
     * @throws JposException
     */
    private void enable() throws JposException {
        if (!isConnected()) {
            JposException jposException = new JposException(JposConst.JPOS_E_OFFLINE);
            new LogPayloadBuilder()
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 18)
                .add(LogField.COMPONENT, "CashDrawerDevice")
                .add(LogField.EVENT_ACTION, "enable")
                .add(LogField.EVENT_OUTCOME, "failure")
                .add(LogField.ERROR_TYPE, "JposException")
                .add(LogField.ERROR_CODE, jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended())
                .add(LogField.ERROR_MESSAGE, jposException.getMessage())
                .add(LogField.MESSAGE, "Cash Drawer is not connected: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended())
                .logError(LOGGER);
            throw jposException;
        }
        deviceListener.startEventListeners();
    }

    /**
     * Gets the device name.
     * @return Device name.
     */
    public String getDeviceName() {
        return dynamicCashDrawer.getDeviceName();
    }

    /**
     * Shows if the device is connected.
     */
    public boolean isConnected() { return deviceConnected; }

    /**
     * Attaches an event listener and adding it to a new instance.
     */
    private void attachEventListeners() {
        CashDrawer cashDrawer;
        synchronized (cashDrawer = dynamicCashDrawer.getDevice()) {
            cashDrawer.addStatusUpdateListener(this);
        }
    }

    /**
     * Removes status update listener for cash drawer device.
     */
    private void detachEventListeners() {
        CashDrawer cashDrawer;
        synchronized (cashDrawer = dynamicCashDrawer.getDevice()) {
            cashDrawer.removeStatusUpdateListener(this);
        }
    }

    /**
     * Waits for CashDrawer to close or check interval.
     */
    private void waitForCashDrawerClose() {
        new LogPayloadBuilder()
            .add(LogField.SERVICE_NAME, "CashDrawer")
            .add(LogField.EVENT_SEVERITY, 1)
            .add(LogField.COMPONENT, "CashDrawerDevice")
            .add(LogField.EVENT_ACTION, "waitForCashDrawerClose")
            .add(LogField.MESSAGE, "Waiting for cash drawer to close...")
            .logTrace(LOGGER);
        //This do/while is necessary for status to stabilize when cash drawer opens
        do {
            try {
                Thread.sleep(DRAWER_STATUS_CHECK_INTERVAL);
            } catch (InterruptedException interruptedException) {
                //don't worry bout it
            }
            // Wait for cash drawer to close or cash drawer offline
        } while (cashDrawerOpen && deviceConnected);
    }

    /**
     * Gives the different cases in which the CashDrawer status updates.
     * @param statusUpdateEvent
     */
    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        int status = statusUpdateEvent.getStatus();
        new LogPayloadBuilder()
            .add(LogField.SERVICE_NAME, "CashDrawer")
            .add(LogField.EVENT_SEVERITY, 1)
            .add(LogField.COMPONENT, "CashDrawerDevice")
            .add(LogField.EVENT_ACTION, "statusUpdateOccurred")
            .add(LogField.TAGS, status)
            .add(LogField.MESSAGE, "Cash Drawer statusUpdateOccurred(): " + status)
            .logTrace(LOGGER);
        switch(status) {
            case JposConst.JPOS_SUE_POWER_OFF:
            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
            case JposConst.JPOS_SUE_POWER_OFFLINE:
                new LogPayloadBuilder()
                    .add(LogField.SERVICE_NAME, "CashDrawer")
                    .add(LogField.EVENT_SEVERITY, 18)
                    .add(LogField.COMPONENT, "CashDrawerDevice")
                    .add(LogField.EVENT_ACTION, "statusUpdateOccurred")
                    .add(LogField.EVENT_OUTCOME, "failure")
                    .add(LogField.TAGS, "POWER_OFFLINE")
                    .add(LogField.MESSAGE, "Cash Drawer Status Update: Power offline")
                    .logError(LOGGER);
                deviceConnected = false;
                break;
            case JposConst.JPOS_SUE_POWER_ONLINE:
                new LogPayloadBuilder()
                    .add(LogField.SERVICE_NAME, "CashDrawer")
                    .add(LogField.EVENT_SEVERITY, 5)
                    .add(LogField.COMPONENT, "CashDrawerDevice")
                    .add(LogField.EVENT_ACTION, "statusUpdateOccurred")
                    .add(LogField.TAGS, "POWER_ONLINE")
                    .add(LogField.MESSAGE, "Status Update: Power online")
                    .logDebug(LOGGER);
                deviceConnected = true;
                break;
            case CashDrawerConst.CASH_SUE_DRAWEROPEN:
                new LogPayloadBuilder()
                    .add(LogField.SERVICE_NAME, "CashDrawer")
                    .add(LogField.EVENT_SEVERITY, 9)
                    .add(LogField.COMPONENT, "CashDrawerDevice")
                    .add(LogField.EVENT_ACTION, "statusUpdateOccurred")
                    .add(LogField.TAGS, "DRAWER_OPEN")
                    .add(LogField.MESSAGE, "Cash drawer opened")
                    .logInfo(LOGGER);
                cashDrawerOpen = true;
                break;
            case CashDrawerConst.CASH_SUE_DRAWERCLOSED:
                new LogPayloadBuilder()
                    .add(LogField.SERVICE_NAME, "CashDrawer")
                    .add(LogField.EVENT_SEVERITY, 9)
                    .add(LogField.COMPONENT, "CashDrawerDevice")
                    .add(LogField.EVENT_ACTION, "statusUpdateOccurred")
                    .add(LogField.TAGS, "DRAWER_CLOSED")
                    .add(LogField.MESSAGE, "Cash drawer closed")
                    .logInfo(LOGGER);
                cashDrawerOpen = false;
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
            new LogPayloadBuilder()
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 1)
                .add(LogField.COMPONENT, "CashDrawerDevice")
                .add(LogField.EVENT_ACTION, "tryLock")
                .add(LogField.EVENT_OUTCOME, isLocked ? "success" : "failed")
                .add(LogField.MESSAGE, "Lock: " + isLocked)
                .logTrace(LOGGER);
        } catch(InterruptedException interruptedException) {
            new LogPayloadBuilder()
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 17)
                .add(LogField.COMPONENT, "CashDrawerDevice")
                .add(LogField.EVENT_ACTION, "tryLock")
                .add(LogField.EVENT_OUTCOME, "failure")
                .add(LogField.ERROR_TYPE, "InterruptedException")
                .add(LogField.ERROR_MESSAGE, interruptedException.getMessage())
                .add(LogField.ERROR_STACK_TRACE, Arrays.toString(interruptedException.getStackTrace()))
                .add(LogField.MESSAGE, "Lock Failed: " + interruptedException.getMessage())
                .logError(LOGGER);
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
