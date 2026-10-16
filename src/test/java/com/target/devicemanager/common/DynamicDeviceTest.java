package com.target.devicemanager.common;

import jpos.BaseJposControl;
import jpos.JposConst;
import jpos.JposException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DynamicDeviceTest {

    private DynamicDevice<BaseJposControl> dynamicDevice;
    @Mock
    private BaseJposControl mockDevice;
    @Mock
    private DevicePower mockDevicePower;
    @Mock
    private DeviceConnector<BaseJposControl> mockDeviceConnector;

    @BeforeEach
    public void setup() {
        dynamicDevice = new DynamicDevice<>(mockDevice, mockDevicePower, mockDeviceConnector);
    }

    @Test
    public void isOpened_WhenStateIdle_ReturnsTrue() {
        when(mockDevice.getState()).thenReturn(JposConst.JPOS_S_IDLE);
        assertTrue(dynamicDevice.isOpened());
    }

    @Test
    public void isOpened_WhenStateBusy_ReturnsTrue() {
        when(mockDevice.getState()).thenReturn(JposConst.JPOS_S_BUSY);
        assertTrue(dynamicDevice.isOpened());
    }

    @Test
    public void isOpened_WhenStateClosed_ReturnsFalse() {
        when(mockDevice.getState()).thenReturn(JposConst.JPOS_S_CLOSED);
        assertFalse(dynamicDevice.isOpened());
    }

    @Test
    public void isClaimed_WhenClaimed_ReturnsTrue() throws JposException {
        when(mockDevice.getClaimed()).thenReturn(true);
        assertTrue(dynamicDevice.isClaimed());
    }

    @Test
    public void isClaimed_WhenNotClaimed_ReturnsFalse() throws JposException {
        when(mockDevice.getClaimed()).thenReturn(false);
        assertFalse(dynamicDevice.isClaimed());
    }

    @Test
    public void isClaimed_WhenJposExceptionThrown_ReturnsFalse() throws JposException {
        when(mockDevice.getClaimed()).thenThrow(new JposException(JposConst.JPOS_E_FAILURE));
        assertFalse(dynamicDevice.isClaimed());
    }

    @Test
    public void isConnected_WhenOpenedClaimedAndOnline_ReturnsTrue() throws JposException {
        when(mockDevice.getState()).thenReturn(JposConst.JPOS_S_IDLE);
        when(mockDevice.getClaimed()).thenReturn(true);
        when(mockDevicePower.getPowerState(mockDevice)).thenReturn(JposConst.JPOS_PS_ONLINE);
        assertTrue(dynamicDevice.isConnected());
    }

    @Test
    public void isConnected_WhenNotOpened_ReturnsFalseAndSkipsPowerCheck() {
        when(mockDevice.getState()).thenReturn(JposConst.JPOS_S_CLOSED);
        assertFalse(dynamicDevice.isConnected());
        verify(mockDevicePower, never()).getPowerState(any());
    }

    @Test
    public void isConnected_WhenNotClaimed_ReturnsFalseAndSkipsPowerCheck() throws JposException {
        when(mockDevice.getState()).thenReturn(JposConst.JPOS_S_IDLE);
        when(mockDevice.getClaimed()).thenReturn(false);
        assertFalse(dynamicDevice.isConnected());
        verify(mockDevicePower, never()).getPowerState(any());
    }

    @Test
    public void isConnected_WhenPowerOffline_ReturnsFalse() throws JposException {
        when(mockDevice.getState()).thenReturn(JposConst.JPOS_S_IDLE);
        when(mockDevice.getClaimed()).thenReturn(true);
        when(mockDevicePower.getPowerState(mockDevice)).thenReturn(JposConst.JPOS_PS_OFFLINE);
        assertFalse(dynamicDevice.isConnected());
    }
}
