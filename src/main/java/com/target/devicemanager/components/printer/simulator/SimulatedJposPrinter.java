package com.target.devicemanager.components.printer.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.printer.PrinterManager;
import com.target.devicemanager.components.printer.entities.PrinterStationType;
import jpos.JposConst;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

public class SimulatedJposPrinter extends POSPrinter  {

    private SimulatorState simulatorState;
    private SimulatedPrintResult simulatedPrintResult;
    private SimulatedCheckPrintResult simulatedCheckPrintResult;
    private int statusUpdateStatus = JposConst.JPOS_SUE_POWER_OFF_OFFLINE;
    private final String simulatedPrinterName = "Simulated Printer";

    public SimulatedJposPrinter() {
        simulatorState = SimulatorState.ONLINE;
        simulatedPrintResult = SimulatedPrintResult.PRINT_COMPLETE;
        simulatedCheckPrintResult = SimulatedCheckPrintResult.PRINT_COMPLETE;
    }

    void setPrintResult(SimulatedPrintResult simulatedPrintResult) {
        this.simulatedPrintResult = simulatedPrintResult;
        switch (simulatedPrintResult) {
            case PRINT_COMPLETE:
                if (statusUpdateStatus == POSPrinterConst.PTR_SUE_COVER_OPEN) {
                    statusUpdateStatus = POSPrinterConst.PTR_SUE_COVER_OK;
                    triggerStatusUpdateEvent();
                    statusUpdateStatus = POSPrinterConst.PTR_SUE_REC_PAPEROK;
                    triggerStatusUpdateEvent();
                }
                if (statusUpdateStatus == POSPrinterConst.PTR_SUE_REC_EMPTY) {
                    statusUpdateStatus = POSPrinterConst.PTR_SUE_REC_PAPEROK;
                    triggerStatusUpdateEvent();
                }
                break;
            case COVER_OPEN:
                if (statusUpdateStatus == POSPrinterConst.PTR_SUE_REC_EMPTY) {
                    statusUpdateStatus = POSPrinterConst.PTR_SUE_REC_PAPEROK;
                    triggerStatusUpdateEvent();
                }
                statusUpdateStatus = POSPrinterConst.PTR_SUE_COVER_OPEN;
                triggerStatusUpdateEvent();
                break;
            case OUT_OF_PAPER:
                statusUpdateStatus = POSPrinterConst.PTR_SUE_REC_EMPTY;
                triggerStatusUpdateEvent();
                break;
            default:
                break;
        }
    }

    public void setCheckPrintResult(SimulatedCheckPrintResult simulatedCheckPrintResult){
        this.simulatedCheckPrintResult = simulatedCheckPrintResult;
        switch (simulatedCheckPrintResult) {
            case PRINT_COMPLETE:
                if (statusUpdateStatus == POSPrinterConst.PTR_SUE_COVER_OPEN) {
                    statusUpdateStatus = POSPrinterConst.PTR_SUE_COVER_OK;
                    triggerStatusUpdateEvent();
                }
                break;
            case COVER_OPEN:
                statusUpdateStatus = POSPrinterConst.PTR_SUE_COVER_OPEN;
                triggerStatusUpdateEvent();
                break;
            default:
                break;
        }
    }

    public void setState(SimulatorState simulatorState) {
        statusUpdateStatus = simulatorState.getStatus();
        this.simulatorState = simulatorState;

        triggerStatusUpdateEvent();
    }

    private void triggerOutputCompleteEvent() {

        OutputCompleteEvent outputCompleteEvent = new OutputCompleteEvent(this, 1);

        for (Object object : outputCompleteListeners) {
            OutputCompleteListener outputCompleteListener = (OutputCompleteListener) object;
            outputCompleteListener.outputCompleteOccurred(outputCompleteEvent);
        }
    }

    private void triggerStatusUpdateEvent() {

        StatusUpdateEvent statusUpdateEvent = new StatusUpdateEvent(this, statusUpdateStatus);
        for (Object object : statusUpdateListeners) {
            StatusUpdateListener statusUpdateListener = (StatusUpdateListener) object;
            statusUpdateListener.statusUpdateOccurred(statusUpdateEvent);
        }
    }

    private void triggerErrorEvent() {

        ErrorEvent errorEvent = new ErrorEvent(new Object(),JposConst.JPOS_E_EXTENDED, statusUpdateStatus, 0, 0);

        for (Object object : errorListeners) {
            ErrorListener errorListener = (ErrorListener) object;
            errorListener.errorOccurred(errorEvent);
        }
    }

    @Override
    public String getPhysicalDeviceName() {
        return simulatedPrinterName;
    }

    @Override
    public int getState() {
        return simulatorState == SimulatorState.ONLINE ? JposConst.JPOS_S_IDLE : JposConst.JPOS_S_CLOSED;
    }

    @Override
    public void setDeviceEnabled(boolean value) {
        //do nothing
    }

    @Override
    public boolean getDeviceEnabled() {
        return false;
    }

    @Override
    public void setAsyncMode(boolean value) {
        //do nothing
    }

    @Override
    public void transactionPrint(int printerType, int transactionType) {
        if (transactionType == POSPrinterConst.PTR_TP_NORMAL) {
            if (printerType == PrinterStationType.RECEIPT_PRINTER.getValue()) {
                switch (simulatedPrintResult) {
                    case PRINT_COMPLETE:
                        triggerOutputCompleteEvent();
                        break;
                    case COVER_OPEN:
                        statusUpdateStatus = POSPrinterConst.PTR_SUE_COVER_OPEN;
                        triggerErrorEvent();
                        break;
                    case OUT_OF_PAPER:
                        statusUpdateStatus = POSPrinterConst.PTR_SUE_REC_EMPTY;
                        triggerErrorEvent();
                        break;
                    case TIME_OUT:
                        try {
                            Thread.sleep((1000 * PrinterManager.getPrinterTimeoutValue()) + 30000); // Add 30s to the timeout value to ensure the timeout occurs and the printer busy behavior can be seen.
                        } catch (InterruptedException interruptedException) {
                            //ignore error
                        }
                        triggerOutputCompleteEvent();
                        break;
                    default:
                        break;
                }
            } else {
                switch (simulatedCheckPrintResult) {
                    case PRINT_COMPLETE:
                        triggerOutputCompleteEvent();
                        break;
                    case COVER_OPEN:
                        statusUpdateStatus = POSPrinterConst.PTR_SUE_COVER_OPEN;
                        triggerErrorEvent();
                        break;
                    case TIME_OUT:
                        statusUpdateStatus = JposConst.JPOS_E_TIMEOUT;
                        triggerErrorEvent();
                        break;
                    case DEVICE_BUSY:
                        statusUpdateStatus = JposConst.JPOS_E_BUSY;
                        triggerErrorEvent();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void clearOutput() {
        //do nothing
    }

    @Override
    public void printNormal(int printerType, String data) {
        //do nothing
    }

    @Override
    public void printMemoryBitmap(int printerType, byte[] bytes, int imageType, int width, int alignment) {
        //do nothing
    }

    @Override
    public void printBarCode(int printerType, String data, int barcodeType, int height, int width, int alignment, int textLocation) {
        //do nothing
    }

    @Override
    public void close(){
        //do nothing
    }

}
