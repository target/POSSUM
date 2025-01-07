package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.scanner.entities.Barcode;
import com.target.devicemanager.components.scanner.entities.ScannerError;
import com.target.devicemanager.components.scanner.entities.ScannerException;
import com.target.devicemanager.components.scanner.entities.ScannerType;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

@EnableScheduling
@EnableCaching
public class ScannerManager {

    @Autowired
    private CacheManager cacheManager;

    private final List<? extends ScannerDevice> scanners;
    private final Lock scannerLock;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScannerManager.class);
    private ExecutorService executor;
    private boolean isTest = false;
    private List<Future<Boolean>> results;

    public ScannerManager(List<? extends ScannerDevice> scanners, Lock scannerLock) {
        this(scanners, scannerLock, null, null, null, false);
    }

    public ScannerManager(List<? extends ScannerDevice> scanners, Lock scannerLock, CacheManager cacheManager, ExecutorService executor, List<Future<Boolean>> results, boolean isTest) {
        if (scanners == null) {
            throw new IllegalArgumentException("scanners cannot be null");
        }
        if (scannerLock == null) {
            throw new IllegalArgumentException("scannerLock cannot be null");
        }
        this.scanners = scanners;
        this.scannerLock = scannerLock;
        this.executor = executor;
        this.results = results;

        if(cacheManager != null) {
            this.cacheManager = cacheManager;
        }

        this.isTest = isTest;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        scanners.forEach(ScannerDevice::connect);

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            for (ScannerDevice scanner : scanners) {
                if(!scanner.isConnected()) {
                    LOGGER.error(scanner.getScannerType() + " Failed to Connect");
                }
            }
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectScanners() throws DeviceException {
        List<Callable<Boolean>> taskList = new ArrayList<>();
        scanners.forEach(scanner -> taskList.add(scanner::reconnect));
        if(!isTest) {
            executor = Executors.newFixedThreadPool(taskList.size());
        }
        try {
            List<Future<Boolean>> executorInvoked = executor.invokeAll(taskList);
            if(!isTest) {
                results = executorInvoked;
            }
            for(Future<Boolean> result: results) {
                if(!result.get()) {
                    LOGGER.error("Failed to reconnect scanner.");
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            }
        } catch (ExecutionException exception) {
            DeviceException deviceException = (DeviceException) exception.getCause();
            throw deviceException;
        } catch (InterruptedException interruptedException) {
            throw new DeviceException(DeviceError.UNEXPECTED_ERROR);
        } finally {
            executor.shutdown();
        }
    }

    Barcode getData(ScannerType scannerType) throws ScannerException {
        LOGGER.trace("getData(in)");
        if (!scannerLock.tryLock()) {
            ScannerException scannerException = new ScannerException(ScannerError.DEVICE_BUSY);
            LOGGER.trace("getData(out)");
            throw scannerException;
        }
        try {
            return enableScanners(scannerType);
        } finally {
            scannerLock.unlock();
            LOGGER.trace("getData(out)");
        }
    }

    private Barcode enableScanners(ScannerType scannerType) throws ScannerException {
        LOGGER.trace("enableScanners(in)");
        List<Callable<Barcode>> taskList = new ArrayList<>();
        for (ScannerDevice scanner : scanners) {
            switch (scannerType.name()) {
                case "FLATBED":
                case "HANDHELD":
                    if (scanner.getScannerType().equals(scannerType.name())) {
                        taskList.add(scanner::getScannerData);
                    }
                    break;
                default:
                    taskList.add(scanner::getScannerData);
            }
        }
        if(!isTest) {
            executor = Executors.newFixedThreadPool(taskList.size());
        }
        try {
            Barcode barcode = executor.invokeAny(taskList);
            disableScanners();
            return barcode;
        } catch (ExecutionException | InterruptedException exception) {
            ScannerException scannerException;
            Throwable cause = exception.getCause();
            if (cause instanceof JposException) {
                scannerException = new ScannerException((JposException) cause);
            } else {
                LOGGER.error("Exception occurred: " + exception.getMessage());
                scannerException = new ScannerException(ScannerError.UNEXPECTED_ERROR);
            }
            LOGGER.debug("enableScanners(): " + scannerException.getDeviceError().getDescription());
            LOGGER.trace("enableScanners(out)");
            throw scannerException;
        }
        finally {
            LOGGER.trace("enableScanners(out)");
            executor.shutdown();
        }
    }

    void cancelScanRequest() throws ScannerException {
        LOGGER.trace("cancelScanRequest(in)");
        //This makes sure no new scan data requests come in while we are cancelling
        if (scannerLock.tryLock()) {
            //Nothing to disable
            try {
                ScannerException scannerException = new ScannerException(ScannerError.ALREADY_DISABLED);
                LOGGER.trace("cancelScanRequest(out)");
                throw scannerException;
            } finally {
                scannerLock.unlock();
            }
        }
        try {
            disableScanners();
        } catch (InterruptedException exception) {
            ScannerException scannerException = new ScannerException(ScannerError.UNEXPECTED_ERROR);
            LOGGER.trace("cancelScanRequest(out)");
            throw scannerException;
        } catch (Exception exception) {
            LOGGER.info("Error in cancelScanRequest: " + exception.getMessage());
            throw new ScannerException(ScannerError.UNEXPECTED_ERROR);
        }
        LOGGER.trace("cancelScanRequest(out)");
    }

    public List<DeviceHealthResponse> getHealth(ScannerType scannerType) {
        LOGGER.trace("getHealth(in)");
        List<DeviceHealthResponse> response = new ArrayList<>();
        for (ScannerDevice scanner : scanners) {
            switch (scannerType.name()) {
                case "FLATBED":
                case "HANDHELD":
                    if(scanner.getScannerType().equals(scannerType.name())) {
                        if (scanner.isConnected()) {
                            response.add(new DeviceHealthResponse(scanner.getDeviceName(), DeviceHealth.READY));
                        } else {
                            response.add(new DeviceHealthResponse(scanner.getDeviceName(), DeviceHealth.NOTREADY));
                        }
                    }
                    break;
                default:
                    if (scanner.isConnected()) {
                        response.add(new DeviceHealthResponse(scanner.getDeviceName(), DeviceHealth.READY));
                    } else {
                        response.add(new DeviceHealthResponse(scanner.getDeviceName(), DeviceHealth.NOTREADY));
                    }
            }
        }
        try {
            Objects.requireNonNull(cacheManager.getCache("scannerHealth")).put("health", response);
        } catch (Exception exception) {
            LOGGER.error("getCache(scannerHealth) Failed: " + exception.getMessage());
        }
        LOGGER.trace("getHealth(out)");
        return response;
    }

    public List<DeviceHealthResponse> getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("scannerHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth(ScannerType.BOTH);
                }
                return (List<DeviceHealthResponse>) Objects.requireNonNull(cacheManager.getCache("scannerHealth")).get("health").get();
            } else {
                LOGGER.debug("Not able to retrieve from cache, checking getHealth()");
                return getHealth(ScannerType.BOTH);
            }
        } catch (Exception exception) {
            return getHealth(ScannerType.BOTH);
        }
    }

    public DeviceHealth getScannerHealthStatus(String scannerType) {
        String scannerName = "";
        for (ScannerDevice scanner : scanners) {
            switch (scannerType) {
                case "FLATBED":
                case "HANDHELD":
                    if (scanner.getScannerType().equals(scannerType)) {
                        scannerName = scanner.getDeviceName();
                    }
                    break;
                default:
                   scannerName = "";
            }
        }
        for(DeviceHealthResponse deviceHealthResponse: getStatus()) {
            if(deviceHealthResponse.getDeviceName().equals(scannerName)) {
                return deviceHealthResponse.getHealthStatus();
            }
        }
        return new DeviceHealthResponse(scannerName, DeviceHealth.NOTREADY).getHealthStatus();
    }

    private void disableScanners() throws InterruptedException {
        LOGGER.trace("disableScanners(in)");
        List<Callable<Void>> taskList = new ArrayList<>();
        try {
            scanners.forEach(scanner -> taskList.add(scanner::cancelScannerData));
            if(!isTest) {
                executor = Executors.newFixedThreadPool(taskList.size());
            }
            executor.invokeAll(taskList);
            executor.shutdown();
        } catch (InterruptedException interruptedException) {
            throw interruptedException;
        }
        LOGGER.trace("disableScanner(out)");
    }
}
