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
    private static final StructuredEventLogger log = StructuredEventLogger.of("common", "DeviceAvailabilityController", LOGGER);

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
        String url = "/v1/deviceerror";
        log.successAPI("API Request Received", 1, url, null, 0);
        try {
            SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
            deviceAvailabilityService.subscribeToDeviceError(sseEmitter);
            return sseEmitter;
        } catch (IOException ioException) {
            log.failureAPI("API Request Failed with IOException", 9, url, ioException.getMessage(), 500, ioException);
            throw ioException;
        } catch (Exception exception) {
            log.failureAPI("API Request Failed with Exception", 9, url, exception.getMessage(), 500, exception);
            throw exception;
        }
    }

    @Operation(description = "Health status of all devices")
    @GetMapping(path = "/v1/health")
    public ResponseEntity<List<DeviceHealthResponse>> getHealth() {
        log.successAPI("API Request Received", 1, "/v1/health", null, 0);
        return deviceAvailabilityService.getHealth();
    }

}
