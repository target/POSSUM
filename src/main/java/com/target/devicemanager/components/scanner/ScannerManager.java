package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.StructuredEventLogger;
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
    private static final StructuredEventLogger log = StructuredEventLogger.of("scanner", "ScannerManager", LOGGER);
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
                    log.failure(scanner.getScannerType() + " Failed to Connect", 17, null);
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
                    log.failure("Failed to reconnect scanner.", 17, null);
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            }
        } catch (ExecutionException exception) {
            DeviceException deviceException = (DeviceException) exception.getCause();
            throw deviceException;
        } catch (InterruptedException interruptedException) {
            throw new DeviceException(DeviceError.UNEXPECTED_ERROR);
        } finally {
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    Barcode getData(ScannerType scannerType) throws ScannerException {
        log.success("getData(in)", 1);
        if (!scannerLock.tryLock()) {
            ScannerException scannerException = new ScannerException(ScannerError.DEVICE_BUSY);
            log.success("getData(out) - device busy", 1);
            throw scannerException;
        }
        try {
            return enableScanners(scannerType);
        } finally {
            scannerLock.unlock();
            log.success("getData(out)", 1);
        }
    }

    private Barcode enableScanners(ScannerType scannerType) throws ScannerException {
        log.success("enableScanners(in)", 1);
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
                log.failure("Exception occurred in enableScanners: " + exception.getMessage(), 17, exception);
                scannerException = new ScannerException(ScannerError.UNEXPECTED_ERROR);
            }
            log.success("enableScanners(): " + scannerException.getDeviceError().getDescription(), 1);
            log.success("enableScanners(out)", 1);
            throw scannerException;
        }
        finally {
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    void cancelScanRequest() throws ScannerException {
        log.success("cancelScanRequest(in)", 1);
        //This makes sure no new scan data requests come in while we are cancelling
        if (scannerLock.tryLock()) {
            //Nothing to disable
            try {
                ScannerException scannerException = new ScannerException(ScannerError.ALREADY_DISABLED);
                log.success("cancelScanRequest(out) - already disabled", 1);
                throw scannerException;
            } finally {
                scannerLock.unlock();
            }
        }
        try {
            disableScanners();
        } catch (InterruptedException exception) {
            ScannerException scannerException = new ScannerException(ScannerError.UNEXPECTED_ERROR);
            log.failure("Interrupted while cancelling scan request", 17, exception);
            throw scannerException;
        } catch (Exception exception) {
            log.failure("Error in cancelScanRequest: " + exception.getMessage(), 17, exception);
        }
        log.success("cancelScanRequest(out)", 1);
    }

    public List<DeviceHealthResponse> getHealth(ScannerType scannerType) {
        log.success("getHealth(in)", 1);
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
            log.failure("getCache(scannerHealth) Failed: " + exception.getMessage(), 17, exception);
        }
        log.success("getHealth(out)", 1);
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
                log.success("Not able to retrieve from cache, checking getHealth()", 6);
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
        log.success("disableScanners(in)", 1);
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
        log.success("disableScanner(out)", 1);
    }
}
