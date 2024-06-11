package com.target.devicemanager.components.linedisplay;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.common.events.ConnectionEvent;
import jpos.JposConst;
import jpos.JposException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LineDisplayManagerTest {

    private LineDisplayManager lineDisplayManager;
    private LineDisplayManager lineDisplayManagerCache;

    @Mock
    private LineDisplayDevice mockLineDisplayDevice;
    @Mock
    private CacheManager mockCacheManager;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "lineDisplayHealth";
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
        lineDisplayManager = new LineDisplayManager(mockLineDisplayDevice);
        lineDisplayManagerCache = new LineDisplayManager(mockLineDisplayDevice, mockCacheManager);
    }

    @Test
    public void ctor_WhenLineDisplayDeviceIsNull_ThrowsException() {
        try {
            new LineDisplayManager(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("lineDisplayDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenLineDisplayDeviceIsNotNull_DoesNotThrowException() {
        try {
            new LineDisplayManager(mockLineDisplayDevice);
        } catch(Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        //arrange
        when(mockLineDisplayDevice.tryLock()).thenReturn(true);

        //act
        lineDisplayManager.connect();

        //assert
        verify(mockLineDisplayDevice).connect();
        verify(mockLineDisplayDevice).unlock();
    }

    @Test
    public void connect_WhenLockFails_DoesNotConnect() {
        //arrange
        when(mockLineDisplayDevice.tryLock()).thenReturn(false);

        //act
        lineDisplayManager.connect();

        //assert
        verify(mockLineDisplayDevice, never()).connect();
        verify(mockLineDisplayDevice, never()).unlock();
    }

    @Test
    public void reconnect_WhenLockAndConnectSucceed_Reconnects() {
        //arrange
        when(mockLineDisplayDevice.tryLock()).thenReturn(true);
        when(mockLineDisplayDevice.connect()).thenReturn(true);

        //act
        try {
            lineDisplayManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            fail("reconnectDevice should not result in an Exception.");
        }

        //assert
        verify(mockLineDisplayDevice).disconnect();
        verify(mockLineDisplayDevice).connect();
        verify(mockLineDisplayDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockFails_DoesNotReconnect() {
        //arrange
        when(mockLineDisplayDevice.tryLock()).thenReturn(false);

        //act
        try {
            lineDisplayManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            verify(mockLineDisplayDevice, never()).disconnect();
            verify(mockLineDisplayDevice, never()).connect();
            verify(mockLineDisplayDevice, never()).unlock();
            return;
        }

        //assert
        fail("Expected DEVICE_BUSY, but got none.");
    }

    @Test
    public void reconnect_WhenDeviceConnectFails_DoesNotReconnect() {
        //arrange
        when(mockLineDisplayDevice.tryLock()).thenReturn(true);
        when(mockLineDisplayDevice.connect()).thenReturn(false);

        //act
        try {
            lineDisplayManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            verify(mockLineDisplayDevice).disconnect();
            verify(mockLineDisplayDevice).connect();
            verify(mockLineDisplayDevice).unlock();
            return;
        }

        //assert
        fail("Expected DEVICE_OFFLINE, but got none.");
    }

    @Test
    public void displayLine_WhenLinesAreLessThan20Chars_NoTruncationAndRightPadsTo20() throws DeviceException, JposException {
        //assert
        String line1 = "lessThan20";
        String buffedLine1 = "lessThan20          ";
        String line2 = "6chars";
        String buffedLine2 = "6chars              ";

        //act
        lineDisplayManager.displayLine(line1, line2);

        //assert
        verify(mockLineDisplayDevice).displayLine(buffedLine1, buffedLine2);
    }

    @Test
    public void displayLine_WhenLinesAreMoreThan20Chars_Truncates() throws DeviceException, JposException {
        //assert
        String line1 = "So Much More Than Twenty Characters";
        String buffedLine1 = "So Much More Than Tw";
        String line2 = "0123456789012345678901234567890";
        String buffedLine2 = "01234567890123456789";

        //act
        lineDisplayManager.displayLine(line1, line2);

        //assert
        verify(mockLineDisplayDevice).displayLine(buffedLine1, buffedLine2);
    }

    @Test
    public void displayLine_WhenLineIsNull_ThenDisplay20Spaces() throws JposException, DeviceException {
        //arrange
        String twentySpaces = "                    ";

        //act
        lineDisplayManager.displayLine(null, null);

        //assert
        verify(mockLineDisplayDevice).displayLine(twentySpaces, twentySpaces);
    }

    @Test
    public void displayLine_WhenLineDisplayIsOffline_ThrowOfflineException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockLineDisplayDevice).displayLine(anyString(), anyString());

        //act
        try {
            lineDisplayManager.displayLine("123", "abd");
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }

        //assert
        fail("Expected OFFLINE Exception, but got none.");
    }

    @Test
    public void connectionEventOccurred_DoesNothing() {
        //Combine these since we default to not connected it's useful to test that we actually do disconnect
        ConnectionEvent connectionEvent = new ConnectionEvent(this, true);
        ConnectionEvent disconnectionEvent = new ConnectionEvent(this, false);

        //Connect
        lineDisplayManager.connectionEventOccurred(connectionEvent);

        //disconnect
        lineDisplayManager.connectionEventOccurred(disconnectionEvent);
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockLineDisplayDevice.isConnected()).thenReturn(false);
        when(mockLineDisplayDevice.getDeviceName()).thenReturn("lineDisplay");
        when(mockCacheManager.getCache("lineDisplayHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("lineDisplay", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = lineDisplayManagerCache.getHealth();

        //assert
        assertEquals("lineDisplay", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockLineDisplayDevice.isConnected()).thenReturn(true);
        when(mockLineDisplayDevice.getDeviceName()).thenReturn("lineDisplay");
        when(mockCacheManager.getCache("lineDisplayHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("lineDisplay", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = lineDisplayManagerCache.getHealth();

        //assert
        assertEquals("lineDisplay", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_CacheNull_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockLineDisplayDevice.isConnected()).thenReturn(true);
        when(mockLineDisplayDevice.getDeviceName()).thenReturn("lineDisplay");
        when(mockCacheManager.getCache("lineDisplayHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("lineDisplay", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = lineDisplayManagerCache.getHealth();

        //assert
        assertEquals("lineDisplay", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("lineDisplayHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("lineDisplay", DeviceHealth.READY);
        testCache.put("health", expected);

        //act
        DeviceHealthResponse deviceHealthResponse = lineDisplayManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("lineDisplayHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("lineDisplay", DeviceHealth.READY);
        testCache.put("health", expected);

        lineDisplayManagerCache.connect(); //set check health flag to CHECK_HEALTH
        when(mockLineDisplayDevice.isConnected()).thenReturn(true); //make sure health returns READY
        when(mockLineDisplayDevice.getDeviceName()).thenReturn("lineDisplay");

        //act
        DeviceHealthResponse deviceHealthResponse = lineDisplayManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() {
        //arrange
        when(mockLineDisplayDevice.isConnected()).thenReturn(false);
        when(mockLineDisplayDevice.getDeviceName()).thenReturn("lineDisplay");
        when(mockCacheManager.getCache("lineDisplayHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("lineDisplay", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = lineDisplayManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() {
        //arrange
        when(mockLineDisplayDevice.isConnected()).thenReturn(true);
        when(mockLineDisplayDevice.getDeviceName()).thenReturn("lineDisplay");
        when(mockCacheManager.getCache("lineDisplayHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("lineDisplay", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = lineDisplayManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_CallHealth() {
        //arrange
        when(mockLineDisplayDevice.isConnected()).thenReturn(true);
        when(mockLineDisplayDevice.getDeviceName()).thenReturn("lineDisplay");
        when(mockCacheManager.getCache("lineDisplayHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("lineDisplay", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = lineDisplayManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }
}