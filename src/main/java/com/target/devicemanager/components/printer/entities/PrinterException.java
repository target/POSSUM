package com.target.devicemanager.components.printer.entities;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import jpos.JposConst;
import jpos.JposException;
import jpos.POSPrinterConst;

public class PrinterException extends DeviceException {
    private static final long serialVersionUID = 1L;

    public PrinterException(DeviceError deviceError) {
        super(deviceError);
    }

    public PrinterException(JposException originalException) {

        causedBy = originalException;
        errorCodeMap.put(POSPrinterConst.PTR_SUE_COVER_OPEN, PrinterError.COVER_OPEN);
        errorCodeMap.put(POSPrinterConst.JPOS_EPTR_COVER_OPEN, PrinterError.COVER_OPEN);
        errorCodeMap.put(POSPrinterConst.JPOS_EPTR_REC_EMPTY, PrinterError.OUT_OF_PAPER);
        errorCodeMap.put(POSPrinterConst.PTR_SUE_REC_EMPTY, PrinterError.OUT_OF_PAPER);
        errorCodeMap.put(JposConst.JPOS_E_ILLEGAL, PrinterError.ILLEGAL_OPERATION);
        errorCodeMap.put(JposConst.JPOS_E_TIMEOUT, PrinterError.MICR_TIME_OUT);
        errorCodeMap.put(POSPrinterConst.JPOS_EPTR_SLP_EMPTY, PrinterError.INVALID_FORMAT);

        super.deviceError = errorCodeMap.getOrDefault(originalException.getErrorCode(),
                errorCodeMap.getOrDefault(originalException.getErrorCodeExtended(), DeviceError.UNEXPECTED_ERROR));
    }
}
