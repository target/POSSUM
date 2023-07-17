package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.printer.entities.PrinterContent;
import com.target.devicemanager.components.printer.entities.PrinterError;
import com.target.devicemanager.components.printer.entities.PrinterException;
import com.target.devicemanager.components.printer.entities.PrinterStationType;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PrinterManagerTest {

    private PrinterManager printerManager;
    private PrinterManager printerManagerCacheFuture;

    @Mock
    private PrinterDevice mockPrinterDevice;
    @Mock
    private Lock mockPrinterLock;
    @Mock
    private CacheManager mockCacheManager;
    @Mock
    private Future<Void> mockFuture;

    private final Cache testCache = new Cache() {
        final Map<Object, Object> cacheMap = new HashMap<>();

        @Override
        public String getName() {
            return "printerHealth";
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
        printerManager = new PrinterManager(mockPrinterDevice, mockPrinterLock);
        printerManagerCacheFuture = new PrinterManager(mockPrinterDevice, mockPrinterLock, mockCacheManager, mockFuture, true);
    }

    @Test
    public void ctor_WhenPrinterDeviceAndLockAreNull_ThrowsException() {
        try {
            new PrinterManager(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("printerDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenPrinterDeviceIsNull_ThrowsException() {
        try {
            new PrinterManager(null, mockPrinterLock);
        } catch (IllegalArgumentException iae) {
            assertEquals("printerDevice cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenPrinterLockIsNull_ThrowsException() {
        try {
            new PrinterManager(mockPrinterDevice, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("printerLock cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenPrinterDeviceAndLockAreNotNull_DoesNotThrowException() {
        try {
            new PrinterManager(mockPrinterDevice, mockPrinterLock);
        } catch(Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockSucceeds_Connects() {
        //arrange
        when(mockPrinterDevice.tryLock()).thenReturn(true);
        when(mockPrinterDevice.connect()).thenReturn(true);

        //act
        printerManager.connect();

        //assert
        verify(mockPrinterDevice).connect();
        verify(mockPrinterDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockSucceeds_Reconnects() {
        //arrange
        when(mockPrinterDevice.tryLock()).thenReturn(true);
        when(mockPrinterDevice.connect()).thenReturn(true);

        //act
        try {
            printerManager.reconnectDevice();
        } catch (Exception deviceException) {
            fail("printerManager.connect() should not result in an Exception");
        }

        //assert
        verify(mockPrinterDevice).connect();
        verify(mockPrinterDevice).unlock();
    }

    @Test
    public void reconnect_WhenLockFails_DoesNotReonnect() {
        //arrange
        when(mockPrinterDevice.tryLock()).thenReturn(false);

        //act
        try {
            printerManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            verify(mockPrinterDevice, never()).connect();
            verify(mockPrinterDevice, never()).unlock();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }

        //assert
        fail("Expected DEVICE_BUSY, but got none.");
    }

    @Test
    public void reconnect_WhenDeviceConnectFails_DoesNotReconnect() {
        //arrange
        when(mockPrinterDevice.tryLock()).thenReturn(true);
        when(mockPrinterDevice.connect()).thenReturn(false);

        //act
        try {
            printerManager.reconnectDevice();
        } catch (DeviceException deviceException) {
            assertEquals(DeviceError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }

        //assert
        fail("Expected DEVICE_OFFLINE, but got none.");
    }

    @Test
    public void printReceipt_WhenLocked_ThrowsException() throws JposException, DeviceException {
        //arrange
        List<PrinterContent> testContents = new ArrayList<>();
        when(mockPrinterLock.tryLock()).thenReturn(false);

        //act
        try {
            printerManager.printReceipt(testContents);
        }

        //assert
        catch(PrinterException deviceException) {
            assertEquals(PrinterError.DEVICE_BUSY, deviceException.getDeviceError());
            verify(mockPrinterDevice, never()).printContent(any(), anyInt());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printReceipt_WhenUnlocked_CallsThroughDevice() throws JposException, DeviceException {
        //arrange
        List<PrinterContent> testContents = new ArrayList<>();
        when(mockPrinterLock.tryLock()).thenReturn(true);

        //act
        try {
            printerManager.printReceipt(testContents);
        }

        //assert
        catch(PrinterException deviceException) {
            fail("Unlocked should not result in exception");
        }

        verify(mockPrinterDevice).printContent(testContents, PrinterStationType.RECEIPT_PRINTER.getValue());
        verify(mockPrinterLock).unlock();
    }

    @Test
    public void printReceipt_WhenFutureThrowsTimeoutException() throws PrinterException, JposException, InterruptedException, ExecutionException, TimeoutException {
        //arrange
        List<PrinterContent> testContents = new ArrayList<>();
        when(mockPrinterLock.tryLock()).thenReturn(true);
        doThrow(new TimeoutException()).when(mockFuture).get(PrinterManager.getPrinterTimeoutValue(), TimeUnit.SECONDS);
        //act
        try {
            printerManagerCacheFuture.printReceipt(testContents);
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockPrinterDevice, never()).printContent(testContents, PrinterStationType.RECEIPT_PRINTER.getValue());
            assertEquals(PrinterError.PRINTER_TIME_OUT, deviceException.getDeviceError());
            verify(mockPrinterLock).unlock();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printReceipt_WhenFutureThrowsInterruptedException() throws DeviceException, JposException, InterruptedException, ExecutionException, TimeoutException {
        //arrange
        List<PrinterContent> testContents = new ArrayList<>();
        when(mockPrinterLock.tryLock()).thenReturn(true);
        doThrow(new InterruptedException()).when(mockFuture).get(PrinterManager.getPrinterTimeoutValue(), TimeUnit.SECONDS);
        //act
        try {
            printerManagerCacheFuture.printReceipt(testContents);
        }

        //assert
        catch (PrinterException printerException) {
            verify(mockPrinterDevice, never()).printContent(testContents, PrinterStationType.RECEIPT_PRINTER.getValue());
            assertEquals(DeviceError.UNEXPECTED_ERROR, printerException.getDeviceError());
            verify(mockPrinterLock).unlock();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printReceipt_WhenDeviceThrowsJposException() throws DeviceException, JposException {
        //arrange
        List<PrinterContent> testContents = new ArrayList<>();
        when(mockPrinterLock.tryLock()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED, 24)).when(mockPrinterDevice).printContent(any(), anyInt());

        //act
        try {
            printerManager.printReceipt(testContents);
        }

        //assert
        catch (PrinterException printerException) {
            verify(mockPrinterDevice).printContent(testContents, PrinterStationType.RECEIPT_PRINTER.getValue());
            assertEquals(PrinterError.OUT_OF_PAPER, printerException.getDeviceError());
            verify(mockPrinterLock).unlock();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printReceipt_WhenDeviceThrowsPrinterException() throws DeviceException, JposException {
        //arrange
        List<PrinterContent> testContents = new ArrayList<>();
        when(mockPrinterLock.tryLock()).thenReturn(true);
        doThrow(new PrinterException(PrinterError.INVALID_FORMAT)).when(mockPrinterDevice).printContent(any(), anyInt());

        //act
        try {
            printerManager.printReceipt(testContents);
        }

        //assert
        catch (PrinterException printerException) {
            verify(mockPrinterDevice).printContent(testContents, PrinterStationType.RECEIPT_PRINTER.getValue());
            assertEquals(PrinterError.INVALID_FORMAT, printerException.getDeviceError());
            verify(mockPrinterLock).unlock();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void frankCheck_WhenLocked_ThrowsException() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> testContents = new ArrayList<>();
        when(mockPrinterLock.tryLock()).thenReturn(false);

        //act
        try {
            printerManager.frankCheck(testContents);
        }

        //assert
        catch(PrinterException printerException) {
            assertEquals(PrinterError.DEVICE_BUSY, printerException.getDeviceError());
            verify(mockPrinterDevice, never()).printContent(any(), anyInt());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void frankCheck_WhenUnlocked_CallsThroughDevice() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> testContents = new ArrayList<>();
        when(mockPrinterLock.tryLock()).thenReturn(true);

        //act
        try {
            printerManager.frankCheck(testContents);
        }

        //assert
        catch(PrinterException printerException) {
            fail("Unlocked should not result in exception");
        }

        verify(mockPrinterDevice).printContent(testContents, PrinterStationType.CHECK_PRINTER.getValue());
        verify(mockPrinterLock).unlock();
    }

    @Test
    public void frankCheck_WhenDeviceThrowsException() throws PrinterException, JposException {
        //arrange
        List<PrinterContent> testContents = new ArrayList<>();
        when(mockPrinterLock.tryLock()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinterDevice).printContent(any(), anyInt());

        //act
        try {
            printerManager.frankCheck(testContents);
        }

        //assert
        catch (PrinterException printerException) {
            verify(mockPrinterDevice).printContent(testContents, PrinterStationType.CHECK_PRINTER.getValue());
            assertEquals(PrinterError.UNEXPECTED_ERROR, printerException.getDeviceError());
            verify(mockPrinterLock).unlock();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getHealth_WhenDeviceOffline_ShouldReturnNotReadyHealthResponse() {
        //arrange
        when(mockPrinterDevice.isConnected()).thenReturn(false);
        when(mockPrinterDevice.getDeviceName()).thenReturn("printer");
        when(mockCacheManager.getCache("printerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("printer", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = printerManagerCacheFuture.getHealth();

        //assert
        assertEquals("printer", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.NOTREADY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getHealth_WhenDeviceOnline_ShouldReturnReadyHealthResponse() {
        //arrange
        when(mockPrinterDevice.isConnected()).thenReturn(true);
        when(mockPrinterDevice.getDeviceName()).thenReturn("printer");
        when(mockCacheManager.getCache("printerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("printer", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = printerManagerCacheFuture.getHealth();

        //assert
        assertEquals("printer", deviceHealthResponse.getDeviceName());
        assertEquals(DeviceHealth.READY, deviceHealthResponse.getHealthStatus());
        assertEquals(expected.toString(), testCache.get("health").get().toString());
    }

    @Test
    public void getStatus_WhenCacheExists() {
        //arrange
        when(mockCacheManager.getCache("printerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("printer", DeviceHealth.READY);
        testCache.put("health", expected);

        //act
        DeviceHealthResponse deviceHealthResponse = printerManagerCacheFuture.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheExists_WhenCheckHealthFlag() {
        //arrange
        when(mockCacheManager.getCache("printerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("printer", DeviceHealth.READY);
        testCache.put("health", expected);

        printerManagerCacheFuture.connect(); //set check health flag to CHECK_HEALTH
        when(mockPrinterDevice.isConnected()).thenReturn(true); //make sure health returns READY
        when(mockPrinterDevice.getDeviceName()).thenReturn("printer");

        //act
        DeviceHealthResponse deviceHealthResponse = printerManagerCacheFuture.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOffline() {
        //arrange
        when(mockPrinterDevice.isConnected()).thenReturn(false);
        when(mockPrinterDevice.getDeviceName()).thenReturn("printer");
        when(mockCacheManager.getCache("printerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("printer", DeviceHealth.NOTREADY);

        //act
        DeviceHealthResponse deviceHealthResponse = printerManagerCacheFuture.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }

    @Test
    public void getStatus_WhenCacheNull_WhenDeviceOnline() {
        //arrange
        when(mockPrinterDevice.isConnected()).thenReturn(true);
        when(mockPrinterDevice.getDeviceName()).thenReturn("printer");
        when(mockCacheManager.getCache("printerHealth")).thenReturn(testCache);
        DeviceHealthResponse expected = new DeviceHealthResponse("printer", DeviceHealth.READY);

        //act
        DeviceHealthResponse deviceHealthResponse = printerManagerCacheFuture.getStatus();

        //assert
        assertEquals(expected.toString(), deviceHealthResponse.toString());
    }
}
