package com.target.devicemanager.components.scale;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.StructuredEventLogger;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.common.events.ConnectionEventListener;
import com.target.devicemanager.components.scale.entities.FormattedWeight;
import com.target.devicemanager.components.scale.entities.WeightErrorEvent;
import com.target.devicemanager.components.scale.entities.WeightEvent;
import jpos.JposConst;
import jpos.JposException;
import jpos.Scale;
import jpos.ScaleConst;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ScaleDevice implements StatusUpdateListener, ErrorListener {
    private static final int STABLE_WEIGHT_READ_TIMEOUT = 1000;
    private final DynamicDevice<Scale> dynamicScale;
    private final List<ScaleEventListener> scaleEventListeners;
    private final List<ConnectionEventListener> connectionEventListeners;
    private FormattedWeight currentLiveWeight;
    private boolean stableWeightInProgress;
    private final ReentrantLock connectLock;
    private boolean isLocked = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScaleDevice.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of("Scale", "ScaleDevice", LOGGER);
    private boolean deviceConnected = false;
    private int[] weight;

    /**
     * Initializes the Scale Device.
     * @param dynamicScale is the dynamic device.
     * @param scaleEventListeners listens for a scale event.
     * @param connectionEventListeners is an instance of event.
     */

    public ScaleDevice(DynamicDevice<Scale> dynamicScale, List<ScaleEventListener> scaleEventListeners, List<ConnectionEventListener> connectionEventListeners){
        this(dynamicScale, scaleEventListeners, connectionEventListeners, new ReentrantLock(true));
    }

    public ScaleDevice(DynamicDevice<Scale> dynamicScale, List<ScaleEventListener> scaleEventListeners, List<ConnectionEventListener> connectionEventListeners, ReentrantLock connectLock)  {
        if (dynamicScale == null) {
            log.failure("Scale Failed in Constructor: dynamicScale cannot be null", 17,
                    new IllegalArgumentException("dynamicScale cannot be null"));
            throw new IllegalArgumentException("dynamicScale cannot be null");
        }
        if (scaleEventListeners == null) {
            log.failure("Scale Failed in Constructor: scaleEventListeners cannot be null", 17,
                    new IllegalArgumentException("scaleEventListeners cannot be null"));
            throw new IllegalArgumentException("scaleEventListeners cannot be null");
        }
        if (connectionEventListeners == null) {
            log.failure("Scale Failed in Constructor: connectionEventListeners cannot be null", 17,
                    new IllegalArgumentException("connectionEventListeners cannot be null"));
            throw new IllegalArgumentException("connectionEventListeners cannot be null");
        }
        this.dynamicScale = dynamicScale;
        this.currentLiveWeight = new FormattedWeight();
        this.connectionEventListeners = connectionEventListeners;
        this.scaleEventListeners = scaleEventListeners;
        this.connectLock = connectLock;
        stableWeightInProgress = false;
        weight = new int[1];

        Scale scale = dynamicScale.getDevice();
        scale.addStatusUpdateListener(this);
    }

    /**
     * Adds a new instance to the list of  scale events.
     * @param scaleEventListener an instance of a scale event.
     */
    void addScaleEventListener(ScaleEventListener scaleEventListener) {
        this.scaleEventListeners.add(scaleEventListener);
    }

    /**
     *  linking manager to device and adding a new instance to List of connectionEventListeners.
     * @param connectionEventListener an instance of an event.
     */
    void addConnectionEventListener(ConnectionEventListener connectionEventListener) {
        this.connectionEventListeners.add(connectionEventListener);
    }

    /**
     * Sends event after connection.
     * @param connectStatus stating event happened.
     */
    private void fireConnectionEvent(boolean connectStatus) {
        this.connectionEventListeners.forEach(listener -> listener.connectionEventOccurred(new ConnectionEvent(this, connectStatus)));
    }

    /**
     * Sends scale event.
     * @param liveWeight the weight of current object on scale.
     */
    private void fireScaleLiveWeightEvent(FormattedWeight liveWeight) {
        currentLiveWeight = liveWeight;
        this.scaleEventListeners.forEach(listener -> listener.scaleLiveWeightEventOccurred(new WeightEvent(this, liveWeight)));
    }

    /**
     * sends stable weight event.
     * @param stableWeight the stable weight event happened.
     */
    private void fireScaleStableWeightDataEvent(FormattedWeight stableWeight) {
        this.scaleEventListeners.forEach(listener -> listener.scaleStableWeightDataEventOccurred(new WeightEvent(this, stableWeight)));
    }

    /**
     * sends weight error event.
     * @param weightError stating weight error event happened.
     */
    private void fireScaleWeightErrorEvent(JposException weightError) {
        this.scaleEventListeners.forEach(listener -> listener.scaleWeightErrorEventOccurred(new WeightErrorEvent(this, weightError)));
    }

    /**
     * Making sure a connection occurs.
     * @return connectionSuccessful
     */
    public boolean connect() {
        Scale scale = dynamicScale.getDevice();
        try {
            DynamicDevice.ConnectionResult connectionResult = dynamicScale.connect();
            if (connectionResult == DynamicDevice.ConnectionResult.NOT_CONNECTED) {
                scale.clearInput();
                return false;
            }
            if (scale.getStatusNotify() != ScaleConst.SCAL_SN_ENABLED) {
                scale.setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
            }
            if (!scale.getDataEventEnabled()) {
                scale.setDataEventEnabled(true);
            }
            if (!scale.getDeviceEnabled()) {
                scale.setDeviceEnabled(true);
            }
            //Only fire the connection even when first connected
            if (connectionResult == DynamicDevice.ConnectionResult.CONNECTED) {
                fireConnectionEvent(true);
            }
        } catch (JposException jposException) {
            log.failure("connect() failed", 17, jposException);
            return false;
        }
        deviceConnected = true;
        return true;
    }

    /**
     * Disconnects scale and starts an event.
     */
    void disconnect() {
        dynamicScale.disconnect();
        deviceConnected = false;
        fireConnectionEvent(false);
        fireScaleLiveWeightEvent(new FormattedWeight());
    }

    /**
     * This method is only used to set 'stableWeightInProgress' for unit testing
     * @param stableWeightInProgress
     */
    public void setStableWeightInProgress(boolean stableWeightInProgress) {
        this.stableWeightInProgress = stableWeightInProgress;
    }

    /**
     * This method is only used to set 'weight' for unit testing
     * @param weight
     */
    public void setWeight(int[] weight) {
        this.weight = weight;
    }

    /**
     * Starts the scale to read weight.
     * @param timeout time out time for device.
     */
    void startStableWeightRead(int timeout) {
        if(stableWeightInProgress) {
            //Only need to trigger this function once until returned
            return;
        }
        Scale scale;
        synchronized (scale = dynamicScale.getDevice()) {
            stableWeightInProgress = true;
            long currentTimeMsec = System.currentTimeMillis();
            long endTimeMsec = currentTimeMsec + timeout;
            while (currentTimeMsec <= endTimeMsec) {
                log.success("Read Weight Time Remaining " + (endTimeMsec - currentTimeMsec), 1);
                try {
                    scale.readWeight(weight, STABLE_WEIGHT_READ_TIMEOUT);
                    log.success("After ReadWeight " + weight[0], 1);
                    fireScaleStableWeightDataEvent(new FormattedWeight(weight[0]));
                    stableWeightInProgress = false;
                    weight = new int[1];
                    return;
                } catch (JposException jposException) {
                    int severity = isConnected() ? 1 : 1;
                    log.failure(isConnected()
                                    ? "Scale Failed to Read Stable Weight"
                                    : "Scale not connected in Read Stable Weight",
                            severity,
                            jposException);
                    if(jposException.getErrorCode() != JposConst.JPOS_E_TIMEOUT) {
                        fireScaleWeightErrorEvent(jposException);
                        stableWeightInProgress = false;
                        return;
                    }
                }
                currentTimeMsec = System.currentTimeMillis();
            }
            fireScaleWeightErrorEvent(new JposException(JposConst.JPOS_E_TIMEOUT));
            stableWeightInProgress = false;
        }
    }

    /**
     * Gets the status update whether scale is online/offline,Stable,zeroed,ready or overweight.
     * @param statusUpdateEvent
     */
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        log.success("statusUpdateOccurred(): " + statusUpdateEvent.getStatus(), 1);
        int status = statusUpdateEvent.getStatus();
        switch (status) {
            case JposConst.JPOS_SUE_POWER_OFF:
            case JposConst.JPOS_SUE_POWER_OFF_OFFLINE:
            case JposConst.JPOS_SUE_POWER_OFFLINE:
                log.failure("Scale Status Update: Power offline", 17, null);
                return;
            case JposConst.JPOS_SUE_POWER_ONLINE:
                connect();
                return;
            case ScaleConst.SCAL_SUE_STABLE_WEIGHT:
                Scale theScale = dynamicScale.getDevice();
                try {
                    int scaleWeight = theScale.getScaleLiveWeight();
                    fireScaleLiveWeightEvent(new FormattedWeight(scaleWeight));
                } catch (JposException jposException) {
                    log.failure("getScaleLiveWeight() failed", 13, jposException);
                    fireScaleLiveWeightEvent(new FormattedWeight());
                }
                break;
            case ScaleConst.SCAL_SUE_WEIGHT_ZERO:
                fireScaleLiveWeightEvent(new FormattedWeight(0));
                break;
            case ScaleConst.SCAL_SUE_NOT_READY:
            case ScaleConst.SCAL_SUE_WEIGHT_OVERWEIGHT:
            case ScaleConst.SCAL_SUE_WEIGHT_UNDER_ZERO:
            case ScaleConst.SCAL_SUE_WEIGHT_UNSTABLE:
            default:
                fireScaleLiveWeightEvent(new FormattedWeight());
                break;
        }
    }

    /**
     * Creates error responses for when error occurs.
     * @param errorEvent event error.
     */
    public void errorOccurred(ErrorEvent errorEvent) {
        JposException jposException = new JposException(errorEvent.getErrorCode(), errorEvent.getErrorCodeExtended());
        log.failure("Scale Received an Error", 17, jposException);
        log.failure("Scale - errorOccurred(): errCode=" + errorEvent.getErrorCode()
                        + " errCodeExt=" + errorEvent.getErrorCodeExtended()
                        + " errLocus=" + errorEvent.getErrorLocus()
                        + " errResponse=" + errorEvent.getErrorResponse(),
                13,
                null);

        int errorCode = errorEvent.getErrorCode();
        switch (errorCode) {
            case JposConst.JPOS_E_OFFLINE:
            case JposConst.JPOS_E_NOHARDWARE:
                disconnect();
                break;
            default:
                break;
        }
    }

    /**
     * Gets live weight.
     * @return current live weight.
     */
    FormattedWeight getLiveWeight() {
        return currentLiveWeight;
    }

    /**
     * This method is only used to set 'currentLiveWeight' for unit testing
     * @param currentLiveWeight
     */
    public void setLiveWeight(FormattedWeight currentLiveWeight) {
        this.currentLiveWeight = currentLiveWeight;
    }

    /**
     * Gets device name.
     * @return Device name.
     */
    public String getDeviceName() {
        return dynamicScale.getDeviceName();
    }

    /**
     * This method is only used to set 'deviceConnected' for unit testing
     * @param connected
     */
    public void setDeviceConnected(boolean connected) {
        this.deviceConnected = connected;
    }

    /**
     * Checks to see if scale is connected.
     * @return connection status.
     */
    public boolean isConnected() {
        return deviceConnected;
    }

    /**
     * Lock the current resource.
     * @return
     */
    public boolean tryLock() {
        try {
            isLocked = connectLock.tryLock(10, TimeUnit.SECONDS);
            log.success("Lock: " + isLocked, 1);
        } catch(InterruptedException interruptedException) {
            log.failure("Scale Device tryLock Failed", 17, interruptedException);
        }
        return isLocked;
    }

    /**
     * unlock the current resource.
     */
    public void unlock() {
        connectLock.unlock();
        isLocked = false;
    }

    /**
     * This method is only used to get "isLocked" for unit testing
     * @return
     */
    public boolean getIsLocked() {
        return isLocked;
    }
}
