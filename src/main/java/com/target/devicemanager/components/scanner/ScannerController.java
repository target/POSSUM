package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.scanner.entities.Barcode;
import com.target.devicemanager.components.scanner.entities.ScannerException;
import com.target.devicemanager.components.scanner.entities.ScannerType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1")
@Tag(name = "Scanner")

public class ScannerController {

    private final ScannerManager scannerManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScannerController.class);

    @Autowired
    public ScannerController(ScannerManager scannerManager) {
        if (scannerManager == null) {
            throw new IllegalArgumentException("scannerManager cannot be null");
        }
        this.scannerManager = scannerManager;
    }

    @Operation(description = "Retrieve barcode data from connected scanner")
    @GetMapping(path = {"/scan", "/scan/{scannerType}"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "204", description = "Scan request was cancelled"),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema( implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema( implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema( implementation = DeviceError.class)))
    })
    public Barcode getScannerData(@Parameter(description = "Scanner to Call") @PathVariable(required = false) ScannerType scannerType) throws ScannerException {
        String url = "";
        if(scannerType == null) {
            url = "/v1/scan";
            LOGGER.info("request : " + url);
            try {
                Barcode data = scannerManager.getData(ScannerType.BOTH);
                LOGGER.info("response: " + url + " - 200 OK");
                return data;
            } catch (ScannerException scannerException) {
                LOGGER.info("response: " + url + " - " + scannerException.getDeviceError().getStatusCode().toString() + ", " + scannerException.getDeviceError());
                throw scannerException;
            }
        } else {
            url = "/v1/scan/" + scannerType;
            LOGGER.info("request: " + url);
            try {
                Barcode data = scannerManager.getData(scannerType);
                LOGGER.info("response: " + url + " - 200 OK");
                return data;
            } catch (ScannerException scannerException) {
                LOGGER.info("response: " + url + " - " + scannerException.getDeviceError().getStatusCode().toString() + ", " + scannerException.getDeviceError());
                throw scannerException;
            }
        }
    }

    @Operation(description = "Cancel previously requested scan")
    @DeleteMapping(path = "/scan")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scan request canceled. Scanner has been disabled"),
            @ApiResponse(responseCode = "412", description = "ALREADY_DISABLED",
                    content = @Content(schema = @Schema( implementation = DeviceError.class)))
    })
    public void cancelScanRequest() throws ScannerException {
        String url = "/v1/scan";
        LOGGER.info("request: DELETE - " + url );
        try {
            scannerManager.cancelScanRequest();
            LOGGER.info("response: " + "DELETE " + url + " - 200 OK");
        } catch (ScannerException scannerException) {
            LOGGER.info("response: " + url + " - " + scannerException.getDeviceError().getStatusCode().toString() + ", " + scannerException.getDeviceError());
            throw scannerException;
        }
    }

    @Operation(description = "Reports the health of one scanner (FLATBED or HANDHELD)")
    @GetMapping(path = {"/scanner/health", "/scanner/health/{scannerType}"})
    public ResponseEntity<List<DeviceHealthResponse>> getHealth(@Parameter(description = "Scanner to Call") @PathVariable(required = false) ScannerType scannerType) {
        List<DeviceHealthResponse> responseList;
        String url = "";
        if(scannerType == null) {
            url = "/v1/scanner/health";
            LOGGER.info("request: " + url);
            responseList = scannerManager.getHealth(ScannerType.BOTH);
        } else {
            url = "/v1/scanner/health/" + scannerType;
            LOGGER.info("request: " + url);
            responseList = scannerManager.getHealth(scannerType);
        }
        for(DeviceHealthResponse deviceResponse : responseList) {
            LOGGER.info("response: " + url + " - " + deviceResponse.toString());
        }
        return ResponseEntity.ok(responseList);
    }

    @Operation(description = "Reports scanners status")
    @GetMapping(path = "/scanner/healthstatus")
    public ResponseEntity<List<DeviceHealthResponse>> getStatus() {
        String url = "/v1/scanner/healthstatus";
        LOGGER.info("request: " + url);        List<DeviceHealthResponse> responseList = scannerManager.getStatus();
        for(DeviceHealthResponse deviceResponse : responseList) {
            LOGGER.info("response: " + url + " - " + deviceResponse.toString());        }
        return ResponseEntity.ok(responseList);
    }

    @Operation(description = "Reconnects scanners")
    @PostMapping(path = "/scanner/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema( implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema( implementation = DeviceError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema( implementation = DeviceError.class)))
    })
    void reconnect() throws DeviceException {
        String url = "/v1/scanner/reconnect";
        LOGGER.info("request: " + url);
        try {
            scannerManager.reconnectScanners();
            LOGGER.info("response: " + url + " - 200 OK");
        } catch (DeviceException deviceException) {
            LOGGER.info("response: " + url + " - " + deviceException.getDeviceError().getStatusCode().toString() + ", " + deviceException.getDeviceError());
            throw deviceException;
        }
    }
}
