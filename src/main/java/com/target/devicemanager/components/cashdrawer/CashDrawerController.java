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
import com.target.devicemanager.common.LogPayloadBuilder;
import com.target.devicemanager.common.entities.LogField;

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
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "CashDrawer")
            .add(LogField.EVENT_SEVERITY, 1)
            .add(LogField.COMPONENT, "CashDrawerController")
            .add(LogField.EVENT_ACTION, "openCashDrawer")
            .add(LogField.MESSAGE, "API Request Received")
            .logTrace(LOGGER);
        try {
            cashDrawerManager.openCashDrawer();
            new LogPayloadBuilder()
                .add(LogField.URL_PATH, url)
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 1)
                .add(LogField.COMPONENT, "CashDrawerController")
                .add(LogField.EVENT_ACTION, "openCashDrawer")
                .add(LogField.EVENT_OUTCOME, "success")
                .add(LogField.HTTP_RESPONSE_STATUS_CODE, 200)
                .add(LogField.HTTP_RESPONSE_BODY_CONTENT, "OK")
                .add(LogField.MESSAGE, "API Request Completed Successfully")
                .logTrace(LOGGER);
        } catch (DeviceException deviceException) {
            new LogPayloadBuilder()
                .add(LogField.URL_PATH, url)
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 17)
                .add(LogField.COMPONENT, "CashDrawerController")
                .add(LogField.EVENT_ACTION, "openCashDrawer")
                .add(LogField.EVENT_OUTCOME, "failure")
                .add(LogField.HTTP_RESPONSE_STATUS_CODE, deviceException.getDeviceError().getStatusCode().value())
                .add(LogField.HTTP_RESPONSE_BODY_CONTENT, deviceException.getDeviceError().toString())
                .add(LogField.MESSAGE, "API Request Failed with DeviceException")
                .logError(LOGGER);
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
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "CashDrawer")
            .add(LogField.EVENT_SEVERITY, 1)
            .add(LogField.COMPONENT, "CashDrawerController")
            .add(LogField.EVENT_ACTION, "reconnect")
            .add(LogField.MESSAGE, "API Request Received")
            .logTrace(LOGGER);
        try {
            cashDrawerManager.reconnectDevice();
            new LogPayloadBuilder()
                .add(LogField.URL_PATH, url)
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 1)
                .add(LogField.COMPONENT, "CashDrawerController")
                .add(LogField.EVENT_ACTION, "reconnect")
                .add(LogField.EVENT_OUTCOME, "success")
                .add(LogField.HTTP_RESPONSE_STATUS_CODE, 200)
                .add(LogField.HTTP_RESPONSE_BODY_CONTENT, "OK")
                .add(LogField.MESSAGE, "API Request Completed Successfully")
                .logTrace(LOGGER);
        } catch (DeviceException deviceException) {
            new LogPayloadBuilder()
                .add(LogField.URL_PATH, url)
                .add(LogField.SERVICE_NAME, "CashDrawer")
                .add(LogField.EVENT_SEVERITY, 17)
                .add(LogField.COMPONENT, "CashDrawerController")
                .add(LogField.EVENT_ACTION, "reconnect")
                .add(LogField.EVENT_OUTCOME, "failure")
                .add(LogField.HTTP_RESPONSE_STATUS_CODE, deviceException.getDeviceError().getStatusCode().value())
                .add(LogField.HTTP_RESPONSE_BODY_CONTENT, deviceException.getDeviceError().toString())
                .add(LogField.MESSAGE, "API Request Failed with DeviceException")
                .logError(LOGGER);
            throw deviceException;
        }
    }

    @Operation(description = "Reports cash drawer health")
    @GetMapping("/health")
    public DeviceHealthResponse getHealth() {
        String url = "/v1/cashdrawer/health";
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "CashDrawer")
            .add(LogField.EVENT_SEVERITY, 1)
            .add(LogField.COMPONENT, "CashDrawerController")
            .add(LogField.EVENT_ACTION, "getHealth")
            .add(LogField.MESSAGE, "API Request Received")
            .logTrace(LOGGER);
        DeviceHealthResponse response = cashDrawerManager.getHealth();
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "CashDrawer")
            .add(LogField.EVENT_SEVERITY, 1)
            .add(LogField.COMPONENT, "CashDrawerController")
            .add(LogField.EVENT_ACTION, "getHealth")
            .add(LogField.EVENT_OUTCOME, "success")
            .add(LogField.HTTP_RESPONSE_STATUS_CODE, 200)
            .add(LogField.HTTP_RESPONSE_BODY_CONTENT, response.toString())
            .add(LogField.TAGS, response.getHealthStatus().toString())
            .add(LogField.MESSAGE, "API Request Completed Successfully")
            .logTrace(LOGGER);
        return response;
    }

    @Operation(description = "Reports cash drawer status")
    @GetMapping("/healthstatus")
    public DeviceHealthResponse getStatus() {
        String url = "/v1/cashdrawer/healthstatus";
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "CashDrawer")
            .add(LogField.EVENT_SEVERITY, 9)
            .add(LogField.COMPONENT, "CashDrawerController")
            .add(LogField.EVENT_ACTION, "getStatus")
            .add(LogField.MESSAGE, "API Request Received")
            .logTrace(LOGGER);
        DeviceHealthResponse response = cashDrawerManager.getStatus();
        new LogPayloadBuilder()
            .add(LogField.URL_PATH, url)
            .add(LogField.SERVICE_NAME, "CashDrawer")
            .add(LogField.EVENT_SEVERITY, 1)
            .add(LogField.COMPONENT, "CashDrawerController")
            .add(LogField.EVENT_ACTION, "getStatus")
            .add(LogField.EVENT_OUTCOME, "success")
            .add(LogField.HTTP_RESPONSE_STATUS_CODE, 200)
            .add(LogField.HTTP_RESPONSE_BODY_CONTENT, response.toString())
            .add(LogField.TAGS, response.getHealthStatus().toString())
            .add(LogField.MESSAGE, "API Request Completed Successfully")
            .logTrace(LOGGER);
        return response;
    }
}
