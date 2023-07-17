package com.target.devicemanager.components.linedisplay;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.linedisplay.entities.LineDisplayData;
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
public class LineDisplayControllerTest {

    private LineDisplayController lineDisplayController;

    @Mock
    private LineDisplayManager mockLineDisplayManager;

    @BeforeEach
    public void testInitialize() {
        lineDisplayController = new LineDisplayController(mockLineDisplayManager);
    }

    @Test
    public void ctor_WhenLineDisplayManagerIsNull_ThrowsException() {
        try {
            new LineDisplayController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("lineDisplayManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenLineDisplayManagerIsNew_DoesNotThrowException() {
        try {
            new LineDisplayController(mockLineDisplayManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void displayLines_CallsThroughToLineDisplayManager() throws DeviceException {
        //arrange
        LineDisplayData testData = new LineDisplayData();
        testData.line1 = "Test Data Line 1";
        testData.line2 = "Test Data Line 2";

        //act
        lineDisplayController.displayLines(testData);

        //assert
        verify(mockLineDisplayManager).displayLine("Test Data Line 1", "Test Data Line 2");
    }

    @Test
    public void displayLines_WhenThrowsError() throws DeviceException {
        //arrange
        LineDisplayData testData = new LineDisplayData();
        testData.line1 = "Test Data Line 1";
        testData.line2 = "Test Data Line 2";
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockLineDisplayManager).displayLine(testData.line1, testData.line2);

        //act
        try {
            lineDisplayController.displayLines(testData);
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockLineDisplayManager).displayLine(testData.line1, testData.line2);
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getHealth_ReturnsHealthFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("line display", DeviceHealth.READY);
        when(mockLineDisplayManager.getHealth()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = lineDisplayController.getHealth();

        //assert
        verify(mockLineDisplayManager).getHealth();
        assertEquals(expected, actual);
    }

    @Test
    public void reconnect_CallsThroughToLineDisplayManager() throws DeviceException {
        //arrange

        //act
        try {
            lineDisplayController.reconnect();
        } catch (Exception exception) {
            fail("lineDisplayController.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockLineDisplayManager).reconnectDevice();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockLineDisplayManager).reconnectDevice();

        //act
        try {
            lineDisplayController.reconnect();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockLineDisplayManager).reconnectDevice();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("line display", DeviceHealth.READY);
        when(mockLineDisplayManager.getStatus()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = lineDisplayController.getStatus();

        //assert
        assertEquals(expected, actual);
        verify(mockLineDisplayManager).getStatus();
    }
}