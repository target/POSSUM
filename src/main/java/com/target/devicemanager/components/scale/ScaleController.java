package com.target.devicemanager.components.scale;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.scale.entities.FormattedWeight;
import com.target.devicemanager.components.scale.entities.ScaleError;
import com.target.devicemanager.components.scale.entities.ScaleException;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


@RequestMapping(value = "/v1")
@Tag(name = "Scale")
@Timed
public class ScaleController {

    private final ScaleManager scaleManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScaleController.class);

    @Autowired
    public ScaleController(ScaleManager scaleManager) {
        if(scaleManager == null) {
            throw new IllegalArgumentException("scaleManager cannot be null");
        }
        this.scaleManager = scaleManager;
    }

    @Operation(description = "Retrieves current weight from scale.  For informational purposes only - DO NOT use for selling.")
    @GetMapping(path = "/liveweight", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "NEEDS_ZEROING, WEIGHT_UNDER_ZERO",
                    content = @Content(schema = @Schema(implementation = ScaleError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public SseEmitter getLiveWeight() throws IOException {
        String url = "/v1/scale/liveweight";
        LOGGER.info("request: " + url);
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        try {
            scaleManager.subscribeToLiveWeight(sseEmitter);
            LOGGER.info("response: " + url + " - 200 OK");
            return sseEmitter;
        } catch (IOException ioException) {
            LOGGER.info("response: " + url + " - " + ioException.getMessage());
            throw ioException;
        }
    }

    @Operation(description = "Retrieves stable weight from scale.  Use for selling weighted items.")
    @GetMapping(path = "/stableweight")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "408", description = "TIMEOUT",
                    content = @Content(schema = @Schema(implementation = ScaleError.class))),
            @ApiResponse(responseCode = "400", description = "NEEDS_ZEROING, WEIGHT_UNDER_ZERO",
                    content = @Content(schema = @Schema(implementation = ScaleError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public FormattedWeight getStableWeight() throws ScaleException {
        long randomWithTS = System.currentTimeMillis();
        String url = "/v1/scale/stableweight";
        LOGGER.info("request: " + randomWithTS + " " + url);
        CompletableFuture<FormattedWeight> completableFuture = new CompletableFuture<>();
        try {
            FormattedWeight weight = scaleManager.getStableWeight(completableFuture);
            LOGGER.info("response: " + randomWithTS + " " + url + " - 200 OK ");
            return weight;
        } catch (ScaleException scaleException) {
            LOGGER.info("response: " + randomWithTS + " " + url + " - " + scaleException.getDeviceError().getStatusCode().toString() + ", " + scaleException.getDeviceError());
            throw scaleException;
        }
    }

    @Operation(description = "Reports scale health")
    @GetMapping(path = "/scale/health")
    public DeviceHealthResponse getHealth() {
        String url = "/v1/scale/health";
        LOGGER.info("request: " + url);
        DeviceHealthResponse response = scaleManager.getHealth();
        LOGGER.info("response: " + url + " - " + response.toString());
        return response;
    }

    @Operation(description = "Reports scale status")
    @GetMapping(path = "/scale/healthstatus")
    public DeviceHealthResponse getStatus() {
        String url = "/v1/scale/healthstatus";
        LOGGER.info("request: " + url);
        DeviceHealthResponse response = scaleManager.getStatus();
        LOGGER.info("response: " + url + " - " + response.toString());
        return response;
    }

    @Operation(description = "Reconnects scanner by disconnecting, then connecting")
    @PostMapping(path = "/scale/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void reconnect() throws DeviceException {
        String url = "/v1/scale/reconnect";
        LOGGER.info("request: " + url);
        try {
            scaleManager.reconnectDevice();
            LOGGER.info("response: " + url + " - 200 OK");
        } catch (DeviceException deviceException) {
            LOGGER.info("response: " + url + " - " + deviceException.getDeviceError().getStatusCode().toString() + ", " + deviceException.getDeviceError());
            throw deviceException;
        }
    }
}
