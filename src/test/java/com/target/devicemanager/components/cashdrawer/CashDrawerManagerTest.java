package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.cashdrawer.entities.CashDrawerError;
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
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CashDrawerManagerTest {

    private CashDrawerManager cashDrawerManager;

    private CashDrawerManager cashDrawerManagerCache;

    @Mock
    private CashDrawerDevice mockCashDrawerDevice;
    @Mock
    private Lock mockCashDrawerLock;
    @Mock
    private CacheManager mockCacheManager;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "cashDrawerHealth";
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
        cashDrawerManager = new CashDrawerManager(mockCashDrawerDevice, mockCashDrawerLock);
        cashDrawerManagerCache = new CashDrawerManager(mockCashDrawerDevice, mockCashDrawerLock, mockCacheManager);
    }

    @Test
    public void ctor_WhenCashDrawerDeviceAndLockAreNull_ThrowsException() {
        try {
            new CashDrawerManager(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("cashDrawerDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenCashDrawerDeviceIsNull_ThrowsException() {
        try {
            new CashDrawerManager(null, mockCashDrawerLock);
        } catch (IllegalArgumentException iae) {
            assertEquals("cashDrawerDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenCashDrawerLockIsNull_ThrowsException() {
        try {
            new CashDrawerManager(mockCashDrawerDevice, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("cashDrawerLock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenCashDrawerDeviceAndLockAreNotNull_DoesNotThrowException() {
        try {
            new CashDrawerManager(mockCashDrawerDevice, mockCashDrawerLock);
        } catch(Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        //arrange
        when(mockCashDrawerDevice.tryLock()).thenReturn(true);

        //act
        cashDrawerManager.connect();

        //assert
        verify(mockCashDrawerDevice).connect();
        verify(mockCashDrawerDevice).unlock();
    }

    @Test
    public void connect_WhenLockFails_DoesNotConnect() {
        //arrange
        when(mockCashDrawerDevice.tryLock()).thenReturn(false);

        //act
        cashDrawerManager.connect();

        //assert
        verify(mockCashDrawerDevice, never()).connect();
        verify(mockCashDrawerDevice, never()).unlock();
    }

    @Test
    public void reconnect_WhenLockSucceeds_Reconnects() {
        //arrange
        when(mockCashDrawerDevice.tryLock()).thenReturn(true);
        when(mockCashDrawerDevice.connect()).thenReturn(true);

        //act
        try {
            cashDrawerManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            fail("reconnectDevice should not result in an Exception");
        }


        //assert
        verify(mockCashDrawerDevice).disconnect();
        verify(mockCashDrawerDevice).connect();
        verify(mockCashDrawerDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockFails_DoesNotReconnect() {
        //arrange
        when(mockCashDrawerDevice.tryLock()).thenReturn(false);

        //act
        try {
            cashDrawerManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            verify(mockCashDrawerDevice, never()).disconnect();
            verify(mockCashDrawerDevice, never()).connect();
            verify(mockCashDrawerDevice, never()).unlock();
            return;
        }

        //assert
        fail("Expected DEVICE_BUSY, but got none");
    }

    @Test
    public void reconnect_WhenDeviceConnectFails_DoesNotReconnect() {
        //arrange
        when(mockCashDrawerDevice.tryLock()).thenReturn(true);
        when(mockCashDrawerDevice.connect()).thenReturn(false);

        //act
        try {
            cashDrawerManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            verify(mockCashDrawerDevice).disconnect();
            verify(mockCashDrawerDevice).connect();
            verify(mockCashDrawerDevice).unlock();
            return;
        }

        //assert
        fail("Expected DEVICE_OFFLINE, but got none");
    }

    @Test
    public void openCashDrawer_WhenLockFails_ThrowsException() {
        //arrange
        when(mockCashDrawerLock.tryLock()).thenReturn(false);

        //act
        try {
            cashDrawerManager.openCashDrawer();
        }
        //assert
        catch (DeviceException deviceException) {
            assertEquals(CashDrawerError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected BUSY, but got none.");
    }

    @Test
    public void openCashDrawer_WhenLockSucceeds_DoesNotThrowException() throws JposException, DeviceException {
        //arrange
        when(mockCashDrawerLock.tryLock()).thenReturn(true);

        //act
        try {
            cashDrawerManager.openCashDrawer();
        }
        //assert
        catch (DeviceException deviceException) {
            fail("Lock Success should not result in Exception");
        }
        verify(mockCashDrawerDevice).openCashDrawer();
        verify(mockCashDrawerLock).unlock();
    }

    @Test
    public void openCashDrawer_WhenCashDrawerIsOffline_ThrowsJposOfflineException() throws JposException, DeviceException {
        //arrange
        when(mockCashDrawerLock.tryLock()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockCashDrawerDevice).openCashDrawer();

        //act
        try {
            cashDrawerManager.openCashDrawer();
        }
        //assert
        catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected OFFLINE Exception, but got none.");
    }

    @Test
    public void openCashDrawer_WhenCashDrawerIsOffline_ThrowsDeviceOfflineException() throws JposException, DeviceException {
        //arrange
        when(mockCashDrawerLock.tryLock()).thenReturn(true);
        doThrow(new DeviceException(DeviceError.DEVICE_OFFLINE)).when(mockCashDrawerDevice).openCashDrawer();

        //act
        try {
            cashDrawerManager.openCashDrawer();
        }
        //assert
        catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected OFFLINE Exception, but got none.");
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockCashDrawerDevice.isConnected()).thenReturn(false);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getHealth();

        //assert
        assertEquals("cashDrawer", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getHealth();

        //assert
        assertEquals("cashDrawer", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenCacheFails_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawerHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getHealth();

        //assert
        assertEquals("cashDrawer", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("cashDrawerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);
        testCache.put("health", expected);

        //act
        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("cashDrawerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);
        testCache.put("health", expected);

        cashDrawerManagerCache.connect(); //set check health flag to CHECK_HEALTH
        when(mockCashDrawerDevice.isConnected()).thenReturn(true); //make sure health returns READY
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");

        //act
        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() {
        //arrange
        when(mockCashDrawerDevice.isConnected()).thenReturn(false);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() {
        //arrange
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_CallHealth() {
        //arrange
        when(mockCashDrawerDevice.isConnected()).thenReturn(true);
        when(mockCashDrawerDevice.getDeviceName()).thenReturn("cashDrawer");
        when(mockCacheManager.getCache("cashDrawerHealth")).thenReturn(null);
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = cashDrawerManagerCache.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }
}