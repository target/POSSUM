package com.target.devicemanager.common;

import jpos.BaseJposControl;
import jpos.JposConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SimulatedDynamicDeviceTest {

    private SimulatedDynamicDevice<BaseJposControl> simulatedDynamicDevice;
    @Mock
    private BaseJposControl mockDevice;
    @Mock
    private DevicePower mockDevicePower;
    @Mock
    private DeviceConnector<BaseJposControl> mockDeviceConnector;

    @BeforeEach
    public void setup() {
        simulatedDynamicDevice = new SimulatedDynamicDevice<>(mockDevice, mockDevicePower, mockDeviceConnector);
    }

    @Test
    public void isOpenedAndIsClaimed_WhenSimulatedDeviceOnline_ReturnTrue() {
        when(mockDevice.getState()).thenReturn(JposConst.JPOS_S_IDLE);
        assertTrue(simulatedDynamicDevice.isOpened());
        assertTrue(simulatedDynamicDevice.isClaimed());
    }

    @Test
    public void isOpenedAndIsClaimed_WhenSimulatedDeviceOffline_ReturnFalse() {
        when(mockDevice.getState()).thenReturn(JposConst.JPOS_S_CLOSED);
        assertFalse(simulatedDynamicDevice.isOpened());
        assertFalse(simulatedDynamicDevice.isClaimed());
    }
}
