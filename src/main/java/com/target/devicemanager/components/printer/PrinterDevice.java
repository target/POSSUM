package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.components.printer.entities.*;
import jpos.JposConst;
import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.util.Base64Utils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PrinterDevice implements StatusUpdateListener{

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
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    private final int[] ref = new int[1];
    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterDevice.class);
    private static final Marker MARKER = MarkerFactory.getMarker("FATAL");

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
            LOGGER.error(MARKER, "Printer Failed in Constructor: dynamicPrinter cannot be null");
            throw new IllegalArgumentException("dynamicPrinter cannot be null");
        }
        if (deviceListener == null) {
            LOGGER.error(MARKER, "Printer Failed in Constructor: deviceListener cannot be null");
            throw new IllegalArgumentException("deviceListener cannot be null");
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
        /*NCR Devices throws exception when setDeviceEnabled is called when device is not connected.
        Enable the device when device is connected so that we can get status update events.
        When device is disabled we will not get the status events*/
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
                LOGGER.error(getDeviceName() + " Unable to disable : " + jposException.getMessage());
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
            LOGGER.error(MARKER, "Printer Failed to Enable Device: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
            throw jposException;
        }
        deviceListener.startEventListeners();
    }

    /**
     * Prints the content on the receipt.
     * @param contents the image on receipt.
     * @param printerStation register where printing occurs.
     * @throws JposException, PrinterException
     */

    public Void printContent(List<PrinterContent> contents, int printerStation) throws JposException, PrinterException {
        LOGGER.debug("printContent()");
        if(tryLock()) {
            if (contents == null || contents.isEmpty()) {
                LOGGER.debug("Receipt contents are empty");
                return null;
            }
            enable();
            POSPrinter printer;
            synchronized (printer = dynamicPrinter.getDevice()) {
                try {
                    if (printerStation != PrinterStationType.CHECK_PRINTER.getValue() && (wasPaperEmpty || paperEmptyCheck())) {
                        // Throw JPOS extended error JPOS_EPTR_REC_EMPTY
                        throw new JposException(114, 203);
                    }
                    reconnectR5Printer();
                    printer.transactionPrint(printerStation, POSPrinterConst.PTR_TP_TRANSACTION);
                    for (PrinterContent content : contents) {
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
                    printer.transactionPrint(printerStation, POSPrinterConst.PTR_TP_NORMAL);
                    deviceListener.waitForOutputToComplete();

                } catch (JposException jposException) {
                    LOGGER.error(MARKER, "Printer Failed to Print Content: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());

                    boolean failureOrDisabledError = jposException.getErrorCode() == 111 || jposException.getErrorCode() == 105;
                    boolean badPrintContentError = jposException.getErrorCode() == 106 || (jposException.getErrorCode() == 114 && jposException.getErrorCodeExtended() == 207);
                    // If failureOrDisabledError or badPrintContentError occur, disconnect and reconnect the printer then throw exception
                    if ((failureOrDisabledError || badPrintContentError)) {
                        LOGGER.error("Received Printer " + jposException.getErrorCode() + " error.  Disconnecting device.");
                        disconnect();
                        LOGGER.error("Received Printer " + jposException.getErrorCode() + " error.  Reconnecting device.");
                        connect();
                        if (badPrintContentError) {
                            throw new PrinterException(PrinterError.INVALID_FORMAT);
                        }
                    }
                    throw jposException;
                } finally {
                    JposException jposException = null;
                    try {
                        printer.clearOutput();
                    } catch (JposException exception) {
                        LOGGER.error("Received printer " + exception.getErrorCode() + " error during clearOutput()");
                        jposException = exception;
                    }
                    // if an exception is thrown during print, make sure check is spit out.
                    if (getIsCheckInserted()) {
                        try {
                            withdrawCheck();
                        } catch (JposException exception) {
                            LOGGER.error("Received printer " + exception.getErrorCode() + " error during withdrawCheck()");
                            if (jposException == null) {
                                jposException = exception;
                            }
                        }
                    }
                    unlock();
                    if (jposException != null) {
                        throw jposException;
                    }
                }
            }
            return null;
        } else {
            throw new PrinterException(PrinterError.PRINTER_BUSY);
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
            LOGGER.error(MARKER, "Printer Failed to Print Barcode: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
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
                Base64Utils.decodeFromString(content.data),
                content.imageFormatType.getValue(),
                POSPrinterConst.PTR_BM_ASIS,
                POSPrinterConst.PTR_BM_CENTER);
        } catch (JposException jposException) {
            LOGGER.error(MARKER, "Printer Failed to Print Image: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
            throw jposException;
        }
    }

    /**
     *Prints the message to the customer on the receipt.
     * @param printer POS printer.
     * @param data The message to customer.
     * @param printerStation register where printing occurs
     * @throws JposException
     */
    private void print(POSPrinter printer, String data, int printerStation) throws JposException {
        try {
            printer.printNormal(printerStation, data);
        } catch (JposException jposException) {
            LOGGER.error(MARKER, "Printer Failed to Print Data: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
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
            LOGGER.error(MARKER, "Printer Failed to Remove Check: " + jposException.getErrorCode() + ", " + jposException.getErrorCodeExtended());
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
        POSPrinter printer;
        synchronized (printer = dynamicPrinter.getDevice()) {
            if(printer.getPhysicalDeviceName().contains(R5PrinterName) && getIsReconnectNeeded()) {
                LOGGER.info("Reconnecting R5 printer");
                disconnect();
                connect();
                setIsReconnectNeeded(false);
            }
        }
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
                // The particular R5 kiosk printer uses this reference number to check whether the paper is empty in the printer
                if (this.ref[0] == -2147482880) {
                    LOGGER.info("paperEmptyCheck(): true");
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
        LOGGER.trace("statusUpdateOccurred(): " + statusUpdateEvent.getStatus());
        int status = statusUpdateEvent.getStatus();
        PrinterErrorHandlingSingleton printerErrorHandlingSingleton = PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton();
        switch (status) {
            case JposConst.JPOS_SUE_POWER_OFF:
            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
            case JposConst.JPOS_SUE_POWER_OFFLINE:
                LOGGER.error(MARKER, "Printer Status Update: Power offline");
                LOGGER.error("Printer: power offline");
                deviceConnected = false;
                break;
            case JposConst.JPOS_SUE_POWER_ONLINE:
                LOGGER.debug("Status Update: Power online");
                deviceConnected = true;
                break;
            case POSPrinterConst.PTR_SUE_COVER_OPEN:
                LOGGER.warn("Status Update: Printer cover is open");
                setWasDoorOpened(true);
                setIsReconnectNeeded(false);
                break;
            case POSPrinterConst.PTR_SUE_COVER_OK:
                LOGGER.debug("Status Update: Printer cover OK");
                if (printerErrorHandlingSingleton.getError() != null) {
                    printerErrorHandlingSingleton.clearError();
                }
                if (getWasDoorOpened()) {
                    setIsReconnectNeeded(true);
                    setWasDoorOpened(false);
                }
                break;
            case POSPrinterConst.PTR_SUE_REC_EMPTY:
                LOGGER.warn("Status Update: Receipt paper is empty");
                if (printerErrorHandlingSingleton.getError() == null) {
                    printerErrorHandlingSingleton.setError(new PrinterException(PrinterError.OUT_OF_PAPER));
                }
                LOGGER.info("SINGLETON: " + PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton().getError());
                setWasPaperEmpty(true);
                setIsReconnectNeeded(false);
                break;
            case POSPrinterConst.PTR_SUE_REC_NEAREMPTY:
                LOGGER.warn("Status Update: Receipt printer paper near empty");
                break;
            case POSPrinterConst.PTR_SUE_REC_PAPEROK:
                LOGGER.debug("Status Update: Receipt paper OK");
                if (printerErrorHandlingSingleton.getError() != null) {
                    printerErrorHandlingSingleton.clearError();
                }
                if (getWasPaperEmpty()) {
                    setIsReconnectNeeded(true);
                    setWasPaperEmpty(false);
                }
                break;
            case POSPrinterConst.PTR_SUE_SLP_EMPTY:
                LOGGER.debug("Status Update: No check present");
                setIsCheckInserted(false);
                break;
            case POSPrinterConst.PTR_SUE_SLP_PAPEROK:
                LOGGER.debug("Status Update: Check inserted");
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

    public int getTryLockTimeout() {
        return TRY_LOCK_TIMEOUT;
    }
}
