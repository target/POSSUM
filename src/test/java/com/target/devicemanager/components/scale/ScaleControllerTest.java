package com.target.devicemanager.components.scale;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.scale.entities.FormattedWeight;
import com.target.devicemanager.components.scale.entities.ScaleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScaleControllerTest {

    private ScaleController scaleController;

    @Mock
    private ScaleManager mockScaleManager;
    @Mock
    private ScaleDevice mockScaleDevice;

    @BeforeEach
    public void testInitialize() {
        scaleController = new ScaleController(mockScaleManager);
    }

    @Test
    public void ctor_WhenScaleManagerIsNull_ThrowsException() {
        try {
            new ScaleController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scaleManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScaleManagerIsNew_DoesNotThrowException() {
        try {
            new ScaleController(mockScaleManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void getLiveWeight_CallsThroughToManager() throws IOException {
        //arrange

        //act
        SseEmitter sseEmitter = scaleController.getLiveWeight();

        //assert
        verify(mockScaleManager).subscribeToLiveWeight(any());
    }

    @Test
    public void getLiveWeight_WhenThrowsError() throws IOException {
        //arrange
        doThrow(new IOException()).when(mockScaleManager).subscribeToLiveWeight(any());

        //act
        try {
            scaleController.getLiveWeight();
        }

        //assert
        catch(IOException ioException) {
            verify(mockScaleManager).subscribeToLiveWeight(any());
            assertEquals(ioException.getMessage(), ioException.getMessage());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getStableWeight_CallsThroughToManager() throws ScaleException {
        //arrange
        when(mockScaleManager.getStableWeight(any())).thenReturn(new FormattedWeight(3000));
        String expected = "3.00";

        //act
        FormattedWeight actual = scaleController.getStableWeight();

        //assert
        verify(mockScaleManager).getStableWeight(any());
        assertEquals(actual.weight, expected);
    }

    @Test
    public void getStableWeight_WhenThrowsError() throws ScaleException {
        //arrange
        doThrow(new ScaleException(DeviceError.DEVICE_BUSY)).when(mockScaleManager).getStableWeight(any());

        //act
        try {
            scaleController.getStableWeight();
        }

        //assert
        catch(ScaleException scaleException) {
            verify(mockScaleManager).getStableWeight(any());
            assertEquals(DeviceError.DEVICE_BUSY, scaleException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getHealth_ReturnsHealthFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.READY);
        when(mockScaleManager.getHealth()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = scaleController.getHealth();

        //assert
        verify(mockScaleManager).getHealth();
        assertEquals(expected, actual);
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("scale", DeviceHealth.READY);
        when(mockScaleManager.getStatus()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = scaleController.getStatus();

        //assert
        assertEquals(expected, actual);
        verify(mockScaleManager).getStatus();
    }

    @Test
    public void reconnect_CallsThroughToPrinterManager() throws DeviceException {
        //arrange

        //act
        try {
            scaleController.reconnect();
        } catch (Exception exception) {
            fail("scaleController.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockScaleManager).reconnectDevice();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockScaleManager).reconnectDevice();

        //act
        try {
            scaleController.reconnect();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockScaleManager).reconnectDevice();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }
}
