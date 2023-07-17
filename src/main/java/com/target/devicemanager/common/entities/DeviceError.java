package com.target.devicemanager.common.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.http.HttpStatus;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceError {

    public static final DeviceError UNEXPECTED_ERROR = new DeviceError("UNEXPECTED_ERROR", "Internal Device Error", HttpStatus.INTERNAL_SERVER_ERROR);
    public static final DeviceError DEVICE_OFFLINE = new DeviceError("DEVICE_OFFLINE", "Device is offline/not connected to register", HttpStatus.NOT_FOUND);
    public static final DeviceError DEVICE_BUSY = new DeviceError("DEVICE_BUSY", "Wait, then try again.", HttpStatus.CONFLICT);
    public static final DeviceError UNKNOWN_DEVICE = new DeviceError("UNKNOWN_DEVICE", "Unknown device type.", HttpStatus.BAD_REQUEST);
    public static final DeviceError SSE_TIMEOUT = new DeviceError("SSE_TIMEOUT", "Timeout reached", HttpStatus.REQUEST_TIMEOUT);
    public static final DeviceError BAD_INPUT = new DeviceError("BAD_INPUT", "Invalid data", HttpStatus.BAD_REQUEST);
    public static final DeviceError REMOVE_CASH = new DeviceError("REMOVE_CASH", "Remove dispensed bill.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String description;
    private final HttpStatus statusCode;

    public DeviceError(String code, String description, HttpStatus statusCode) {
        this.code = code;
        this.description = description;
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @JsonIgnore
    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public String toString() {
        return "DeviceError{" +
                "description='" + description + '\'' +
                ", code='" + code + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
