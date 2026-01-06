package com.target.devicemanager.common;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ExceptionController extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionController.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("Common", "ExceptionController", LOGGER);

    @ExceptionHandler(DeviceException.class)
    public ResponseEntity<DeviceError> handleDeviceException(DeviceException exception) {
        log.failure("Device Exception: " + exception, 1, exception);
        return new ResponseEntity<>(exception.getDeviceError(), exception.getDeviceError().getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DeviceError> handleAllExceptions(Exception exception) {
        DeviceError unknownErr = new DeviceError("Unhandled Exception", exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        log.failure("Unhandled Exception: " + unknownErr, 18, exception);
        return new ResponseEntity<>(unknownErr, unknownErr.getStatusCode());
    }
}
