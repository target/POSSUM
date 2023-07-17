package com.target.devicemanager.components.check;

import com.target.devicemanager.common.DeviceAvailabilitySingleton;
import com.target.devicemanager.common.DeviceConnector;
import com.target.devicemanager.common.DevicePower;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.components.check.simulator.SimulatedJposMicr;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.MICR;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
class MicrConfig {
    private final ApplicationConfig applicationConfig;
    private final SimulatedJposMicr simulatedMicr;

    @Autowired
    MicrConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.simulatedMicr = new SimulatedJposMicr();
    }

    @Bean
    public MicrManager getMicrManager() {
        DynamicDevice<? extends MICR> dynamicMicr;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();

        if (applicationConfig.IsSimulationMode()) {
            dynamicMicr = new DynamicDevice<>(simulatedMicr, new DevicePower(), new DeviceConnector<>(simulatedMicr, deviceRegistry));
        } else {
            MICR micr = new MICR();
            dynamicMicr = new DynamicDevice<>(micr, new DevicePower(), new DeviceConnector<>(micr, deviceRegistry));
        }

        MicrManager micrManager = new MicrManager(
                new MicrDevice(dynamicMicr,new CopyOnWriteArrayList<>(),new CopyOnWriteArrayList<>()));

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setMicrManager(micrManager);
        return micrManager;
    }

    @Bean
    SimulatedJposMicr getSimulatedMicr(){return simulatedMicr;}
}
