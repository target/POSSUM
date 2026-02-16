package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.scanner.entities.ScannerType;
import com.target.devicemanager.components.scanner.simulator.SimulatedJposScanner;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.Scanner;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
class ScannerConfig {
    private final ApplicationConfig applicationConfig;
    private final SimulatedJposScanner simulatedFlatbedScanner;
    private final SimulatedJposScanner simulatedHandheldScanner;

    @Autowired
    ScannerConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.simulatedFlatbedScanner = new SimulatedJposScanner(ScannerType.FLATBED);
        this.simulatedHandheldScanner = new SimulatedJposScanner(ScannerType.HANDHELD);
    }

    List<ScannerDevice> getScanners() {
        List<ScannerDevice> scanners = new ArrayList<>();
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();
        if (applicationConfig.IsSimulationMode()) {
            scanners.add(new ScannerDevice(
                    new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                    new SimulatedDynamicDevice<>(
                            simulatedFlatbedScanner,
                            new DevicePower(),
                            new DeviceConnector<>(simulatedFlatbedScanner, deviceRegistry)
                    ),
                    ScannerType.FLATBED,
                    applicationConfig
            ));

            scanners.add(new ScannerDevice(
                    new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                    new SimulatedDynamicDevice<>(
                            simulatedHandheldScanner,
                            new DevicePower(),
                            new DeviceConnector<>(simulatedHandheldScanner, deviceRegistry)
                    ),
                    ScannerType.HANDHELD,
                    applicationConfig
            ));
        } else {
            Scanner flatbedScanner = new Scanner();
            scanners.add(new ScannerDevice(
                    new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                    new DynamicDevice<>(flatbedScanner, new DevicePower(), new DeviceConnector<>(flatbedScanner, deviceRegistry, new SimpleEntry<>("deviceType", "Flatbed"))),
                    ScannerType.FLATBED, applicationConfig));

            Scanner handScanner = new Scanner();
            scanners.add(new ScannerDevice(
                    new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                    new DynamicDevice<>(handScanner, new DevicePower(), new DeviceConnector<>(handScanner, deviceRegistry, new SimpleEntry<>("deviceType", "HandScanner"))),
                    ScannerType.HANDHELD, applicationConfig));
        }

        return scanners;
    }

    @Bean
    public ScannerManager getScannerManager() {
        ScannerManager scannerManager = new ScannerManager(getScanners(), new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setScannerManager(scannerManager);
        return scannerManager;
    }

    @Bean(name = "simulatedFlatbedScanner")
    SimulatedJposScanner getSimulatedFlatbedScanner() {
        return simulatedFlatbedScanner;
    }

    @Bean(name = "simulatedHandheldScanner")
    SimulatedJposScanner getSimulatedHandheldScanner() {
        return simulatedHandheldScanner;
    }

}
