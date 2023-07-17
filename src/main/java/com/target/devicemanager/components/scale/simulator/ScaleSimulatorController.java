package com.target.devicemanager.components.scale.simulator;

import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Scale")
public class ScaleSimulatorController {
    private final SimulatedJposScale simulatedJposScale;
    private final ApplicationConfig applicationConfig;

    public ScaleSimulatorController(ApplicationConfig applicationConfig, SimulatedJposScale simulatedJposScale) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposScale == null) {
            throw new IllegalArgumentException("simulatedJposScale cannot be null");
        }

        this.simulatedJposScale = simulatedJposScale;
        this.applicationConfig = applicationConfig;
    }

    @PostMapping(path = "scaleState")
    public void setDeviceState(@RequestParam ScaleSimulatorState scaleSimulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposScale.setState(scaleSimulatorState);
    }

    @PostMapping(path = "scaleWeight")
    public void setWeight(@RequestParam BigDecimal weight) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }
        simulatedJposScale.setWeight(weight);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
