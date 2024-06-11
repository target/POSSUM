package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.scanner.entities.ScannerError;
import com.target.devicemanager.components.scanner.entities.ScannerException;
import com.target.devicemanager.components.scanner.entities.ScannerType;
import jpos.JposConst;
import jpos.JposException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScannerManagerTest {

    private ScannerManager scannerManager;
    private ScannerManager scannerManagerCache;
    private List<ScannerDevice> scannerDevices;
    private List<Future<Boolean>> reconnectResults;

    @Mock
    private ScannerDevice mockHandheldScannerDevice;
    @Mock
    private ScannerDevice mockFlatbedScannerDevice;
    @Mock
    private Lock mockScannerLock;
    @Mock
    private ExecutorService mockExecutor;
    @Mock
    private Future<Boolean> mockFuture;
    @Mock
    private CacheManager mockCacheManager;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "scannerHealth";
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
        scannerDevices = new ArrayList<>();
        scannerDevices.add(mockHandheldScannerDevice);
        scannerDevices.add(mockFlatbedScannerDevice);
        reconnectResults = new ArrayList<>();
        reconnectResults.add(mockFuture);
        scannerManager = new ScannerManager(scannerDevices, mockScannerLock);
        scannerManagerCache = Mockito.spy(new ScannerManager(scannerDevices, mockScannerLock, mockCacheManager, mockExecutor, reconnectResults, true));
    }

    @Test
    public void ctor_WhenScannerDeviceListAndLockAreNull_ThrowsException() {
        try {
            new ScannerManager(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scanners cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScannerDeviceListIsNull_ThrowsException() {
        try {
            new ScannerManager(null, mockScannerLock);
        } catch (IllegalArgumentException iae) {
            assertEquals("scanners cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScannerLockIsNull_ThrowsException() {
        try {
            new ScannerManager(scannerDevices, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scannerLock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScannerDeviceListAndLockAreNotNull_DoesNotThrowException() {
        try {
            new ScannerManager(scannerDevices, mockScannerLock);
        } catch(Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        //arrange

        //act
        scannerManager.connect();

        //assert
        verify(mockHandheldScannerDevice).connect();
        verify(mockFlatbedScannerDevice).connect();
    }

    @Test
    public void reconnectScanners_WhenScannerSucceeds_Reconnects() throws DeviceException {
        //arrange
        when(mockFlatbedScannerDevice.reconnect()).thenReturn(true);
        when(mockHandheldScannerDevice.reconnect()).thenReturn(true);

        //act
        try {
            scannerManager.reconnectScanners();
        } catch (DeviceException deviceException) {
            fail("scannerManager.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockFlatbedScannerDevice).reconnect();
        verify(mockHandheldScannerDevice).reconnect();

    }

    @Test
    public void reconnectScanners_WhenScannerFails_ThrowsError() throws DeviceException {
        //arrange
        when(mockFlatbedScannerDevice.reconnect()).thenReturn(false);
        when(mockHandheldScannerDevice.reconnect()).thenReturn(true);

        //act
        try {
            scannerManager.reconnectScanners();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockFlatbedScannerDevice).reconnect();
            verify(mockHandheldScannerDevice).reconnect();
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none");

    }

    @Test
    public void reconnectScanners_WhenExecutor_ThrowsError() throws DeviceException, InterruptedException {
        //arrange
        when(mockFlatbedScannerDevice.reconnect()).thenReturn(true);
        when(mockHandheldScannerDevice.reconnect()).thenReturn(true);
        doThrow(new InterruptedException()).when(mockExecutor).invokeAll(any());

        //act
        try {
            scannerManagerCache.reconnectScanners();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockFlatbedScannerDevice, never()).reconnect();
            verify(mockHandheldScannerDevice, never()).reconnect();
            assertEquals(DeviceError.UNEXPECTED_ERROR, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none");

    }

    @Test
    public void reconnectScanners_WhenGetResults_ThrowsError() throws DeviceException, InterruptedException, ExecutionException {
        //arrange
        when(mockFlatbedScannerDevice.reconnect()).thenReturn(true);
        when(mockHandheldScannerDevice.reconnect()).thenReturn(true);
        doThrow(new ExecutionException(new DeviceException(DeviceError.DEVICE_BUSY))).when(mockFuture).get();

        //act
        try {
            scannerManagerCache.reconnectScanners();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockFlatbedScannerDevice, never()).reconnect();
            verify(mockHandheldScannerDevice, never()).reconnect();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none");

    }

    @Test
    public void getData_WhenAlreadyLocked_ThrowsException() {
        //arrange
        when(mockScannerLock.tryLock()).thenReturn(false);

        //act
        try {
            scannerManagerCache.getData(ScannerType.BOTH);
        }
        //assert
        catch (ScannerException scannerException) {
            verify(mockScannerLock, never()).unlock();
            assertEquals(DeviceError.DEVICE_BUSY, scannerException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");

    }

    @Test
    public void getData_WhenExecutor_ThrowsInterruptedException() throws InterruptedException {
        //arrange
        when(mockScannerLock.tryLock()).thenReturn(true);
        doThrow(new InterruptedException()).when(mockExecutor).invokeAll(any());

        //act
        try {
            scannerManagerCache.getData(ScannerType.BOTH);
        }
        //assert
        catch(ScannerException scannerException) {
            assertEquals(DeviceError.UNEXPECTED_ERROR, scannerException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getData_WhenNotBoth() throws ScannerException, JposException {
        //arrange
        when(mockScannerLock.tryLock()).thenReturn(true);
        when(mockFlatbedScannerDevice.getScannerType()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getScannerType()).thenReturn("HANDHELD");

        //act
        scannerManager.getData(ScannerType.FLATBED);

        //assert
        verify(mockFlatbedScannerDevice).getScannerData();
        verify(mockHandheldScannerDevice, never()).getScannerData();
    }

    @Test
    public void getData_WhenScannerData_ThrowsException() throws JposException, InterruptedException {
        //arrange
        when(mockScannerLock.tryLock()).thenReturn(true);
        when(mockFlatbedScannerDevice.getScannerType()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getScannerType()).thenReturn("HANDHELD");
        doThrow(new JposException(JposConst.JPOS_E_DISABLED)).when(mockHandheldScannerDevice).getScannerData();
        doThrow(new JposException(JposConst.JPOS_E_DISABLED)).when(mockFlatbedScannerDevice).getScannerData();

        //act
        try {
            scannerManager.getData(ScannerType.BOTH);
        }
        //assert
        catch (ScannerException scannerException) {
            verify(mockFlatbedScannerDevice).getScannerData();
            verify(mockHandheldScannerDevice).getScannerData();
            assertEquals(ScannerError.DISABLED, scannerException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none");
    }

    @Test
    public void cancelScanRequest_WhenAlreadyLocked_ThrowsException() {
        //arrange
        when(mockScannerLock.tryLock()).thenReturn(true);

        //act
        try {
            scannerManager.cancelScanRequest();
        }

        //assert
        catch (ScannerException scannerException) {
            verify(mockFlatbedScannerDevice, never()).cancelScannerData();
            verify(mockHandheldScannerDevice, never()).cancelScannerData();
            verify(mockScannerLock).unlock();
            assertEquals(ScannerError.ALREADY_DISABLED, scannerException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none");
    }

    @Test
    public void cancelScanRequest_WhenExecutor_ThrowsInterruptedException() throws InterruptedException {
        //arrange
        when(mockScannerLock.tryLock()).thenReturn(false);
        doThrow(new InterruptedException()).when(mockExecutor).invokeAll(any());

        //act
        try {
            scannerManagerCache.cancelScanRequest();
        }

        //assert
        catch (ScannerException scannerException) {
            verify(mockFlatbedScannerDevice, never()).cancelScannerData();
            verify(mockHandheldScannerDevice, never()).cancelScannerData();
            assertEquals(ScannerError.UNEXPECTED_ERROR, scannerException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none");
    }

    @Test
    public void cancelScanRequest_WhenCancelSucceeds() throws InterruptedException, ScannerException {
        //arrange
        when(mockScannerLock.tryLock()).thenReturn(false);

        //act
        scannerManager.cancelScanRequest();

        //assert
        verify(mockFlatbedScannerDevice).cancelScannerData();
        verify(mockHandheldScannerDevice).cancelScannerData();
    }

    @Test
    public void getHealth_WhenFlatbedDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockFlatbedScannerDevice.isConnected()).thenReturn(false);
        when(mockFlatbedScannerDevice.getDeviceName()).thenReturn("FLATBED");
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(testCache);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("FLATBED", DeviceHealth.NOTREADY));
        when(mockFlatbedScannerDevice.getScannerType()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getScannerType()).thenReturn("HANDHELD");

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getHealth(ScannerType.FLATBED);

        //assert
        assertEquals("FLATBED", deviceHealthResponseList.get(0).getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponseList.get(0).getHealthStatus());
        assertEquals(expectedList.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenBothDevicesOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockHandheldScannerDevice.isConnected()).thenReturn(true);
        when(mockHandheldScannerDevice.getDeviceName()).thenReturn("HANDHELD");
        when(mockFlatbedScannerDevice.isConnected()).thenReturn(true);
        when(mockFlatbedScannerDevice.getDeviceName()).thenReturn("FLATBED");
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(testCache);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        expectedList.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        when(mockFlatbedScannerDevice.getScannerType()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getScannerType()).thenReturn("HANDHELD");

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getHealth(ScannerType.BOTH);

        //assert
        assertEquals("HANDHELD", deviceHealthResponseList.get(0).getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponseList.get(0).getHealthStatus());
        assertEquals("FLATBED", deviceHealthResponseList.get(1).getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponseList.get(1).getHealthStatus());
        assertEquals(expectedList.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_CacheNull_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockHandheldScannerDevice.isConnected()).thenReturn(true);
        when(mockHandheldScannerDevice.getDeviceName()).thenReturn("HANDHELD");
        when(mockFlatbedScannerDevice.isConnected()).thenReturn(true);
        when(mockFlatbedScannerDevice.getDeviceName()).thenReturn("FLATBED");
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(null);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        expectedList.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        when(mockFlatbedScannerDevice.getScannerType()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getScannerType()).thenReturn("HANDHELD");

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getHealth(ScannerType.BOTH);

        //assert
        assertEquals("HANDHELD", deviceHealthResponseList.get(0).getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponseList.get(0).getHealthStatus());
        assertEquals("FLATBED", deviceHealthResponseList.get(1).getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponseList.get(1).getHealthStatus());
    }

    @Test
    public void getHealth_WhenHandheldDeviceOnline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockHandheldScannerDevice.isConnected()).thenReturn(true);
        when(mockHandheldScannerDevice.getDeviceName()).thenReturn("HANDHELD");
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(testCache);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        when(mockFlatbedScannerDevice.getScannerType()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getScannerType()).thenReturn("HANDHELD");

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getHealth(ScannerType.HANDHELD);

        //assert
        assertEquals("HANDHELD", deviceHealthResponseList.get(0).getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponseList.get(0).getHealthStatus());
        assertEquals(expectedList.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenOneDeviceOffline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockHandheldScannerDevice.isConnected()).thenReturn(false);
        when(mockHandheldScannerDevice.getDeviceName()).thenReturn("HANDHELD");
        when(mockFlatbedScannerDevice.isConnected()).thenReturn(true);
        when(mockFlatbedScannerDevice.getDeviceName()).thenReturn("FLATBED");
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(testCache);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.NOTREADY));
        expectedList.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        when(mockFlatbedScannerDevice.getScannerType()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getScannerType()).thenReturn("HANDHELD");

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getHealth(ScannerType.BOTH);

        //assert
        assertEquals("HANDHELD", deviceHealthResponseList.get(0).getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponseList.get(0).getHealthStatus());
        assertEquals("FLATBED", deviceHealthResponseList.get(1).getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponseList.get(1).getHealthStatus());
        assertEquals(expectedList.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(testCache);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.NOTREADY));
        expectedList.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        testCache.put("health", expectedList);

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getStatus();

        //assert
        assertEquals(expectedList.toString(), deviceHealthResponseList.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(testCache);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        expectedList.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        testCache.put("health", expectedList);

        scannerManagerCache.connect(); //set check health flag to CHECK_HEALTH
        when(mockHandheldScannerDevice.isConnected()).thenReturn(true);
        when(mockFlatbedScannerDevice.isConnected()).thenReturn(true);//make sure health returns READY
        when(mockFlatbedScannerDevice.getDeviceName()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getDeviceName()).thenReturn("HANDHELD");

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getStatus();

        //assert
        assertEquals(expectedList.toString(), deviceHealthResponseList.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() {
        //arrange
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(testCache);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.NOTREADY));
        expectedList.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        testCache.put("health", expectedList);

        when(mockHandheldScannerDevice.isConnected()).thenReturn(false);
        when(mockFlatbedScannerDevice.isConnected()).thenReturn(true);//make sure health returns READY
        when(mockFlatbedScannerDevice.getDeviceName()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getDeviceName()).thenReturn("HANDHELD");

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getStatus();

        //assert
        assertEquals(expectedList.toString(), deviceHealthResponseList.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() {
        //arrange
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(testCache);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        expectedList.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));

        when(mockHandheldScannerDevice.isConnected()).thenReturn(true);
        when(mockFlatbedScannerDevice.isConnected()).thenReturn(true);
        when(mockFlatbedScannerDevice.getDeviceName()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getDeviceName()).thenReturn("HANDHELD");

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getStatus();

        //assert
        assertEquals(expectedList.toString(), deviceHealthResponseList.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_CheckHealth() {
        //arrange
        when(mockCacheManager.getCache("scannerHealth")).thenReturn(null);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        expectedList.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));

        when(mockHandheldScannerDevice.isConnected()).thenReturn(true);
        when(mockFlatbedScannerDevice.isConnected()).thenReturn(true);
        when(mockFlatbedScannerDevice.getDeviceName()).thenReturn("FLATBED");
        when(mockHandheldScannerDevice.getDeviceName()).thenReturn("HANDHELD");

        //act
        List<DeviceHealthResponse> deviceHealthResponseList = scannerManagerCache.getStatus();

        //assert
        assertEquals(expectedList.toString(), deviceHealthResponseList.toString());
    }

    @Test
    void getScannerHealthStatus_FlatbedScanner() {
        //arrange
        List<DeviceHealthResponse> devReady = new ArrayList<>();
        devReady.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        devReady.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        Mockito.doReturn(devReady).when(scannerManagerCache).getStatus();

        //act
        DeviceHealth actual = scannerManagerCache.getScannerHealthStatus("HANDHELD");

        //assert
        assertEquals(DeviceHealth.READY, actual);
    }

    @Test
    void getScannerHealthStatus_HandScanner() {
        //arrange
        List<DeviceHealthResponse> devReady = new ArrayList<>();
        devReady.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        devReady.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        Mockito.doReturn(devReady).when(scannerManagerCache).getStatus();

        //act
        DeviceHealth actual = scannerManagerCache.getScannerHealthStatus("HANDHELD");

        //assert
        assertEquals(DeviceHealth.READY, actual);
    }

    @Test
    void getScannerHealthStatus_HandheldScanner_missing() {
        //arrange
        List<DeviceHealthResponse> devReady = new ArrayList<>();
        devReady.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        Mockito.doReturn(devReady).when(scannerManagerCache).getStatus();

        //act
        DeviceHealth actual = scannerManagerCache.getScannerHealthStatus("HANDHELD");

        //assert
        assertEquals(DeviceHealth.NOTREADY, actual);
    }
}
