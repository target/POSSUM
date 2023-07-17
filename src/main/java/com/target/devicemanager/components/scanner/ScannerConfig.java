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
    private final SimulatedJposScanner simulatedScanner;
    private final ApplicationConfig applicationConfig;

    @Autowired
    ScannerConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.simulatedScanner = new SimulatedJposScanner();
    }

    List<ScannerDevice> getScanners() {
        List<ScannerDevice> scanners = new ArrayList<>();
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();

        if (applicationConfig.IsSimulationMode()) {
            scanners.add(new ScannerDevice(
                    new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                    new SimulatedDynamicDevice<>(simulatedScanner, new DevicePower(), new DeviceConnector<>(simulatedScanner, deviceRegistry)),
                    ScannerType.FLATBED));
        } else {
            Scanner flatbedScanner = new Scanner();
            scanners.add(new ScannerDevice(
                    new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                    new DynamicDevice<>(flatbedScanner, new DevicePower(), new DeviceConnector<>(flatbedScanner, deviceRegistry, new SimpleEntry<>("deviceType", "Flatbed"))),
                    ScannerType.FLATBED));

            Scanner handScanner = new Scanner();
            scanners.add(new ScannerDevice(
                    new ScannerDeviceListener(new EventSynchronizer(new Phaser(1))),
                    new DynamicDevice<>(handScanner, new DevicePower(), new DeviceConnector<>(handScanner, deviceRegistry, new SimpleEntry<>("deviceType", "HandScanner"))),
                    ScannerType.HANDHELD));
        }

        return scanners;
    }

    @Bean
    public ScannerManager getScannerManager() {
        ScannerManager scannerManager = new ScannerManager(getScanners(), new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setScannerManager(scannerManager);
        return scannerManager;
    }

    @Bean
    SimulatedJposScanner getSimulatedScanner() {
        return simulatedScanner;
    }
}
