package com.target.devicemanager.common.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.target.devicemanager.common.DeviceAvailabilityController;
import com.target.devicemanager.common.DeviceAvailabilityService;
import com.target.devicemanager.common.entities.RegisterType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/v1/simulate")
@Tag(name = "Device Availability")
@Profile("local")
public class RegisterTypeSimulatorController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceAvailabilityController.class);

    @Autowired
    private DeviceAvailabilityService deviceAvailabilityService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/register_type")
    public ResponseEntity<String> setRegisterType(@RequestParam RegisterType registerType) {
        deviceAvailabilityService.setSimulatorRegisterType(registerType.getDisplayName());
        return ResponseEntity.ok("Register type set to: " + registerType.getDisplayName());
    }

    @GetMapping("/register_type")
    public ResponseEntity<String> getRegisterType() {
        String registerType = deviceAvailabilityService.getSimulatorRegisterType();
        return ResponseEntity.ok(registerType != null ? registerType : RegisterType.SELF_CHECKOUT_ATM.getDisplayName());
    }

    @PostMapping("/custom_configuration")
    public ResponseEntity<String> setCustomConfiguration(@RequestBody DeviceSelection config) {
        try {
            Map<String, List<Map<String, Object>>> allDevices = loadAllDevices();

            Map<String, List<Map<String, Object>>> customConfig = new HashMap<>();

            for (Map.Entry<String, String> entry : config.getDevices().entrySet()) {
                String deviceType = entry.getKey();
                String model = entry.getValue();

                if (allDevices.containsKey(deviceType)) {
                    List<Map<String, Object>> devices = allDevices.get(deviceType);
                    Optional<Map<String, Object>> selectedDevice = devices.stream()
                            .filter(d -> model.equals(d.get("model")))
                            .findFirst();

                    if (selectedDevice.isPresent()) {
                        customConfig.put(deviceType, Collections.singletonList(selectedDevice.get()));
                    }
                }
            }
            Path tempFile = Files.createTempFile("simulator_confirmout_CUSTOM-", ".json");
            objectMapper.writeValue(tempFile.toFile(), customConfig);
            deviceAvailabilityService.setCustomConfigPath(tempFile.toString());
            deviceAvailabilityService.setSimulatorRegisterType(RegisterType.CUSTOM.name());

            return ResponseEntity.ok("Custom configuration saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save custom configuration: " + e.getMessage());
        }
    }

    @GetMapping("/available_device_models")
    public ResponseEntity<Map<String, List<String>>> getAvailableModels() {
        try {
            Map<String, List<Map<String, Object>>> allDevices = loadAllDevices();
            Map<String, List<String>> models = new HashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : allDevices.entrySet()) {
                String deviceType = entry.getKey();
                List<Map<String, Object>> devices = entry.getValue();

                if (devices == null) {
                    continue;
                }

                List<String> deviceModels = devices.stream()
                        .filter(d -> d != null && d.get("model") != null)
                        .map(d -> d.get("model").toString())
                        .distinct()
                        .collect(Collectors.toList());
                if (!deviceModels.isEmpty()) {
                    models.put(deviceType, deviceModels);
                }
            }
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.emptyMap());
        }
    }

    private Map loadAllDevices() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("allDevices.json")) {
            if (is == null) {
                throw new IllegalStateException("allDevices.json not found in resources");
            }
            return objectMapper.readValue(is, Map.class);
        }
    }
}
