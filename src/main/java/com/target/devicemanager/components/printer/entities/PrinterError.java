package com.target.devicemanager.components.printer.entities;

import com.target.devicemanager.common.entities.DeviceError;
import org.springframework.http.HttpStatus;

public class PrinterError extends DeviceError {

    public static final PrinterError COVER_OPEN = new PrinterError("COVER_OPEN","Close cover and try print again.", HttpStatus.BAD_REQUEST);
    public static final PrinterError OUT_OF_PAPER = new PrinterError("OUT_OF_PAPER","Refill paper and try print again.", HttpStatus.BAD_REQUEST);
    public static final PrinterError INVALID_FORMAT = new PrinterError("INVALID_FORMAT","Invalid print contents", HttpStatus.BAD_REQUEST);
    public static final PrinterError MICR_TIME_OUT = new PrinterError("INSERT_CHECK_TIME_OUT ","Insert the check promptly and try again", HttpStatus.REQUEST_TIMEOUT);
    public static final PrinterError ILLEGAL_OPERATION = new PrinterError("ILLEGAL_OPERATION","The printer does not exist or the content is invalid. Check if receipt paper is empty.", HttpStatus.NOT_FOUND);
    public static final PrinterError PRINTER_TIME_OUT = new PrinterError("PRINTER_TIME_OUT", "The printer timed out while trying to print the receipt. Try again.", HttpStatus.REQUEST_TIMEOUT);
    public static final PrinterError PRINTER_BUSY = new PrinterError("PRINTER_BUSY", "The printer is still processing the previous request. Wait for the receipt to print.", HttpStatus.CONFLICT);

    public PrinterError(String code, String description, HttpStatus statusCode) {
        super(code, description, statusCode);
    }
}
