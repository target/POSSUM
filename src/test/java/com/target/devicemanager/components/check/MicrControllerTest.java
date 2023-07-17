package com.target.devicemanager.components.check;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.check.entities.MicrException;
import com.target.devicemanager.components.printer.PrinterManager;
import com.target.devicemanager.components.printer.entities.PrinterContent;
import com.target.devicemanager.components.printer.entities.PrinterError;
import com.target.devicemanager.components.printer.entities.PrinterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MicrControllerTest {

    private MicrController micrController;

    @Mock
    private MicrManager mockMicrManager;
    @Mock
    private PrinterManager mockPrinterManager;

    @BeforeEach
    public void testInitialize() {
        micrController = new MicrController(mockPrinterManager, mockMicrManager);
    }

    @Test
    public void ctor_WhenPrinterManagerAndMicrManagerAreNull_ThrowsException() {
        try {
            new MicrController(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("printerManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenPrinterManagerIsNull_ThrowsException() {
        try {
            new MicrController(null, mockMicrManager);
        } catch (IllegalArgumentException iae) {
            assertEquals("printerManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenMicrManagerIsNull_ThrowsException() {
        try {
            new MicrController(mockPrinterManager, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("micrManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenPrinterManagerAndMicrManagerAreNotNull_DoesNotThrowException() {
        try {
            new MicrController(mockPrinterManager, mockMicrManager);
        } catch(Exception exception) {
            fail("Existing Manager Arguments should not result in an Exception");
        }
    }

    @Test
    public void print_CallsThroughToPrinterManager() throws PrinterException {
        //arrange
        List<PrinterContent> testData = new ArrayList<>();
        PrinterContent testContent = new PrinterContent() {};
        testData.add(testContent);

        //act
        micrController.print(testData);

        //assert
        verify(mockPrinterManager).frankCheck(testData);
    }

    @Test
    public void print_WhenTestDataIsLong_DoesNotCallThroughToPrinterManager() throws PrinterException {
        //arrange
        List<PrinterContent> testData = new ArrayList<>();
        PrinterContent testContent = new PrinterContent() {};
        for(int i = 0; i < 65; i++) {
            testData.add(testContent);
        }

        //act
        micrController.print(testData);

        //assert
        verify(mockPrinterManager, never()).frankCheck(testData);
    }

    @Test
    public void print_WhenThrowsError() throws PrinterException {
        //arrange
        List<PrinterContent> testData = new ArrayList<>();
        PrinterContent testContent = new PrinterContent() {};
        testData.add(testContent);
        doThrow(new PrinterException(DeviceError.DEVICE_BUSY)).when(mockPrinterManager).frankCheck(testData);

        //act
        try {
            micrController.print(testData);
        }

        //assert
        catch(PrinterException printerException) {
            verify(mockPrinterManager).frankCheck(testData);
            assertEquals(DeviceError.DEVICE_BUSY, printerException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void readCheck_CallsThroughToMicrManager() throws MicrException {
        //arrange

        //act
        micrController.readCheck();

        //assert
        verify(mockMicrManager).readMICR(any());
    }

    @Test
    public void readCheck_WhenThrowsError() throws MicrException {
        //arrange
        doThrow(new MicrException(DeviceError.DEVICE_BUSY)).when(mockMicrManager).readMICR(any());

        //act
        try {
            micrController.readCheck();
        }

        //assert
        catch(MicrException micrException) {
            verify(mockMicrManager).readMICR(any());
            assertEquals(DeviceError.DEVICE_BUSY, micrException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void cancelCheckRead_CallsThroughToMicrManager() {
        //arrange

        //act
        micrController.cancelCheckRead();

        //assert
        verify(mockMicrManager).cancelCheckRead();
        verify(mockMicrManager).ejectCheck();
    }

    @Test
    public void reconnect_CallsThroughToMicrManager() throws DeviceException {
        //arrange

        //act
        try {
            micrController.reconnect();
        } catch (DeviceException deviceException) {
            fail("micrController.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockMicrManager).reconnectDevice();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockMicrManager).reconnectDevice();

        //act
        try {
            micrController.reconnect();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockMicrManager).reconnectDevice();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void handleBadRequest_ReturnsPrinterInvalidRequest(){
        //arrange
        HttpInputMessage httpInputMessage = null;
        HttpMessageNotReadableException myException = new HttpMessageNotReadableException("BAD DAT@1", httpInputMessage);

        //act
        ResponseEntity<DeviceError> actual = micrController.handleInvalidFormat(myException);

        //assert
        assertEquals(PrinterError.INVALID_FORMAT, actual.getBody());
        assertEquals(PrinterError.INVALID_FORMAT.getStatusCode(), actual.getStatusCode());
    }


    @Test
    public void getHealth_ReturnsHealthFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("micr", DeviceHealth.READY);
        when(mockMicrManager.getHealth()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = micrController.getHealth();

        //assert
        verify(mockMicrManager).getHealth();
        assertEquals(expected, actual);
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("cashDrawer", DeviceHealth.READY);
        when(mockMicrManager.getStatus()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = micrController.getStatus();

        //assert
        assertEquals(expected, actual);
        verify(mockMicrManager).getStatus();
    }
}