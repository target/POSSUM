package com.target.devicemanager.components.linedisplay;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.linedisplay.entities.LineDisplayData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/linedisplay")
@Tag(name = "Line Display (2x20)")
@Profile({"local","dev","prod"})
public class LineDisplayController {

    private final LineDisplayManager lineDisplayManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(LineDisplayController.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("line_display", "LineDisplayController", LOGGER);
    @Autowired
    public LineDisplayController(LineDisplayManager lineDisplayManager) {
        if (lineDisplayManager == null) {
            throw new IllegalArgumentException("lineDisplayManager cannot be null");
        }
        this.lineDisplayManager = lineDisplayManager;
    }

    @Operation(description = "Displays text on 2x20.  To clear out a line, omit it from the request.")
    @PostMapping(path = "/display")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR", content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE", content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY", content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void displayLines(@RequestBody LineDisplayData data) throws DeviceException {
        String url = "/v1/linedisplay/display";
        log.successAPI("request", 1, url, null, 0);
        try {
            lineDisplayManager.displayLine(data.line1, data.line2);
            log.successAPI("response", 1, url, null, 200);
        } catch (DeviceException deviceException) {
            int statusCode = deviceException.getDeviceError().getStatusCode().value();
            log.failureAPI("response", 13, url, deviceException.getDeviceError().toString(), statusCode, deviceException);
            throw deviceException;
        }
    }

    @Operation(description = "Reports linedisplay health")
    @GetMapping(path = "/health")
    public DeviceHealthResponse getHealth() {
        String url = "/v1/linedisplay/health";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = lineDisplayManager.getHealth();
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }

    @Operation(description = "Reports linedisplay status")
    @GetMapping(path = "/healthstatus")
    public DeviceHealthResponse getStatus() {
        String url = "/v1/linedisplay/healthstatus";
        log.successAPI("request", 1, url, null, 0);
        DeviceHealthResponse response = lineDisplayManager.getStatus();
        log.successAPI("response", 1, url, response.toString(), 200);
        return response;
    }

    @Operation(description = "Reconnects the line display device by releasing, then connecting.")
    @PostMapping(path = "/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE", content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY", content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void reconnect() throws DeviceException {
        String url = "/v1/linedisplay/reconnect";
        log.successAPI("request", 1, url, null, 0);
        try {
            lineDisplayManager.reconnectDevice();
            log.successAPI("response", 1, url, null, 200);
        } catch (DeviceException deviceException) {
            int statusCode = deviceException.getDeviceError().getStatusCode().value();
            log.failureAPI("response", 13, url, deviceException.getDeviceError().toString(), statusCode, deviceException);
            throw deviceException;
        }
    }

}
