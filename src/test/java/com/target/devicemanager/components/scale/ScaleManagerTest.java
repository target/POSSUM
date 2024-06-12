package com.target.devicemanager.components.scale;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.components.scale.entities.FormattedWeight;
import com.target.devicemanager.components.scale.entities.ScaleException;
import com.target.devicemanager.components.scale.entities.WeightErrorEvent;
import com.target.devicemanager.components.scale.entities.WeightEvent;
import jpos.JposException;
import jpos.ScaleConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScaleManagerTest {

    private ScaleManager scaleManager;
    private ScaleManager scaleManagerListCacheEmitter;
    private List<CompletableFuture<FormattedWeight>> completableFutureFormattedWeightList;
    private List<SseEmitter> sseEmitterList;

    @Mock
    private ScaleDevice mockScaleDevice;
    @Mock
    private CacheManager mockCacheManager;
    @Mock
    private List<SseEmitter> mockSseEmitterList;
    @Mock
    private List<CompletableFuture<FormattedWeight>> mockCompletableFutureFormattedWeightList;
    @Mock
    private CompletableFuture<FormattedWeight> mockCompletableFutureFormattedWeight;
    @Mock
    private ConnectionEvent mockConnectionEvent;
    @Mock
    private SseEmitter mockSseEmitter;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "scaleHealth";
        }

        @Override
        public Object getNativeCache() {
            return null;
        }

        @Override
        public ValueWrapper get(Object key) {
            if(cacheMap.containsKey(key)) {
                return () -> cacheMap.get(key);
            } else {
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            return null;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            return null;
        }

        @Override
        public void put(Object key, Object value) {
            cacheMap.put(key, value);
        }

        @Override
        public void evict(Object key) {

        }

        @Override
        public void clear() {

        }
    };

    @BeforeEach
    public void testInitialize() {
        completableFutureFormattedWeightList = new ArrayList<>();
        completableFutureFormattedWeightList.add(mockCompletableFutureFormattedWeight);
        sseEmitterList = new ArrayList<>();
        sseEmitterList.add(mockSseEmitter);
        scaleManager = new ScaleManager(mockScaleDevice, mockSseEmitterList, mockCompletableFutureFormattedWeightList);
        scaleManagerListCacheEmitter = new ScaleManager(mockScaleDevice, sseEmitterList, completableFutureFormattedWeightList, mockCacheManager, mockSseEmitterList);
    }

    @Test
    public void ctor_WhenScaleDeviceAndLiveWeightAndStableWeightAreNull_ThrowsException() {
        try {
            new ScaleManager(null, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scaleDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScaleDeviceAndLiveWeightAreNull_ThrowsException() {
        try {
            new ScaleManager(null, null, mockCompletableFutureFormattedWeightList);
        } catch (IllegalArgumentException iae) {
            assertEquals("scaleDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScaleDeviceAndStableWeightAreNull_ThrowsException() {
        try {
            new ScaleManager(null, mockSseEmitterList, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scaleDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenLiveWeightAndStableWeightAreNull_ThrowsException() {
        try {
            new ScaleManager(mockScaleDevice, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("liveWeightClients cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScaleDeviceIsNull_ThrowsException() {
        try {
            new ScaleManager(null, mockSseEmitterList, mockCompletableFutureFormattedWeightList);
        } catch (IllegalArgumentException iae) {
            assertEquals("scaleDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenLiveWeightIsNull_ThrowsException() {
        try {
            new ScaleManager(mockScaleDevice, null, mockCompletableFutureFormattedWeightList);
        } catch (IllegalArgumentException iae) {
            assertEquals("liveWeightClients cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenStableWeightIsNull_ThrowsException() {
        try {
            new ScaleManager(mockScaleDevice, mockSseEmitterList, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("stableWeightClients cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScaleDeviceAndLiveWeightAndStableWeightAreNotNull_DoesNotThrowException() {
        try {
            new ScaleManager(mockScaleDevice, mockSseEmitterList, mockCompletableFutureFormattedWeightList);
        } catch (Exception exception) {
            fail("Existing Device Arguments should not result in an Exception");
        }
        verify(mockScaleDevice, times(3)).addScaleEventListener(any());
        verify(mockScaleDevice, times(3)).addConnectionEventListener(any());
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        //arrange
        when(mockScaleDevice.tryLock()).thenReturn(true);

        //act
        scaleManager.connect();

        //assert
        verify(mockScaleDevice).connect();
        verify(mockScaleDevice).unlock();
    }

    @Test
    public void connect_WhenLockFails_DoesNotConnect() {
        //arrange
        when(mockScaleDevice.tryLock()).thenReturn(false);

        //act
        scaleManager.connect();

        //assert
        verify(mockScaleDevice, never()).connect();
        verify(mockScaleDevice, never()).unlock();
    }

    @Test
    public void reconnect_WhenLockAndConnectSucceed_Reconnects() {
        //arrange
        when(mockScaleDevice.tryLock()).thenReturn(true);
        when(mockScaleDevice.connect()).thenReturn(true);

        //act
        try {
            scaleManager.reconnectDevice();
        } catch (Exception exception) {
            fail("reconnectDevice() should not result in an Exception.");
        }

        //assert
        verify(mockScaleDevice).connect();
        verify(mockScaleDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockFails_DoesNotReconnect() {
        //arrange
        when(mockScaleDevice.tryLock()).thenReturn(false);

        //act
        try {
            scaleManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            verify(mockScaleDevice, never()).connect();
            verify(mockScaleDevice, never()).unlock();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }

        //assert
        fail("Expected DEVICE_BUSY, but got none.");
    }

    @Test
    public void reconnect_WhenDeviceConnectFails_DoesNotReconnect() {
        //arrange
        when(mockScaleDevice.tryLock()).thenReturn(true);
        when(mockScaleDevice.connect()).thenReturn(false);

        //act
        try {
            scaleManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }

        //assert
        fail("Expected DEVICE_OFFLINE, but got none.");
    }

    @Test
    public void subscribeToLiveWeight_WhenEmitterCompletes() throws IOException {
        //arrange
        mockSseEmitter.complete();

        //act
        scaleManagerListCacheEmitter.subscribeToLiveWeight(mockSseEmitter);

        //assert
        verify(mockSseEmitter).onCompletion(any());
        verify(mockSseEmitter).send(any(), eq(MediaType.APPLICATION_JSON));

    }

    @Test
    public void subscribeToLiveWeight_WhenEmitterTimesOut() throws IOException {
        //arrange

        //act
        scaleManagerListCacheEmitter.subscribeToLiveWeight(mockSseEmitter);

        //assert
        verify(mockSseEmitter).onTimeout(any());
        verify(mockSseEmitter).send(any(), eq(MediaType.APPLICATION_JSON));

    }

    @Test
    public void subscribeToLiveWeight_VerifyEmitterList() throws IOException {
        //arrange

        //act
        scaleManager.subscribeToLiveWeight(mockSseEmitter);

        //assert
        verify(mockSseEmitterList).add(mockSseEmitter);
        verify(mockSseEmitter).send(any(), eq(MediaType.APPLICATION_JSON));

    }

    @Test
    public void getStableWeight_ReturnsWeight() throws ScaleException, ExecutionException, InterruptedException, TimeoutException {
        //arrange
        FormattedWeight expected = new FormattedWeight(3);
        when(mockCompletableFutureFormattedWeight.get(30000, TimeUnit.MILLISECONDS)).thenReturn(expected);
        when(mockScaleDevice.tryLock()).thenReturn(true);
        when(scaleManager.isScaleReady()).thenReturn(true);
        //act
        FormattedWeight actual = scaleManager.getStableWeight(mockCompletableFutureFormattedWeight);

        //assert
        verify(mockCompletableFutureFormattedWeightList).add(any());
        verify(mockScaleDevice).startStableWeightRead(10000);
        assertEquals(expected, actual);
    }

    @Test
    public void getStableWeight_ReturnsBusy() throws ScaleException, ExecutionException, InterruptedException, TimeoutException {
        //arrange
        when(mockScaleDevice.tryLock()).thenReturn(false);
        when(scaleManager.isScaleReady()).thenReturn(true);
        //act
        try{
            scaleManager.getStableWeight(mockCompletableFutureFormattedWeight);
        }
        catch(ScaleException scaleException) {
            //assert
            assertEquals("DEVICE_BUSY", scaleException.getDeviceError().getCode());
            return;
        }
        fail("Expected Exception, but got none");
    }

    @Test
    public void getStableWeight_DeviceOffline_ReturnsOffline() throws ScaleException, ExecutionException, InterruptedException, TimeoutException {
        //arrange
        when(mockScaleDevice.tryLock()).thenReturn(true);
        when(scaleManager.isScaleReady()).thenReturn(false);
        //act
        try{
            scaleManager.getStableWeight(mockCompletableFutureFormattedWeight);
        }
        catch(ScaleException scaleException) {
            //assert
            assertEquals("DEVICE_OFFLINE", scaleException.getDeviceError().getCode());
            return;
        }
        fail("Expected Exception, but got none");
    }

    @Test
    public void getStableWeight_ThrowsExecutionException() throws ExecutionException, InterruptedException, TimeoutException {
        //arrange
        ExecutionException executionException = new ExecutionException(new JposException(ScaleConst.JPOS_ESCAL_UNDER_ZERO));
        when(mockCompletableFutureFormattedWeight.get(30000, TimeUnit.MILLISECONDS)).thenThrow(executionException);
        when(mockScaleDevice.tryLock()).thenReturn(true);
        when(scaleManager.isScaleReady()).thenReturn(true);
        //act
        try {
            scaleManager.getStableWeight(mockCompletableFutureFormattedWeight);
        }

        //assert
        catch(ScaleException scaleException) {
            assertEquals("WEIGHT_UNDER_ZERO", scaleException.getDeviceError().getCode());
            return;
        }
        fail("Expected Exception, but got none");
    }

    @Test
    public void getStableWeight_ThrowsInterruptedException() throws ExecutionException, InterruptedException, TimeoutException {
        //arrange
        InterruptedException interruptedException = new InterruptedException();
        when(mockCompletableFutureFormattedWeight.get(30000, TimeUnit.MILLISECONDS)).thenThrow(interruptedException);
        when(mockScaleDevice.tryLock()).thenReturn(true);
        when(scaleManager.isScaleReady()).thenReturn(true);
        //act
        try {
            scaleManager.getStableWeight(mockCompletableFutureFormattedWeight);
        }

        //assert
        catch(ScaleException scaleException) {
            assertEquals("UNEXPECTED_ERROR", scaleException.getDeviceError().getCode());
            return;
        }
        fail("Expected Exception, but got none");
    }

    @Test
    public void getStableWeight_ThrowsTimeoutException() throws ExecutionException, InterruptedException, TimeoutException {
        //arrange
        TimeoutException timeoutException = new TimeoutException();
        when(mockCompletableFutureFormattedWeight.get(30000, TimeUnit.MILLISECONDS)).thenThrow(timeoutException);
        when(mockScaleDevice.tryLock()).thenReturn(true);
        when(scaleManager.isScaleReady()).thenReturn(true);
        //act
        try {
            scaleManager.getStableWeight(mockCompletableFutureFormattedWeight);
        }

        //assert
        catch(ScaleException scaleException) {
            assertEquals("TIMEOUT", scaleException.getDeviceError().getCode());
            return;
        }
        fail("Expected Exception, but got none");
    }

    @Test
    public void scaleLiveWeightEventOccurred_DoesNotThrowIOException() throws IOException {
        //arrange
        FormattedWeight formattedWeight = new FormattedWeight(3);
        WeightEvent weightEvent = new WeightEvent(mockScaleDevice, formattedWeight);

        //act
        scaleManagerListCacheEmitter.scaleLiveWeightEventOccurred(weightEvent);

        //assert
        verify(mockSseEmitter).send(weightEvent.getWeight(), MediaType.APPLICATION_JSON);
        verify(mockSseEmitterList).clear();
    }

    @Test
    public void scaleLiveWeightEventOccurred_ThrowsIOException() throws IOException {
        //arrange
        FormattedWeight formattedWeight = new FormattedWeight(3);
        WeightEvent weightEvent = new WeightEvent(mockScaleDevice, formattedWeight);
        doThrow(new IOException()).when(mockSseEmitter).send(weightEvent.getWeight(), MediaType.APPLICATION_JSON);

        //act
        scaleManagerListCacheEmitter.scaleLiveWeightEventOccurred(weightEvent);

        //assert
        verify(mockSseEmitterList).add(mockSseEmitter);
        verify(mockSseEmitterList).clear();
    }

    @Test
    public void scaleWeightErrorEventOccurred_CheckCompleteExceptionable() {
        //arrange
        JposException weightError = new JposException(ScaleConst.JPOS_ESCAL_UNDER_ZERO);
        WeightErrorEvent weightErrorEvent = new WeightErrorEvent(mockScaleDevice, weightError);

        //act
        scaleManagerListCacheEmitter.scaleWeightErrorEventOccurred(weightErrorEvent);

        //assert
        verify(mockCompletableFutureFormattedWeight).completeExceptionally(weightErrorEvent.getError());
    }

    @Test
    public void scaleWeightErrorEventOccurred_CheckClear() {
        //arrange
        JposException weightError = new JposException(ScaleConst.JPOS_ESCAL_UNDER_ZERO);
        WeightErrorEvent weightErrorEvent = new WeightErrorEvent(mockScaleDevice, weightError);

        //act
        scaleManagerListCacheEmitter.scaleWeightErrorEventOccurred(weightErrorEvent);

        //assert
        verify(mockCompletableFutureFormattedWeight).completeExceptionally(weightErrorEvent.getError());
    }

    @Test
    public void scaleStableWeightDataEventOccurred_CheckComplete() {
        //arrange
        FormattedWeight formattedWeight = new FormattedWeight(3);
        WeightEvent weightEvent = new WeightEvent(mockScaleDevice, formattedWeight);

        //act
        scaleManagerListCacheEmitter.scaleStableWeightDataEventOccurred(weightEvent);

        //assert
        verify(mockCompletableFutureFormattedWeight).complete(any());
    }

    @Test
    public void scaleStableWeightDataEventOccurred_CheckCleared() {
        //arrange
        FormattedWeight formattedWeight = new FormattedWeight(3);
        WeightEvent weightEvent = new WeightEvent(mockScaleDevice, formattedWeight);

        //act
        scaleManager.scaleStableWeightDataEventOccurred(weightEvent);

        //assert
        verify(mockCompletableFutureFormattedWeightList).clear();
    }

    @Test
    public void connectionEventOccurred_SetsIsScaleReady() {
        //arrange
        when(mockConnectionEvent.isConnected()).thenReturn(true);

        //act
        scaleManager.connectionEventOccurred(mockConnectionEvent);

        //assert
        verify(mockConnectionEvent).isConnected();
        assertTrue(scaleManager.getIsScaleReady());
    }

    @Test
    public void isScaleReady_CallsThroughToDevice() {
        //arrange
        when(mockScaleDevice.isConnected()).thenReturn(true);
        boolean expected = true;

        //act
        boolean actual = scaleManager.isScaleReady();

        //assert
        verify(mockScaleDevice).isConnected();
        assertEquals(actual, expected);
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockScaleDevice.isConnected()).thenReturn(false);
        when(mockScaleDevice.getDeviceName()).thenReturn("scale");
        when(mockCacheManager.getCache("scaleHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = scaleManagerListCacheEmitter.getHealth();

        //assert
        assertEquals("scale", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockScaleDevice.isConnected()).thenReturn(true);
        when(mockScaleDevice.getDeviceName()).thenReturn("scale");
        when(mockCacheManager.getCache("scaleHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = scaleManagerListCacheEmitter.getHealth();

        //assert
        assertEquals("scale", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_CacheNull_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockScaleDevice.isConnected()).thenReturn(true);
        when(mockScaleDevice.getDeviceName()).thenReturn("scale");
        when(mockCacheManager.getCache("scaleHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = scaleManagerListCacheEmitter.getHealth();

        //assert
        assertEquals("scale", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("scaleHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.READY);
        testCache.put("health", expected);

        //act
        DeviceHealthResponse deviceHealthResponse = scaleManagerListCacheEmitter.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("scaleHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.READY);
        testCache.put("health", expected);

        scaleManagerListCacheEmitter.connect(); //set check health flag to CHECK_HEALTH
        when(mockScaleDevice.isConnected()).thenReturn(true); //make sure health returns READY
        when(mockScaleDevice.getDeviceName()).thenReturn("scale");

        //act
        DeviceHealthResponse deviceHealthResponse = scaleManagerListCacheEmitter.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() {
        //arrange
        when(mockScaleDevice.isConnected()).thenReturn(false);
        when(mockScaleDevice.getDeviceName()).thenReturn("scale");
        when(mockCacheManager.getCache("scaleHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = scaleManagerListCacheEmitter.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() {
        //arrange
        when(mockScaleDevice.isConnected()).thenReturn(true);
        when(mockScaleDevice.getDeviceName()).thenReturn("scale");
        when(mockCacheManager.getCache("scaleHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = scaleManagerListCacheEmitter.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_CheckHealth() {
        //arrange
        when(mockScaleDevice.isConnected()).thenReturn(true);
        when(mockScaleDevice.getDeviceName()).thenReturn("scale");
        when(mockCacheManager.getCache("scaleHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = scaleManagerListCacheEmitter.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }
}
