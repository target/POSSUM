package com.target.devicemanager.components.scale;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.scale.simulator.SimulatedJposScale;
import com.target.devicemanager.configuration.ApplicationConfig;

import jpos.config.JposEntryRegistry;
import jpos.Scale;
import jpos.loader.JposServiceLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
class ScaleConfig {
    private final SimulatedJposScale simulatedJposScale;
    private final ApplicationConfig applicationConfig;

    @Autowired
    ScaleConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.simulatedJposScale = new SimulatedJposScale();
    }

    @Bean
    public ScaleManager getScaleManager() {
        DynamicDevice<Scale> dynamicScale;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();

        if (applicationConfig.IsSimulationMode()) {
            dynamicScale = new SimulatedDynamicDevice<>(simulatedJposScale, new DevicePower(), new DeviceConnector<>(simulatedJposScale, deviceRegistry));
        } else {
            Scale scale = new Scale();
            dynamicScale = new DynamicDevice<>(scale, new DevicePower(), new DeviceConnector<>(scale, deviceRegistry ));
        }

        ScaleManager scaleManager = new ScaleManager(
                new ScaleDevice(dynamicScale, new CopyOnWriteArrayList<>(), new CopyOnWriteArrayList<>()),
                new CopyOnWriteArrayList<>(),
                new CopyOnWriteArrayList<>());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setScaleManager(scaleManager);
        return scaleManager;
    }

    @Bean
    SimulatedJposScale getSimulatedJposScale() {
        return simulatedJposScale;
    }

}
