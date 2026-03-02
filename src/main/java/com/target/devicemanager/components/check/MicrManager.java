package com.target.devicemanager.components.check;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.common.events.ConnectionEventListener;
import com.target.devicemanager.components.check.entities.MicrData;
import com.target.devicemanager.components.check.entities.MicrDataEvent;
import com.target.devicemanager.components.check.entities.MicrErrorEvent;
import com.target.devicemanager.components.check.entities.MicrException;
import jpos.JposConst;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@EnableScheduling
@EnableCaching
public class MicrManager implements MicrEventListener, ConnectionEventListener {

    @Autowired
    private CacheManager cacheManager;

    private final MicrDevice micrDevice;
    private CompletableFuture<MicrData> micrDataClient = null;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrManager.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of(StructuredEventLogger.getCheckServiceName(), "MicrManager", LOGGER);

    public MicrManager(MicrDevice micrDevice) {
        this(micrDevice, null, null);
    }

    public MicrManager(MicrDevice micrDevice, CacheManager cacheManager, CompletableFuture<MicrData> micrDataClient) {
        if (micrDevice == null) {
            throw new IllegalArgumentException("micrDevice cannot be null");
        }
        this.micrDevice = micrDevice;
        this.micrDevice.addMicrEventListener(this);
        this.micrDevice.addConnectionEventListener(this);

        if(cacheManager != null) {
            this.cacheManager = cacheManager;
        }

        this.micrDataClient = micrDataClient;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        if (micrDevice.tryLock()) {
            try {
                micrDevice.connect();
            } finally {
                micrDevice.unlock();
            }
        }

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectDevice() throws DeviceException {
        if (micrDevice.tryLock()) {
            try {
                micrDevice.disconnect();
                if (!micrDevice.connect()) {
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            } finally {
                micrDevice.unlock();
            }
        } else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
    }

    MicrData readMICR(CompletableFuture<MicrData> micrDataClient) throws MicrException {
        this.micrDataClient = micrDataClient;
        micrDevice.setCheckCancelReceived(false);
        micrDevice.insertCheck();
        try {
            //Timeout as a double check against timing errors that would cause us to hang forever
            return micrDataClient.get();
        } catch (ExecutionException executionException) {
            cancelCheckRead();
            ejectCheck();
            Throwable jposException = executionException.getCause();
            MicrException micrException = new MicrException((JposException)jposException);
            throw micrException;
        } catch (InterruptedException interruptedException) {
            cancelCheckRead();
            ejectCheck();
            MicrException micrException = new MicrException(new JposException(JposConst.JPOS_E_FAILURE));
            throw micrException;
        }
    }

    public void cancelCheckRead(){
        if (micrDataClient != null) {
            micrDataClient.cancel(true);
        }
        micrDevice.setCheckCancelReceived(true);
    }

    public void ejectCheck() {
        try {
            micrDevice.withdrawCheck();
        } catch (JposException exception) {
            MicrException micrException = new MicrException(exception);
            log.failure(micrException.getDeviceError().getDescription(), 13, micrException);
        }
    }

    @Override
    public void micrDataEventOccurred(MicrDataEvent micrDataEvent) {
        log.success("micrDataEventOccurred()", 5);
        if (this.micrDataClient != null) {
            this.micrDataClient.complete(micrDataEvent.getMicrData());
        }
    }

    @Override
    public void micrErrorEventOccurred(MicrErrorEvent micrErrorEvent) {
        log.success("micrErrorEventOccurred(): " + micrErrorEvent.getError(), 5);
        if (this.micrDataClient != null) {
            this.micrDataClient.completeExceptionally(micrErrorEvent.getError());
        }
    }

    @Override
    public void connectionEventOccurred(ConnectionEvent connectionEvent) {
    }

    public DeviceHealthResponse getHealth() {
        DeviceHealthResponse deviceHealthResponse;
        if (micrDevice.isConnected()) {
            deviceHealthResponse = new DeviceHealthResponse(micrDevice.getDeviceName(), DeviceHealth.READY);
        } else {
            deviceHealthResponse = new DeviceHealthResponse(micrDevice.getDeviceName(), DeviceHealth.NOTREADY);
        }
        try {
            Objects.requireNonNull(cacheManager.getCache("micrHealth")).put("health", deviceHealthResponse);
        } catch (Exception exception) {
            log.failure("getCache(micrHealth) Failed", 17, exception);
        }
        return deviceHealthResponse;
    }

    public DeviceHealthResponse getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("micrHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (DeviceHealthResponse) Objects.requireNonNull(cacheManager.getCache("micrHealth")).get("health").get();
            } else {
                log.success("Not able to retrieve from cache, checking getHealth()", 5);
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }
}
