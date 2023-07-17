package com.target.devicemanager.components.scale.entities;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import jpos.JposConst;
import jpos.JposException;
import jpos.ScaleConst;

public class ScaleException extends DeviceException {
    private static final long serialVersionUID = 1L;

    public ScaleException(DeviceError scaleError) {
        super(scaleError);
    }

    public ScaleException(JposException originalException) {
        causedBy = originalException;
        //Scale specific error codes not defined in JposConst
        final int SCALE_NEEDS_ZEROING_CODE_EXTENDED = -108;

        errorCodeMap.put(JposConst.JPOS_E_TIMEOUT, ScaleError.TIMEOUT);
        errorCodeMap.put(SCALE_NEEDS_ZEROING_CODE_EXTENDED, ScaleError.NEEDS_ZEROING);
        errorCodeMap.put(ScaleConst.JPOS_ESCAL_UNDER_ZERO, ScaleError.WEIGHT_UNDER_ZERO);
        errorCodeMap.put(ScaleConst.JPOS_ESCAL_OVERWEIGHT, ScaleError.OVER_WEIGHT);

        //extended must be first as they overloaded the failure exception for the scale :(
        super.deviceError = errorCodeMap.getOrDefault(originalException.getErrorCodeExtended(),
                errorCodeMap.getOrDefault(originalException.getErrorCode(), DeviceError.UNEXPECTED_ERROR));
    }
}
