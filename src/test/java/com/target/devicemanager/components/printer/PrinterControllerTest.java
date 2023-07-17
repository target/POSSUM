package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.printer.entities.PrinterContent;
import com.target.devicemanager.components.printer.entities.PrinterError;
import com.target.devicemanager.components.printer.entities.PrinterException;
import jpos.JposConst;
import jpos.JposException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PrinterControllerTest {

    private PrinterController printerController;

    @Mock
    private PrinterManager mockPrinterManager;

    @BeforeEach
    public void testInitialize() {
        printerController = new PrinterController(mockPrinterManager);
    }

    @Test
    public void ctor_WhenPrinterManagerIsNull_ThrowsException() {
        try {
            new PrinterController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("printerManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenPrinterManagerIsNew_DoesNotThrowException() {
        try {
            new PrinterController(mockPrinterManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void print_CallsThroughToPrinterManager() throws DeviceException {
        //arrange
        List<PrinterContent> testData = new ArrayList<>();
        PrinterContent testContent = new PrinterContent() {};
        testData.add(testContent);

        //act
        printerController.print(testData);

        //assert
        verify(mockPrinterManager).printReceipt(testData);
    }

    @Test
    public void print_WhenTestDataIsLong_DoesNotThroughToPrinterManager() throws DeviceException {
        //arrange
        List<PrinterContent> testData = new ArrayList<>();
        PrinterContent testContent = new PrinterContent() {};
        for(int i = 0; i < 65; i++) {
            testData.add(testContent);
        }

        //act
        printerController.print(testData);

        //assert
        verify(mockPrinterManager, never()).printReceipt(testData);
    }

    @Test
    public void print_ThrowsException() throws DeviceException {
        //arrange
        List<PrinterContent> testData = new ArrayList<PrinterContent>();
        PrinterContent testContent = new PrinterContent() {};
        testData.add(testContent);
        doThrow(new PrinterException(new JposException(JposConst.JPOS_E_ILLEGAL))).when(mockPrinterManager).printReceipt(any());

        //act
        try {
            printerController.print(testData);
        }

        //assert
        catch (DeviceException deviceException) {
            assertEquals(PrinterError.ILLEGAL_OPERATION, deviceException.getDeviceError());
        }
    }

    @Test
    public void getHealth_ReturnsHealthFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("printer", DeviceHealth.READY);
        when(mockPrinterManager.getHealth()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = printerController.getHealth();

        //assert
        verify(mockPrinterManager).getHealth();
        assertEquals(expected, actual);
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expected = new DeviceHealthResponse("printer", DeviceHealth.READY);
        when(mockPrinterManager.getStatus()).thenReturn(expected);

        //act
        DeviceHealthResponse actual = printerController.getStatus();

        //assert
        assertEquals(expected, actual);
        verify(mockPrinterManager).getStatus();
    }

    @Test
    public void reconnect_CallsThroughToPrinterManager() throws DeviceException {
        //arrange

        //act
        try {
            printerController.reconnect();
        } catch (Exception exception) {
            fail("printerController.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockPrinterManager).reconnectDevice();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockPrinterManager).reconnectDevice();

        //act
        try {
            printerController.reconnect();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockPrinterManager).reconnectDevice();
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
        ResponseEntity<DeviceError> actual = printerController.handleInvalidFormat(myException);

        //assert
        assertEquals(PrinterError.INVALID_FORMAT, actual.getBody());
        assertEquals(PrinterError.INVALID_FORMAT.getStatusCode(), actual.getStatusCode());
    }
}
