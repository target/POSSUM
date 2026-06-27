package com.target.devicemanager.common.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.target.devicemanager.common.DeviceAvailabilityService;
import com.target.devicemanager.common.StructuredEventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DeviceErrorStatusResponse {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceErrorStatusResponse.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of(StructuredEventLogger.getCommonServiceName(), "DeviceErrorStatusResponse", LOGGER);
    private static final DeviceErrorStatusResponse deviceErrorStatusResponse = new DeviceErrorStatusResponse();
    private static List<DeviceErrorStatus> deviceErrorStatuses;

    private DeviceErrorStatusResponse(){
        deviceErrorStatuses = loadDeviceErrorStatuses(new File("/var/tmp/CONFIRMOUT/confirmout.json"));
    }

    static List<DeviceErrorStatus> loadDeviceErrorStatuses(File jsonConfirm){
        List<DeviceErrorStatus> statuses = new CopyOnWriteArrayList<>();
        if(jsonConfirm == null || !jsonConfirm.exists() || !jsonConfirm.isFile()){
            log.failure("confirmout.json not found at " + (jsonConfirm == null ? "null" : jsonConfirm.getPath()), 17, null);
            return statuses;
        }

        JsonNode rootDevNode;
        try {
            rootDevNode = new ObjectMapper().readTree(jsonConfirm);
        } catch (IOException ioException) {
            log.failure("JSON is in wrong format", 17, ioException);
            return statuses;
        }

        Iterator<String> fieldNames = rootDevNode.fieldNames();
        while(fieldNames.hasNext()){
            statuses.add(new DeviceErrorStatus(fieldNames.next(), false, null));
        }
        return statuses;
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
