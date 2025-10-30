package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.printer.entities.PrinterContent;
import com.target.devicemanager.components.printer.entities.PrinterError;
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
        LOGGER.info("request: " + url);
        try {
            if(contents.size() < PRINT_CONTENT_SIZE){
                printerManager.printReceipt(contents);
                LOGGER.info("response: " + url + " - 200 OK");
            } else {
                LOGGER.info("Printer print content more than expected limit");
            }
        }
        catch (DeviceException deviceException) {
            LOGGER.info("response: " + url + " - "+ deviceException.getDeviceError().getStatusCode().toString());
            throw deviceException;
        }
    }

    @Operation(description = "Reports printer health")
    @GetMapping(path = "/printer/health")
    public DeviceHealthResponse getHealth() {
        String url = "/v1/printer/health";
        LOGGER.info("request: " + url);
        DeviceHealthResponse response = printerManager.getHealth();
        LOGGER.info("response: " + url + " - " + response.toString());
        return response;
    }

    @Operation(description = "Reports printer status")
    @GetMapping(path = "/printer/healthstatus")
    public DeviceHealthResponse getStatus() {
        String url = "/v1/printer/healthstatus";
        LOGGER.info("response: " + url);
        DeviceHealthResponse response = printerManager.getStatus();
        LOGGER.info("response: " + url + " - " + response.toString());
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
        LOGGER.info("request: " + url);
        try {
            printerManager.reconnectDevice();
            LOGGER.info("response: " + url + " - 200 OK");
        } catch (DeviceException deviceException) {
            LOGGER.info("response: " + url + " - " + deviceException.getDeviceError().getStatusCode().toString() + ", " + deviceException.getDeviceError());
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
