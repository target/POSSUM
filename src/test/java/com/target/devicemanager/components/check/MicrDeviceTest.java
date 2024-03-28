package com.target.devicemanager.components.check;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.common.events.ConnectionEventListener;
import com.target.devicemanager.components.check.entities.MicrData;
import com.target.devicemanager.components.check.entities.MicrDataEvent;
import com.target.devicemanager.components.check.entities.MicrErrorEvent;
import com.target.devicemanager.components.check.entities.MicrException;
import jpos.JposConst;
import jpos.JposException;
import jpos.MICR;
import jpos.MICRConst;
import jpos.events.DataEvent;
import jpos.events.ErrorEvent;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MicrDeviceTest {

    private MicrDevice micrDevice;
    private MicrDevice micrDeviceListenerLock;
    private final List<ConnectionEventListener> testConnectionEventListenerList = new CopyOnWriteArrayList<>();
    private final List<MicrEventListener> testMicrEventListenerList = new CopyOnWriteArrayList<>();

    @Mock
    private MICR mockMicr;
    @Mock
    private DynamicDevice<MICR> mockDynamicMicr;
    @Mock
    private ConnectionEventListener mockConnectionEventListener;
    @Mock
    private List<ConnectionEventListener> mockConnectionEventListenerList;
    @Mock
    private MicrEventListener mockMicrEventListener;
    @Mock
    private List<MicrEventListener> mockMicrEventListenerList;
    @Mock
    private ReentrantLock mockConnectLock;

    @BeforeEach
    public void testInitialize() {
        when(mockDynamicMicr.getDevice()).thenReturn(mockMicr);

        micrDevice = new MicrDevice(mockDynamicMicr, mockConnectionEventListenerList, mockMicrEventListenerList);
        micrDeviceListenerLock = new MicrDevice(mockDynamicMicr, testConnectionEventListenerList, testMicrEventListenerList, mockConnectLock);

        //Default Mock Behavior
        when(mockDynamicMicr.isConnected()).thenReturn(true);
    }

    @Test
    public void ctor_WhenDynamicMicrAndConnectionAndEventAreNull_ThrowsException() {
        try {
            new MicrDevice(null, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicMICR cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicMicrAndConnectionAreNull_ThrowsException() {
        try {
            new MicrDevice(null, null, mockMicrEventListenerList);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicMICR cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicMicrAndEventAreNull_ThrowsException() {
        try {
            new MicrDevice(null, mockConnectionEventListenerList, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicMICR cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenConnectionAndEventAreNull_ThrowsException() {
        try {
            new MicrDevice(mockDynamicMicr, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("connectionEventListeners cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicMicrIsNull_ThrowsException() {
        try {
            new MicrDevice(null, mockConnectionEventListenerList, mockMicrEventListenerList);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicMICR cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenConnectionIsNull_ThrowsException() {
        try {
            new MicrDevice(mockDynamicMicr, null, mockMicrEventListenerList);
        } catch (IllegalArgumentException iae) {
            assertEquals("connectionEventListeners cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenEventIsNull_ThrowsException() {
        try {
            new MicrDevice(mockDynamicMicr, mockConnectionEventListenerList, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("micrEventListeners cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicMicrAndConnectionAndEventAreNotNull_DoesNotThrowException() {
        try {
            new MicrDevice(mockDynamicMicr, mockConnectionEventListenerList, mockMicrEventListenerList);
        } catch (Exception exception) {
            fail("Existing Device Arguments should not result in an Exception");
        }
        verify(mockDynamicMicr, times(3)).getDevice();
    }

    @Test
    public void addMicrEventListener_CallsThroughToAdd() {
        //arrange

        //act
        micrDevice.addMicrEventListener(mockMicrEventListener);

        //assert
        verify(mockMicrEventListenerList).add(mockMicrEventListener);
    }

    @Test
    public void addConnectionEventListener_CallsThroughToAdd() {
        //arrange

        //act
        micrDevice.addConnectionEventListener(mockConnectionEventListener);

        //assert
        verify(mockConnectionEventListenerList).add(mockConnectionEventListener);
    }

    @Test
    public void connect_WhenNotConnected_ReturnFalse() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        //act
        boolean result = micrDevice.connect();

        //assert
        verify(mockDynamicMicr).connect();
        verify(mockMicr, never()).getDeviceEnabled();
        assertFalse(result);
    }

    @Test
    public void connect_WhenConnected_ReturnTrue() {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = micrDeviceListenerLock.connect();

        //assert
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void connect_WhenGetDataEventEnabledFalse_SetDataEventEnabled() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockMicr.getDataEventEnabled()).thenReturn(false);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = micrDeviceListenerLock.connect();

        //assert
        verify(mockMicr).setDataEventEnabled(true);
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void connect_WhenGetDataEventEnabledTrue_DoesNotSetDataEventEnabled() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockMicr.getDataEventEnabled()).thenReturn(true);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = micrDeviceListenerLock.connect();

        //assert
        verify(mockMicr, never()).setDataEventEnabled(true);
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void connect_WhenGetDataEventEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMicr).getDataEventEnabled();
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = micrDeviceListenerLock.connect();

        //assert
        verify(mockMicr, never()).setDataEventEnabled(true);
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertFalse(result);
    }

    @Test
    public void connect_WhenSetDataEventEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMicr).setDataEventEnabled(true);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = micrDeviceListenerLock.connect();

        //assert
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertFalse(result);
    }

    @Test
    public void connect_WhenGetDeviceEnabledFalse_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockMicr.getDeviceEnabled()).thenReturn(false);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = micrDeviceListenerLock.connect();

        //assert
        verify(mockMicr).setDeviceEnabled(true);
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void connect_WhenGetDeviceEnabledTrue_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockMicr.getDeviceEnabled()).thenReturn(true);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = micrDeviceListenerLock.connect();

        //assert
        verify(mockMicr, never()).setDeviceEnabled(true);
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void connect_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMicr).getDeviceEnabled();
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = micrDeviceListenerLock.connect();

        //assert
        verify(mockMicr, never()).setDeviceEnabled(true);
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertFalse(result);
    }

    @Test
    public void connect_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMicr).setDeviceEnabled(true);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        boolean result = micrDeviceListenerLock.connect();

        //assert
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertFalse(result);
    }

    @Test
    public void connect_WhenAlreadyConnected_EnableDevice() throws JposException {
        //arrange
        when(mockDynamicMicr.connect()).thenReturn(DynamicDevice.ConnectionResult.ALREADY_CONNECTED);
        when(mockMicr.getDeviceEnabled()).thenReturn(false);

        //act
        boolean result = micrDevice.connect();

        //assert
        verify(mockMicr).setDeviceEnabled(true);
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertTrue(result);
    }

    @Test
    public void disconnect_WhenDeviceIsConnected_DisconnectsFromDeviceAndFireDisconnectEvent() throws JposException {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(true);
        when(mockMicr.getDataEventEnabled()).thenReturn(true);
        when(mockMicr.getDeviceEnabled()).thenReturn(true);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        micrDeviceListenerLock.disconnect();

        //assert
        verify(mockMicr).setDataEventEnabled(false);
        verify(mockMicr).setDeviceEnabled(false);
        verify(mockDynamicMicr).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
        assertFalse(connectionEvent.getValue().isConnected());
    }

    @Test
    public void disconnect_WhenDynamicMICRisNotConnected() throws JposException {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(false);

        //act
        micrDeviceListenerLock.disconnect();

        //assert
        verify(mockMicr, never()).getDataEventEnabled();
        verify(mockMicr, never()).setDataEventEnabled(false);
        verify(mockMicr, never()).getDeviceEnabled();
        verify(mockMicr, never()).setDeviceEnabled(false);
        verify(mockDynamicMicr, never()).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener,never()).connectionEventOccurred(connectionEvent.capture());
    }

    @Test
    public void disconnect_WhenGetDataEventEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMicr).getDataEventEnabled();
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        micrDeviceListenerLock.disconnect();

        //assert
        verify(mockMicr, never()).setDataEventEnabled(false);
        verify(mockMicr, never()).setDeviceEnabled(false);
        verify(mockDynamicMicr, never()).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
    }

    @Test
    public void disconnect_WhenGetDataEventEnabledReturnsFalse() throws JposException {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(true);
        when(mockMicr.getDataEventEnabled()).thenReturn(false);
        when(mockMicr.getDeviceEnabled()).thenReturn(true);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        micrDeviceListenerLock.disconnect();

        //assert
        verify(mockMicr, never()).setDataEventEnabled(false);
        verify(mockMicr).setDeviceEnabled(false);
        verify(mockDynamicMicr).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
    }

    @Test
    public void disconnect_WhenSetDataEventEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(true);
        when(mockMicr.getDataEventEnabled()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMicr).setDataEventEnabled(false);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        micrDeviceListenerLock.disconnect();

        //assert
        verify(mockMicr).setDataEventEnabled(false);
        verify(mockMicr, never()).setDeviceEnabled(false);
        verify(mockDynamicMicr, never()).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener,never()).connectionEventOccurred(connectionEvent.capture());
    }

    @Test
    public void disconnect_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(true);
        when(mockMicr.getDataEventEnabled()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMicr).getDeviceEnabled();
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        micrDeviceListenerLock.disconnect();

        //assert
        verify(mockMicr).setDataEventEnabled(false);
        verify(mockMicr, never()).setDeviceEnabled(false);
        verify(mockDynamicMicr, never()).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener,never()).connectionEventOccurred(connectionEvent.capture());
    }

    @Test
    public void disconnect_WhenGetDeviceEnabledReturnsFalse() throws JposException {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(true);
        when(mockMicr.getDataEventEnabled()).thenReturn(true);
        when(mockMicr.getDeviceEnabled()).thenReturn(false);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        micrDeviceListenerLock.disconnect();

        //assert
        verify(mockMicr).setDataEventEnabled(false);
        verify(mockMicr, never()).setDeviceEnabled(false);
        verify(mockDynamicMicr).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener).connectionEventOccurred(connectionEvent.capture());
    }

    @Test
    public void disconnect_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(true);
        when(mockMicr.getDataEventEnabled()).thenReturn(true);
        when(mockMicr.getDeviceEnabled()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMicr).setDeviceEnabled(false);
        micrDeviceListenerLock.addConnectionEventListener(mockConnectionEventListener);

        //act
        micrDeviceListenerLock.disconnect();

        //assert
        verify(mockMicr).setDataEventEnabled(false);
        verify(mockMicr).setDeviceEnabled(false);
        verify(mockDynamicMicr, never()).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener,never()).connectionEventOccurred(connectionEvent.capture());
    }

    @Test
    public void isConnected_ReturnsTrueFromDynamicDevice() {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(true);

        //act
        boolean actual = micrDevice.isConnected();

        //assert
        assertTrue(actual);
    }

    @Test
    public void isConnected_ReturnsFalseFromDynamicDevice() {
        //arrange
        when(mockDynamicMicr.isConnected()).thenReturn(false);

        //act
        boolean actual = micrDevice.isConnected();

        //assert
        assertFalse(actual);
    }

    @Test
    public void insertCheck_Exception_FireErrorEvent() throws JposException {
        //arrange
        micrDeviceListenerLock.addMicrEventListener(mockMicrEventListener);
        doThrow(new JposException(MICRConst.JPOS_EMICR_COVEROPEN)).when(mockMicr).beginInsertion(anyInt());

        //act
        try {
            micrDeviceListenerLock.insertCheck();
        }

        //assert
        catch (DeviceException deviceException) {
            ArgumentCaptor<MicrErrorEvent> micrErrorEvent = ArgumentCaptor.forClass(MicrErrorEvent.class);
            verify(mockMicrEventListener).micrErrorEventOccurred(micrErrorEvent.capture());
            assertEquals(MICRConst.JPOS_EMICR_COVEROPEN, micrErrorEvent.getValue().getError().getErrorCode());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void insertCheck_OnCancel_FireTimeoutErrorEvent() throws MicrException {
        //arrange
        micrDeviceListenerLock.addMicrEventListener(mockMicrEventListener);
        micrDeviceListenerLock.setCheckCancelReceived(true);

        //act
        micrDeviceListenerLock.insertCheck();

        //assert
        ArgumentCaptor<MicrErrorEvent> micrErrorEvent = ArgumentCaptor.forClass(MicrErrorEvent.class);
        verify(mockMicrEventListener).micrErrorEventOccurred(micrErrorEvent.capture());
        assertEquals(JposConst.JPOS_E_TIMEOUT, micrErrorEvent.getValue().getError().getErrorCode());
    }

    @Test
    public void insertCheck_BeginEndInsertion_UntilDone() throws JposException, MicrException{
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_TIMEOUT)).doNothing().when(mockMicr).beginInsertion(250);

        //act
        micrDevice.insertCheck();

        //assert
        verify(mockMicr).endInsertion();
    }

    @Test
    public void insertCheck_InsertionExceptionAndWithdrawException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_FAILURE)).when(mockMicr).endInsertion();
        doThrow(new JposException(MICRConst.JPOS_EMICR_COVEROPEN)).when(mockMicr).endRemoval();

        //act
        try {
            micrDevice.insertCheck();
        } catch (MicrException micrException) {
            assertEquals(micrException.getDeviceError(), DeviceError.UNEXPECTED_ERROR);
            return;
        }

        //assert
        fail("Expected exception, but got none");
    }

    @Test
    public void withdrawCheck_BeginEndRemoval() throws JposException{
        //arrange
        when(mockDynamicMicr.getDevice()).thenReturn(mockMicr);

        //act
        micrDevice.withdrawCheck();

        //assert
        verify(mockMicr).beginRemoval(0);
        verify(mockMicr).endRemoval();
    }

    @Test
    public void withdrawCheck_ThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockMicr).beginRemoval(anyInt());
        when(mockDynamicMicr.getDevice()).thenReturn(mockMicr);

        //act
        try {
            micrDevice.withdrawCheck();
        }
        //assert
        catch (JposException jposException) {
            assertEquals(JposConst.JPOS_E_EXTENDED, jposException.getErrorCode());
        }
    }

    @Test
    public void isCheckCancelReceived_setCheckCancelReceived_ReturnTrue() {
        //arrange

        //act
        micrDevice.setCheckCancelReceived(true);

        //assert
        assertTrue(micrDevice.isCheckCancelReceived());
    }

    @Test
    public void isCheckCancelReceived_setCheckCancelReceived_ReturnFalse() {
        //arrange

        //act
        micrDevice.setCheckCancelReceived(false);

        //assert
        assertFalse(micrDevice.isCheckCancelReceived());
    }

    @Test
    public void statusUpdateOccurred_WhenMicrPowerOff_Disconnect() {
        //arrange
        MicrDevice spyMicrDevice = spy(micrDevice);
        StatusUpdateEvent mockStatusUpdateEvent = mock(StatusUpdateEvent.class);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF);

        //act
        spyMicrDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(spyMicrDevice).disconnect();
    }

    @Test
    public void statusUpdateOccurred_WhenMicrPowerOffOffline_Disconnect() {
        //arrange
        MicrDevice spyMicrDevice = spy(micrDevice);
        StatusUpdateEvent mockStatusUpdateEvent = mock(StatusUpdateEvent.class);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF_OFFLINE);

        //act
        spyMicrDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(spyMicrDevice).disconnect();
    }

    @Test
    public void statusUpdateOccurred_WhenMicrPowerOffline_Disconnect() {
        //arrange
        MicrDevice spyMicrDevice = spy(micrDevice);
        StatusUpdateEvent mockStatusUpdateEvent = mock(StatusUpdateEvent.class);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFFLINE);

        //act
        spyMicrDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(spyMicrDevice).disconnect();
    }

    @Test
    public void statusUpdateOccurred_WhenMicrTurnsOn_Connect() {
        //arrange
        MicrDevice spyMicrDevice = spy(micrDevice);
        StatusUpdateEvent mockStatusUpdateEvent = mock(StatusUpdateEvent.class);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_ONLINE);

        //act
        spyMicrDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(spyMicrDevice).connect();
    }

    @Test
    public void statusUpdateOccurred_UnknownStatus_DoesNothing() {
        //arrange
        StatusUpdateEvent mockStatusUpdateEvent = mock(StatusUpdateEvent.class);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(572);

        //act
        micrDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(mockDynamicMicr, never()).connect();
        verify(mockDynamicMicr, never()).disconnect();

    }

    @Test
    public void errorOccurred_FireJamDataEvent() {
        //arrange
        micrDeviceListenerLock.addMicrEventListener(mockMicrEventListener);
        JposException expected = new JposException(JposConst.JPOS_E_EXTENDED, MICRConst.JPOS_EMICR_JAM);
        ErrorEvent errorEvent = new ErrorEvent(this, expected.getErrorCode(), expected.getErrorCodeExtended(), 0, 0);

        //act
        micrDeviceListenerLock.errorOccurred(errorEvent);

        //assert
        ArgumentCaptor<MicrErrorEvent> micrErrorEvent = ArgumentCaptor.forClass(MicrErrorEvent.class);
        verify(mockMicrEventListener).micrErrorEventOccurred(micrErrorEvent.capture());
        JposException actual = micrErrorEvent.getValue().getError();
        assertEquals(expected.getErrorCode(), actual.getErrorCode());
        assertEquals(expected.getErrorCodeExtended(), actual.getErrorCodeExtended());
    }

    @Test
    public void dataOccurred_FireDataEvent() throws JposException {
        //arrange
        micrDeviceListenerLock.addMicrEventListener(mockMicrEventListener);
        MicrData expected = new MicrData("123", "456", "789", "123456789", "0100");
        when(mockMicr.getAccountNumber()).thenReturn(expected.account_number);
        when(mockMicr.getBankNumber()).thenReturn(expected.bank_number);
        when(mockMicr.getTransitNumber()).thenReturn(expected.transit_number);
        when(mockMicr.getRawData()).thenReturn(expected.raw_data);
        DataEvent dataEvent = new DataEvent(mockMicr, JposConst.JPOS_SUE_POWER_ONLINE);

        //act
        micrDeviceListenerLock.dataOccurred(dataEvent);

        //assert
        ArgumentCaptor<MicrDataEvent> micrDataEvent = ArgumentCaptor.forClass(MicrDataEvent.class);
        verify(mockMicrEventListener).micrDataEventOccurred(micrDataEvent.capture());
        MicrData actual = micrDataEvent.getValue().getMicrData();
        assertEquals(expected.account_number, actual.account_number);
        assertEquals(expected.bank_number, actual.bank_number);
        assertEquals(expected.transit_number, actual.transit_number);
        assertEquals(expected.raw_data, actual.raw_data);
    }

    @Test
    public void dataOccurred_WhenExceptionOccurs_FireErrorEvent() throws JposException {
        //arrange
        micrDeviceListenerLock.addMicrEventListener(mockMicrEventListener);
        JposException expected = new JposException(JposConst.JPOS_E_OFFLINE);
        when(mockMicr.getAccountNumber()).thenThrow(new JposException(JposConst.JPOS_E_OFFLINE));
        DataEvent dataEvent = new DataEvent(mockMicr, JposConst.JPOS_SUE_POWER_ONLINE);

        //act
        micrDeviceListenerLock.dataOccurred(dataEvent);

        //assert
        ArgumentCaptor<MicrErrorEvent> micrErrorEvent = ArgumentCaptor.forClass(MicrErrorEvent.class);
        verify(mockMicrEventListener).micrErrorEventOccurred(micrErrorEvent.capture());
        JposException actual = micrErrorEvent.getValue().getError();
        assertEquals(expected.getErrorCode(), actual.getErrorCode());
    }

    @Test
    public void tryLock_WhenLock_ReturnsTrue() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);

        //act
        micrDeviceListenerLock.tryLock();

        //assert
        assertTrue(micrDeviceListenerLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenNotLock_ReturnsFalse() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(false);

        //act
        micrDeviceListenerLock.tryLock();

        //assert
        assertFalse(micrDeviceListenerLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenLockThrowsException_ReturnsFalse() throws InterruptedException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(10, TimeUnit.SECONDS);

        //act
        micrDeviceListenerLock.tryLock();

        //assert
        assertFalse(micrDeviceListenerLock.getIsLocked());
    }

    @Test
    public void unlock_CallsThrough() {
        //arrange

        //act
        micrDeviceListenerLock.unlock();

        //assert
        verify(mockConnectLock).unlock();
        assertFalse(micrDeviceListenerLock.getIsLocked());
    }

    @Test
    public void getDeviceName_ReturnsName() {
        //arrange
        String expectedDeviceName = "micr";
        when(mockDynamicMicr.getDeviceName()).thenReturn(expectedDeviceName);

        //act
        String actualDeviceName = micrDevice.getDeviceName();

        //assert
        assertEquals(expectedDeviceName, actualDeviceName);
    }
}