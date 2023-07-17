package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.components.cashdrawer.entities.CashDrawerError;
import jpos.CashDrawer;
import jpos.CashDrawerConst;
import jpos.JposConst;
import jpos.JposException;
import jpos.events.StatusUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CashDrawerDeviceTest {

    private CashDrawerDevice cashDrawerDevice;
    private CashDrawerDevice cashDrawerDeviceLock;

    @Mock
    private DynamicDevice<CashDrawer> mockDynamicCashDrawer;
    @Mock
    private CashDrawer mockCashDrawer;
    @Mock
    private StatusUpdateEvent mockStatusUpdateEvent;
    @Mock
    private DeviceListener mockDeviceListener;
    @Mock
    private ReentrantLock mockConnectLock;

    @BeforeEach
    public void testInitialize() throws Exception {
        when(mockDynamicCashDrawer.getDevice()).thenReturn(mockCashDrawer);

        cashDrawerDevice = new CashDrawerDevice(mockDynamicCashDrawer, mockDeviceListener);
        cashDrawerDeviceLock = new CashDrawerDevice(mockDynamicCashDrawer, mockDeviceListener, mockConnectLock);

        //Default Mock Behavior
        when(mockCashDrawer.getDrawerOpened()).thenReturn(true);
    }

    @Test
    public void ctor_WhenDynamicCashDrawerAndDeviceListenerAreNull_ThrowsException() {
        try {
            new CashDrawerDevice(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("simpleCashDrawer cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicCashDrawerIsNull_ThrowsException() {
        try {
            new CashDrawerDevice(null, mockDeviceListener);
        } catch (IllegalArgumentException iae) {
            assertEquals("simpleCashDrawer cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerIsNull_ThrowsException() {
        try {
            new CashDrawerDevice(mockDynamicCashDrawer, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("deviceListener cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicCashDrawerAndDeviceListenerAreNotNull_DoesNotThrowException() {
        try {
            new CashDrawerDevice(mockDynamicCashDrawer, mockDeviceListener);
        } catch(Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_CallsDynamicConnect() {
        //arrange

        //act
        assertTrue(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_CallsDynamicConnect_ReturnsNotConnected() {
        //arrange
        when(mockDynamicCashDrawer.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        //act
        assertFalse(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockDynamicCashDrawer, never()).getDevice();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_WhenAreListenersAttachedFalse_AttachListeners() {
        //arrange
        cashDrawerDevice.setAreListenersAttached(false);

        //act
        assertTrue(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockCashDrawer).addStatusUpdateListener(any());
        assertTrue(cashDrawerDevice.getAreListenersAttached());
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_WhenAreListenersAttachedTrue_DoesNotAttachListeners() {
        //arrange
        cashDrawerDevice.setAreListenersAttached(true);

        //act
        assertTrue(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockCashDrawer, never()).addStatusUpdateListener(any());
        assertTrue(cashDrawerDevice.getAreListenersAttached());
        verify(mockDynamicCashDrawer, times(1)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledFalse_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockCashDrawer.getDeviceEnabled()).thenReturn(false);

        //act
        assertTrue(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer).getDrawerOpened();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledTrue_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockCashDrawer.getDeviceEnabled()).thenReturn(true);

        //act
        assertTrue(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockCashDrawer, never()).setDeviceEnabled(true);
        verify(mockCashDrawer, never()).getDrawerOpened();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockCashDrawer).getDeviceEnabled();

        //act
        assertFalse(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockCashDrawer, never()).setDeviceEnabled(true);
        verify(mockCashDrawer, never()).getDrawerOpened();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockCashDrawer).setDeviceEnabled(true);

        //act
        assertFalse(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer, never()).getDrawerOpened();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDrawerOpenedFalse_DoesNotSetOpened() throws JposException {
        //arrange
        when(mockCashDrawer.getDrawerOpened()).thenReturn(false);

        //act
        assertTrue(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer).getDrawerOpened();
        assertFalse(cashDrawerDevice.getCashDrawerOpen());
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDrawerOpenedTrue_SetsOpened() throws JposException {
        //arrange
        when(mockCashDrawer.getDrawerOpened()).thenReturn(true);

        //act
        assertTrue(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer).getDrawerOpened();
        assertTrue(cashDrawerDevice.getCashDrawerOpen());
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDrawerOpenedThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockCashDrawer).getDrawerOpened();

        //act
        assertFalse(cashDrawerDevice.connect());

        //assert
        verify(mockDynamicCashDrawer).connect();
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer).getDrawerOpened();
        assertFalse(cashDrawerDevice.getCashDrawerOpen());
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(true);

        //act
        cashDrawerDevice.disconnect();

        //assert
        assertFalse(cashDrawerDevice.getAreListenersAttached());
        verify(mockCashDrawer, times(2)).getDeviceEnabled();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenAreListenersAttachedFalse_DoesNotDetachListeners() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(true);
        cashDrawerDevice.setAreListenersAttached(false);

        //act
        cashDrawerDevice.disconnect();

        //assert
        assertFalse(cashDrawerDevice.getAreListenersAttached());
        verify(mockCashDrawer, times(2)).getDeviceEnabled();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenAreListenersAttachedTrue_DetachListeners() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(true);
        cashDrawerDevice.setAreListenersAttached(true);

        //act
        cashDrawerDevice.disconnect();

        //assert
        assertFalse(cashDrawerDevice.getAreListenersAttached());
        verify(mockCashDrawer, times(2)).getDeviceEnabled();
        verify(mockDynamicCashDrawer, times(3)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }


    @Test
    public void disconnect_WhenIsConnectedTrue_WhenGetDeviceEnabledFalse_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(true);
        when(mockCashDrawer.getDeviceEnabled()).thenReturn(false);

        //act
        cashDrawerDevice.disconnect();

        //assert
        assertFalse(cashDrawerDevice.getAreListenersAttached());
        verify(mockCashDrawer, times(2)).getDeviceEnabled();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        verify(mockCashDrawer, never()).setDeviceEnabled(false);
        verify(mockDynamicCashDrawer, never()).disconnect();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenGetDeviceEnabledTrue_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(true);
        when(mockCashDrawer.getDeviceEnabled()).thenReturn(true);

        //act
        cashDrawerDevice.disconnect();

        //assert
        assertFalse(cashDrawerDevice.getAreListenersAttached());
        verify(mockCashDrawer, times(2)).getDeviceEnabled();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        verify(mockCashDrawer, times(1)).setDeviceEnabled(false);
        verify(mockDynamicCashDrawer, times(1)).disconnect();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockCashDrawer).getDeviceEnabled();

        //act
        cashDrawerDevice.disconnect();

        //assert
        assertFalse(cashDrawerDevice.getAreListenersAttached());
        verify(mockCashDrawer, times(2)).getDeviceEnabled();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        verify(mockCashDrawer, never()).setDeviceEnabled(false);
        verify(mockDynamicCashDrawer, never()).disconnect();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedTrue_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(true);

        //act
        cashDrawerDevice.disconnect();

        //assert
        assertFalse(cashDrawerDevice.getAreListenersAttached());
        verify(mockCashDrawer, times(2)).getDeviceEnabled();
        verify(mockDynamicCashDrawer, times(2)).getDevice();
        verify(mockDynamicCashDrawer, never()).disconnect();
        assertTrue(cashDrawerDevice.isConnected());
    }


    @Test
    public void disconnect_WhenIsConnectedFalse() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(false);
        cashDrawerDevice.setAreListenersAttached(true);

        //act
        cashDrawerDevice.disconnect();

        //assert
        assertTrue(cashDrawerDevice.getAreListenersAttached());
        verify(mockCashDrawer, never()).setDeviceEnabled(false);
        verify(mockDynamicCashDrawer, times(1)).getDevice();
        verify(mockCashDrawer, times(1)).getDeviceEnabled();
        verify(mockCashDrawer, times(1)).setDeviceEnabled(true);
        verify(mockDynamicCashDrawer, never()).disconnect();
        verify(mockCashDrawer, times(1)).getDrawerOpened();
        assertTrue(cashDrawerDevice.isConnected());
    }


    @Test
    public void disconnect_WhenIsConnectedFalse_WhenGetDeviceEnabledFalse_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(false);
        when(mockCashDrawer.getDeviceEnabled()).thenReturn(false);

        //act
        cashDrawerDevice.disconnect();

        //assert
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer).getDrawerOpened();
        verify(mockDynamicCashDrawer, times(1)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenGetDeviceEnabledTrue_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(false);
        when(mockCashDrawer.getDeviceEnabled()).thenReturn(true);

        //act
        cashDrawerDevice.disconnect();

        //assert
        verify(mockCashDrawer, never()).setDeviceEnabled(true);
        verify(mockCashDrawer, never()).getDrawerOpened();
        verify(mockDynamicCashDrawer, times(1)).getDevice();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockCashDrawer).getDeviceEnabled();

        //act
        cashDrawerDevice.disconnect();

        //assert
        verify(mockCashDrawer, never()).setDeviceEnabled(true);
        verify(mockCashDrawer, never()).getDrawerOpened();
        verify(mockDynamicCashDrawer, times(1)).getDevice();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockCashDrawer).setDeviceEnabled(true);

        //act
        cashDrawerDevice.disconnect();

        //assert
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer, never()).getDrawerOpened();
        verify(mockDynamicCashDrawer, times(1)).getDevice();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenGetDrawerOpenedFalse_DoesNotSetOpened() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(false);
        when(mockCashDrawer.getDrawerOpened()).thenReturn(false);

        //act
        cashDrawerDevice.disconnect();

        //assert
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer).getDrawerOpened();
        assertFalse(cashDrawerDevice.getCashDrawerOpen());
        verify(mockDynamicCashDrawer, times(1)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenGetDrawerOpenedTrue_SetsOpened() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(false);
        when(mockCashDrawer.getDrawerOpened()).thenReturn(true);

        //act
        cashDrawerDevice.disconnect();

        //assert
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer).getDrawerOpened();
        assertTrue(cashDrawerDevice.getCashDrawerOpen());
        verify(mockDynamicCashDrawer, times(1)).getDevice();
        assertTrue(cashDrawerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenIsConnectedFalse_WhenGetDrawerOpenedThrowsException() throws JposException {
        //arrange
        when(mockDynamicCashDrawer.isConnected()).thenReturn(false);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockCashDrawer).getDrawerOpened();

        //act
        cashDrawerDevice.disconnect();

        //assert
        verify(mockCashDrawer).setDeviceEnabled(true);
        verify(mockCashDrawer).getDrawerOpened();
        assertFalse(cashDrawerDevice.getCashDrawerOpen());
        verify(mockDynamicCashDrawer, times(1)).getDevice();
        assertFalse(cashDrawerDevice.isConnected());
    }

    @Test
    public void openCashDrawer_WhenIsConnectedFalse_ThrowsException() throws DeviceException {
        //arrange
        cashDrawerDevice.setDeviceConnected(false);

        //act
        try {
            cashDrawerDevice.openCashDrawer();
        }

        //assert
        catch(JposException jposException) {
            assertEquals(JposConst.JPOS_E_OFFLINE, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void openCashDrawer_WhenIsConnectedTrue_CallsDeviceListeners() throws JposException, DeviceException {
        //arrange
        cashDrawerDevice.setDeviceConnected(true);

        //act
        cashDrawerDevice.openCashDrawer();

        //assert
        verify(mockDeviceListener).startEventListeners();
    }

    @Test
    public void openCashDrawer_WhenCashDrawerOpenTrue_ThrowsException() throws JposException {
        //arrange
        cashDrawerDevice.setDeviceConnected(true);
        cashDrawerDevice.setCashDrawerOpen(true);

        //act
        try {
            cashDrawerDevice.openCashDrawer();
        }

        //assert
        catch (DeviceException deviceException) {
            assertEquals(CashDrawerError.ALREADY_OPEN, deviceException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void openCashDrawer_WhenCashDrawerOpenFalse_OpenDrawer() throws JposException, DeviceException {
        //arrange
        cashDrawerDevice.setDeviceConnected(true);
        cashDrawerDevice.setCashDrawerOpen(false);

        //act
        cashDrawerDevice.openCashDrawer();

        ///assert
        verify(mockCashDrawer).openDrawer();
    }

    @Test
    public void openCashDrawer_WhenTimeoutFails_DoNothing() throws JposException {
        //arrange
        cashDrawerDevice.setDeviceConnected(true);
        cashDrawerDevice.setCashDrawerOpen(false);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(CashDrawerConst.CASH_SUE_DRAWEROPEN);
        doAnswer(invocation -> {
            cashDrawerDevice.statusUpdateOccurred(mockStatusUpdateEvent);
            return null;
        }).when(mockCashDrawer).openDrawer();

        //act
        TestInterruptingThread openDrawerThread = new TestInterruptingThread();
        openDrawerThread.start();
        openDrawerThread.interrupt();

        //assert
        //nothing
    }

    class TestInterruptingThread extends Thread{
        public void run() {
            try {
                cashDrawerDevice.openCashDrawer();
            } catch (Exception exception) {
                //do nothing
            }
        }
    }

    @Test
    public void openCashDrawer_WhenCashDrawerDisconnects_ThrowsException() throws JposException {
        //arrange
        cashDrawerDevice.setDeviceConnected(true);
        cashDrawerDevice.setCashDrawerOpen(false);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(CashDrawerConst.CASH_SUE_DRAWEROPEN);
        doAnswer(invocation -> {
            cashDrawerDevice.statusUpdateOccurred(mockStatusUpdateEvent);
            cashDrawerDevice.setDeviceConnected(false);
            return null;
        }).when(mockCashDrawer).openDrawer();

        //act
        try {
            cashDrawerDevice.openCashDrawer();
            cashDrawerDevice.setDeviceConnected(false);
        }

        //assert
        catch (DeviceException deviceException) {
            assertEquals(CashDrawerError.DEVICE_OFFLINE, deviceException.getDeviceError());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getDeviceName_ReturnsName() {
        // arrange
        String expectedDeviceName = "cashDrawer";
        when(mockDynamicCashDrawer.getDeviceName()).thenReturn(expectedDeviceName);

        //act
        String actual = cashDrawerDevice.getDeviceName();

        // act/assert
        assertEquals(expectedDeviceName, actual);
    }

    @Test
    public void isConnected_ReturnsTrueFromDynamicDevice() {
        //arrange
        cashDrawerDevice.setDeviceConnected(true);

        //act
        boolean actual = cashDrawerDevice.isConnected();

        //assert
        assertTrue(actual);
    }

    @Test
    public void isConnected_ReturnsFalseFromDynamicDevice() {
        //arrange
        cashDrawerDevice.setDeviceConnected(false);

        //act
        boolean actual = cashDrawerDevice.isConnected();

        //assert
        assertFalse(actual);
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOff_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF);
        CashDrawerDevice cashDrawerSpy = spy(cashDrawerDevice);

        //act
        cashDrawerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(cashDrawerSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF_OFFLINE );
        CashDrawerDevice cashDrawerSpy = spy(cashDrawerDevice);

        //act
        cashDrawerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(cashDrawerSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFFLINE );
        CashDrawerDevice cashDrawerSpy = spy(cashDrawerDevice);

        //act
        cashDrawerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(cashDrawerSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOnline_Connected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_ONLINE);
        CashDrawerDevice cashDrawerSpy = spy(cashDrawerDevice);

        //act
        cashDrawerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(cashDrawerSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDrawerOpen() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(CashDrawerConst.CASH_SUE_DRAWEROPEN);
        CashDrawerDevice cashDrawerSpy = spy(cashDrawerDevice);

        //act
        cashDrawerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(cashDrawerSpy.getCashDrawerOpen());
    }

    @Test
    public void statusUpdateOccurred_WhenDrawerClose() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(CashDrawerConst.CASH_SUE_DRAWERCLOSED);
        CashDrawerDevice cashDrawerSpy = spy(cashDrawerDevice);

        //act
        cashDrawerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(cashDrawerSpy.getCashDrawerOpen());
    }

    @Test
    public void statusUpdateOccurred_WhenOtherStatus_DoNothing() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(572);
        CashDrawerDevice cashDrawerSpy = spy(cashDrawerDevice);

        //act
        cashDrawerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        //do nothing
    }

    @Test
    public void tryLock_WhenLock_ReturnsTrue() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);

        //act
        cashDrawerDeviceLock.tryLock();

        //assert
        assertTrue(cashDrawerDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenNotLock_ReturnsFalse() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(false);

        //act
        cashDrawerDeviceLock.tryLock();

        //assert
        assertFalse(cashDrawerDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenLockThrowsException_ReturnsFalse() throws InterruptedException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(10, TimeUnit.SECONDS);

        //act
        cashDrawerDeviceLock.tryLock();

        //assert
        assertFalse(cashDrawerDeviceLock.getIsLocked());
    }

    @Test
    public void unlock_CallsThrough() {
        //arrange

        //act
        cashDrawerDeviceLock.unlock();

        //assert
        verify(mockConnectLock).unlock();
        assertFalse(cashDrawerDeviceLock.getIsLocked());
    }
}