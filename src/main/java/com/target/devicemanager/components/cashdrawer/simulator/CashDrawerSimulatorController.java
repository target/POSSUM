package com.target.devicemanager.components.cashdrawer.simulator;

import com.target.devicemanager.common.SimulatorState;
import com.target.devicemanager.configuration.ApplicationConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Cash Drawer")
@Profile("local")
public class CashDrawerSimulatorController {
    private final SimulatedJposCashDrawer simulatedJposCashDrawer;
    private final ApplicationConfig applicationConfig;

    public CashDrawerSimulatorController(ApplicationConfig applicationConfig, SimulatedJposCashDrawer simulatedJposCashDrawer) {
        if (applicationConfig == null) {
            throw new IllegalArgumentException("applicationConfig cannot be null");
        }

        if (simulatedJposCashDrawer == null) {
            throw new IllegalArgumentException("simulatedJposCashDrawer cannot be null");
        }

        this.simulatedJposCashDrawer = simulatedJposCashDrawer;
        this.applicationConfig = applicationConfig;
    }

    @Operation(description = "Set current status of the cashdrawer")
    @PostMapping(path = "cashdrawerStatus")
    public void setDeviceStatus(@RequestParam CashDrawerStatus cashDrawerStatus) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }

        simulatedJposCashDrawer.setStatus(cashDrawerStatus);
    }

    @Operation(description = "Set current state of the cashdrawer")
    @PostMapping(path = "cashdrawerState")
    public void setDeviceState(@RequestParam SimulatorState simulatorState) {
        if (!applicationConfig.IsSimulationMode()) {
            throw new UnsupportedOperationException("Simulation mode is not enabled.");
        }

        simulatedJposCashDrawer.setState(simulatorState);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleException(UnsupportedOperationException exception) {
        return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
    }
}
