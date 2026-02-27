package com.target.devicemanager.components.scale;

import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.entities.*;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.common.events.ConnectionEventListener;

import com.target.devicemanager.components.scale.entities.*;
import jpos.JposConst;
import jpos.JposException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@EnableScheduling
@EnableCaching
public class ScaleManager implements ScaleEventListener, ConnectionEventListener {

    @Autowired
    private CacheManager cacheManager;

    private final ScaleDevice scaleDevice;
    private boolean isScaleReady = false;
    private final List<SseEmitter> liveWeightClients;
    private final List<CompletableFuture<FormattedWeight>> stableWeightClients;
    private static final int STABLE_WEIGHT_TIMEOUT_MSEC = 10000;
    private static final int HANG_TIMEOUT_MSEC = STABLE_WEIGHT_TIMEOUT_MSEC + 20000;
    private ConnectEnum connectStatus = ConnectEnum.FIRST_CONNECT;
    private List<SseEmitter> deadEmitterList;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScaleManager.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of(StructuredEventLogger.getScaleServiceName(), "ScaleManager", LOGGER);

    public ScaleManager(ScaleDevice scaleDevice, List<SseEmitter> liveWeightClients, List<CompletableFuture<FormattedWeight>> stableWeightClients) {
        this(scaleDevice, liveWeightClients, stableWeightClients, null, null);
    }

    public ScaleManager(ScaleDevice scaleDevice, List<SseEmitter> liveWeightClients, List<CompletableFuture<FormattedWeight>> stableWeightClients, CacheManager cacheManager, List<SseEmitter> deadEmitterList) {
        if (scaleDevice == null) {
            throw new IllegalArgumentException("scaleDevice cannot be null");
        }
        if (liveWeightClients == null) {
            throw new IllegalArgumentException("liveWeightClients cannot be null");
        }
        if (stableWeightClients == null) {
            throw new IllegalArgumentException("stableWeightClients cannot be null");
        }
        this.scaleDevice = scaleDevice;
        this.liveWeightClients = liveWeightClients;
        this.stableWeightClients = stableWeightClients;
        this.scaleDevice.addScaleEventListener(this);
        this.scaleDevice.addConnectionEventListener(this);

        if(cacheManager != null) {
            this.cacheManager = cacheManager;
        }

        if(deadEmitterList != null) {
            this.deadEmitterList = deadEmitterList;
        } else {
            this.deadEmitterList = new ArrayList<>();
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void connect() {
        if (scaleDevice.tryLock()) {
            try {
                scaleDevice.connect();
            } finally {
                scaleDevice.unlock();
            }
        }

        if (connectStatus == ConnectEnum.FIRST_CONNECT) {
            connectStatus = ConnectEnum.CHECK_HEALTH;
        }
    }

    public void reconnectDevice() throws DeviceException {
        if (scaleDevice.tryLock()) {
            try {
                scaleDevice.disconnect();
                if(!scaleDevice.connect()) {
                    throw new DeviceException(DeviceError.DEVICE_OFFLINE);
                }
            } finally {
                scaleDevice.unlock();
            }
        }
        else {
            throw new DeviceException(DeviceError.DEVICE_BUSY);
        }
    }

    void subscribeToLiveWeight(SseEmitter liveWeightEmitter) throws IOException {
        liveWeightEmitter.onCompletion(() -> this.liveWeightClients.remove(liveWeightEmitter));
        liveWeightEmitter.onTimeout(() -> this.liveWeightClients.remove(liveWeightEmitter));
        liveWeightClients.add(liveWeightEmitter);
        liveWeightEmitter.send(scaleDevice.getLiveWeight(), MediaType.APPLICATION_JSON);
    }

    public FormattedWeight getStableWeight(CompletableFuture<FormattedWeight> stableWeightClient) throws ScaleException {
        if (isScaleReady() && scaleDevice.tryLock()) {
            //Create new future and add it to the list
            stableWeightClients.add(stableWeightClient);
            scaleDevice.startStableWeightRead(STABLE_WEIGHT_TIMEOUT_MSEC);
            try {
                //Timeout as a double check against timing errors that would cause us to hang forever
                return stableWeightClient.get(HANG_TIMEOUT_MSEC, TimeUnit.MILLISECONDS);
            } catch (ExecutionException executionException) {
                Throwable jposException = executionException.getCause();
                throw (new ScaleException((JposException)jposException));
            } catch (InterruptedException interruptedException) {
                throw (new ScaleException(new JposException(JposConst.JPOS_E_FAILURE)));
            } catch (TimeoutException timeoutException) {
                throw (new ScaleException(new JposException(JposConst.JPOS_E_TIMEOUT)));
            }
            finally {
                scaleDevice.unlock();
            }
        } else {
            if(!isScaleReady()){
                throw (new ScaleException(new JposException(JposConst.JPOS_E_OFFLINE)));
            }
            log.failure("Scale Device Busy. Please Wait To Get Stable Weight.", 17, null);
            throw (new ScaleException(new JposException(JposConst.JPOS_E_BUSY)));
        }
    }

    @Override
    public void scaleLiveWeightEventOccurred(WeightEvent liveWeightEvent) {
        this.liveWeightClients.forEach(emitter -> {
            try {
                emitter.send(liveWeightEvent.getWeight(), MediaType.APPLICATION_JSON);
            } catch(IOException ioException) {
                //Remove the client from the connection pool
                deadEmitterList.add(emitter);
            }
        });
        this.liveWeightClients.removeAll(deadEmitterList);
        deadEmitterList.clear();
    }

    @Override
    public void scaleWeightErrorEventOccurred(WeightErrorEvent weightErrorEvent) {
        this.stableWeightClients.forEach(client -> client.completeExceptionally(weightErrorEvent.getError()));
        this.stableWeightClients.clear();
    }

    @Override
    public void scaleStableWeightDataEventOccurred(WeightEvent stableWeightEvent) {
        this.stableWeightClients.forEach(client -> client.complete(stableWeightEvent.getWeight()));
        this.stableWeightClients.clear();
    }

    @Override
    public void connectionEventOccurred(ConnectionEvent connectionEvent) {
        isScaleReady = connectionEvent.isConnected();
    }

    /**
     * This method is only used to get "isScaleReady" for unit testing
     * @return
     */
    public boolean getIsScaleReady() {
        return isScaleReady;
    }

    public boolean isScaleReady() {
        return scaleDevice.isConnected();
    }

    public DeviceHealthResponse getHealth() {
        DeviceHealthResponse deviceHealthResponse;
        if (isScaleReady()) {
            deviceHealthResponse = new DeviceHealthResponse(scaleDevice.getDeviceName(), DeviceHealth.READY);
        } else {
            deviceHealthResponse = new DeviceHealthResponse(scaleDevice.getDeviceName(), DeviceHealth.NOTREADY);
        }
        try {
            Objects.requireNonNull(cacheManager.getCache("scaleHealth")).put("health", deviceHealthResponse);
        } catch (Exception exception) {
            log.failure("getCache(scaleHealth) Failed", 17, exception);
        }
        return deviceHealthResponse;
    }

    public DeviceHealthResponse getStatus() {
        try {
            if (cacheManager != null && Objects.requireNonNull(cacheManager.getCache("scaleHealth")).get("health") != null) {
                if (connectStatus == ConnectEnum.CHECK_HEALTH) {
                    connectStatus = ConnectEnum.HEALTH_UPDATED;
                    return getHealth();
                }
                return (DeviceHealthResponse) Objects.requireNonNull(cacheManager.getCache("scaleHealth")).get("health").get();
            } else {
                log.success("Not able to retrieve from cache, checking getHealth()", 5);
                return getHealth();
            }
        } catch (Exception exception) {
            return getHealth();
        }
    }
}
