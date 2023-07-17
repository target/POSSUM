package com.target.devicemanager.components.printer.entities;

import com.target.devicemanager.common.entities.DeviceErrorStatusResponse;

public class PrinterErrorHandlingSingleton {

    private static final PrinterErrorHandlingSingleton printerErrorHandlingSingleton = new PrinterErrorHandlingSingleton();
    private PrinterException PrinterException = null;

    private PrinterErrorHandlingSingleton() {
        //do nothing at the moment
    }

    public static PrinterErrorHandlingSingleton getPrinterErrorHandlingSingleton() {
        return printerErrorHandlingSingleton;
    }

    public PrinterException getError() {
        return this.PrinterException;
    }

    public void setError(PrinterException PrinterException) {
        this.PrinterException = PrinterException;
    }

    public void clearError() {
        DeviceErrorStatusResponse.sendClearError();
        this.PrinterException = null;
    }
}
