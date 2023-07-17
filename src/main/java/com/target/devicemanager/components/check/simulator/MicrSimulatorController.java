package com.target.devicemanager.components.check.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.check.entities.MicrData;
import com.target.devicemanager.components.printer.simulator.SimulatedCheckPrintResult;
import com.target.devicemanager.components.printer.simulator.SimulatedJposPrinter;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Check")
public class MicrSimulatorController {
    private final SimulatedJposPrinter simulatedJposPrinter;
    private final ApplicationConfig applicationConfig;
    private final SimulatedJposMicr simulatedJposMicr;

    public MicrSimulatorController(ApplicationConfig applicationConfig, SimulatedJposPrinter simulatedJposPrinter, SimulatedJposMicr simulatedJposMicr){
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposPrinter == null) {
            throw new IllegalArgumentException("simulatedJposPrinter cannot be null");
        }

        if (simulatedJposMicr == null) {
            throw new IllegalArgumentException("simulatedJposMicr cannot be null");
        }

        this.simulatedJposPrinter = simulatedJposPrinter;
        this.applicationConfig = applicationConfig;
        this.simulatedJposMicr = simulatedJposMicr;
    }

    @Operation(description = "Set the result of the check printer when trying a print command")
    @PostMapping(path = "CheckPrintOutput")
    public void setPrintOutputResult(@RequestParam SimulatedCheckPrintResult simulatedCheckPrintResult) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposPrinter.setCheckPrintResult(simulatedCheckPrintResult);
    }

    @Operation(description = "Sets current state of the device")
    @PostMapping(path = "CheckPrinterState")
    public void setDeviceState(@RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposPrinter.setState(simulatorState);
        simulatedJposMicr.setState(simulatorState);
    }

    @Operation(description = "Sets the data returned from the MICR device")
    @PostMapping(path = "CheckMicrData")
    public void setCheckInserted(@RequestBody MicrData micrData) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposMicr.setCheckInserted(micrData);
    }

   //TODO: Allow for Errors in the Micr

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
