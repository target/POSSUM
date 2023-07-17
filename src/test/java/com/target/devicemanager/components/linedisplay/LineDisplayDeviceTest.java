package com.target.devicemanager.components.linedisplay;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.common.events.ConnectionEventListener;
import jpos.JposConst;
import jpos.JposException;
import jpos.LineDisplay;
import jpos.LineDisplayConst;
import jpos.events.StatusUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LineDisplayDeviceTest {

    private LineDisplayDevice lineDisplayDevice;
    private LineDisplayDevice lineDisplayDeviceConnectionEventListLock;

    @Mock
    private DynamicDevice<LineDisplay> mockDynamicLineDisplay;
    @Mock
    private LineDisplay mockLineDisplay;
    @Mock
    private StatusUpdateEvent mockStatusUpdateEvent;
    @Mock
    private ConnectionEventListener mockConnectionEventListener;
    @Mock
    private List<ConnectionEventListener> mockConnectionEventListenerList;
    @Mock
    private ReentrantLock mockConnectLock;

    @BeforeEach
    public void testInitialize() {
        when(mockDynamicLineDisplay.getDevice()).thenReturn(mockLineDisplay);

        lineDisplayDevice = new LineDisplayDevice(mockDynamicLineDisplay);
        lineDisplayDeviceConnectionEventListLock = new LineDisplayDevice(mockDynamicLineDisplay, mockConnectionEventListenerList, mockConnectLock);

        //Default Mock Behavior
        when(mockDynamicLineDisplay.getDevice()).thenReturn(mockLineDisplay);
    }

    @Test
    public void ctor_WhenLineDisplayIsNull_ThrowsException() {
        try {
            new LineDisplayDevice(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicLineDisplay cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenLineDisplayIsNotNull_DoesNotThrowException() {
        try {
            new LineDisplayDevice(mockDynamicLineDisplay);
        } catch(Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
        verify(mockDynamicLineDisplay, times(3)).getDevice();
    }

    @Test
    public void addConnectionEventListener_CallsThroughToAdd() {
        //arrange

        //act
        lineDisplayDeviceConnectionEventListLock.addConnectionEventListener(mockConnectionEventListener);

        //assert
        verify(mockConnectionEventListenerList).add(mockConnectionEventListener);
    }

    @Test
    public void connect_WhenNotConnected_ReturnFalse() throws JposException {
        //arrange
        when(mockDynamicLineDisplay.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        //act
        boolean result = lineDisplayDevice.connect();

        //assert
        verify(mockDynamicLineDisplay).connect();
        verify(mockLineDisplay, never()).getDeviceEnabled();
        assertFalse(result);
    }

    @Test
    public void connect_WhenConnected_ReturnTrue() throws JposException {
        //arrange
        when(mockDynamicLineDisplay.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        lineDisplayDevice.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = lineDisplayDevice.connect();

        //assert
        verify(mockLineDisplay).clearText();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void connect_WhenGetDeviceEnabledFalse_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicLineDisplay.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockLineDisplay.getDeviceEnabled()).thenReturn(false);
        lineDisplayDevice.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = lineDisplayDevice.connect();

        //assert
        verify(mockLineDisplay).setDeviceEnabled(true);
        verify(mockLineDisplay).clearText();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void connect_WhenGetDeviceEnabledTrue_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicLineDisplay.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockLineDisplay.getDeviceEnabled()).thenReturn(true);
        lineDisplayDevice.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = lineDisplayDevice.connect();

        //assert
        verify(mockLineDisplay, never()).setDeviceEnabled(true);
        verify(mockLineDisplay).clearText();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void connect_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicLineDisplay.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockLineDisplay).getDeviceEnabled();
        lineDisplayDevice.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = lineDisplayDevice.connect();

        //assert
        verify(mockLineDisplay, never()).setDeviceEnabled(true);
        verify(mockLineDisplay, never()).clearText();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertFalse(result);
    }

    @Test
    public void connect_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicLineDisplay.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockLineDisplay).setDeviceEnabled(true);
        lineDisplayDevice.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = lineDisplayDevice.connect();

        //assert
        verify(mockLineDisplay, never()).clearText();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertFalse(result);
    }

    @Test
    public void connect_WhenAlreadyConnected_EnableDevice() throws JposException {
        //arrange
        when(mockDynamicLineDisplay.connect()).thenReturn(DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
        when(mockLineDisplay.getDeviceEnabled()).thenReturn(false);

        //act
        boolean result = lineDisplayDevice.connect();

        //assert
        verify(mockLineDisplay).setDeviceEnabled(true);
        verify(mockLineDisplay, never()).clearText();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void disconnect_WhenDeviceIsConnected_DisconnectsFromDeviceAndFireDisconnectEvent() {
        //arrange
        when(mockDynamicLineDisplay.isConnected()).thenReturn(true);
        lineDisplayDevice.addConnectionEventListener(mockConnectionEventListener);

        //act
        lineDisplayDevice.disconnect();

        //assert
        verify(mockDynamicLineDisplay).isConnected();
        verify(mockDynamicLineDisplay).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertFalse(connectionEvent.getValue().isConnected());
    }

    @Test
    public void disconnect_WhenDeviceIsNotConnected_DoesNotDisconnectFromDevice() {
        //arrange
        when(mockDynamicLineDisplay.isConnected()).thenReturn(false);

        //act
        lineDisplayDevice.disconnect();

        //assert
        verify(mockDynamicLineDisplay).isConnected();
        verify(mockDynamicLineDisplay, never()).disconnect();
    }

    @Test
    public void isConnected_ReturnsTrueFromDynamicDevice() {
        //arrange
        when(mockDynamicLineDisplay.isConnected()).thenReturn(true);

        //act
        boolean actual = lineDisplayDevice.isConnected();

        //assert
        assertTrue(actual);
    }

    @Test
    public void isConnected_ReturnsFalseFromDynamicDevice() {
        //arrange
        when(mockDynamicLineDisplay.isConnected()).thenReturn(false);

        //act
        boolean actual = lineDisplayDevice.isConnected();

        //assert
        assertFalse(actual);
    }

    @Test
    public void displayLine_WhenReady_DisplaysOnLineDisplay() throws JposException {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_ONLINE);

        lineDisplayDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //act
        lineDisplayDevice.displayLine("Test Data Line 1", "Test Data Line 2");

        //assert
        verify(mockLineDisplay).displayTextAt(0, 0, "Test Data Line 1", LineDisplayConst.DISP_DT_NORMAL);
        verify(mockLineDisplay).displayTextAt(1, 0, "Test Data Line 2", LineDisplayConst.DISP_DT_NORMAL);
    }

    @Test
    public void displayLine_WhenDisplayThrowsException_ConnectedFalse() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockLineDisplay).displayTextAt(anyInt(),anyInt(),any(),anyInt());

        //act
        try {
            lineDisplayDevice.displayLine("Test Data Line 1", "Test Data Line 2");
        }

        //assert
        catch (JposException jposException) {
            assertEquals(JposConst.JPOS_E_EXTENDED, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void displayLine_WhenDisplayThrowsException_ConnectedTrue() throws JposException {
        //arrange
        when(mockDynamicLineDisplay.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockLineDisplay).displayTextAt(anyInt(),anyInt(),any(),anyInt());

        //act
        try {
            lineDisplayDevice.displayLine("Test Data Line 1", "Test Data Line 2");
        }

        //assert
        catch (JposException jposException) {
            assertEquals(JposConst.JPOS_E_EXTENDED, jposException.getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getDeviceName_ReturnsName() {
        //arrange
        String expectedDeviceName = "lineDisplay";
        when(mockDynamicLineDisplay.getDeviceName()).thenReturn(expectedDeviceName);

        //act
        String actualDeviceName = lineDisplayDevice.getDeviceName();

        //assert
        assertEquals(expectedDeviceName, actualDeviceName);
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOff_DisconnectAndFiresConnectionEvent() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF );
        LineDisplayDevice lineDisplaySpy = spy(lineDisplayDevice);

        //act
        lineDisplaySpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(lineDisplaySpy).disconnect();
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffOffline_DisconnectAndFiresConnectionEvent() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF_OFFLINE );
        LineDisplayDevice lineDisplaySpy = spy(lineDisplayDevice);

        //act
        lineDisplaySpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(lineDisplaySpy).disconnect();
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffline_DisconnectAndFiresConnectionEvent() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFFLINE );
        LineDisplayDevice lineDisplaySpy = spy(lineDisplayDevice);

        //act
        lineDisplaySpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(lineDisplaySpy).disconnect();
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOnline_Connects() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_ONLINE);
        LineDisplayDevice lineDisplaySpy = spy(lineDisplayDevice);

        //act
        lineDisplaySpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(lineDisplaySpy).connect();
    }

    @Test
    public void statusUpdateOccurred_WhenOtherStatus_DoNothing() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(572);
        LineDisplayDevice lineDisplaySpy = spy(lineDisplayDevice);

        //act
        lineDisplaySpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(lineDisplaySpy, never()).connect();
        verify(lineDisplaySpy, never()).disconnect();
    }

    @Test
    public void tryLock_WhenLock_ReturnsTrue() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);

        //act
        lineDisplayDeviceConnectionEventListLock.tryLock();

        //assert
        assertTrue(lineDisplayDeviceConnectionEventListLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenNotLock_ReturnsFalse() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(false);

        //act
        lineDisplayDeviceConnectionEventListLock.tryLock();

        //assert
        assertFalse(lineDisplayDeviceConnectionEventListLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenLockThrowsException_ReturnsFalse() throws InterruptedException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(10, TimeUnit.SECONDS);

        //act
        lineDisplayDeviceConnectionEventListLock.tryLock();

        //assert
        assertFalse(lineDisplayDeviceConnectionEventListLock.getIsLocked());
    }

    @Test
    public void unlock_CallsThrough() {
        //arrange

        //act
        lineDisplayDeviceConnectionEventListLock.unlock();

        //assert
        verify(mockConnectLock).unlock();
        assertFalse(lineDisplayDeviceConnectionEventListLock.getIsLocked());
    }

}