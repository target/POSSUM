package com.target.devicemanager.components.check.entities;

import com.target.devicemanager.common.entities.DeviceError;
import org.springframework.http.HttpStatus;

public class MicrError extends DeviceError {

    public static final MicrError BAD_DATA = new MicrError("BAD_DATA", "MICR detected bad check", HttpStatus.BAD_REQUEST);
    public static final MicrError HARDWARE_ERROR = new MicrError("HARDWARE_ERROR", "CHECK FOR PAPER JAM / COVER OPEN",  HttpStatus.NOT_FOUND);
    public static final MicrError CLIENT_CANCELLED_REQUEST = new MicrError("CLIENT_CANCELLED_REQUEST", "Client cancelled the request",  HttpStatus.NO_CONTENT);


    public MicrError(String code, String description, HttpStatus statusCode) {
        super(code, description, statusCode);
    }
}
