package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.LogPayloadBuilder;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.printer.entities.PrinterContent;
import com.target.devicemanager.components.printer.entities.PrinterError;
import com.target.devicemanager.common.entities.LogField;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping(value = "/v1")
@Tag(name = "Printer")
@Timed
public class PrinterController {

    private final PrinterManager printerManager;
    private final int PRINT_CONTENT_SIZE = 64; // Assumes maximum allowed array size for the printer is 64
    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterController.class);

    @Autowired
    public PrinterController(PrinterManager printerManager) {
        if (printerManager == null) {
            throw new IllegalArgumentException("printerManager cannot be null");
        }
        this.printerManager = printerManager;
    }

    @Operation(description = "Sends entities to attached printer")
    @PostMapping(value = "/print")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "COVER_OPEN, OUT_OF_PAPER, INVALID_FORMAT",
                    content = @Content(schema = @Schema(implementation = PrinterError.class))),
            @ApiResponse(responseCode = "408", description = "PRINTER_TIME_OUT",
                    content = @Content(schema = @Schema(implementation = PrinterError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void print(@Parameter(description = "Receipt entities") 
                          @Valid @RequestBody List<PrinterContent> contents) throws DeviceException {
        String url = "/v1/print";
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "Printer")
            .add(LogField.EVENT_SEVERITY, 9)
            .add(LogField.COMPONENT, "print")
            .add(LogField.EVENT_ACTION, "print")
            .add(LogField.MESSAGE, "API Request Received")
            .logInfo(LOGGER);
        try {
            if(contents.size() < PRINT_CONTENT_SIZE){
                printerManager.printReceipt(contents);
                new LogPayloadBuilder()
                    .add(LogField.URL_PATH, url)
                    .add(LogField.SERVICE_NAME, "Printer")
                    .add(LogField.EVENT_SEVERITY, 9)
                    .add(LogField.COMPONENT, "PrinterController")
                    .add(LogField.EVENT_ACTION, "print")
                    .add(LogField.EVENT_OUTCOME, "success")
                    .add(LogField.HTTP_RESPONSE_STATUS_CODE, 200)
                    .add(LogField.HTTP_RESPONSE_BODY_CONTENT, "OK")
                    .add(LogField.MESSAGE, "API Request Completed Successfully")
                    .logInfo(LOGGER);
            } else {
                new LogPayloadBuilder()
                    .add(LogField.SERVICE_NAME, "Printer")
                    .add(LogField.EVENT_SEVERITY, 17)
                    .add(LogField.COMPONENT, "PrinterController")
                    .add(LogField.EVENT_ACTION, "print")
                    .add(LogField.EVENT_OUTCOME, "failure")
                    .add(LogField.MESSAGE, "Printer print content more than expected limit")
                    .logError(LOGGER);
                 throw new DeviceException(PrinterError.INVALID_FORMAT);
            }
        }
        catch (DeviceException deviceException) {
            new LogPayloadBuilder()
                .add(LogField.URL_PATH, url)
                .add(LogField.SERVICE_NAME, "Printer")
                .add(LogField.EVENT_SEVERITY, 17)
                .add(LogField.COMPONENT, "PrinterController")
                .add(LogField.EVENT_ACTION, "print")
                .add(LogField.EVENT_OUTCOME, "failure")
                .add(LogField.HTTP_RESPONSE_STATUS_CODE, deviceException.getDeviceError().getStatusCode().value())
                .add(LogField.HTTP_RESPONSE_BODY_CONTENT, deviceException.getDeviceError().toString())
                .add(LogField.MESSAGE, "API Request Failed with DeviceException")
                .logError(LOGGER);
            throw deviceException;
        }
    }

    @Operation(description = "Reports printer health")
    @GetMapping(path = "/printer/health")
    public DeviceHealthResponse getHealth() {
        String url = "/v1/printer/health";
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "Printer")
            .add(LogField.EVENT_SEVERITY, 9)
            .add(LogField.COMPONENT, "PrinterController")
            .add(LogField.EVENT_ACTION, "getHealth")
            .add(LogField.MESSAGE, "API Request Received")
            .logInfo(LOGGER);
        DeviceHealthResponse response = printerManager.getHealth();
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "Printer")
            .add(LogField.EVENT_SEVERITY, 9)
            .add(LogField.COMPONENT, "PrinterController")
            .add(LogField.EVENT_ACTION, "getHealth")
            .add(LogField.EVENT_OUTCOME, "success")
            .add(LogField.HTTP_RESPONSE_STATUS_CODE, 200)
            .add(LogField.HTTP_RESPONSE_BODY_CONTENT, response.toString())
            .add(LogField.TAGS, response.getHealthStatus().toString())
            .add(LogField.MESSAGE, "API Request Completed Successfully")
            .logInfo(LOGGER);
        return response;
    }

    @Operation(description = "Reports printer status")
    @GetMapping(path = "/printer/healthstatus")
    public DeviceHealthResponse getStatus() {
        String url = "/v1/printer/healthstatus";
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "Printer")
            .add(LogField.EVENT_SEVERITY, 9)
            .add(LogField.COMPONENT, "PrinterController")
            .add(LogField.EVENT_ACTION, "getStatus")
            .add(LogField.MESSAGE, "API Request Received")
            .logInfo(LOGGER);
        DeviceHealthResponse response = printerManager.getStatus();
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "Printer")
            .add(LogField.EVENT_SEVERITY, 9)
            .add(LogField.COMPONENT, "PrinterController")
            .add(LogField.EVENT_ACTION, "getStatus")
            .add(LogField.EVENT_OUTCOME, "success")
            .add(LogField.HTTP_RESPONSE_STATUS_CODE, 200)
            .add(LogField.HTTP_RESPONSE_BODY_CONTENT, response.toString())
            .add(LogField.TAGS, response.getHealthStatus().toString())
            .add(LogField.MESSAGE, "API Request Completed Successfully")
            .logInfo(LOGGER);
        return response;
    }

    @Operation(description = "Reconnects the printer")
    @PostMapping(path = "/printer/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void reconnect() throws DeviceException {
        String url = "/v1/printer/reconnect";
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "Printer")
            .add(LogField.EVENT_SEVERITY, 9)
            .add(LogField.COMPONENT, "PrinterController")
            .add(LogField.EVENT_ACTION, "reconnect")
            .add(LogField.MESSAGE, "API Request Received")
            .logInfo(LOGGER);
        try {
            printerManager.reconnectDevice();
            new LogPayloadBuilder()
                .add(LogField.URL_PATH, url)
                .add(LogField.SERVICE_NAME, "Printer")
                .add(LogField.EVENT_SEVERITY, 9)
                .add(LogField.COMPONENT, "PrinterController")
                .add(LogField.EVENT_ACTION, "reconnect")
                .add(LogField.EVENT_OUTCOME, "success")
                .add(LogField.HTTP_RESPONSE_STATUS_CODE, 200)
                .add(LogField.HTTP_RESPONSE_BODY_CONTENT, "OK")
                .add(LogField.MESSAGE, "API Request Completed Successfully")
                .logInfo(LOGGER);
        } catch (DeviceException deviceException) {
            new LogPayloadBuilder()
                .add(LogField.URL_PATH, url)
                .add(LogField.SERVICE_NAME, "Printer")
                .add(LogField.EVENT_SEVERITY, 17)
                .add(LogField.COMPONENT, "PrinterController")
                .add(LogField.EVENT_ACTION, "reconnect")
                .add(LogField.EVENT_OUTCOME, "failure")
                .add(LogField.HTTP_RESPONSE_STATUS_CODE, deviceException.getDeviceError().getStatusCode().value())
                .add(LogField.HTTP_RESPONSE_BODY_CONTENT, deviceException.getDeviceError().toString())
                .add(LogField.MESSAGE, "API Request Failed with DeviceException")
                .logError(LOGGER);
            throw deviceException;
        }
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<DeviceError> handleInvalidFormat(HttpMessageNotReadableException originalException) {
        DeviceException printerException = new DeviceException(PrinterError.INVALID_FORMAT);
        return new ResponseEntity<>(printerException.getDeviceError(),
                printerException.getDeviceError().getStatusCode());
    }
}
