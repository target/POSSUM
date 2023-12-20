package com.target.devicemanager.components.scale;

import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.common.events.ConnectionEvent;
import com.target.devicemanager.common.events.ConnectionEventListener;
import com.target.devicemanager.components.scale.entities.FormattedWeight;
import com.target.devicemanager.components.scale.entities.WeightEvent;
import jpos.JposConst;
import jpos.JposException;
import jpos.Scale;
import jpos.ScaleConst;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScaleDeviceTest {

    private ScaleDevice scaleDevice;
    private ScaleDevice scaleDeviceListLock;
    private List<ScaleEventListener> scaleEventListenerList;
    private List<ConnectionEventListener> connectionEventListenerList;

    @Mock
    private DynamicDevice<Scale> mockDynamicScale;
    @Mock
    private Scale mockScale;
    @Mock
    private List<ScaleEventListener> mockScaleEventListenerList;
    @Mock
    private List<ConnectionEventListener> mockConnectionEventListenerList;
    @Mock
    private ScaleEventListener mockScaleEventListener;
    @Mock
    private ConnectionEventListener mockConnectionEventListener;
    @Mock
    private ReentrantLock mockConnectLock;
    @Mock
    private StatusUpdateEvent mockStatusUpdateEvent;

    @BeforeEach
    public void testInitialize() {
        when(mockDynamicScale.getDevice()).thenReturn(mockScale);

        scaleEventListenerList = new ArrayList<>();
        scaleEventListenerList.add(mockScaleEventListener);
        connectionEventListenerList = new ArrayList<>();
        connectionEventListenerList.add(mockConnectionEventListener);

        scaleDevice = new ScaleDevice(mockDynamicScale, mockScaleEventListenerList, mockConnectionEventListenerList);
        scaleDeviceListLock = new ScaleDevice(mockDynamicScale, scaleEventListenerList, connectionEventListenerList, mockConnectLock);
    }

    @Test
    public void ctor_WhenDynamicScaleAndScaleEventListenersAndConnectionEventListenersAreNull_ThrowsException() {
        try {
            new ScaleDevice(null, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicScale cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicScaleAndScaleEventListenersAreNull_ThrowsException() {
        try {
            new ScaleDevice(null, null, mockConnectionEventListenerList);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicScale cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicScaleAndConnectionEventListenersAreNull_ThrowsException() {
        try {
            new ScaleDevice(null, mockScaleEventListenerList, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicScale cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScaleEventListenersAndConnectionEventListenersAreNull_ThrowsException() {
        try {
            new ScaleDevice(mockDynamicScale, null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scaleEventListeners cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicScaleIsNull_ThrowsException() {
        try {
            new ScaleDevice(null, mockScaleEventListenerList, mockConnectionEventListenerList);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicScale cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScaleEventListenersIsNull_ThrowsException() {
        try {
            new ScaleDevice(mockDynamicScale, null, mockConnectionEventListenerList);
        } catch (IllegalArgumentException iae) {
            assertEquals("scaleEventListeners cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenConnectionEventListenersIsNull_ThrowsException() {
        try {
            new ScaleDevice(mockDynamicScale, mockScaleEventListenerList, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("connectionEventListeners cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicScaleAndScaleEventListenersAndConnectionEventListenersAreNotNull_DoesNotThrowException() {
        try {
            new ScaleDevice(mockDynamicScale, mockScaleEventListenerList, mockConnectionEventListenerList);
        } catch (Exception exception) {
            fail("Existing Device Arguments should not result in an Exception");
        }
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockScale, times(3)).addStatusUpdateListener(any());
    }

    @Test
    public void connect_WhenNotConnected_DoesNotConnect() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);
        boolean expected = false;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale).clearInput();
        verify(mockScale, never()).getStatusNotify();
        verify(mockScale, never()).setStatusNotify(anyInt());
        verify(mockScale, never()).getDataEventEnabled();
        verify(mockScale, never()).setDataEventEnabled(anyBoolean());
        verify(mockScale, never()).getDeviceEnabled();
        verify(mockScale, never()).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertEquals(expected, actual);
        assertFalse(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenScaleStatusNotify_IsEnabled() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockScale.getStatusNotify()).thenReturn(ScaleConst.SCAL_SN_ENABLED);
        boolean expected = true;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale, never()).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale).getDataEventEnabled();
        verify(mockScale).setDataEventEnabled(anyBoolean());
        verify(mockScale).getDeviceEnabled();
        verify(mockScale).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, times(2)).connectionEventOccurred(connectionEvent.capture());
        assertTrue(connectionEvent.getValue().isConnected());
        assertEquals(expected, actual);
        assertTrue(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenGetDataEventEnabled_IsTrue() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockScale.getDataEventEnabled()).thenReturn(true);
        boolean expected = true;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale).getDataEventEnabled();
        verify(mockScale, never()).setDataEventEnabled(anyBoolean());
        verify(mockScale).getDeviceEnabled();
        verify(mockScale).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, times(2)).connectionEventOccurred(connectionEvent.capture());
        assertTrue(connectionEvent.getValue().isConnected());
        assertEquals(expected, actual);
        assertTrue(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabled_IsTrue() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockScale.getDeviceEnabled()).thenReturn(true);
        boolean expected = true;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale).getDataEventEnabled();
        verify(mockScale).setDataEventEnabled(anyBoolean());
        verify(mockScale).getDeviceEnabled();
        verify(mockScale, never()).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, times(2)).connectionEventOccurred(connectionEvent.capture());
        assertTrue(connectionEvent.getValue().isConnected());
        assertEquals(expected, actual);
        assertTrue(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenConnectionResult_IsAlreadyConnected() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockScale.getDataEventEnabled()).thenReturn(true);
        boolean expected = true;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale).getDataEventEnabled();
        verify(mockScale, never()).setDataEventEnabled(anyBoolean());
        verify(mockScale).getDeviceEnabled();
        verify(mockScale).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, times(2)).connectionEventOccurred(connectionEvent.capture());
        assertEquals(expected, actual);
        assertTrue(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenClearInput_ThrowsException() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockScale).clearInput();
        boolean expected = false;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale).clearInput();
        verify(mockScale, never()).getStatusNotify();
        verify(mockScale, never()).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale, never()).getDataEventEnabled();
        verify(mockScale, never()).setDataEventEnabled(anyBoolean());
        verify(mockScale, never()).getDeviceEnabled();
        verify(mockScale, never()).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertEquals(expected, actual);
        assertFalse(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenGetStatusNotify_ThrowsException() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockScale).getStatusNotify();
        boolean expected = false;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale, never()).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale, never()).getDataEventEnabled();
        verify(mockScale, never()).setDataEventEnabled(anyBoolean());
        verify(mockScale, never()).getDeviceEnabled();
        verify(mockScale, never()).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertEquals(expected, actual);
        assertFalse(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenSetStatusNotify_ThrowsException() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockScale).setStatusNotify(anyInt());
        boolean expected = false;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale, never()).getDataEventEnabled();
        verify(mockScale, never()).setDataEventEnabled(anyBoolean());
        verify(mockScale, never()).getDeviceEnabled();
        verify(mockScale, never()).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertEquals(expected, actual);
        assertFalse(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenGetDataEventEnabled_ThrowsException() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockScale).getDataEventEnabled();
        boolean expected = false;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale).getDataEventEnabled();
        verify(mockScale, never()).setDataEventEnabled(anyBoolean());
        verify(mockScale, never()).getDeviceEnabled();
        verify(mockScale, never()).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertEquals(expected, actual);
        assertFalse(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenSetDataEventEnabled_ThrowsException() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockScale).setDataEventEnabled(anyBoolean());
        boolean expected = false;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale).getDataEventEnabled();
        verify(mockScale).setDataEventEnabled(anyBoolean());
        verify(mockScale, never()).getDeviceEnabled();
        verify(mockScale, never()).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertEquals(expected, actual);
        assertFalse(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabled_ThrowsException() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockScale).getDeviceEnabled();
        boolean expected = false;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale).getDataEventEnabled();
        verify(mockScale).setDataEventEnabled(anyBoolean());
        verify(mockScale).getDeviceEnabled();
        verify(mockScale, never()).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertEquals(expected, actual);
        assertFalse(scaleDeviceListLock.isConnected());
    }

    @Test
    public void connect_WhenSetDeviceEnabled_ThrowsException() throws JposException {
        //arrange
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        when(mockDynamicScale.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_OFFLINE)).when(mockScale).setDeviceEnabled(anyBoolean());
        boolean expected = false;

        //act
        boolean actual = scaleDeviceListLock.connect();

        //assume
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockDynamicScale).connect();
        verify(mockScale, never()).clearInput();
        verify(mockScale).getStatusNotify();
        verify(mockScale).setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        verify(mockScale).getDataEventEnabled();
        verify(mockScale).setDataEventEnabled(anyBoolean());
        verify(mockScale).getDeviceEnabled();
        verify(mockScale).setDeviceEnabled(anyBoolean());
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, never()).connectionEventOccurred(connectionEvent.capture());
        assertEquals(expected, actual);
        assertFalse(scaleDeviceListLock.isConnected());
    }

    @Test
    public void disconnect_WhenDeviceIsConnected_DisconnectsFromDeviceAndFireDisconnectEvent() {
        //arrange
        scaleDeviceListLock.setDeviceConnected(true);
        scaleDeviceListLock.addConnectionEventListener(mockConnectionEventListener);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);

        //act
        scaleDeviceListLock.disconnect();

        //assert
        verify(mockDynamicScale, times(2)).getDevice();
        verify(mockDynamicScale).disconnect();
        ArgumentCaptor<ConnectionEvent> connectionEvent = forClass(ConnectionEvent.class);
        verify(mockConnectionEventListener, times(2)).connectionEventOccurred(connectionEvent.capture());
        assertFalse(connectionEvent.getValue().isConnected());
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleLiveWeightEventOccurred(weightEvent.capture());
        assertFalse(scaleDeviceListLock.isConnected());
    }

    @Test
    public void startStableWeightRead_NotInProgress() throws JposException {
        //arrange
        scaleDevice.setStableWeightInProgress(true);
        //act
        scaleDevice.startStableWeightRead(30);

        //assert
        verify(mockDynamicScale, times(2)).getDevice();
        verify(mockScale, never()).readWeight(any(), anyInt());
    }

    @Test
    public void startStableWeightRead_InProgress() throws JposException {
        //arrange
        scaleDeviceListLock.setStableWeightInProgress(false);
        scaleDeviceListLock.setReadyForStableWeight(true);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);
        int[] weight = new int[] {3000, 0};
        doAnswer(invocation -> {
            scaleDeviceListLock.setWeight(weight);
            return null;
        }).when(mockScale).readWeight(any(), anyInt());

        //act
        scaleDeviceListLock.startStableWeightRead(3000);

        //assert
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockScale).readWeight(any(), anyInt());
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleStableWeightDataEventOccurred(weightEvent.capture());
        assertEquals(weightEvent.getValue().getWeight().weight, "3.00");
    }

    @Test
    public void startStableWeightRead_ThrowsException() throws JposException {
        //arrange
        scaleDeviceListLock.setStableWeightInProgress(false);
        scaleDeviceListLock.setReadyForStableWeight(true);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);
        JposException jposException = new JposException(JposConst.JPOS_E_OFFLINE);
        doThrow(jposException).when(mockScale).readWeight(any(), anyInt());
        scaleDeviceListLock.setDeviceConnected(true);

        //act
        scaleDeviceListLock.startStableWeightRead(3000);

        //assert
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockScale).readWeight(any(), anyInt());
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleWeightErrorEventOccurred(any());
    }

    @Test
    public void startStableWeightRead_TimesOut() throws JposException {
        //arrange
        scaleDeviceListLock.setStableWeightInProgress(false);
        scaleDeviceListLock.setReadyForStableWeight(true);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);
        JposException jposException = new JposException(JposConst.JPOS_E_TIMEOUT);
        doThrow(jposException).when(mockScale).readWeight(any(), anyInt());

        //act
        scaleDeviceListLock.startStableWeightRead(0);

        //assert
        verify(mockDynamicScale, times(3)).getDevice();
        verify(mockScale, atLeast(1)).readWeight(any(), anyInt());
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleWeightErrorEventOccurred(any());
    }

    @Test
    public void statusUpdateOccurred_PowerOff() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF);

        //act
        scaleDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(scaleDevice.isConnected());
    }

    @Test
    public void statusUpdateOccurred_PowerOffOffline() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF_OFFLINE);

        //act
        scaleDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(scaleDevice.isConnected());
    }

    @Test
    public void statusUpdateOccurred_PowerOffline() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFFLINE);

        //act
        scaleDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(scaleDevice.isConnected());
    }

    @Test
    public void statusUpdateOccurred_PowerOnline() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_ONLINE);

        //act
        scaleDevice.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(scaleDevice.isConnected());
    }

    @Test
    public void statusUpdateOccurred_StableWeight() throws JposException {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(ScaleConst.SCAL_SUE_STABLE_WEIGHT);
        when(mockScale.getScaleLiveWeight()).thenReturn(3000);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);

        //act
        scaleDeviceListLock.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(mockDynamicScale, times(3)).getDevice();
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleLiveWeightEventOccurred(weightEvent.capture());
        assertEquals(weightEvent.getValue().getWeight().weight, "3.00");
    }

    @Test
    public void statusUpdateOccurred_StableWeightThrowsError() throws JposException {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(ScaleConst.SCAL_SUE_STABLE_WEIGHT);
        when(mockScale.getScaleLiveWeight()).thenThrow(new JposException(JposConst.JPOS_E_TIMEOUT));
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);

        //act
        scaleDeviceListLock.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        verify(mockDynamicScale, times(3)).getDevice();
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleLiveWeightEventOccurred(weightEvent.capture());
        assertEquals(weightEvent.getValue().getWeight().weight, "-.--");
    }

    @Test
    public void statusUpdateOccurred_WeightZero() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(ScaleConst.SCAL_SUE_WEIGHT_ZERO);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);

        //act
        scaleDeviceListLock.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleLiveWeightEventOccurred(weightEvent.capture());
        assertEquals(weightEvent.getValue().getWeight().weight, "0.00");
    }

    @Test
    public void statusUpdateOccurred_NotReady() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(ScaleConst.SCAL_SUE_NOT_READY);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);

        //act
        scaleDeviceListLock.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleLiveWeightEventOccurred(weightEvent.capture());
        assertEquals(weightEvent.getValue().getWeight().weight, "-.--");
    }

    @Test
    public void statusUpdateOccurred_Overweight() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(ScaleConst.SCAL_SUE_WEIGHT_OVERWEIGHT);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);

        //act
        scaleDeviceListLock.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleLiveWeightEventOccurred(weightEvent.capture());
        assertEquals(weightEvent.getValue().getWeight().weight, "-.--");
    }

    @Test
    public void statusUpdateOccurred_UnderZero() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(ScaleConst.SCAL_SUE_WEIGHT_UNDER_ZERO);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);

        //act
        scaleDeviceListLock.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleLiveWeightEventOccurred(weightEvent.capture());
        assertEquals(weightEvent.getValue().getWeight().weight, "-.--");
    }

    @Test
    public void statusUpdateOccurred_Unstable() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(ScaleConst.SCAL_SUE_WEIGHT_UNSTABLE);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);

        //act
        scaleDeviceListLock.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleLiveWeightEventOccurred(weightEvent.capture());
        assertEquals(weightEvent.getValue().getWeight().weight, "-.--");
    }

    @Test
    public void statusUpdateOccurred_OtherStatus() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(333);
        scaleDeviceListLock.addScaleEventListener(mockScaleEventListener);

        //act
        scaleDeviceListLock.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        ArgumentCaptor<WeightEvent> weightEvent = forClass(WeightEvent.class);
        verify(mockScaleEventListener, times(2)).scaleLiveWeightEventOccurred(weightEvent.capture());
        assertEquals(weightEvent.getValue().getWeight().weight, "-.--");
    }

    @Test
    public void errorOccurred_Offline() {
        //arrange
        JposException exception = new JposException(JposConst.JPOS_E_OFFLINE);
        ErrorEvent errorEvent = new ErrorEvent(this, exception.getErrorCode(), exception.getErrorCodeExtended(), 0, 0);

        //act
        scaleDevice.errorOccurred(errorEvent);

        //assert
        verify(mockDynamicScale).disconnect();
    }

    @Test
    public void errorOccurred_NoHardware() {
        //arrange
        JposException exception = new JposException(JposConst.JPOS_E_NOHARDWARE);
        ErrorEvent errorEvent = new ErrorEvent(this, exception.getErrorCode(), exception.getErrorCodeExtended(), 0, 0);

        //act
        scaleDevice.errorOccurred(errorEvent);

        //assert
        verify(mockDynamicScale).disconnect();
    }

    @Test
    public void errorOccurred_OtherError() {
        //arrange
        JposException exception = new JposException(JposConst.JPOS_E_TIMEOUT);
        ErrorEvent errorEvent = new ErrorEvent(this, exception.getErrorCode(), exception.getErrorCodeExtended(), 0, 0);

        //act
        scaleDevice.errorOccurred(errorEvent);

        //assert
        verify(mockDynamicScale, never()).disconnect();
    }

    @Test
    public void getLiveWeight_ReturnsLiveWeight() {
        //arrange
        FormattedWeight expected = new FormattedWeight(3);
        scaleDevice.setLiveWeight(expected);

        //act
        FormattedWeight actual = scaleDevice.getLiveWeight();

        //assert
        assertEquals(expected, actual);
    }

    @Test
    public void getDeviceName_CallsDynamicDevice() {
        //arrange

        //act
        scaleDevice.getDeviceName();

        //assert
        verify(mockDynamicScale).getDeviceName();
    }

    @Test
    public void tryLock_WhenLock_ReturnsTrue() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);

        //act
        scaleDeviceListLock.tryLock();

        //assert
        assertTrue(scaleDeviceListLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenNotLock_ReturnsFalse() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(10, TimeUnit.SECONDS)).thenReturn(false);

        //act
        scaleDeviceListLock.tryLock();

        //assert
        assertFalse(scaleDeviceListLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenLockThrowsException_ReturnsFalse() throws InterruptedException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(10, TimeUnit.SECONDS);

        //act
        scaleDeviceListLock.tryLock();

        //assert
        assertFalse(scaleDeviceListLock.getIsLocked());
    }

    @Test
    public void unlock_CallsThrough() {
        //arrange

        //act
        scaleDeviceListLock.unlock();

        //assert
        verify(mockConnectLock).unlock();
        assertFalse(scaleDeviceListLock.getIsLocked());
    }
}
