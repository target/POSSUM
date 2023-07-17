package com.target.devicemanager.components.check.entities;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import jpos.JposConst;
import jpos.JposException;
import jpos.MICRConst;

public class MicrException extends DeviceException {
    private static final long serialVersionUID = 1L;

    public MicrException(DeviceError micrError) {
        super(micrError);
    }

    public MicrException(JposException originalException) {
        causedBy = originalException;

        errorCodeMap.put(MICRConst.JPOS_EMICR_BADDATA, MicrError.BAD_DATA);
        errorCodeMap.put(MICRConst.JPOS_EMICR_NODATA, MicrError.BAD_DATA);
        errorCodeMap.put(MICRConst.JPOS_EMICR_BADSIZE, MicrError.BAD_DATA);
        errorCodeMap.put(MICRConst.JPOS_EMICR_CHECKDIGIT, MicrError.BAD_DATA);
        errorCodeMap.put(MICRConst.JPOS_EMICR_COVEROPEN, MicrError.HARDWARE_ERROR);
        errorCodeMap.put(MICRConst.JPOS_EMICR_JAM, MicrError.HARDWARE_ERROR);
        errorCodeMap.put(JposConst.JPOS_E_TIMEOUT, MicrError.CLIENT_CANCELLED_REQUEST);


        super.deviceError = errorCodeMap.getOrDefault(originalException.getErrorCodeExtended(),
                errorCodeMap.getOrDefault(originalException.getErrorCode(), DeviceError.UNEXPECTED_ERROR));
    }
}