package com.target.devicemanager.components.cashdrawer;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CashDrawerControllerTest {

    private CashDrawerController cashDrawerController;

    @Mock
    private CashDrawerManager mockCashDrawerManager;

    @BeforeEach
    public void testInitialize() {
        cashDrawerController = new CashDrawerController(mockCashDrawerManager);
    }

    @Test
    public void ctor_WhenCashDrawerManagerIsNull_ThrowsException() {
        try {
            new CashDrawerController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("cashDrawerManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenCashDrawerManagerIsNew_DoesNotThrowException() {
        try {
            new CashDrawerController(mockCashDrawerManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void openCashDrawer_CallsThroughToCashDrawerManager() throws DeviceException {
        //arrange

        //act
        cashDrawerController.openCashDrawer();

        //assert
        verify(mockCashDrawerManager).openCashDrawer();
    }

    @Test
    public void openCashDrawer_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockCashDrawerManager).openCashDrawer();

        //act
        try {
            cashDrawerController.openCashDrawer();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockCashDrawerManager).openCashDrawer();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void reconnect_CallsThroughToCashDrawerManager() throws DeviceException {
        //arrange

        //act
        try {
            cashDrawerController.reconnect();
        } catch (DeviceException deviceException) {
            fail("cashDrawerController.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockCashDrawerManager).reconnectDevice();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockCashDrawerManager).reconnectDevice();

        //act
        try {
            cashDrawerController.reconnect();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockCashDrawerManager).reconnectDevice();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getHealth_ReturnsHealthFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);
        when(mockCashDrawerManager.getHealth()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = cashDrawerController.getHealth();

        //assert
        assertEquals(expected, actual);
        verify(mockCashDrawerManager).getHealth();
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);
        when(mockCashDrawerManager.getStatus()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = cashDrawerController.getStatus();

        //assert
        assertEquals(expected, actual);
        verify(mockCashDrawerManager).getStatus();
    }
}