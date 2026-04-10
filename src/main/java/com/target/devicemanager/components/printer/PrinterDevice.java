package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.components.printer.entities.*;
import jpos.JposConst;
import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class PrinterDevice implements StatusUpdateListener {

    private final DynamicDevice<? extends POSPrinter> dynamicPrinter;
    private final DeviceListener deviceListener;
    private boolean areListenersAttached;
    private boolean deviceConnected = false;
    private static final int RETURN_IMMEDIATE = 0;
    private boolean isCheckInserted;
    private boolean wasDoorOpened = false;
    private boolean wasPaperEmpty = false;
    private boolean isReconnectNeeded = false;
    private static final String R5PrinterName = "NCR Kiosk POS Printer";
    private static final int TRY_LOCK_TIMEOUT = 1;
    private static final int PRINT_TIMEOUT_SECONDS = 30;
    private final ReentrantLock connectLock;
    private volatile boolean isLocked = false;
    private final AtomicReference<Thread> lockOwnerThread = new AtomicReference<>(null);
    private volatile boolean interruptedByTimeout = false;
    private final AtomicBoolean recoveryInProgress = new AtomicBoolean(false);
    private final int[] ref = new int[1];
    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterDevice.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of(StructuredEventLogger.getPrinterServiceName(), "PrinterDevice", LOGGER);

    /**
     * constructor
     * @param dynamicPrinter
     * @param deviceListener
     */
    public PrinterDevice(DynamicDevice<? extends POSPrinter> dynamicPrinter, DeviceListener deviceListener) {
        this(dynamicPrinter, deviceListener, new ReentrantLock(true));
    }

    public PrinterDevice(DynamicDevice<? extends POSPrinter> dynamicPrinter, DeviceListener deviceListener, ReentrantLock connectLock) {
        if (dynamicPrinter == null) {
            IllegalArgumentException ex = new IllegalArgumentException("dynamicPrinter cannot be null");
            log.failure("Printer Failed in Constructor: dynamicPrinter cannot be null", 18, ex);
            throw ex;
        }
        if (deviceListener == null) {
            IllegalArgumentException ex = new IllegalArgumentException("deviceListener cannot be null");
            log.failure("Printer Failed in Constructor: deviceListener cannot be null", 18, ex);
            throw ex;
        }
        this.dynamicPrinter = dynamicPrinter;
        this.deviceListener = deviceListener;
        this.connectLock = connectLock;
    }

    /**
     * Method name connect. Connecting printer device through service object.
     * @return connectionSuccessful
     */
    public boolean connect() {
        DynamicDevice.ConnectionResult connectionResult = dynamicPrinter.connect();
        if (connectionResult == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
            return false;
        }

        if (!areListenersAttached) {
            attachEventListeners();
            areListenersAttached = true;
        }
        POSPrinter printer;
        synchronized (printer = dynamicPrinter.getDevice()) {
            try {
                if (!printer.getDeviceEnabled()) {
                    printer.setDeviceEnabled(true);
                    printer.setAsyncMode(true);
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
     * Disconnects the printer device
     */
    public void disconnect() {
        if (areListenersAttached) {
            detachDeviceListeners();
            areListenersAttached = false;
        }
        POSPrinter printer;
        synchronized (printer = dynamicPrinter.getDevice()) {
            try {
                if (printer.getDeviceEnabled()) {
                    printer.setDeviceEnabled(false);
                }
            } catch (JposException jposException) {
                log.failure("Unable to disable: " + getDeviceName() + " - " + jposException.getMessage(), 17, jposException);
            }
        }
        dynamicPrinter.disconnect();
        deviceConnected = false;
    }

    /**
     * Makes sure printer device is enabled and connected and online.
     * @throws JposException
     */
    private void enable() throws JposException {
        if (!isConnected()) {
            JposException jposException = new JposException(JposConst.JPOS_E_OFFLINE);
            log.failure("Printer Failed to Enable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 18, jposException);
            throw jposException;
        }
    }

    /**
     * Prints the content on the receipt.
     *
     * @param contents       the image on receipt.
     * @param printerStation register where printing occurs.
     * @throws JposException, PrinterException
     */

    public void printContent(List<PrinterContent> contents, int printerStation)
            throws JposException, PrinterException {
        log.success("printContent() invoked", 5);

        if (!tryLock()) {
            log.success("Printer lock unavailable", 5);
            throw new PrinterException(PrinterError.PRINTER_BUSY);
        }

        POSPrinter printer = null;
        boolean transactionStarted = false;

        try {
            synchronized (printer = dynamicPrinter.getDevice()) {

                log.success("Acquired printer and entered synchronized block", 5);

                if (interruptedByTimeout) {
                    log.failure("Aborting printContent — interrupted by forceUnlock timeout before print started", 17, null);
                    throw new JposException(JposConst.JPOS_E_TIMEOUT);
                }
                // Clear status
                PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton().clearError();
                log.success("Cleared singleton error state", 5);

                if (contents == null || contents.isEmpty()) {
                    log.failure("Receipt contents are empty", 17, null);
                    throw new PrinterException(PrinterError.INVALID_FORMAT);
                }

                enable();
                log.success("Printer enabled successfully", 5);

                if (printerStation != PrinterStationType.CHECK_PRINTER.getValue()
                        && (wasPaperEmpty || paperEmptyCheck())) {
                    log.failure("Paper empty detected before print", 13, null);
                    throw new JposException(114, 203);
                }

                reconnectR5Printer();
                log.success("Reconnect Check for R5 completed", 5);
                printer.transactionPrint(printerStation, POSPrinterConst.PTR_TP_TRANSACTION);
                transactionStarted = true;
                log.success("Transaction started", 5);

                int index = 0;
                for (PrinterContent content : contents) {
                    index++;

                    if (content == null || content.type == null) {
                        log.failure("Invalid content at index " + index, 13, null);
                        throw new PrinterException(PrinterError.INVALID_FORMAT);
                    }

                    switch (content.type.toString()) {
                        case "BARCODE":
                            print(printer, (BarcodeContent) content, printerStation);
                            break;
                        case "IMAGE":
                            print(printer, (ImageContent) content, printerStation);
                            break;
                        case "TEXT":
                        default:
                            print(printer, content.data, printerStation);
                            break;
                    }
                }

                log.success("All content sent to printer buffer", 5);

                deviceListener.startEventListeners();
                log.success("Event synchronizer re-armed before wait", 5);

                printer.transactionPrint(printerStation, POSPrinterConst.PTR_TP_NORMAL);
                transactionStarted = false;
                log.success("Transaction ended (PTR_TP_NORMAL)", 5);

                deviceListener.waitForOutputToComplete(PRINT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.success("Output complete event received", 5);

                PrinterException statusError =
                        PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton().getError();

                if (statusError != null) {
                    log.failure("Singleton error detected after print: "
                            + statusError.getDeviceError().getDescription(), 17, statusError);
                    throw statusError;
                }

                    log.success("printContent() completed successfully", 5);
            }

        } catch (PrinterException printerException) {
            log.failure("PrinterException: " + printerException.getDeviceError().getDescription(), 18, printerException);
            throw printerException;

        } catch (JposException jposException) {
            log.failure("JposException: " + jposException.getErrorCode()
                    + ", " + jposException.getErrorCodeExtended(), 18, jposException);

            boolean printTimeoutError = jposException.getErrorCode() == JposConst.JPOS_E_TIMEOUT;

            if (printTimeoutError) {
                if (interruptedByTimeout) {
                    log.failure("Print timeout after forceUnlock interrupt — skipping disconnect, recovery owned by forceUnlock", 17, jposException);
                    interruptedByTimeout = false;
                } else {
                    log.failure("Print timed out after " + PRINT_TIMEOUT_SECONDS
                            + "s waiting for OutputComplete event. Scheduling background recovery.", 18, jposException);
                    interruptedByTimeout = true;
                    if (recoveryInProgress.compareAndSet(false, true)) {
                        Thread recoveryThread = new Thread(() -> {
                            try {
                                dynamicPrinter.disconnect();
                                log.failure("Inner timeout recovery: disconnect completed", 17, null);
                            } catch (Exception ex) {
                                log.failure("Inner timeout recovery: disconnect threw: " + ex.getMessage(), 17, null);
                            }
                            boolean reconnected = false;
                            for (int attempt = 0; attempt < 3; attempt++) {
                                if (tryLock()) {
                                    try {
                                        log.failure("Inner timeout recovery: reconnecting (attempt " + attempt + ")", 17, null);
                                        boolean success = connect();
                                        if (success) {
                                            log.failure("Inner timeout recovery: printer reconnected successfully", 5, null);
                                            reconnected = true;
                                        } else {
                                            log.failure("Inner timeout recovery: reconnect attempt " + attempt + " failed", 17, null);
                                        }
                                    } finally {
                                        unlock();
                                    }
                                    break;
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    log.failure("Inner timeout recovery: polling interrupted", 17, null);
                                    break;
                                }
                            }
                            if (!reconnected) {
                                log.failure("Inner timeout recovery: printer did not reconnect — @Scheduled connect() will retry", 17, null);
                            }
                            interruptedByTimeout = false;
                            recoveryInProgress.set(false);
                        }, "printer-inner-timeout-recovery");
                        recoveryThread.setDaemon(true);
                        recoveryThread.start();
                    } else {
                        log.failure("Inner timeout recovery: forceUnlock recovery already in progress — skipping", 17, null);
                        interruptedByTimeout = false;
                    }
                }
                throw jposException;
            }

            boolean failureOrDisabledError = jposException.getErrorCode() == 111
                    || jposException.getErrorCode() == 105;

            boolean paperEmptyError = jposException.getErrorCode() == 114
                    && jposException.getErrorCodeExtended() == 203;

            boolean badPrintContentError = jposException.getErrorCode() == 106
                    || (jposException.getErrorCode() == 114 && jposException.getErrorCodeExtended() == 207);

            if (badPrintContentError) {
                log.failure("Detected invalid print content error", 13, jposException);
                throw new PrinterException(PrinterError.INVALID_FORMAT);
            }

            if (failureOrDisabledError) {
                log.failure("Device failure/disabled detected.", 18, jposException);
            }

            if (paperEmptyError) {
                log.failure("Paper empty detected", 18, jposException);
            }

            throw jposException;

        } finally {
            log.success("Entering finally block", 5);

            try {
                if (printer != null && transactionStarted) {
                    log.failure("Closing open transaction in finally", 17,null);
                    printer.transactionPrint(printerStation, POSPrinterConst.PTR_TP_NORMAL);
                }
            } catch (JposException cleanupException) {
                log.failure("Failed to end transaction: "
                        + cleanupException.getErrorCode() + ", "
                        + cleanupException.getErrorCodeExtended(), 17, cleanupException);
            }

            try {
                if (printer != null) {
                    printer.clearOutput();
                    log.success("Cleared printer output buffer", 5);
                }
            } catch (JposException cleanupException) {
                log.failure("clearOutput failed: "
                        + cleanupException.getErrorCode(), 17, cleanupException);
            }

            if (getIsCheckInserted()) {
                try {
                    withdrawCheck();
                    log.success("Withdraw check executed", 5);
                } catch (JposException cleanupException) {
                    log.failure("withdrawCheck failed: "
                            + cleanupException.getErrorCode(), 17, cleanupException);
                }
            }
            unlock();
            log.success("Printer lock released", 5);
        }
    }

    /**
     * Prints the barcode.
     * @param printer From POS.
     * @param content content of the barcode.
     * @param printerStation register where printing occurs.
     * @throws JposException
     */
    private void print(POSPrinter printer, BarcodeContent content, int printerStation) throws JposException {
        try {
            printer.printBarCode(printerStation,
                    content.data,
                    content.barcodeType.getValue(),
                    content.height,
                    content.width,
                    content.barcodeAlign.getValue(),
                    content.textLocation.getValue());
        } catch (JposException jposException) {
            log.failure("Printer Failed to Print Barcode: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 18, jposException);
            throw jposException;
        }
    }

    /**
     * Prints the target image on the receipt.
     * @param printer From POS.
     * @param content image on the receipt.
     * @param printerStation register where printing occurs.
     * @throws JposException
     */
    private void print(POSPrinter printer, ImageContent content, int printerStation) throws JposException {
        try {
            printer.printMemoryBitmap(printerStation,
                    Base64.getDecoder().decode(content.data),
                    content.imageFormatType.getValue(),
                    POSPrinterConst.PTR_BM_ASIS,
                    POSPrinterConst.PTR_BM_CENTER);
        } catch (JposException jposException) {
            log.failure("Printer Failed to Print Image: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 18, jposException);
            throw jposException;
        }
    }

    /**
     * Prints the message to the customer on the receipt.
     * @param printer POS printer.
     * @param data The message to customer.
     * @param printerStation register where printing occurs
     * @throws JposException
     */
    private void print(POSPrinter printer, String data, int printerStation) throws JposException {
        try {
            printer.printNormal(printerStation, data);
        } catch (JposException jposException) {
            log.failure("Printer Failed to Print Data: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 18, jposException);
            throw jposException;
        }
    }

    /**
     * Allows for the check to be released to cashier.
     * @throws JposException
     */
    public void withdrawCheck() throws JposException {
        try {
            POSPrinter printer;
            synchronized (printer = dynamicPrinter.getDevice()) {
                printer.beginRemoval(RETURN_IMMEDIATE);
                printer.endRemoval();
            }
        } catch (JposException jposException) {
            log.failure("Printer Failed to Remove Check: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 18, jposException);
            throw jposException;
        }
    }

    public String getDeviceName() {
        return dynamicPrinter.getDeviceName();
    }

    public boolean isConnected() {
        return deviceConnected;
    }

    public boolean getIsCheckInserted() {
        return isCheckInserted;
    }

    public void setIsCheckInserted(boolean checkInserted) {
        isCheckInserted = checkInserted;
    }

    public boolean getIsReconnectNeeded() {
        return isReconnectNeeded;
    }

    public void setIsReconnectNeeded(boolean reconnectNeeded) {
        isReconnectNeeded = reconnectNeeded;
    }

    public boolean getWasDoorOpened() {
        return wasDoorOpened;
    }

    public void setWasDoorOpened(boolean doorOpened) {
        wasDoorOpened = doorOpened;
    }

    public boolean getWasPaperEmpty() {
        return wasPaperEmpty;
    }

    public void setWasPaperEmpty(boolean paperEmpty) {
        wasPaperEmpty = paperEmpty;
    }

    /**
     * Checks if R5 printer needs to reconnected before printing
     * This prevents the R5 printer from going into an Internal Device Error after reloading receipt paper.
     * @throws JposException
     */
    private void reconnectR5Printer() throws JposException {
        boolean acquiredLock = false;
        if (!connectLock.isHeldByCurrentThread()) {
            if (!tryLock()) {
                log.success("reconnectR5Printer: lock unavailable, skipping reconnect", 5);
                return;
            }
            acquiredLock = true;
        }
        try {
            POSPrinter printer = dynamicPrinter.getDevice();
            if (printer.getPhysicalDeviceName().contains(R5PrinterName) && getIsReconnectNeeded()) {
                log.success("Reconnecting R5 printer", 9);
                disconnect();
                connect();
                setIsReconnectNeeded(false);
            }
        } finally {
            if (acquiredLock) {
                unlock();
            }
        }
    }

    private void clearPrinterBuffer(){
        new Thread(() -> {
            try {
                POSPrinter posPrinter;
                synchronized (posPrinter = dynamicPrinter.getDevice()) {
                    String selectReceipt = "\u001B" + "c" + "0" + "\u0001";
                    posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, selectReceipt);
                    String init = "\u001B" + "@";
                    posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, init);
                }
            } catch (JposException jposException) {
                log.failure("Printer Failed to clear buffer: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended(), 18, jposException);
            }
        }).start();
    }

    /**
     * This method is only used to set 'ref' for unit testing
     * @param ref
     */
    public void setRef(int ref) {
        this.ref[0] = ref;
    }

    /**
     * Checks to see if receipt paper is empty or not
     * @throws JposException
     */
    public boolean paperEmptyCheck() throws JposException {
        POSPrinter printer;
        synchronized (printer = dynamicPrinter.getDevice()) {
            if (printer.getPhysicalDeviceName().contains(R5PrinterName)) {
                printer.directIO(105, ref, null);
                if (this.ref[0] == -2147482880) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gives the cases in which the printer status updates.
     * @param statusUpdateEvent gets the status of device.
     */
    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        log.success("statusUpdateOccurred(): " + statusUpdateEvent.getStatus(), 1);
        int status = statusUpdateEvent.getStatus();
        PrinterErrorHandlingSingleton printerErrorHandlingSingleton = PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton();
        switch (status) {
            case JposConst.JPOS_SUE_POWER_OFF:
            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
            case JposConst.JPOS_SUE_POWER_OFFLINE:
                log.failure("Printer Status Update: Power offline", 18, null);
                deviceConnected = false;
                break;
            case JposConst.JPOS_SUE_POWER_ONLINE:
                log.success("Printer Status Update: Power offline", 5);
                deviceConnected = true;
                break;
            case POSPrinterConst.PTR_SUE_COVER_OPEN:
                log.success("Status Update: Printer cover is open", 13);
                setWasDoorOpened(true);
                setIsReconnectNeeded(false);
                deviceListener.statusUpdateOccurred(statusUpdateEvent);
                break;
            case POSPrinterConst.PTR_SUE_COVER_OK:
                log.success("Status Update: Printer cover OK", 5);
                if (printerErrorHandlingSingleton.getError() != null) {
                    printerErrorHandlingSingleton.clearError();
                }
                if (getWasDoorOpened()) {
                    setIsReconnectNeeded(true);
                    setWasDoorOpened(false);
                }
                break;
            case POSPrinterConst.PTR_SUE_REC_EMPTY:
                log.success("Status Update: Receipt paper is empty", 13);
                if (printerErrorHandlingSingleton.getError() == null) {
                    printerErrorHandlingSingleton.setError(new PrinterException(PrinterError.OUT_OF_PAPER));
                }
                log.success("SINGLETON: " + PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton().getError(), 5);
                setWasPaperEmpty(true);
                setIsReconnectNeeded(false);
                break;
            case POSPrinterConst.PTR_SUE_REC_NEAREMPTY:
                log.success("Status Update: Receipt printer paper near empty", 5);
                if (getWasPaperEmpty()) {
                    clearPrinterBuffer();
                    setWasPaperEmpty(false);
                }
                break;
            case POSPrinterConst.PTR_SUE_REC_PAPEROK:
                log.success("Status Update: Receipt paper OK", 5);
                clearPrinterBuffer();
                if (printerErrorHandlingSingleton.getError() != null) {
                    printerErrorHandlingSingleton.clearError();
                }
                if (getWasPaperEmpty()) {
                    setIsReconnectNeeded(true);
                    setWasPaperEmpty(false);
                }
                break;
            case POSPrinterConst.PTR_SUE_SLP_EMPTY:
                log.success("Status Update: No check present", 5);
                setIsCheckInserted(false);
                break;
            case POSPrinterConst.PTR_SUE_SLP_PAPEROK:
                log.success("Status Update: Check inserted", 5);
                setIsCheckInserted(true);
                break;
            default:
                break;
        }
    }

    /**
     * Listens for device event.
     */
    private void attachEventListeners() {
        POSPrinter printer;
        synchronized (printer = dynamicPrinter.getDevice()) {
            printer.addErrorListener(deviceListener);
            printer.addOutputCompleteListener(deviceListener);
            printer.addStatusUpdateListener(this);
        }
    }

    /**
     * Removes the error, output and status listeners
     */
    private void detachDeviceListeners() {
        POSPrinter printer;
        synchronized (printer = dynamicPrinter.getDevice()) {
            printer.removeErrorListener(deviceListener);
            printer.removeOutputCompleteListener(deviceListener);
            printer.removeStatusUpdateListener(this);
        }
    }

    /**
     * Lock the current resource.
     * @return
     */
    public boolean tryLock() {
        try {
            isLocked = connectLock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.SECONDS);
            log.success("Lock: " + isLocked, 1);
            if (isLocked) {
                lockOwnerThread.set(Thread.currentThread());
            }
        } catch (InterruptedException interruptedException) {
            log.failure("Lock Failed: " + interruptedException.getMessage(), 17, interruptedException);
        }
        return isLocked;
    }

    /**
     * unlock the current resource.
     */
    public void unlock() {
        isLocked = false;
        connectLock.unlock();
        lockOwnerThread.set(null);
    }

    public void forceUnlock() {
        log.failure("forceUnlock() called — interrupting stuck worker thread and disconnecting device", 17, null);
        Thread owner = lockOwnerThread.get();

        if (owner == null) {
            log.failure("forceUnlock: worker already finished — no action needed", 5, null);
            deviceConnected = false;
            areListenersAttached = false;
            return;
        }

        if (owner.isAlive()) {
            interruptedByTimeout = true;
            owner.interrupt();
        }
        Thread disconnectThread = new Thread(() -> {
            if (lockOwnerThread.get() != owner) {
                log.failure("forceUnlock: skipping disconnect — lock owner changed, no longer stuck", 5, null);
                interruptedByTimeout = false;
                recoveryInProgress.set(false);
                return;
            }
            if (!recoveryInProgress.compareAndSet(false, true)) {
                log.failure("forceUnlock: inner timeout recovery already in progress — skipping duplicate disconnect", 5, null);
                interruptedByTimeout = false;
                return;
            }
            try {
                dynamicPrinter.disconnect();
                log.failure("forceUnlock: background disconnect completed", 17, null);
            } catch (Exception ex) {
                log.failure("forceUnlock: dynamicPrinter.disconnect() threw: " + ex.getMessage(), 17, null);
            }

            boolean reconnected = false;
            for (int attempt = 0; attempt < 3; attempt++) {
                if (tryLock()) {
                    try {
                        log.failure("forceUnlock: reconnecting printer (attempt " + attempt + ")", 17, null);
                        boolean success = connect();
                        if (success) {
                            log.failure("forceUnlock: printer reconnected successfully", 5, null);
                            reconnected = true;
                        } else {
                            log.failure("forceUnlock: reconnect attempt " + attempt + " failed", 17, null);
                        }
                    } finally {
                        unlock();
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.failure("forceUnlock: reconnect polling interrupted", 17, null);
                    break;
                }
            }
            if (!reconnected) {
                log.failure("forceUnlock: printer did not reconnect — @Scheduled connect() will retry", 17, null);
            }
            interruptedByTimeout = false;
            recoveryInProgress.set(false);
        }, "printer-force-disconnect");
        disconnectThread.setDaemon(true);
        disconnectThread.start();

        isLocked = false;
        deviceConnected = false;
        areListenersAttached = false;
    }

    /**
     * This method is only used to get "isLocked" for unit testing
     * @return
     */
    public boolean getIsLocked() {
        return isLocked;
    }

    public int getTryLockTimeout() {
        return TRY_LOCK_TIMEOUT;
    }
}

