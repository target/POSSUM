package com.target.devicemanager.components.scanner.entities;

import com.target.devicemanager.common.entities.DeviceError;
import org.springframework.http.HttpStatus;

public class ScannerError extends DeviceError {
    public static final ScannerError DISABLED = new ScannerError("DISABLED", "Scan request canceled", HttpStatus.NO_CONTENT);
    public static final ScannerError ALREADY_DISABLED = new ScannerError("ALREADY_DISABLED", "Scan not in progress. Nothing to delete.", HttpStatus.PRECONDITION_FAILED);

    public ScannerError(String code, String description, HttpStatus statusCode) {
        super(code, description, statusCode);
    }
}
