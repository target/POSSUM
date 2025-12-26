package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.components.cashdrawer.entities.CashDrawerError;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;
import java.util.concurrent.locks.Lock;

@Profile({"local", "dev", "prod"})
@EnableScheduling
@EnableCaching
public class CashDrawerManager {

    @Autowired
    private CacheManager cacheManager;

    private final CashDrawerDevice cashDrawerDevice;
    private final Lock cashDrawerLock;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(CashDrawerManager.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("CashDrawer", "CashDrawerManager", LOGGER);

    public CashDrawerManager(CashDrawerDevice cashDrawerDevice, Lock cashDrawerLock) {
        this(cashDrawerDevice, cashDrawerLock, null);
    }

    public CashDrawerManager(CashDrawerDevice cashDrawerDevice, Lock cashDrawerLock, CacheManager cacheManager) {
        if (cashDrawerDevice == null) {
            log.failure("cashDrawerDevice cannot be null", 17, new IllegalArgumentException("cashDrawerDevice cannot be null"));
            throw new IllegalArgumentException("cashDrawerDevice cannot be null");
        }
        if (cashDrawerLock == null) {
            log.failure("cashDrawerLock cannot be null", 17, new IllegalArgumentException("cashDrawerLock cannot be null"));
            throw new IllegalArgumentException("cashDrawerLock cannot be null");
        }
        this.cashDrawerDevice = cashDrawerDevice;
        this.cashDrawerLock = cashDrawerLock;

        if(cacheManager != null) {
            this.cacheManager = cacheManager;
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        if (cashDrawerDevice.tryLock()) {
            try {
                cashDrawerDevice.connect();
            } finally {
                cashDrawerDevice.unlock();
            }
        }

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectDevice() throws DeviceException {
        if (cashDrawerDevice.tryLock()) {
            try {
                cashDrawerDevice.disconnect();
                if (!cashDrawerDevice.connect()) {
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            } finally {
                cashDrawerDevice.unlock();
            }
        }
        else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
    }

    public void openCashDrawer() throws DeviceException {
        if (!cashDrawerLock.tryLock()) {
            DeviceException cashDrawerException = new DeviceException(CashDrawerError.DEVICE_BUSY);
            throw cashDrawerException;
        }
        try {
            cashDrawerDevice.openCashDrawer();
        } catch (JposException jposException) {
            DeviceException cashDrawerException = new DeviceException(jposException);
            throw cashDrawerException;
        } catch (DeviceException cashDrawerException) {
            throw cashDrawerException;
        } finally {
            cashDrawerLock.unlock();
        }
    }

    public DeviceHealthResponse getHealth() {
        DeviceHealthResponse deviceHealthResponse;
        if (cashDrawerDevice.isConnected()) {
            deviceHealthResponse = new DeviceHealthResponse(cashDrawerDevice.getDeviceName(), DeviceHealth.READY);
        } else {
            deviceHealthResponse = new DeviceHealthResponse(cashDrawerDevice.getDeviceName(), DeviceHealth.NOTREADY);
        }
        try {
            Objects.requireNonNull(cacheManager.getCache("cashDrawerHealth")).put("health", deviceHealthResponse);
        } catch (Exception exception) {
            log.failure("getCache(cashDrawerHealth) Failed", 17, exception);
        }
        return deviceHealthResponse;
    }

    public DeviceHealthResponse getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("cashDrawerHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (DeviceHealthResponse) Objects.requireNonNull(cacheManager.getCache("cashDrawerHealth")).get("health").get();
            } else {
                log.success("Not able to retrieve from cache, checking getHealth()", 5);
                return getHealth();
            }
        } catch (Exception exception) {
            log.failure("getStatus() failed, falling back to getHealth()", 13, exception);
            return getHealth();
        }
    }
}
