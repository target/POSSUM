package com.target.devicemanager.components.scanner.entities;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import jpos.JposConst;
import jpos.JposException;

public class ScannerException extends DeviceException {
    private static final long serialVersionUID = 1L;

    public ScannerException(DeviceError scannerError) {
        super(scannerError);
    }

    public ScannerException(JposException originalException) {
        causedBy = originalException;

        errorCodeMap.put(JposConst.JPOS_E_DISABLED, ScannerError.DISABLED);
        errorCodeMap.put(JposConst.JPOS_E_TIMEOUT, ScannerError.DISABLED);
        errorCodeMap.put(JposConst.JPOS_E_FAILURE, ScannerError.UNEXPECTED_ERROR);
        errorCodeMap.put(JposConst.JPOS_E_ILLEGAL, ScannerError.UNEXPECTED_ERROR);
        errorCodeMap.put(JposConst.JPOS_E_CLOSED, ScannerError.DEVICE_OFFLINE);

        super.deviceError = errorCodeMap.getOrDefault(originalException.getErrorCode(),
                errorCodeMap.getOrDefault(originalException.getErrorCodeExtended(), DeviceError.UNEXPECTED_ERROR));
    }
}
