package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.cashdrawer.entities.CashDrawerError;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/cashdrawer")
@Tag(name = "Cash Drawer")
@Profile({"local", "dev", "prod"})
public class CashDrawerController {

    private final CashDrawerManager cashDrawerManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(CashDrawerController.class);

    @Autowired
    public CashDrawerController(CashDrawerManager cashDrawerManager) {
        if (cashDrawerManager == null) {
            throw new IllegalArgumentException("cashDrawerManager cannot be null");
        }
        this.cashDrawerManager = cashDrawerManager;
    }

    @Operation(description = "Opens the cash drawer and waits until the cash drawer is closed before returning.")
    @PostMapping("/open")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "412", description = "ALREADY_OPEN, OPEN_FAILED",
                    content = @Content(schema = @Schema(implementation = CashDrawerError.class))),
            @ApiResponse(responseCode = "500", description = "UNEXPECTED_ERROR",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void openCashDrawer() throws DeviceException {
        String url = "/v1/cashdrawer/open";
        LOGGER.info("request: " + url );
        try {
            cashDrawerManager.openCashDrawer();
            LOGGER.info("response: " + url  + " - 200 OK");
        } catch (DeviceException deviceException) {
            LOGGER.info("response: " + url + " - " + deviceException.getDeviceError().getStatusCode().toString() + ", " + deviceException.getDeviceError());
            throw deviceException;
        }
    }

    @Operation(description = "Reconnects to the cash drawer")
    @PostMapping("/reconnect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "DEVICE_OFFLINE",
                    content = @Content(schema = @Schema(implementation = DeviceError.class))),
            @ApiResponse(responseCode = "409", description = "DEVICE_BUSY",
                    content = @Content(schema = @Schema(implementation = DeviceError.class)))
    })
    public void reconnect() throws DeviceException {
        String url = "/v1/cashdrawer/reconnect";
        LOGGER.info("request: " + url);
        try {
            cashDrawerManager.reconnectDevice();
            LOGGER.info("response: " + url + " - 200 OK");
        } catch (DeviceException deviceException) {
            LOGGER.info("response: " + url + " - " + deviceException.getDeviceError().getStatusCode().toString() + ", " + deviceException.getDeviceError());
            throw deviceException;
        }
    }

    @Operation(description = "Reports cash drawer health")
    @GetMapping("/health")
    public DeviceHealthResponse getHealth() {
        String url = "/v1/cashdrawer/health";
        LOGGER.info("request: " + url);
        DeviceHealthResponse response = cashDrawerManager.getHealth();
        LOGGER.info("response: " + url + " - " + response.toString());
        return response;
    }

    @Operation(description = "Reports cash drawer status")
    @GetMapping("/healthstatus")
    public DeviceHealthResponse getStatus() {
        String url = "/v1/cashdrawer/healthstatus";
        LOGGER.info("request: " + url);
        DeviceHealthResponse response = cashDrawerManager.getStatus();
        LOGGER.info("response: " + url + " - " + response.toString());
        return response;
    }
}
