package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.printer.simulator.SimulatedJposPrinter;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.POSPrinter;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
class PrinterConfig {
    private final SimulatedJposPrinter simulatedPrinter;
    private final ApplicationConfig applicationConfig;

    @Autowired
    PrinterConfig(ApplicationConfig applicationConfig) {

        this.simulatedPrinter = new SimulatedJposPrinter();
        this.applicationConfig = applicationConfig;
    }

    @Bean
    public PrinterManager getReceiptPrinterManager() {
        DynamicDevice<? extends POSPrinter> dynamicPrinter;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();

        if (applicationConfig.IsSimulationMode()) {
            dynamicPrinter = new SimulatedDynamicDevice<>(simulatedPrinter, new DevicePower(), new DeviceConnector<>(simulatedPrinter, deviceRegistry));

        } else {
            POSPrinter posPrinter = new POSPrinter();
            dynamicPrinter = new DynamicDevice<>(posPrinter, new DevicePower(), new DeviceConnector<>(posPrinter, deviceRegistry));
        }

        PrinterManager printerManager = new PrinterManager(
                new PrinterDevice(dynamicPrinter, new PrinterDeviceListener(new EventSynchronizer(new Phaser(1)))),
                new ReentrantLock());

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setPrinterManager(printerManager);
        return printerManager;
    }

    @Bean
    SimulatedJposPrinter getMyPrinter() {
        return simulatedPrinter;
    }
}
