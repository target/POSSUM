package com.target.devicemanager.components.scanner.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.scanner.entities.Barcode;
import com.target.devicemanager.components.scanner.entities.ScannerError;
import com.target.devicemanager.components.scanner.entities.ScannerException;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Scanner")
public class ScanSimulatorController {
    private final List<SimulatedJposScanner> scanners;
    private final ApplicationConfig applicationConfig;

    @Autowired
    public ScanSimulatorController(ApplicationConfig applicationConfig,
                                   List<SimulatedJposScanner> scanners) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }
        if (scanners == null || scanners.isEmpty()) {
            throw new IllegalArgumentException("scanners cannot be null or empty");
        }
        this.applicationConfig = applicationConfig;
        this.scanners = scanners;
    }

    private SimulatedJposScanner pickScanner(com.target.devicemanager.components.scanner.entities.ScannerType type) {
        return scanners.stream()
                .filter(s -> type.name().equalsIgnoreCase(s.getPhysicalDeviceName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown device: " + type));
    }

    @Operation(description = "Set barcode to complete the currently pending scan request")
    @PostMapping(path = "scan")
    public void setBarcodeData(@RequestBody Barcode barcode) throws ScannerException {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        if (barcode.source == null) {
            throw new ScannerException(ScannerError.UNKNOWN_DEVICE);
        }
        try {
            pickScanner(barcode.source).setBarcode(barcode);
        } catch (IllegalArgumentException e) {
            throw new ScannerException(ScannerError.UNKNOWN_DEVICE);
        }
    }

    @Operation(description = "Set current state of the scanner")
    @PostMapping(path = "scannerState")
    public void setDeviceState(@RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        scanners.forEach(s -> s.setState(simulatorState));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
