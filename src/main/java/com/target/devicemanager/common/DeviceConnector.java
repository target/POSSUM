package com.target.devicemanager.common;

import jpos.BaseJposControl;
import jpos.JposException;
import jpos.config.JposEntryRegistry;
import jpos.config.simple.SimpleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class DeviceConnector<T extends BaseJposControl> {

    private final T device;
    private final AbstractMap.SimpleEntry<String, String> customFilter;
    private final JposEntryRegistry deviceRegistry;
    private String connectedDeviceName;
    private static final int CLAIM_TIMEOUT_IN_MSEC = 30000;
    private final int RETRY_REGISTRY_LOAD = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConnector.class);

    public DeviceConnector(T device, JposEntryRegistry deviceRegistry) {
        this(device, deviceRegistry, null);
    }

    public DeviceConnector(T device, JposEntryRegistry deviceRegistry, AbstractMap.SimpleEntry<String, String> customFilter) {
        if (device == null) {
            throw new IllegalArgumentException("device cannot be null");
        }
        if (deviceRegistry == null) {
            throw new IllegalArgumentException("deviceRegistry cannot be null");
        }
        this.device = device;
        this.customFilter = customFilter;
        this.deviceRegistry = deviceRegistry;
        this.connectedDeviceName = getDefaultDeviceName();
    }

    boolean discoverConnectedDevice() {
        List<String> configNames = getLogicalNamesForDeviceType();
        for (String configName : configNames) {
            clearDeviceCache(); //this clears any caches that exist (both datalogic and ncr have caches that need to get cleared)
            boolean isConnected = connect(configName);
            if (isConnected) {
                LOGGER.info("device found '" + connectedDeviceName + "'");
                return true;
            }
        }
        return false;
    }

    String getConnectedDeviceName() {
        return this.connectedDeviceName;
    }

    private void clearDeviceCache() {
        synchronized (device) {
            try {
                device.setDeviceEnabled(false);
            } catch (Exception exception) {
                LOGGER.trace("failed to disable device '" + getDefaultDeviceName() + "'" + exception);
            }
            try {
                device.release();
            } catch (Exception exception) {
                LOGGER.trace("failed to release device '" + getDefaultDeviceName() + "'" + exception);
            }
            try {
                device.close();
            } catch (Exception exception) {
                LOGGER.trace("failed to close device '" + getDefaultDeviceName() + "'" + exception);
            }
        }
    }

    private boolean connect(String configName) {
            synchronized (device) {
                try {
                    device.open(configName);
                } catch (JposException jposException){
                    LOGGER.error("failed to open " + configName + " with error " + jposException.getErrorCode());
                    return false;
                }
                try {
                    device.claim(CLAIM_TIMEOUT_IN_MSEC);
                } catch (JposException jposException){
                    LOGGER.error("failed to claim " + configName + " with error " + jposException.getErrorCode());
                    return false;
                }
                //this is a test, some devices wont signal connected status until enabled
                //then disable to put it back in the same state
                try {
                    device.setDeviceEnabled(true);
                } catch (JposException jposException){
                    LOGGER.error("failed to enable " + configName + " with error " + jposException.getErrorCode());
                    return false;
                }
                try {
                    device.setDeviceEnabled(false);
                } catch (JposException jposException){
                    LOGGER.error("failed to disable " + configName + " with error " + jposException.getErrorCode());
                    return false;
                }
                this.connectedDeviceName = configName;
                LOGGER.info("successfully connected " + configName);
                return true;
            }
    }

    private List<String> getLogicalNamesForDeviceType() {
        for(int i = 0; i < RETRY_REGISTRY_LOAD && deviceRegistry.getSize() == 0; i++) {
            deviceRegistry.load();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                //ignore
            }
        }

        ArrayList<SimpleEntry> list = Collections.list((Enumeration<SimpleEntry>)deviceRegistry.getEntries());
        return list
                .stream()
                .filter(x -> {
                    String deviceCategory = x.getPropertyValue("deviceCategory").toString();
                    Class<? extends BaseJposControl> deviceClass = device.getClass();
                    return deviceCategory.equals(deviceClass.getSimpleName());
                })
                .filter(x -> {
                    if (customFilter == null) return true;

                    String customFilterValue = x.getPropertyValue(customFilter.getKey()).toString();
                    return customFilter.getValue().equals(customFilterValue);
                })
                .map(x -> x.getPropertyValue("logicalName").toString())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String getDefaultDeviceName() {
        if(this.customFilter != null) {
            return customFilter.getValue();
        }
        return device.getClass().getSimpleName();
    }
}
