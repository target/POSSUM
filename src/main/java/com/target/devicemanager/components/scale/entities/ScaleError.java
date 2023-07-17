package com.target.devicemanager.components.scale.entities;

import com.target.devicemanager.common.entities.DeviceError;
import org.springframework.http.HttpStatus;

public class ScaleError extends DeviceError {

    public static final ScaleError WEIGHT_UNDER_ZERO = new ScaleError("WEIGHT_UNDER_ZERO","Scale weight under zero", HttpStatus.PRECONDITION_FAILED);
    public static final ScaleError NEEDS_ZEROING = new ScaleError("NEEDS_ZEROING","Scale needs zeroing", HttpStatus.PRECONDITION_FAILED);
    public static final ScaleError OVER_WEIGHT = new ScaleError("OVER_WEIGHT","Scale is overweight", HttpStatus.PRECONDITION_FAILED);
    public static final ScaleError TIMEOUT = new ScaleError("TIMEOUT","Scale read timed out", HttpStatus.REQUEST_TIMEOUT);

    public ScaleError(String code, String description, HttpStatus statusCode) {
        super(code, description, statusCode);
    }
}
