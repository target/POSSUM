package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.components.scanner.entities.Barcode;
import com.target.devicemanager.components.scanner.entities.ScannerType;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.JposConst;
import jpos.JposException;
import jpos.Scanner;
import jpos.events.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ScannerDevice {
    private final DynamicDevice<? extends Scanner> dynamicScanner;
    private final DeviceListener deviceListener;
    private final ScannerType scannerType;
    private boolean deviceConnected = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScannerDevice.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("scanner", "ScannerDevice", LOGGER);
    private static final int MAX_RETRIES = 3;
    private static final int SCANNER_CMD_TIMEOUT = 999;
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    private boolean isTest = false;
    ApplicationConfig applicationConfig;

    /**
     * initializes scanner device.
     * @param deviceListener
     * @param dynamicScanner is the dynamic device.
     * @param scannerType is the scanner type.
     */
    public ScannerDevice(DeviceListener deviceListener, DynamicDevice<? extends Scanner> dynamicScanner, ScannerType scannerType, ApplicationConfig applicationConfig) {
        this(deviceListener, dynamicScanner, scannerType, new ReentrantLock(true), applicationConfig);
    }

    public ScannerDevice(DeviceListener deviceListener, DynamicDevice<? extends Scanner> dynamicScanner, ScannerType scannerType, ReentrantLock connectLock, ApplicationConfig applicationConfig) {
        if(scannerType == null) {
            log.failure("Failed in Constructor: scannerType cannot be null", 17, null);
            throw new IllegalArgumentException("scannerType cannot be null");
        }
        if (deviceListener == null) {
            log.failure(scannerType + " Failed in Constructor: deviceListener cannot be null", 17, null);
            throw new IllegalArgumentException("deviceListener cannot be null");
        }
        if(dynamicScanner == null){
            log.failure(scannerType + " Failed in Constructor: dynamicScanner cannot be null", 17, null);
            throw new IllegalArgumentException("dynamicScanner cannot be null");
        }
        this.dynamicScanner = dynamicScanner;
        this.deviceListener = deviceListener;
        this.scannerType = scannerType;
        this.connectLock = connectLock;
        this.applicationConfig = applicationConfig;
    }

    /**
     * Makes sure a connection occurs.
     */
    public void connect() {
        if(tryLock()) {
            try {
                DynamicDevice.ConnectionResult connectionResult = dynamicScanner.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    attachEventListeners();
                    deviceConnected = true;
                } else if (connectionResult == DynamicDevice.ConnectionResult.NOT_CONNECTED ){
                    deviceConnected = false;
                } else {
                    deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
                }
            } finally {
                unlock();
            }
        }
    }

    public Boolean getDeviceConnected() {
        return this.deviceConnected;
    }

    public void setDeviceConnected(boolean deviceConnected) {
        this.deviceConnected = deviceConnected;
    }

    public Boolean reconnect() throws DeviceException {
        if (tryLock()) {
            try {
                if (deviceConnected) {
                    dynamicScanner.disconnect();
                    detachEventListeners();
                }
                DynamicDevice.ConnectionResult connectionResult = dynamicScanner.connect();
                if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                    attachEventListeners();
                }
                deviceConnected = (connectionResult == DynamicDevice.ConnectionResult.CONNECTED || connectionResult == DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
            } finally {
                unlock();
            }
        }
        else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
        return deviceConnected;
    }

    /**
     * Gets the scanner data from the barcode.
     * @return
     * @throws JposException
     */
    public Barcode getScannerData() throws JposException {
        log.success(getScannerType() + " getScannerData(in)", 1);
        enable();
        //waitForData can potentially block forever
        try {
            DataEvent dataEvent = deviceListener.waitForData();
            return handleDataEvent(dataEvent);
        } catch (JposException jposException) {
            throw jposException;
        }
    }

    /**
     * Handles the data based on scanner type and barcode.
     * @param dataEvent instance of data event.
     * @return
     * @throws JposException
     */
    private Barcode handleDataEvent(DataEvent dataEvent) throws JposException {
        if (!(dataEvent.getSource() instanceof Scanner)) {
            log.success(getScannerType() + " getScannerData(out)", 1);
            JposException jposException = new JposException(JposConst.JPOS_E_FAILURE);
            log.failure(getScannerType() + " Failed to Handle Data: " + jposException.getMessage(), 17, jposException);
            throw jposException;
        }
        try {
            String data;
            int type;
            ScannerType source;
            Scanner scanner;
            synchronized (scanner = (Scanner) dataEvent.getSource()) {
                data = new String(scanner.getScanDataLabel(), Charset.defaultCharset());
                type = scanner.getScanDataType();
                if (applicationConfig != null && applicationConfig.IsSimulationMode()) {
                    source = ScannerType.fromValue(scanner.getPhysicalDeviceName());
                } else {
                    source = scannerType;
                }
            }
            Barcode barcode = new Barcode(data, type, source);
            log.success(barcode.source + " - returning scanned data type: " + barcode.type + " of size " + data.length(), 9);
            log.success(barcode.source + " getScannerData(out)", 1);
            return barcode;
        } catch (JposException jposException) {
            log.failure(getScannerType() + " Failed to Handle Data: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            throw jposException;
        }
    }

    /**
     * Disables scanner and cancels scanner data.
     * @return null.
     */
    public Void cancelScannerData() {
        log.success(getScannerType() + " cancelScannerData(in)", 1);
        try{
            disable();
        } catch(JposException jposException){
            log.failure("Received exception in cancelScannerData", 17, jposException);
        } finally {
            deviceListener.stopWaitingForData();
        }
        log.success(getScannerType() + " cancelScannerData(out)", 1);
        return null;
    }

    /**
     * Gets the device name.
     * @return device name.
     */
    public String getDeviceName() {
        return dynamicScanner.getDeviceName();
    }

    /**
     * Makes sure scanner is connected.
     * @return Connection status.
     */
    public boolean isConnected() {
        return dynamicScanner.isConnected();
    }


    public void setIsTest(boolean isTest) {
        this.isTest = isTest;
    }

    /**
     * Makes sure scanner is connected and enabled.
     * @throws JposException
     */
    protected void enable() throws JposException {
        log.success(getScannerType() + " enable(in)", 1);
        if (!isConnected()) {
            JposException jposException = new JposException(JposConst.JPOS_E_OFFLINE);
            throw jposException;
        }
        deviceListener.startEventListeners();
        try {
            Scanner scanner;
            synchronized (scanner = dynamicScanner.getDevice()) {
                scanner.setAutoDisable(true);
                scanner.setDecodeData(true);
                scanner.setDataEventEnabled(true);
                scanner.setDeviceEnabled(true);
                if(isTest) { // used to test timeouts in unit testing
                    try {
                        Thread.sleep(1100);
                    } catch (InterruptedException interruptedException) {
                        //ignore
                    }
                }
            }
        } catch (JposException jposException) {
            if(isConnected()) {
                log.failure(getScannerType() + " Failed to Enable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            } else {
                log.failure(getScannerType() + " Failed to Enable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            }

            if(getScannerType().equalsIgnoreCase("HANDHELD")) {
                log.success("Attempting HANDHELD Reconnect enable", 9);
                handheldReconnect();
            } else {
                throw jposException;
            }
        }
        log.success(getScannerType() + " scanner enabled", 9);
        log.success(getScannerType() + " enable(out)", 1);
    }

    /**
     * Disables scanner.
     * @throws JposException
     */
    private void disable() throws JposException {
        log.success(getScannerType() + " disable(in)", 1);
        try {
            Scanner scanner;
            synchronized (scanner = dynamicScanner.getDevice()) {
                scanner.setDeviceEnabled(false);
            }
        } catch (JposException jposException) {
            if(isConnected()) {
                log.failure(getScannerType() + " Failed to Disable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            } else if(jposException.getErrorCode() != JposConst.JPOS_E_CLOSED) {
                log.failure(getScannerType() + " Failed to Disable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 17, jposException);
            }
            if(getScannerType().equalsIgnoreCase("HANDHELD")) {
                log.success("Attempting HANDHELD Reconnect disable", 9);
                handheldReconnect();
            } else {
                throw jposException;
            }
        }
        log.success(getScannerType() + " scanner disabled", 9);
        log.success(getScannerType() + " disable(out)", 1);
    }

    /**
     * Attaches an event listener and adding it to a new instances.
     */
    private void attachEventListeners() {
        Scanner scanner;
        synchronized (scanner = dynamicScanner.getDevice()) {
            scanner.addErrorListener(deviceListener);
            scanner.addDataListener(deviceListener);
            scanner.addStatusUpdateListener(deviceListener);
        }
    }

    /**
     * Removes error, data, and status update device listeners
     */
    private void detachEventListeners() {
        Scanner scanner;
        synchronized (scanner = dynamicScanner.getDevice()) {
            scanner.removeErrorListener(deviceListener);
            scanner.removeDataListener(deviceListener);
            scanner.removeStatusUpdateListener(deviceListener);
        }
    }

    /** Gets the scanner type.
     * @return Scanner type.
     */
    public String getScannerType() {
        return this.scannerType.toString();
    }
    // Returns the elapsed time, need this for handscanner timeout/slow scan issue
    // when we enable the handscanner - if this process takes longer than 1sec something went wrong
    // and the handscanner timed out. We want to know the total time taken to enable the scanner, so we know
    // whether we need to disconnect and reconnect the handscanner

    /**
     * Reconnects the handheld scanner.
     */
    private void handheldReconnect() {
        log.success("handheldTimeoutOccurredCheck(in)", 1);
        int retries = 0;
        Instant start;
        Instant end;
        long timeElapsedMillis;
        while (retries < MAX_RETRIES) {
            try {
                retries += 1;
                dynamicScanner.disconnect();
                connect();
                start = Instant.now();
                enable();
                end = Instant.now();
                timeElapsedMillis = Duration.between(start, end).toMillis();
                if(timeElapsedMillis < SCANNER_CMD_TIMEOUT){
                    log.success("Reconnect handheld : recovered dead hand scanner in " + retries + " attempt(s).", 9);
                    break;
                } else {
                    log.success("Reconnect handheld : still taking longer " + timeElapsedMillis + " milliseconds", 9);
                }
            } catch (JposException jposException) {
                log.failure("Hand scanner reconnect exception: " + jposException.getMessage(), 17, jposException);
            }
        }

        log.success("handheldTimeoutOccurredCheck(out)", 1);
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
            log.failure("Lock Failed: " + interruptedException.getMessage(), 17, interruptedException);
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
}
