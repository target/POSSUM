package com.target.devicemanager.components.printer.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Printer")
public class PrintSimulatorController {
    private final SimulatedJposPrinter simulatedJposPrinter;
    private final ApplicationConfig applicationConfig;

    public PrintSimulatorController(ApplicationConfig applicationConfig, SimulatedJposPrinter simulatedJposPrinter) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposPrinter == null) {
        throw new IllegalArgumentException("simulatedJposPrinter cannot be null");
    }

        this.simulatedJposPrinter = simulatedJposPrinter;
        this.applicationConfig = applicationConfig;
}

    @Operation(description = "Set the result of the printer when trying a print command")
    @PostMapping(path = "printOutput")
    public void setPrintOutputResult(@RequestParam SimulatedPrintResult simulatedPrintResult) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposPrinter.setPrintResult(simulatedPrintResult);
    }

    @Operation(description = "Sets current state of the device")
    @PostMapping(path = {"printerState"})
    public void setDeviceState(@RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposPrinter.setState(simulatorState);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
