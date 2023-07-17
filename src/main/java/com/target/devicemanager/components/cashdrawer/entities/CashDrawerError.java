package com.target.devicemanager.components.cashdrawer.entities;

import com.target.devicemanager.common.entities.DeviceError;
import org.springframework.http.HttpStatus;

public class CashDrawerError extends DeviceError {
    public static final DeviceError ALREADY_OPEN = new CashDrawerError("ALREADY_OPEN","Already open", HttpStatus.PRECONDITION_FAILED);
    public static final DeviceError OPEN_FAILED = new CashDrawerError("OPEN_FAILED", "Failed to open", HttpStatus.PRECONDITION_FAILED);

    public CashDrawerError(String code, String description, HttpStatus statusCode) {
        super(code, description, statusCode);
    }
}