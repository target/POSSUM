package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.StructuredEventLogger;
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
    private static final StructuredEventLogger log = StructuredEventLogger.of("CashDrawer", "CashDrawerDevice", LOGGER);

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
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("simpleCashDrawer cannot be null");
            log.failure("Cash Drawer Failed in Constructor: simpleCashDrawer cannot be null", 18,
                    illegalArgumentException);
            throw illegalArgumentException;
        }
        if (deviceListener == null) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("deviceListener cannot be null");
            log.failure("Cash Drawer Failed in Constructor: deviceListener cannot be null", 18,
                    illegalArgumentException);
            throw illegalArgumentException;
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
                    log.failure("Cash Drawer Failed to Disconnect", 18, jposException);
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
                log.failure("Cash Drawer Failed to Enable Device", 18, jposException);
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
                DeviceException deviceException = new DeviceException(CashDrawerError.ALREADY_OPEN);
                log.failure("Cash Drawer is already open: " + CashDrawerError.ALREADY_OPEN.getDescription(), 17, deviceException);
                throw deviceException;
            }
            log.success("Opening cash drawer...", 1);
            cashDrawer.openDrawer();
            waitForCashDrawerClose();
            if(!deviceConnected) {
                DeviceException deviceException = new DeviceException(CashDrawerError.DEVICE_OFFLINE);
                log.failure("Cash Drawer is offline after closing: " + CashDrawerError.DEVICE_OFFLINE.getDescription(), 18, deviceException);
                throw deviceException;
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
            log.failure("Cash Drawer is not connected", 18, jposException);
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
        log.success("Waiting for cash drawer to close...", 1);
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
        log.success("Cash Drawer statusUpdateOccurred(): " + status, 1);
        switch(status) {
            case JposConst.JPOS_SUE_POWER_OFF:
            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
            case JposConst.JPOS_SUE_POWER_OFFLINE:
                log.failure("Cash Drawer Status Update: Power offline", 13, null);
                deviceConnected = false;
                break;
            case JposConst.JPOS_SUE_POWER_ONLINE:
                log.success("Status Update: Power online", 5);
                deviceConnected = true;
                break;
            case CashDrawerConst.CASH_SUE_DRAWEROPEN:
                log.success("Cash drawer opened", 1);
                cashDrawerOpen = true;
                break;
            case CashDrawerConst.CASH_SUE_DRAWERCLOSED:
                log.success("Cash drawer closed", 1);
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
            log.success("Lock: " + isLocked, 1);
        } catch(InterruptedException interruptedException) {
            log.failure("Lock Failed", 17, interruptedException);
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
