package com.target.devicemanager.components.scanner.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.scanner.entities.Barcode;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Scanner")
public class ScanSimulatorController {
    private final SimulatedJposScanner simulatedJposScanner;
    private final ApplicationConfig applicationConfig;

    @Autowired
    public ScanSimulatorController(ApplicationConfig applicationConfig, SimulatedJposScanner simulatedJposScanner) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposScanner == null) {
            throw new IllegalArgumentException("simulatedJposScanner cannot be null");
        }

        this.simulatedJposScanner = simulatedJposScanner;
        this.applicationConfig = applicationConfig;
    }

    @Operation(description = "Set barcode to complete the currently pending scan request")
    @PostMapping(path = "scan")
    public void setBarcodeData(@RequestBody Barcode barcode) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposScanner.setBarcode(barcode);
    }

    @Operation(description = "Set current state of the scanner")
    @PostMapping(path = "scannerState")
    public void setDeviceState(@RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposScanner.setState(simulatorState);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
