package com.target.devicemanager.common.entities;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.target.devicemanager.common.DeviceAvailabilityService;
import com.target.devicemanager.common.StructuredEventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class DeviceErrorStatusResponse {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceErrorStatusResponse.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of(StructuredEventLogger.getCommonServiceName(), "DeviceErrorStatusResponse", LOGGER);
    private static final DeviceErrorStatusResponse deviceErrorStatusResponse = new DeviceErrorStatusResponse();
    private static List<DeviceErrorStatus> deviceErrorStatuses;

    private DeviceErrorStatusResponse(){
        deviceErrorStatuses = new CopyOnWriteArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonConfirm = new File("/var/tmp/CONFIRMOUT/confirmout.json");
        if(jsonConfirm.exists() && jsonConfirm.isFile()){
            JsonNode rootDevNode = null;
            try {
                rootDevNode = objectMapper.readTree(jsonConfirm);
            } catch (JacksonException ioException) {
                log.failure("Error in parsing confirmout", 17, ioException);
            }
            for (Map.Entry<String, JsonNode> field : rootDevNode.properties()) {
                deviceErrorStatuses.add(new DeviceErrorStatus(field.getKey(), false, null));
            }
        } else {
            log.failure("JSON is in wrong format", 17, null);
        }
    }

    public static List<DeviceErrorStatus> getDeviceErrorStatusResponse(){
        return deviceErrorStatusResponse.deviceErrorStatuses;
    }

    public static void setDeviceErrorStatusResponse(String deviceName, DeviceError deviceError){
        for(DeviceErrorStatus deviceErrorStatus : deviceErrorStatuses){
            if(deviceErrorStatus.deviceName.equals(deviceName)){
                deviceErrorStatus.faultPresent = true;
                deviceErrorStatus.deviceError = deviceError;
            }
        }
        DeviceAvailabilityService.fireDeviceErrorEvent();
        clearError();
    }

    private static void clearError(){
        for(DeviceErrorStatus deviceErrorStatus : deviceErrorStatuses){
                deviceErrorStatus.faultPresent = false;
                deviceErrorStatus.deviceError = null;
        }
    }

    public static void sendClearError(){
        clearError();
        DeviceAvailabilityService.fireDeviceErrorEvent();
    }
}
