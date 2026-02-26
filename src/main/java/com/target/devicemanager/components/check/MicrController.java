package com.target.devicemanager.components.check;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.check.entities.MicrData;
import com.target.devicemanager.components.check.entities.MicrError;
import com.target.devicemanager.components.check.entities.MicrException;
import com.target.devicemanager.components.printer.PrinterManager;
import com.target.devicemanager.components.printer.entities.PrinterContent;
import com.target.devicemanager.components.printer.entities.PrinterError;
import com.target.devicemanager.components.printer.entities.PrinterException;
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
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(value = "/v1")
@Tag(name = "Check")
public class MicrController {

    private final PrinterManager printerManager;
    private final MicrManager micrManager;
    private final int PRINT_CONTENT_SIZE = 64;
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrController.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("check", "MicrController", LOGGER);

    @Autowired
    public MicrController(PrinterManager printerManager, MicrManager micrManager) {
        if (printerManager == null) {
            throw new IllegalArgumentException("printerManager cannot be null");
        }
        if(micrManager == null){
            throw new IllegalArgumentException("micrManager cannot be null");

        }
        this.printerManager = printerManager;
        this.micrManager = micrManager;
    }

    @Operation(description = "print data on a CHECK / SLIP")
    @PostMapping(value = "/check")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "COVER_OPEN, INVALID_FORMAT",
                    content = @Content(schema = @Schema(implementation = PrinterError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE / PRINT STATION MISSING",
                    content = @Content(schema = @Schema(implementation = PrinterError.class))),
            @ApiResponse(responseCode = "408", description = "DEVICE_TIMEOUT",
                    content = @Content(schema = @Schema(implementation = PrinterError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void print(@Parameter(description = "Check Print Data") @Valid @RequestBody List<PrinterContent> contents) throws PrinterException {
        String url = "/v1/check";
        log.successAPI("request", 1, url, null, 0);
        if(contents.size() < PRINT_CONTENT_SIZE){
            try {
                printerManager.frankCheck(contents);
                log.successAPI("response", 1, url, null, 200);
            } catch (PrinterException printerException) {
                int statusCode = printerException.getDeviceError() == null ? 0 : printerException.getDeviceError().getStatusCode().value();
                String body = printerException.getDeviceError() == null ? null : printerException.getDeviceError().toString();
                log.failureAPI("response", 13, url, body, statusCode, printerException);
                throw printerException;
            }
        } else {
            log.failureAPI("MICR Print content more than expected limit", 17, url, null, 400, null);
        }
    }

    @Operation(description = "read data from a CHECK / SLIP")
    @GetMapping("/check")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "BAD_DATA",
                    content = @Content(schema = @Schema(implementation = MicrError.class))),
            @ApiResponse(responseCode = "404", description = "HARDWARE_ERROR / DEVICE OFFLINE",
                    content = @Content(schema = @Schema(implementation = MicrError.class))),
            @ApiResponse(responseCode = "204", description = "CLIENT_CANCELLED_REQUEST",
                    content = @Content(schema = @Schema(implementation = MicrError.class)))
    })
    public MicrData readCheck() throws MicrException {
        String url = "/v1/check";
        log.successAPI("request", 1, url, null, 0);
        CompletableFuture<MicrData> micrDataClient = new CompletableFuture<>();
        try {
            MicrData data = micrManager.readMICR(micrDataClient);
            log.successAPI("response", 1, url, null, 200);
            return data;
        } catch (MicrException micrException) {
            int statusCode = micrException.getDeviceError() == null ? 0 : micrException.getDeviceError().getStatusCode().value();
            String body = micrException.getDeviceError() == null ? null : micrException.getDeviceError().toString();
            log.failureAPI("response", 13, url, body, statusCode, micrException);
            throw micrException;
        }
    }

    @Operation(description = "cancel get MICR data call and/or eject the check from the station")
    @DeleteMapping(value= "/check")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")})
    public void cancelCheckRead(){
        String url = "/v1/check";
        log.successAPI("request", 1, url, null, 0);
        micrManager.cancelCheckRead();
        micrManager.ejectCheck();
        log.successAPI("response", 1, url, null, 200);
    }

    @Operation(description = "Reconnect to check device")
    @PostMapping("/check/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void reconnect() throws DeviceException {
        String url = "/v1/check/reconnect";
        log.successAPI("request", 1, url, null, 0);
        try {
            micrManager.reconnectDevice();
            log.successAPI("response", 1, url, null, 200);
        } catch (DeviceException deviceException) {
            int statusCode = deviceException.getDeviceError() == null ? 0 : deviceException.getDeviceError().getStatusCode().value();
            String body = deviceException.getDeviceError() == null ? null : deviceException.getDeviceError().toString();
            log.failureAPI("response", 13, url, body, statusCode, deviceException);
            throw deviceException;
        }
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<DeviceError> handleInvalidFormat(HttpMessageNotReadableException originalException) {
        String url = "/v1/check";
        DeviceException printerException = new DeviceException(PrinterError.INVALID_FORMAT);
        return new ResponseEntity<>(printerException.getDeviceError(), printerException.getDeviceError().getStatusCode());
    }

    @Operation(description = "Reports MICR health")
    @GetMapping(path = "/check/health")
    public DeviceHealthResponse getHealth() {
        String url = "/v1/check/health";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = micrManager.getHealth();
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }

    @Operation(description = "Reports MICR status")
    @GetMapping(path = "/check/healthstatus")
    public DeviceHealthResponse getStatus() {
        String url = "/v1/check/healthstatus";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = micrManager.getStatus();
        return response;
    }
}
