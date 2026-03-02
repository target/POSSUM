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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DeviceAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceAvailabilityService.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of(StructuredEventLogger.getCommonServiceName(), "DeviceAvailabilityService", LOGGER);
    public static final List<SseEmitter> deviceErrorClientList = new CopyOnWriteArrayList<>();
    ApplicationConfig applicationConfig;
    private String simulatorRegisterType = "default";
    private String customConfigPath;

    public DeviceAvailabilityService(){}
    @Autowired
    public DeviceAvailabilityService(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public void setSimulatorRegisterType(String registerType) {
        this.simulatorRegisterType = registerType;
    }

    public String getSimulatorRegisterType() {
        return this.simulatorRegisterType;
    }

    public DeviceAvailabilityResponse getAvailableDevices(String confirmOutLoc) {
        DeviceAvailabilityResponse deviceAvailabilityResponse = new DeviceAvailabilityResponse();
        File jsonConfirm = null;

        if(applicationConfig != null && applicationConfig.IsSimulationMode()) {
            deviceAvailabilityResponse.possumversion = "possum_simulator";
            deviceAvailabilityResponse.confirmversion = "confirm_simulator";
            log.success("Simulator register type: " + simulatorRegisterType, 9);

            if (!simulatorRegisterType.equals("default")) {
                if ("CUSTOM".equals(simulatorRegisterType) && customConfigPath != null && !customConfigPath.isEmpty()) {
                    jsonConfirm = new File(customConfigPath);
                    if (!jsonConfirm.exists()) {
                        log.success("Custom config path doesn't exist: " + customConfigPath, 13);
                    }
                }

                if (jsonConfirm == null || !jsonConfirm.exists()) {
                    String fileName = "simulator_confirmout_" + simulatorRegisterType + ".json";
                    jsonConfirm = new File("src/main/resources/" + fileName);

                    if (!jsonConfirm.exists()) {
                        try (InputStream in = this.getClass().getClassLoader()
                                .getResourceAsStream(fileName)) {

                            if (in == null) {
                                throw new IllegalStateException(fileName + " not found as resource");
                            }

                            Path tempFile = Files.createTempFile("simulator_confirmout-", ".json");
                            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                            jsonConfirm = tempFile.toFile();
                            jsonConfirm.deleteOnExit();
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to load resource file: " + fileName, e);
                        }
                    }
                }
            } else {
                jsonConfirm = new File("src/main/resources/simulator_confirmout.json");

                if (!jsonConfirm.exists()) {
                    try (InputStream in = this.getClass().getClassLoader()
                            .getResourceAsStream("simulator_confirmout.json")) {

                        if (in == null) {
                            throw new IllegalStateException("simulator_confirmout.json not found as resource");
                        }

                        Path tempFile = Files.createTempFile("simulator_confirmout-", ".json");
                        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        jsonConfirm = tempFile.toFile();
                        jsonConfirm.deleteOnExit();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load resource file", e);
                    }
                }
            }
        } else {
            deviceAvailabilityResponse.possumversion = System.getenv("POSSUM_VERSION");
            deviceAvailabilityResponse.confirmversion = System.getenv("CONFIRM_VERSION");
            jsonConfirm = new File(confirmOutLoc);
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            if(jsonConfirm != null && jsonConfirm.exists() && jsonConfirm.isFile()){
                JsonNode rootDevNode = objectMapper.readTree(jsonConfirm);
                Iterator<String> fieldNames = rootDevNode.fieldNames();

                while(fieldNames.hasNext()){
                    String devName = fieldNames.next();
                    JsonNode devices = rootDevNode.path(devName);
                    if(devices.isArray()) {
                        for (JsonNode device : devices) {
                            if (!devName.equals("scale")) {
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
                                        !device.get("vidpid").asText().isEmpty()
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
                                        !device.get("vidpid").asText().isEmpty(),
                                        device.hasNonNull("calibrated")?device.get("calibrated").asBoolean():null,
                                        device.hasNonNull("calibrated_count")?device.get("calibrated_count").asInt():null,
                                        device.hasNonNull("has_remote_display")?device.get("has_remote_display").asBoolean():null
                                ));
                            }
                        }
                    }
                }
            } else {
                log.failure("JSON file not found or invalid", 17, null);
            }
        } catch (IOException ioException) {
            log.failure("Received IOException", 17, ioException);
        }

        return deviceAvailabilityResponse;
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
                    log.failure("Failed to Connect to " + devName, 1, null);
                }
                break;
            case "handscanner":
                if(deviceAvailabilitySingleton.getScannerManager() != null) {
                    healthStatus = deviceAvailabilitySingleton.getScannerManager().getScannerHealthStatus("HANDHELD");
                } else {
                    log.failure("Failed to Connect to " + devName, 1, null);
                }
                break;
            case "scale":
                if(deviceAvailabilitySingleton.getScaleManager() != null) {
                    healthStatus = deviceAvailabilitySingleton.getScaleManager().getStatus().getHealthStatus();
                } else {
                    log.failure("Failed to Connect to " + devName, 1, null);
                }
                break;
            case "printer":
                if(deviceAvailabilitySingleton.getPrinterManager() != null) {
                    healthStatus = deviceAvailabilitySingleton.getPrinterManager().getStatus().getHealthStatus();
                } else {
                    log.failure("Failed to Connect to " + devName, 1, null);
                }
                break;
            case "linedisplay":
                if(deviceAvailabilitySingleton.getLineDisplayManager() != null) {
                    healthStatus = deviceAvailabilitySingleton.getLineDisplayManager().getStatus().getHealthStatus();
                } else {
                    log.failure("Failed to Connect to " + devName, 1, null);
                }
                break;
            default:
                log.failure("Not a known device: " + devName, 1, null);
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

    public void setCustomConfigPath(String path) {
        this.customConfigPath = path;
    }

    public String getCustomConfigPath() {
        return this.customConfigPath;
    }
}
