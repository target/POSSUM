package com.target.devicemanager.components.linedisplay.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.components.linedisplay.entities.LineDisplayData;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Line Display (2x20)")
@Profile({"local","dev","prod"})
public class LineDisplaySimulatorController {
    private final SimulatedJposLineDisplay simulatedJposLineDisplay;
    private final ApplicationConfig applicationConfig;

    public LineDisplaySimulatorController(ApplicationConfig applicationConfig, SimulatedJposLineDisplay simulatedJposLineDisplay) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposLineDisplay == null) {
            throw new IllegalArgumentException("simulatedJposLineDisplay cannot be null");
        }

        this.simulatedJposLineDisplay = simulatedJposLineDisplay;
        this.applicationConfig = applicationConfig;
    }

    @Operation(description = "Set current state of the line display")
    @PostMapping(path = "lineDisplayState")
    public void setDeviceState(@RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }

        simulatedJposLineDisplay.setState(simulatorState);
    }

    @Operation(description = "Get text in the line display")
    @GetMapping(path = "lineDisplayText")
    public LineDisplayData lineDisplayText() {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }

        return simulatedJposLineDisplay.getDisplayText();
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
