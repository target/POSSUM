package com.target.devicemanager.components.check;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.components.check.entities.*;
import jpos.JposConst;
import jpos.JposException;
import jpos.MICRConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MicrManagerTest {

    private MicrManager micrManager;
    private MicrManager micrManagerCacheClient;

    @Mock
    private MicrDevice mockMicrDevice;
    @Mock
    private CacheManager mockCacheManager;
    @Mock
    CompletableFuture<MicrData> mockFutureClient;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "micrHealth";
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
        micrManager = new MicrManager(mockMicrDevice);
        micrManagerCacheClient = new MicrManager(mockMicrDevice, mockCacheManager, mockFutureClient);
    }

    @Test
    public void ctor_WhenMicrDeviceIsNull_ThrowsException() {
        try {
            new MicrManager(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("micrDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenMicrDeviceIsNotNull_DoesNotThrowException() {
        try {
            new MicrManager(mockMicrDevice);
        } catch(Exception exception) {
            fail("Existing Device Arguments should not result in an Exception");
        }
        verify(mockMicrDevice, times(3)).addMicrEventListener(any());
        verify(mockMicrDevice, times(3)).addConnectionEventListener(any());
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        //arrange
        when(mockMicrDevice.tryLock()).thenReturn(true);

        //act
        micrManager.connect();

        //assert
        verify(mockMicrDevice).connect();
        verify(mockMicrDevice).unlock();
    }

    @Test
    public void connect_WhenLockFails_DoesNotConnect() {
        //arrange
        when(mockMicrDevice.tryLock()).thenReturn(false);

        //act
        micrManager.connect();

        //assert
        verify(mockMicrDevice, never()).connect();
        verify(mockMicrDevice, never()).unlock();
    }

    @Test
    public void reconnect_WhenLockAndConnectSucceed_Reconnects() {
        //arrange
        when(mockMicrDevice.tryLock()).thenReturn(true);
        when(mockMicrDevice.connect()).thenReturn(true);

        //act
        try {
            micrManager.reconnectDevice();
        } catch (Exception exception) {
            fail("reconnectDevice() should not result in an Exception.");
        }

        //assert
        verify(mockMicrDevice).connect();
        verify(mockMicrDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockFails_DoesNotReconnect() {
        //arrange
        when(mockMicrDevice.tryLock()).thenReturn(false);

        //act
        try {
            micrManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            verify(mockMicrDevice, never()).connect();
            verify(mockMicrDevice, never()).unlock();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }

        //assert
        fail("Expected DEVICE_BUSY, but got none.");
    }

    @Test
    public void reconnect_WhenDeviceConnectFails_DoesNotReconnect() {
        //arrange
        when(mockMicrDevice.tryLock()).thenReturn(true);
        when(mockMicrDevice.connect()).thenReturn(false);

        //act
        try {
            micrManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }

        //assert
        fail("Expected DEVICE_OFFLINE, but got none.");
    }


    @Test
    public void readMICR_ReturnsMICRDataFromDevice() throws MicrException, ExecutionException, InterruptedException {
        //arrange
        CompletableFuture<MicrData> mockFuture = mock(CompletableFuture.class);
        MicrData micrData = new MicrData( "1234567890",  "12345","123456789", "o0500o t123456789t 1234567890 o", "0100");
        when(mockFuture.get()).thenReturn(micrData);

        //act
        MicrData actual = micrManagerCacheClient.readMICR(mockFuture);

        //assert
        verify(mockMicrDevice).setCheckCancelReceived(false);
        verify(mockMicrDevice).insertCheck();
        assertEquals(micrData, actual);
    }

    @Test
    public void readMICR_WhenBadDataRead_ThrowException() throws ExecutionException, InterruptedException, MicrException {
        //arrange
        CompletableFuture<MicrData> mockFuture = mock(CompletableFuture.class);
        doThrow(new ExecutionException("Test", new JposException(MICRConst.JPOS_EMICR_BADDATA))).when(mockFuture).get();

        //act
        try {
            micrManagerCacheClient.readMICR(mockFuture);
        }

        //assert
        catch (MicrException micrException) {
            verify(mockMicrDevice).setCheckCancelReceived(false);
            verify(mockMicrDevice).insertCheck();
            assertEquals(MicrError.BAD_DATA, micrException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void readMICR_WhenDeviceError_ThrowException() throws ExecutionException, InterruptedException, MicrException {
        //arrange
        CompletableFuture<MicrData> mockFuture = mock(CompletableFuture.class);
        doThrow(new ExecutionException("Test", new JposException(MICRConst.JPOS_EMICR_COVEROPEN))).when(mockFuture).get();

        //act
        try {
            micrManagerCacheClient.readMICR(mockFuture);
        }

        //assert
        catch (MicrException micrException) {
            verify(mockMicrDevice).setCheckCancelReceived(false);
            verify(mockMicrDevice).insertCheck();
            assertEquals(MicrError.HARDWARE_ERROR, micrException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void readMICR_WhenInterrupted_ThrowInternalError() throws ExecutionException, InterruptedException, MicrException {
        //arrange
        CompletableFuture<MicrData> mockFuture = mock(CompletableFuture.class);
        Exception exception = new InterruptedException("quick question");
        doThrow(exception).when(mockFuture).get();

        //act
        try {
            micrManagerCacheClient.readMICR(mockFuture);
        }

        //assert
        catch (MicrException micrException) {
            verify(mockMicrDevice).setCheckCancelReceived(false);
            verify(mockMicrDevice).insertCheck();
            assertEquals(MicrError.UNEXPECTED_ERROR, micrException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void cancelCheckRead_CallsThroughToMicrDevice() {
        //arrange

        //act
        micrManagerCacheClient.cancelCheckRead();

        //assert
        verify(mockFutureClient).cancel(true);
        verify(mockMicrDevice).setCheckCancelReceived(true);
    }

    @Test
    public void ejectCheck_CallsThroughToMicrDevice() throws JposException {
        //arrange

        //act
        micrManager.ejectCheck();

        //assert
        verify(mockMicrDevice).withdrawCheck();
    }

    @Test
    public void ejectCheck_WhenWithdrawThrowsException_DoNothing() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockMicrDevice).withdrawCheck();

        //act
        micrManager.ejectCheck();

        //assert
        verify(mockMicrDevice).withdrawCheck();
    }

    @Test
    public void micrDataEventOccurred_CompletesFutureWithData() {
        //arrange
        MicrData micrData = new MicrData( "1234567890",  "12345","123456789", "o0500o t123456789t 1234567890 o", "0100");
        MicrDataEvent micrDataEvent = new MicrDataEvent(mockMicrDevice, micrData);

        //act
        micrManagerCacheClient.micrDataEventOccurred(micrDataEvent);

        //assert
        verify(mockFutureClient).complete(micrData);
    }

    @Test
    public void micrErrorEventOccurred_CompletesFutureWithException() {
        //arrange
        JposException micrError = new JposException(MICRConst.JPOS_EMICR_BADDATA);
        MicrErrorEvent micrErrorEvent = new MicrErrorEvent(mockMicrDevice, micrError);

        //act
        micrManagerCacheClient.micrErrorEventOccurred(micrErrorEvent);

        //assert
        verify(mockFutureClient).completeExceptionally(micrError);
    }

    @Test
    public void connectionEventOccurred_DoesNothing() {
        //Combine these since we default to not connected it's useful to test that we actually do disconnect
        ConnectionEvent connectionEvent = new ConnectionEvent(this, true);
        ConnectionEvent disconnectionEvent = new ConnectionEvent(this, false);

        //Connect
        micrManager.connectionEventOccurred(connectionEvent);

        //disconnect
        micrManager.connectionEventOccurred(disconnectionEvent);
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockMicrDevice.isConnected()).thenReturn(false);
        when(mockMicrDevice.getDeviceName()).thenReturn("micr");
        when(mockCacheManager.getCache("micrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("micr", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = micrManagerCacheClient.getHealth();

        //assert
        assertEquals("micr", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockMicrDevice.isConnected()).thenReturn(true);
        when(mockMicrDevice.getDeviceName()).thenReturn("micr");
        when(mockCacheManager.getCache("micrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("micr", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = micrManagerCacheClient.getHealth();

        //assert
        assertEquals("micr", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenCacheNull_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockMicrDevice.isConnected()).thenReturn(true);
        when(mockMicrDevice.getDeviceName()).thenReturn("micr");
        when(mockCacheManager.getCache("micrHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("micr", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = micrManagerCacheClient.getHealth();

        //assert
        assertEquals("micr", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("micrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("micr", DeviceHealth.READY);
        testCache.put("health", expected);

        //act
        DeviceHealthResponse deviceHealthResponse = micrManagerCacheClient.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("micrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("micr", DeviceHealth.READY);
        testCache.put("health", expected);

        micrManagerCacheClient.connect(); //set check health flag to CHECK_HEALTH
        when(mockMicrDevice.isConnected()).thenReturn(true); //make sure health returns READY
        when(mockMicrDevice.getDeviceName()).thenReturn("micr");

        //act
        DeviceHealthResponse deviceHealthResponse = micrManagerCacheClient.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() {
        //arrange
        when(mockMicrDevice.isConnected()).thenReturn(false);
        when(mockMicrDevice.getDeviceName()).thenReturn("micr");
        when(mockCacheManager.getCache("micrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("micr", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = micrManagerCacheClient.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() {
        //arrange
        when(mockMicrDevice.isConnected()).thenReturn(true);
        when(mockMicrDevice.getDeviceName()).thenReturn("micr");
        when(mockCacheManager.getCache("micrHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("micr", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = micrManagerCacheClient.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_CallHealth() {
        //arrange
        when(mockMicrDevice.isConnected()).thenReturn(true);
        when(mockMicrDevice.getDeviceName()).thenReturn("micr");
        when(mockCacheManager.getCache("micrHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("micr", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = micrManagerCacheClient.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }
}