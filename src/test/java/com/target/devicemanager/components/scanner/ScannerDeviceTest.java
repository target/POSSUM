package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.components.cashdrawer.CashDrawerDeviceListener;
import com.target.devicemanager.components.scanner.entities.Barcode;
import com.target.devicemanager.components.scanner.entities.ScannerType;
import com.target.devicemanager.configuration.ApplicationConfig;
import jpos.JposConst;
import jpos.JposException;
import jpos.Scanner;
import jpos.events.DataEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScannerDeviceTest {

    private ScannerDevice handheldScannerDevice;
    private ScannerDevice flatbedScannerDevice;
    private ScannerDevice scannerDeviceLock;

    @Mock
    private Scanner mockHandheldScanner;
    @Mock
    private Scanner mockFlatbedScanner;
    @Mock
    private DeviceListener mockDeviceListener;
    @Mock
    private DynamicDevice<Scanner> mockDynamicHandheldScanner;
    @Mock
    private DynamicDevice<Scanner> mockDynamicFlatbedScanner;
    @Mock
    private ReentrantLock mockConnectLock;
    @Mock
    private CashDrawerDeviceListener mockCashDrawerDeviceListener;

    @BeforeEach
    public void testInitialize() {
        when(mockDynamicHandheldScanner.getDevice()).thenReturn(mockHandheldScanner);
        when(mockDynamicFlatbedScanner.getDevice()).thenReturn(mockFlatbedScanner);

        handheldScannerDevice = new ScannerDevice(mockDeviceListener, mockDynamicHandheldScanner, ScannerType.HANDHELD, new ApplicationConfig());
        flatbedScannerDevice = new ScannerDevice(mockDeviceListener, mockDynamicFlatbedScanner, ScannerType.FLATBED, new ApplicationConfig());
        scannerDeviceLock = new ScannerDevice(mockDeviceListener, mockDynamicFlatbedScanner, ScannerType.FLATBED, mockConnectLock, new ApplicationConfig());

        //Default Mock Behavior
        when(mockDynamicHandheldScanner.isConnected()).thenReturn(true);
        when(mockDynamicHandheldScanner.getDeviceName()).thenReturn("HANDHELD");
        when(mockDynamicFlatbedScanner.isConnected()).thenReturn(true);
        when(mockDynamicFlatbedScanner.getDeviceName()).thenReturn("FLATBED");
    }

    @Test
    public void ctor_WhenDeviceListenerAndDynamicScannerAndScannerTypeAreNull_ThrowsException() {
        try {
            new ScannerDevice(null, null, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scannerType cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerAndDynamicScannerAreNull_ThrowsException() {
        try {
            new ScannerDevice(null, null, ScannerType.BOTH, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("deviceListener cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerAndScannerTypeAreNull_ThrowsException() {
        try {
            new ScannerDevice(null, mockDynamicHandheldScanner, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scannerType cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicScannerAndScannerTypeAreNull_ThrowsException() {
        try {
            new ScannerDevice(mockDeviceListener, null, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scannerType cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerIsNull_ThrowsException() {
        try {
            new ScannerDevice(null, mockDynamicHandheldScanner, ScannerType.BOTH, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("deviceListener cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicScannerIsNull_ThrowsException() {
        try {
            new ScannerDevice(mockDeviceListener, null, ScannerType.BOTH, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicScanner cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScannerTypeIsNull_ThrowsException() {
        try {
            new ScannerDevice(mockDeviceListener, mockDynamicFlatbedScanner, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scannerType cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerAndDynamicScannerAndScannerTypeAreNotNull_DoesNotThrowException() {
        try {
            new ScannerDevice(mockDeviceListener, mockDynamicFlatbedScanner, ScannerType.BOTH, null);
        } catch (Exception exception) {
            fail("Existing Device Arguments should not result in an Exception");
        }
    }

    @Test
    public void connect_WhenLockFails() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(false);

        //act
        scannerDeviceLock.connect();

        //assert
        verify(mockDynamicFlatbedScanner, never()).connect();
        verify(mockConnectLock, never()).unlock();
    }

    @Test
    public void connect_WhenConnected() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicFlatbedScanner.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);

        //act
        scannerDeviceLock.connect();

        //assert
        verify(mockDynamicFlatbedScanner).connect();
        verify(mockConnectLock).unlock();
        assertTrue(scannerDeviceLock.getDeviceConnected());
        verify(mockFlatbedScanner).addErrorListener(any());
        verify(mockFlatbedScanner).addDataListener(any());
        verify(mockFlatbedScanner).addStatusUpdateListener(any());
    }

    @Test
    public void connect_WhenNotConnected() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicFlatbedScanner.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        //act
        scannerDeviceLock.connect();

        //assert
        verify(mockDynamicFlatbedScanner).connect();
        verify(mockConnectLock).unlock();
        assertFalse(scannerDeviceLock.getDeviceConnected());
        verify(mockFlatbedScanner, never()).addErrorListener(any());
        verify(mockFlatbedScanner, never()).addDataListener(any());
        verify(mockFlatbedScanner, never()).addStatusUpdateListener(any());
    }

    @Test
    public void connect_WhenAlreadyConnected() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicFlatbedScanner.connect()).thenReturn(DynamicDevice.ConnectionResult.ALREADY_CONNECTED);

        //act
        scannerDeviceLock.connect();

        //assert
        verify(mockDynamicFlatbedScanner).connect();
        verify(mockConnectLock).unlock();
        assertTrue(scannerDeviceLock.getDeviceConnected());
        verify(mockFlatbedScanner, never()).addErrorListener(any());
        verify(mockFlatbedScanner, never()).addDataListener(any());
        verify(mockFlatbedScanner, never()).addStatusUpdateListener(any());
    }

    @Test
    public void reconnect_WhenTryLockFails() throws InterruptedException, DeviceException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(10, TimeUnit.SECONDS);

        //act
        try {
            scannerDeviceLock.reconnect();
        }

        //assert
        catch (DeviceException deviceException) {
            verify(mockDynamicFlatbedScanner, never()).disconnect();
            verify(mockDynamicFlatbedScanner, never()).connect();
            verify(mockConnectLock, never()).unlock();
            assertFalse(scannerDeviceLock.getDeviceConnected());
            verify(mockFlatbedScanner, never()).addErrorListener(any());
            verify(mockFlatbedScanner, never()).addDataListener(any());
            verify(mockFlatbedScanner, never()).addStatusUpdateListener(any());
            verify(mockFlatbedScanner, never()).removeErrorListener(any());
            verify(mockFlatbedScanner, never()).removeDataListener(any());
            verify(mockFlatbedScanner, never()).removeStatusUpdateListener(any());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void reconnect_WhenAlreadyConnected() throws InterruptedException, DeviceException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicFlatbedScanner.connect()).thenReturn(DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
        scannerDeviceLock.setDeviceConnected(false);

        //act
        boolean result = scannerDeviceLock.reconnect();

        //assert
        verify(mockDynamicFlatbedScanner, never()).disconnect();
        verify(mockDynamicFlatbedScanner).connect();
        verify(mockConnectLock).unlock();
        assertTrue(result);
        verify(mockFlatbedScanner, never()).addErrorListener(any());
        verify(mockFlatbedScanner, never()).addDataListener(any());
        verify(mockFlatbedScanner, never()).addStatusUpdateListener(any());
        verify(mockFlatbedScanner, never()).removeErrorListener(any());
        verify(mockFlatbedScanner, never()).removeDataListener(any());
        verify(mockFlatbedScanner, never()).removeStatusUpdateListener(any());
    }

    @Test
    public void reconnect_WhenNotConnected() throws InterruptedException, DeviceException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicFlatbedScanner.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);
        scannerDeviceLock.setDeviceConnected(false);

        //act
        boolean result = scannerDeviceLock.reconnect();

        //assert
        verify(mockDynamicFlatbedScanner, never()).disconnect();
        verify(mockDynamicFlatbedScanner).connect();
        verify(mockConnectLock).unlock();
        assertFalse(result);
        verify(mockFlatbedScanner, never()).addErrorListener(any());
        verify(mockFlatbedScanner, never()).addDataListener(any());
        verify(mockFlatbedScanner, never()).addStatusUpdateListener(any());
        verify(mockFlatbedScanner, never()).removeErrorListener(any());
        verify(mockFlatbedScanner, never()).removeDataListener(any());
        verify(mockFlatbedScanner, never()).removeStatusUpdateListener(any());
    }

    @Test
    public void reconnect_WhenConnected() throws InterruptedException, DeviceException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
        when(mockDynamicFlatbedScanner.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        scannerDeviceLock.setDeviceConnected(true);

        //act
        boolean result = scannerDeviceLock.reconnect();

        //assert
        verify(mockDynamicFlatbedScanner).disconnect();
        verify(mockDynamicFlatbedScanner).connect();
        verify(mockConnectLock).unlock();
        assertTrue(result);
        verify(mockFlatbedScanner).addErrorListener(any());
        verify(mockFlatbedScanner).addDataListener(any());
        verify(mockFlatbedScanner).addStatusUpdateListener(any());
        verify(mockFlatbedScanner).removeErrorListener(any());
        verify(mockFlatbedScanner).removeDataListener(any());
        verify(mockFlatbedScanner).removeStatusUpdateListener(any());
    }

    @Test
    public void getScannerData_WhenNotConnected_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicHandheldScanner.isConnected()).thenReturn(false);

        //act
        try {
            handheldScannerDevice.getScannerData();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDeviceListener, never()).startEventListeners();
            verify(mockHandheldScanner, never()).setAutoDisable(true);
            verify(mockHandheldScanner, never()).setDecodeData(true);
            verify(mockHandheldScanner, never()).setDataEventEnabled(true);
            verify(mockHandheldScanner, never()).setDeviceEnabled(true);
            assertEquals(JposConst.JPOS_E_OFFLINE, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getScannerData_WhenSetAutoDisable_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicFlatbedScanner.isConnected()).thenReturn(true).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_NOHARDWARE)).when(mockFlatbedScanner).setAutoDisable(true);

        //act
        try {
            flatbedScannerDevice.getScannerData();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDeviceListener).startEventListeners();
            verify(mockFlatbedScanner).setAutoDisable(true);
            verify(mockFlatbedScanner, never()).setDecodeData(true);
            verify(mockFlatbedScanner, never()).setDataEventEnabled(true);
            verify(mockFlatbedScanner, never()).setDeviceEnabled(true);
            assertEquals(JposConst.JPOS_E_NOHARDWARE, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getScannerData_WhenSetDecodeData_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicFlatbedScanner.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_DISABLED)).when(mockFlatbedScanner).setDecodeData(true);

        //act
        try {
            flatbedScannerDevice.getScannerData();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDeviceListener).startEventListeners();
            verify(mockFlatbedScanner).setAutoDisable(true);
            verify(mockFlatbedScanner).setDecodeData(true);
            verify(mockFlatbedScanner, never()).setDataEventEnabled(true);
            verify(mockFlatbedScanner, never()).setDeviceEnabled(true);
            assertEquals(JposConst.JPOS_E_DISABLED, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getScannerData_WhenSetDataEventEnabledWithHandScanner_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicHandheldScanner.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_DISABLED)).doNothing().when(mockHandheldScanner).setDataEventEnabled(true);
        byte[] expectedData = {'T', 'E', 'S', 'T'};
        int expectedType = 101;
        when(mockHandheldScanner.getScanDataLabel()).thenReturn(expectedData);
        when(mockHandheldScanner.getScanDataType()).thenReturn(expectedType);
        when(mockDeviceListener.waitForData()).thenReturn(new DataEvent(mockHandheldScanner, 1));

        //act
        handheldScannerDevice.getScannerData();

        //assert
        verify(mockDeviceListener, times(2)).startEventListeners();
        verify(mockHandheldScanner, times(2)).setAutoDisable(true);
        verify(mockHandheldScanner, times(2)).setDecodeData(true);
        verify(mockHandheldScanner, times(2)).setDataEventEnabled(true);
        verify(mockHandheldScanner, times(1)).setDeviceEnabled(true);
        verify(mockHandheldScanner).getScanDataLabel();
        verify(mockHandheldScanner).getScanDataType();
    }

    @Test
    public void getScannerData_WhenSetDeviceEnabledWithHandScanner_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicHandheldScanner.isConnected()).thenReturn(true).thenReturn(true).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_FAILURE)).doNothing().when(mockHandheldScanner).setDeviceEnabled(true);
        when(mockDeviceListener.waitForData()).thenReturn(new DataEvent(mockCashDrawerDeviceListener, 1));

        //act
        try {
            handheldScannerDevice.getScannerData();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDeviceListener, times(1)).startEventListeners();
            verify(mockHandheldScanner, times(1)).setAutoDisable(true);
            verify(mockHandheldScanner, times(1)).setDecodeData(true);
            verify(mockHandheldScanner, times(1)).setDataEventEnabled(true);
            verify(mockHandheldScanner, times(1)).setDeviceEnabled(true);
            verify(mockHandheldScanner, never()).getScanDataLabel();
            verify(mockHandheldScanner, never()).getScanDataType();
            assertEquals(JposConst.JPOS_E_FAILURE, jposException.getErrorCode());
        }
    }

    @Test
    public void getScannerData_WhenTimeoutWithHandScanner_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicHandheldScanner.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_TIMEOUT)).doNothing().when(mockHandheldScanner).setAutoDisable(true);
        handheldScannerDevice.setIsTest(true);
        int expectedType = 101;
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockHandheldScanner).getScanDataLabel();
        when(mockHandheldScanner.getScanDataType()).thenReturn(expectedType);
        when(mockDeviceListener.waitForData()).thenReturn(new DataEvent(mockHandheldScanner, 1));

        //act
        try {
            handheldScannerDevice.getScannerData();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDeviceListener, times(4)).startEventListeners();
            verify(mockHandheldScanner, times(4)).setAutoDisable(true);
            verify(mockHandheldScanner, times(3)).setDecodeData(true);
            verify(mockHandheldScanner, times(3)).setDataEventEnabled(true);
            verify(mockHandheldScanner, times(3)).setDeviceEnabled(true);
            verify(mockHandheldScanner).getScanDataLabel();
            verify(mockHandheldScanner, never()).getScanDataType();
            assertEquals(JposConst.JPOS_E_EXTENDED, jposException.getErrorCode());
        }
    }

    @Test
    public void getScannerData_WhenTimeoutWithHandScanner_ThrowsInterruptedException() throws JposException {
        //arrange
        when(mockDynamicHandheldScanner.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_TIMEOUT)).doNothing().when(mockHandheldScanner).setAutoDisable(true);
        handheldScannerDevice.setIsTest(true);
        byte[] expectedData = {'T', 'E', 'S', 'T'};
        int expectedType = 101;
        when(mockHandheldScanner.getScanDataLabel()).thenReturn(expectedData);
        when(mockHandheldScanner.getScanDataType()).thenReturn(expectedType);
        when(mockDeviceListener.waitForData()).thenReturn(new DataEvent(mockHandheldScanner, 1));

        //act
        handheldScannerDevice.getScannerData();
        ScannerDeviceTest.TestInterruptingThread scannerDataThread = new TestInterruptingThread();
        scannerDataThread.start();
        scannerDataThread.interrupt();

        //assert
        verify(mockDeviceListener, atLeast(1)).startEventListeners();
        verify(mockHandheldScanner, atLeast(1)).setAutoDisable(true);
        verify(mockHandheldScanner, atLeast(1)).setDecodeData(true);
        verify(mockHandheldScanner, atLeast(1)).setDataEventEnabled(true);
        verify(mockHandheldScanner, atLeast(1)).setDeviceEnabled(true);
        verify(mockHandheldScanner, atLeast(1)).getScanDataLabel();
        verify(mockHandheldScanner, atLeast(1)).getScanDataType();
    }

    @Test
    public void getScannerData_WhenIsSimulationMode() throws JposException {
        //arrange
        System.setProperty("useSimulators", "true");
        when(mockDynamicHandheldScanner.isConnected()).thenReturn(true);
        handheldScannerDevice.setIsTest(true);
        byte[] expectedData = {'T', 'E', 'S', 'T'};
        int expectedType = 101;
        String expectedSource = "HANDHELD";
        when(mockHandheldScanner.getScanDataLabel()).thenReturn(expectedData);
        when(mockHandheldScanner.getScanDataType()).thenReturn(expectedType);
        when(mockHandheldScanner.getPhysicalDeviceName()).thenReturn(expectedSource);
        when(mockDeviceListener.waitForData()).thenReturn(new DataEvent(mockHandheldScanner, 1));

        //act
        Barcode barcode = handheldScannerDevice.getScannerData();

        //assert
        verify(mockDeviceListener, atLeast(1)).startEventListeners();
        verify(mockHandheldScanner, atLeast(1)).setAutoDisable(true);
        verify(mockHandheldScanner, atLeast(1)).setDecodeData(true);
        verify(mockHandheldScanner, atLeast(1)).setDataEventEnabled(true);
        verify(mockHandheldScanner, atLeast(1)).setDeviceEnabled(true);
        verify(mockHandheldScanner, atLeast(1)).getScanDataLabel();
        verify(mockHandheldScanner, atLeast(1)).getScanDataType();
        assertEquals(new String(expectedData, Charset.defaultCharset()), barcode.data);
        assertEquals(expectedType, barcode.type.getValue());
        assertEquals(expectedSource, barcode.source);
    }

    class TestInterruptingThread extends Thread{
        public void run() {
            try {
                handheldScannerDevice.getScannerData();
            } catch (Exception exception) {
                //do nothing
            }
        }
    }

    @Test
    public void cancelScannerData_WhenSetDeviceEnabledFails_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicFlatbedScanner.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockFlatbedScanner).setDeviceEnabled(false);

        //act
        flatbedScannerDevice.cancelScannerData();

        //assert
        verify(mockFlatbedScanner).setDeviceEnabled(false);
        verify(mockDeviceListener).stopWaitingForData();
    }

    @Test
    public void cancelScannerData_WhenSetDeviceEnabledFailsHandheld_ThrowsException() throws JposException {
        //arrange
        when(mockDynamicHandheldScanner.isConnected()).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockHandheldScanner).setDeviceEnabled(false);

        //act
        handheldScannerDevice.cancelScannerData();

        //assert
        verify(mockHandheldScanner).setDeviceEnabled(false);
        verify(mockDeviceListener).stopWaitingForData();
    }

    @Test
    public void cancelScannerData_WhenSetDeviceEnabledSucceeds() throws JposException {
        //arrange
        when(mockDynamicHandheldScanner.isConnected()).thenReturn(true);

        //act
        handheldScannerDevice.cancelScannerData();

        //assert
        verify(mockHandheldScanner).setDeviceEnabled(false);
        verify(mockDeviceListener).stopWaitingForData();
    }

    @Test
    public void getDeviceName_Returns() throws JposException {
        //arrange
        String expected = "HANDHELD";

        //act
        String actual = handheldScannerDevice.getDeviceName();

        //assert
        assertEquals(expected, actual);
    }
}
