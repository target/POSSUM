package com.target.devicemanager.components.linedisplay;

import com.target.devicemanager.common.*;
import com.target.devicemanager.components.linedisplay.simulator.SimulatedJposLineDisplay;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.LineDisplay;
import jpos.config.JposEntryRegistry;
import jpos.loader.JposServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local","dev","prod"})
class LineDisplayConfig {

    private final SimulatedJposLineDisplay simulatedLineDisplay;
    private final ApplicationConfig applicationConfig;

    @Autowired
    LineDisplayConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.simulatedLineDisplay = new SimulatedJposLineDisplay();
    }

    @Bean
    public LineDisplayManager getLineDisplayManager() {
        DynamicDevice<LineDisplay> dynamicLineDisplay;
        JposEntryRegistry deviceRegistry = JposServiceLoader.getManager().getEntryRegistry();

        if (applicationConfig.IsSimulationMode()) {
            dynamicLineDisplay = new DynamicDevice<>(simulatedLineDisplay, new DevicePower(), new DeviceConnector<>(simulatedLineDisplay, deviceRegistry));
        } else {
            LineDisplay lineDisplay = new LineDisplay();
            dynamicLineDisplay = new DynamicDevice<>(lineDisplay, new DevicePower(), new DeviceConnector<>(lineDisplay, deviceRegistry ));
        }

        LineDisplayManager lineDisplayManager = new LineDisplayManager(
                new LineDisplayDevice(dynamicLineDisplay));

        DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton().setLineDisplayManager(lineDisplayManager);
        return lineDisplayManager;
    }

    @Bean
    SimulatedJposLineDisplay getSimulatedLineDisplay() {
        return simulatedLineDisplay;
    }
}
