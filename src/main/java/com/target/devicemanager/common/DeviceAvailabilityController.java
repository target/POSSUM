package com.target.devicemanager.common;

import com.target.devicemanager.common.entities.DeviceHealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@Tag(name = "Device Availability")
public class DeviceAvailabilityController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceAvailabilityController.class);

    private static final String CONFIRMOUT_LOCATION  = "/var/tmp/CONFIRMOUT/confirmout.json";

    @Autowired
    public DeviceAvailabilityService deviceAvailabilityService;

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/v1/peripherals")
    public DeviceAvailabilityResponse getDeviceAvailability() {
        return deviceAvailabilityService.getAvailableDevices(CONFIRMOUT_LOCATION);
    }

    @GetMapping("/v1/deviceerror")
    public SseEmitter getDeviceError() throws IOException {
        LOGGER.info("GET : /v1/deviceerror");
        try {
            SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
            deviceAvailabilityService.subscribeToDeviceError(sseEmitter);
            return sseEmitter;
        } catch (IOException ioException) {
            LOGGER.info("IOException in DeviceError: " + ioException.getMessage());
            throw ioException;
        }
    }

    @Operation(description = "Health status of all devices")
    @GetMapping(path = "/v1/health")
    public ResponseEntity<List<DeviceHealthResponse>> getHealth() {
        LOGGER.info("GET : /v1/health");
        return deviceAvailabilityService.getHealth();
    }

}
