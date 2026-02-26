package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.LogPayloadBuilder;
import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.printer.entities.PrinterContent;
import com.target.devicemanager.components.printer.entities.PrinterError;
import com.target.devicemanager.components.printer.entities.PrinterException;
import com.target.devicemanager.components.printer.entities.PrinterStationType;
import jpos.JposConst;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

@EnableScheduling
@EnableCaching
public class PrinterManager {

    @Autowired
    private CacheManager cacheManager;

    private final PrinterDevice printerDevice;
    private final Lock printerLock;
    private static final int PRINTER_TIMEOUT = 10;  // Timeout value for printContent call in seconds
    private Future<Void> future;
    private boolean isTest = false;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterManager.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("printer", "PrinterManager", LOGGER);

    public PrinterManager(PrinterDevice printerDevice, Lock printerLock) {
        this(printerDevice, printerLock, null, null, false);
    }

    public PrinterManager(PrinterDevice printerDevice, Lock printerLock, CacheManager cacheManager, Future<Void> future, boolean isTest) {
        if (printerDevice == null) {
            throw new IllegalArgumentException("printerDevice cannot be null");
        }
        if (printerLock == null) {
            throw new IllegalArgumentException("printerLock cannot be null");
        }

        this.printerDevice = printerDevice;
        this.printerLock = printerLock;

        if(cacheManager != null) {
            this.cacheManager = cacheManager;
        }

        if(future != null) {
            this.future = future;
        }

        this.isTest = isTest;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        if (printerDevice.tryLock()) {
            try {
                printerDevice.connect();
            } finally {
                printerDevice.unlock();
            }
        }

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectDevice() throws DeviceException {
        if (printerDevice.tryLock()) {
            try {
                printerDevice.disconnect();
                if (!printerDevice.connect()) {
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            } finally {
                printerDevice.unlock();
            }
        }
        else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
    }

    public void printReceipt(List<PrinterContent> contents) throws DeviceException {
        if (!printerLock.tryLock()) {
            PrinterException printerException = new PrinterException(PrinterError.DEVICE_BUSY);
            throw printerException;
        }

        ExecutorService executorService = null;
        Callable<Void> task;

        try {
            executorService = Executors.newSingleThreadExecutor();
            task = () -> printerDevice.printContent(contents,PrinterStationType.RECEIPT_PRINTER.getValue());
            if(!isTest) {
                future = executorService.submit(task);
            }
            future.get(PRINTER_TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause();
            PrinterException printerException;
            if (cause instanceof PrinterException) {
                printerException = (PrinterException) cause;
            }
            else {  // JposException
                printerException = new PrinterException((JposException) cause);
            }
            log.failure(printerException.getDeviceError().getDescription(),17, printerException);
            throw printerException;
        } catch (TimeoutException timeoutException) {
            log.failure(PrinterError.PRINTER_TIME_OUT.getDescription(),17, timeoutException);
            throw new PrinterException(PrinterError.PRINTER_TIME_OUT);
        } catch (InterruptedException exception) {
            PrinterException printerException = new PrinterException(new JposException(JposConst.JPOS_E_FAILURE));
            log.failure(printerException.getDeviceError().getDescription(),17, printerException);
            throw printerException;
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
            printerLock.unlock();
        }
    }

    public void frankCheck(List<PrinterContent> contents) throws PrinterException {
        if (!printerLock.tryLock()) {
            PrinterException printerException = new PrinterException(PrinterError.DEVICE_BUSY);
            throw printerException;
        }

        try {
            printerDevice.printContent(contents,PrinterStationType.CHECK_PRINTER.getValue());
        } catch (JposException exception) {
            PrinterException printerException = new PrinterException(exception);
            throw printerException;
        } finally {
            printerLock.unlock();
        }
    }

    public DeviceHealthResponse getHealth() {
        DeviceHealthResponse deviceHealthResponse;
        if (printerDevice.isConnected()) {
            deviceHealthResponse = new DeviceHealthResponse(printerDevice.getDeviceName(), DeviceHealth.READY);
        } else {
            deviceHealthResponse = new DeviceHealthResponse(printerDevice.getDeviceName(), DeviceHealth.NOTREADY);
        }
        try {
            Objects.requireNonNull(cacheManager.getCache("printerHealth")).put("health", deviceHealthResponse);
        } catch (Exception exception) {
            log.failure("getCache(printerHealth) Failed",17, exception);
        }
        return deviceHealthResponse;
    }

    public DeviceHealthResponse getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("printerHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (DeviceHealthResponse) Objects.requireNonNull(cacheManager.getCache("printerHealth")).get("health").get();
            } else {
                log.failure("Not able to retrieve from cache, checking getHealth()", 5, null);
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }

    public static int getPrinterTimeoutValue() {
        return PRINTER_TIMEOUT;
    }
}
