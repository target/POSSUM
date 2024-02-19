package com.target.devicemanager.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.target.devicemanager.common.entities.DeviceErrorStatusResponse;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.scanner.entities.ScannerType;
import com.target.devicemanager.configuration.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DeviceAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceAvailabilityService.class);
    public static final List<SseEmitter> deviceErrorClientList = new CopyOnWriteArrayList<>();
    ApplicationConfig applicationConfig;

    public DeviceAvailabilityService(){}

    @Autowired
    public DeviceAvailabilityService(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public DeviceAvailabilityResponse getAvailableDevices(String confirmOutLoc) {
        DeviceAvailabilityResponse deviceAvailabilityResponse = new DeviceAvailabilityResponse();
        File jsonConfirm = null;
        if(applicationConfig != null && applicationConfig.IsSimulationMode()){
            deviceAvailabilityResponse.possumversion = "possum_simulator";
            deviceAvailabilityResponse.confirmversion = "confirm_simulator";
            jsonConfirm = new File(this.getClass().getClassLoader().getResource("simulator_confirmout.json").getFile());
        } else {
            deviceAvailabilityResponse.possumversion = System.getenv("POSSUM_VERSION");
            deviceAvailabilityResponse.confirmversion = System.getenv("CONFIRM_VERSION");
            jsonConfirm = new File(confirmOutLoc);
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            if(jsonConfirm.exists() && jsonConfirm.isFile()){
                JsonNode rootDevNode = objectMapper.readTree(jsonConfirm);
                Iterator<String> fieldNames = rootDevNode.fieldNames();

                while(fieldNames.hasNext()){
                    String devName = fieldNames.next();
                    JsonNode devices = rootDevNode.path(devName);
                    if(devices.isArray()) {
                        for (JsonNode device : devices) {
                            if (devName != "scale") {
                                deviceAvailabilityResponse.devicelist.add(new DeviceConfigResponse(
                                        devName,
                                        device.get("vidpid").asText(),
                                        device.get("usbport").asText(),
                                        device.get("manufacturer").asText(),
                                        device.get("model").asText(),
                                        device.get("config").asText(),
                                        device.get("firmware").asText(),
                                        device.get("serialnumber").asText(),
                                        findDevStatus(devName) == DeviceHealth.READY,
                                        device.get("vidpid").asText().toUpperCase() != ""
                                ));
                            } else {
                                deviceAvailabilityResponse.devicelist.add(new ScaleConfigResponse(
                                        devName,
                                        device.get("vidpid").asText(),
                                        device.get("usbport").asText(),
                                        device.get("manufacturer").asText(),
                                        device.get("model").asText(),
                                        device.get("config").asText(),
                                        device.get("firmware").asText(),
                                        device.get("serialnumber").asText(),
                                        findDevStatus(devName) == DeviceHealth.READY,
                                        device.get("vidpid").asText().toUpperCase() != "",
                                        device.hasNonNull("calibrated")?device.get("calibrated").asBoolean():null,
                                        device.hasNonNull("calibrated_count")?device.get("calibrated_count").asInt():null,
                                        device.hasNonNull("has_remote_display")?device.get("has_remote_display").asBoolean():null
                                ));
                            }
                        }
                    }
                }
            } else {
                LOGGER.error("JSON is in wrong format");
            }
            return (deviceAvailabilityResponse);
        } catch (IOException ioException) {
            LOGGER.error("Received IOException " + ioException.getMessage());
            return (deviceAvailabilityResponse);
        }
    }

    public DeviceHealth findDevStatus(String devName) {
        DeviceAvailabilitySingleton deviceAvailabilitySingleton = DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton();
        DeviceHealth healthStatus = DeviceHealth.NOTREADY;
        if(applicationConfig != null && applicationConfig.IsSimulationMode()){
            return DeviceHealth.READY;
        }
        switch (devName){
            case "flatbedscanner":
                if(deviceAvailabilitySingleton.getScannerManager() != null) {
                    healthStatus = deviceAvailabilitySingleton.getScannerManager().getScannerHealthStatus("FLATBED");
                } else {
                    LOGGER.trace("Failed to Connect to " + devName);
                }
                break;
            case "handscanner":
                if(deviceAvailabilitySingleton.getScannerManager() != null) {
                    healthStatus = deviceAvailabilitySingleton.getScannerManager().getScannerHealthStatus("HANDHELD");
                } else {
                    LOGGER.trace("Failed to Connect to " + devName);
                }
                break;
            case "scale":
                if(deviceAvailabilitySingleton.getScaleManager() != null) {
                    healthStatus = deviceAvailabilitySingleton.getScaleManager().getStatus().getHealthStatus();
                } else {
                    LOGGER.trace("Failed to Connect to " + devName);
                }
                break;
            case "printer":
                if(deviceAvailabilitySingleton.getPrinterManager() != null) {
                    healthStatus = deviceAvailabilitySingleton.getPrinterManager().getStatus().getHealthStatus();
                } else {
                    LOGGER.trace("Failed to Connect to " + devName);
                }
                break;
            case "linedisplay":
                if(deviceAvailabilitySingleton.getLineDisplayManager() != null) {
                    healthStatus = deviceAvailabilitySingleton.getLineDisplayManager().getStatus().getHealthStatus();
                } else {
                    LOGGER.trace("Failed to Connect to " + devName);
                }
                break;
            default:
                LOGGER.trace("Not a known device. " + devName);
        }
        return healthStatus;
    }

    public void subscribeToDeviceError(SseEmitter sseEmitter) throws IOException {
        sseEmitter.onCompletion(() -> this.deviceErrorClientList.remove(sseEmitter));
        sseEmitter.onTimeout(() -> this.deviceErrorClientList.remove(sseEmitter));
        deviceErrorClientList.add(sseEmitter);
        sseEmitter.send(DeviceErrorStatusResponse.getDeviceErrorStatusResponse(), MediaType.APPLICATION_JSON);
    }

    public static void fireDeviceErrorEvent() {
        List<SseEmitter> deadEmitterList = new ArrayList<>();
        deviceErrorClientList.forEach(emitter -> {
            try {
                emitter.send(DeviceErrorStatusResponse.getDeviceErrorStatusResponse(), MediaType.APPLICATION_JSON);
            } catch(IOException ioException) {
                //Remove the client from the connection pool
                deadEmitterList.add(emitter);
            }
        });
        deviceErrorClientList.removeAll(deadEmitterList);
    }

    public ResponseEntity<List<DeviceHealthResponse>> getHealth() {
        List<DeviceHealthResponse> responseList = new ArrayList<>();
        if(applicationConfig != null && applicationConfig.IsSimulationMode()){
            responseList.add(new DeviceHealthResponse("All Simulated Devices", DeviceHealth.READY));
            return ResponseEntity.ok(responseList);
        }
        if(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getCashDrawerManager() != null) {
            responseList.add(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getCashDrawerManager().getHealth());
        }
        if(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getMicrManager() != null) {
            responseList.add(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getMicrManager().getHealth());
        }

        if(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getLineDisplayManager() != null) {
            responseList.add(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getLineDisplayManager().getHealth());
        }
        if(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getPrinterManager() != null) {
            responseList.add(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getPrinterManager().getHealth());
        }
        if(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getScaleManager() != null) {
            responseList.add(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getScaleManager().getHealth());
        }
        if(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getScannerManager() != null) {
                responseList.addAll(DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().getScannerManager().getHealth(ScannerType.BOTH));
        }
        return ResponseEntity.ok(responseList);
    }
}
