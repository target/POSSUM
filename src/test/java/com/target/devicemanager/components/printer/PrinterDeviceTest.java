package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.DynamicDevice;
import com.target.devicemanager.components.printer.entities.*;
import jpos.JposConst;
import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.events.StatusUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PrinterDeviceTest {

    private PrinterDevice printerDevice;
    private PrinterDevice printerDeviceLock;

    @Mock
    private DynamicDevice<POSPrinter> mockDynamicPrinter;
    @Mock
    private POSPrinter mockPrinter;
    @Mock
    private StatusUpdateEvent mockStatusUpdateEvent;
    @Mock
    private DeviceListener mockDeviceListener;
    @Mock
    private ReentrantLock mockConnectLock;

    @BeforeEach
    public void testInitialize() {
        when(mockDynamicPrinter.getDevice()).thenReturn(mockPrinter);

        printerDevice = new PrinterDevice(mockDynamicPrinter, mockDeviceListener);
        printerDeviceLock = new PrinterDevice(mockDynamicPrinter, mockDeviceListener, mockConnectLock);
    }

    @Test
    public void ctor_WhenDynamicPrinterAndDeviceListenerAreNull_ThrowsException() {
        try {
            new PrinterDevice(null, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicPrinter cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicPrinterIsNull_ThrowsException() {
        try {
            new PrinterDevice(null, mockDeviceListener);
        } catch (IllegalArgumentException iae) {
            assertEquals("dynamicPrinter cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDeviceListenerIsNull_ThrowsException() {
        try {
            new PrinterDevice(mockDynamicPrinter, null);
        } catch (IllegalArgumentException iae) {
            assertEquals("deviceListener cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenDynamicCashDrawerAndDeviceListenerAreNotNull_DoesNotThrowException() {
        try {
            new PrinterDevice(mockDynamicPrinter, mockDeviceListener);
        } catch(Exception exception) {
            fail("Existing Device Argument should not result in an Exception");
        }
    }

    @Test
    public void connect_DynamicConnect_DoesNotConnect() {
        //arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        //act
        boolean result = printerDevice.connect();

        //assert
        verify(mockDynamicPrinter).connect();
        verify(mockDynamicPrinter, never()).getDevice();
        assertFalse(result);
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void connect_DynamicConnect_Connects() {
        //arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);

        //act
        boolean result = printerDevice.connect();

        //assert
        verify(mockDynamicPrinter).connect();
        verify(mockDynamicPrinter, times(2)).getDevice();
        assertTrue(result);
        assertTrue(printerDevice.isConnected());
    }

    @Test
    public void connect_WhenAreListenersAttachedFalse_AttachListeners() {
        //arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        printerDevice.setAreListenersAttached(false);

        //act
        boolean result = printerDevice.connect();

        //assert
        verify(mockDynamicPrinter).connect();
        verify(mockPrinter).addStatusUpdateListener(any());
        assertTrue(printerDevice.getAreListenersAttached());
        verify(mockDynamicPrinter, times(2)).getDevice();
        assertTrue(result);
        assertTrue(printerDevice.isConnected());
    }

    @Test
    public void connect_WhenAreListenersAttachedTrue_DoesNotAttachListeners() {
        //arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        printerDevice.setAreListenersAttached(true);

        //act
        boolean result = printerDevice.connect();

        //assert
        verify(mockDynamicPrinter).connect();
        verify(mockPrinter, never()).addStatusUpdateListener(any());
        assertTrue(printerDevice.getAreListenersAttached());
        verify(mockDynamicPrinter, times(1)).getDevice();
        assertTrue(result);
        assertTrue(printerDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledFalse_SetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockPrinter.getDeviceEnabled()).thenReturn(false);

        //act
        boolean result = printerDevice.connect();

        //assert
        verify(mockDynamicPrinter).connect();
        verify(mockPrinter).setDeviceEnabled(true);
        verify(mockPrinter).setAsyncMode(true);
        verify(mockDynamicPrinter, times(2)).getDevice();
        assertTrue(result);
        assertTrue(printerDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledTrue_DoesNotSetDeviceEnabled() throws JposException {
        //arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        when(mockPrinter.getDeviceEnabled()).thenReturn(true);

        //act
        boolean result = printerDevice.connect();

        //assert
        verify(mockDynamicPrinter).connect();
        verify(mockPrinter, never()).setDeviceEnabled(true);
        verify(mockPrinter, never()).setAsyncMode(true);
        verify(mockDynamicPrinter, times(2)).getDevice();
        assertTrue(result);
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void connect_WhenGetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).getDeviceEnabled();

        //act
        boolean result = printerDevice.connect();

        //assert
        verify(mockDynamicPrinter).connect();
        verify(mockPrinter, never()).setDeviceEnabled(true);
        verify(mockPrinter, never()).setAsyncMode(true);
        verify(mockDynamicPrinter, times(2)).getDevice();
        assertFalse(result);
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void connect_WhenSetDeviceEnabledThrowsException() throws JposException {
        //arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).setDeviceEnabled(true);

        //act
        boolean result = printerDevice.connect();

        //assert
        verify(mockDynamicPrinter).connect();
        verify(mockPrinter).setDeviceEnabled(true);
        verify(mockPrinter, never()).setAsyncMode(true);
        verify(mockDynamicPrinter, times(2)).getDevice();
        assertFalse(result);
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void connect_WhenSetAsyncModeThrowsException() throws JposException {
        //arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).setAsyncMode(true);

        //act
        boolean result = printerDevice.connect();

        //assert
        verify(mockDynamicPrinter).connect();
        verify(mockPrinter).setDeviceEnabled(true);
        verify(mockPrinter).setAsyncMode(true);
        verify(mockDynamicPrinter, times(2)).getDevice();
        assertFalse(result);
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenAreListenersAttachedTrue_DetachListeners() throws JposException {
        //arrange
        printerDevice.setAreListenersAttached(true);

        //act
        printerDevice.disconnect();

        //assert
        assertFalse(printerDevice.getAreListenersAttached());
        verify(mockDynamicPrinter, times(2)).getDevice();
        verify(mockPrinter).getDeviceEnabled();
        verify(mockDynamicPrinter).disconnect();
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenAreListenersAttachedFalse_DoesNotDetachListeners() throws JposException{
        //arrange
        printerDevice.setAreListenersAttached(false);

        //act
        printerDevice.disconnect();

        //assert
        assertFalse(printerDevice.getAreListenersAttached());
        verify(mockDynamicPrinter, times(1)).getDevice();
        verify(mockPrinter).getDeviceEnabled();
        verify(mockDynamicPrinter).disconnect();
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenGetDeviceEnabledTrue_DisableDevice() throws JposException{
        //arrange
        when(mockPrinter.getDeviceEnabled()).thenReturn(true);

        //act
        printerDevice.disconnect();

        //assert
        assertFalse(printerDevice.getAreListenersAttached());
        verify(mockDynamicPrinter, times(1)).getDevice();
        verify(mockPrinter).getDeviceEnabled();
        verify(mockPrinter).setDeviceEnabled(false);
        verify(mockDynamicPrinter).disconnect();
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenGetDeviceEnabledFalse_DoesNotDisableDevice() throws JposException{
        //arrange
        when(mockPrinter.getDeviceEnabled()).thenReturn(false);

        //act
        printerDevice.disconnect();

        //assert
        assertFalse(printerDevice.getAreListenersAttached());
        verify(mockDynamicPrinter, times(1)).getDevice();
        verify(mockPrinter).getDeviceEnabled();
        verify(mockPrinter, never()).setDeviceEnabled(false);
        verify(mockDynamicPrinter).disconnect();
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenGetDeviceEnabled_ThrowsException() throws JposException {
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).getDeviceEnabled();

        //act
        printerDevice.disconnect();

        //assert
        assertFalse(printerDevice.getAreListenersAttached());
        verify(mockDynamicPrinter, times(1)).getDevice();
        verify(mockPrinter).getDeviceEnabled();
        verify(mockPrinter, never()).setDeviceEnabled(false);
        verify(mockDynamicPrinter).disconnect();
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void disconnect_WhenSetDeviceEnabled_ThrowsException() throws JposException{
        //arrange
        when(mockPrinter.getDeviceEnabled()).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).setDeviceEnabled(false);

        //act
        printerDevice.disconnect();

        //assert
        assertFalse(printerDevice.getAreListenersAttached());
        verify(mockDynamicPrinter, times(1)).getDevice();
        verify(mockPrinter).getDeviceEnabled();
        verify(mockPrinter).setDeviceEnabled(false);
        verify(mockDynamicPrinter).disconnect();
        assertFalse(printerDevice.isConnected());
    }

    @Test
    public void printContent_WhenContentsNull() throws JposException, PrinterException {
        //arrange

        //act
        try {
            printerDevice.printContent(null, 0);
        }
        //assert
        catch (PrinterException printerException) {
            assert(printerException.getDeviceError().equals(PrinterError.INVALID_FORMAT));
            verify(mockDynamicPrinter, times(1)).getDevice();
            verify(mockPrinter, never()).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter, times(1)).clearOutput();
            return;
        } catch (JposException jposException) {
            fail("Expected PrinterException, got JposException");
        }
    }

    @Test
    public void printContent_WhenContentsEmpty() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();

        //act
        try {
            printerDevice.printContent(contents, 0);
        }
        //assert
        catch (PrinterException printerException) {
            assert(printerException.getDeviceError().equals(PrinterError.INVALID_FORMAT));
            verify(mockDynamicPrinter, times(1)).getDevice();
            verify(mockPrinter, never()).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter, times(1)).clearOutput();
            return;
        } catch (JposException jposException) {
            fail("Expected PrinterException, got JposException");
        }
    }

    @Test
    public void printContent_WhenEnable_ThrowsException() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        PrinterContent printerContent = new PrinterContent() {
            @Override
            public String toString() {
                return super.toString();
            }
        };
        contents.add(printerContent);
        printerDevice.setDeviceConnected(false);

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            assert(jposException.getErrorCode() == JposConst.JPOS_E_OFFLINE);
            verify(mockDynamicPrinter, times(1)).getDevice();
            verify(mockPrinter, never()).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter, times(1)).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenWasPaperEmptyFalse_ThrowsException() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        PrinterContent printerContent = new PrinterContent() {
            @Override
            public String toString() {
                return super.toString();
            }
        };
        contents.add(printerContent);
        printerDevice.setDeviceConnected(true);
        printerDevice.setWasPaperEmpty(true);

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter).getDevice();
            verify(mockPrinter, never()).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenPaperEmptyCheckFalse_ThrowsException() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        PrinterContent printerContent = new PrinterContent() {
            @Override
            public String toString() {
                return super.toString();
            }
        };
        contents.add(printerContent);
        printerDevice.setDeviceConnected(true);
        printerDevice.setWasPaperEmpty(false);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NCR Kiosk POS Printer");
        printerDevice.setRef(-2147482880);

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(2)).getDevice();
            verify(mockPrinter).getPhysicalDeviceName();
            verify(mockPrinter).directIO(anyInt(), any(), any());
            verify(mockPrinter, never()).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenPaperEmptyCheck_ThrowsException() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        PrinterContent printerContent = new PrinterContent() {
            @Override
            public String toString() {
                return super.toString();
            }
        };
        contents.add(printerContent);
        printerDevice.setDeviceConnected(true);
        printerDevice.setWasPaperEmpty(false);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).getPhysicalDeviceName();

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(2)).getDevice();
            verify(mockPrinter).getPhysicalDeviceName();
            verify(mockPrinter, never()).directIO(anyInt(), any(), any());
            verify(mockPrinter, never()).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenReconnectR5Printer_ThrowsException() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        PrinterContent printerContent = new PrinterContent() {
            @Override
            public String toString() {
                return super.toString();
            }
        };
        contents.add(printerContent);
        printerDevice.setDeviceConnected(true);
        printerDevice.setWasPaperEmpty(false);
        printerDevice.setIsReconnectNeeded(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NCR Kiosk POS Printer").thenThrow(new JposException(JposConst.JPOS_E_EXTENDED));

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(3)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter).directIO(anyInt(), any(), any());
            assertTrue(printerDevice.getIsReconnectNeeded());
            verify(mockPrinter, never()).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenTransactionPrintTransaction_ThrowException() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        PrinterContent printerContent = new PrinterContent() {
            @Override
            public String toString() {
                return super.toString();
            }
        };
        contents.add(printerContent);
        printerDevice.setDeviceConnected(true);
        printerDevice.setWasPaperEmpty(false);
        printerDevice.setIsReconnectNeeded(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).transactionPrint(0, POSPrinterConst.PTR_TP_TRANSACTION);

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(3)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, never()).directIO(anyInt(), any(), any());
            assertTrue(printerDevice.getIsReconnectNeeded());
            verify(mockPrinter, times(1)).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenContentBarcode() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        BarcodeContent barcodeContent = new BarcodeContent();
        barcodeContent.setType(ContentType.BARCODE);
        contents.add(barcodeContent);
        printerDevice.setDeviceConnected(true);
        printerDevice.setWasPaperEmpty(false);
        printerDevice.setIsReconnectNeeded(false);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NCR Kiosk POS Printer");

        //act
        printerDevice.printContent(contents, 0);

        //assert
        verify(mockDynamicPrinter, times(3)).getDevice();
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter).directIO(anyInt(), any(), any());
        assertFalse(printerDevice.getIsReconnectNeeded());
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).printBarCode(anyInt(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(mockPrinter).clearOutput();
    }

    @Test
    public void printContent_WhenContentBarcodeFails() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        BarcodeContent barcodeContent = new BarcodeContent();
        barcodeContent.setType(ContentType.BARCODE);
        contents.add(barcodeContent);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).printBarCode(anyInt(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        printerDevice.setDeviceConnected(true);
        printerDevice.setWasPaperEmpty(false);
        printerDevice.setIsReconnectNeeded(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NCR Kiosk POS Printer");

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(6)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockDynamicPrinter).disconnect();
            verify(mockDynamicPrinter).connect();
            verify(mockPrinter).directIO(anyInt(), any(), any());
            assertFalse(printerDevice.getIsReconnectNeeded());
            verify(mockPrinter, times(1)).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).printBarCode(anyInt(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenContentImage() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        ImageContent imageContent = new ImageContent();
        imageContent.setType(ContentType.IMAGE);
        imageContent.setData("abc123");
        contents.add(imageContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("notR5");

        //act
        printerDevice.printContent(contents, 0);

        //assert
        verify(mockDynamicPrinter, times(3)).getDevice();
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).printMemoryBitmap(anyInt(), any(), anyInt(), anyInt(), anyInt());
        verify(mockPrinter).clearOutput();
    }

    @Test
    public void printContent_WhenContentImageFails() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        ImageContent imageContent = new ImageContent();
        imageContent.setType(ContentType.IMAGE);
        imageContent.setData("abc123");
        contents.add(imageContent);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).printMemoryBitmap(anyInt(), any(), anyInt(), anyInt(), anyInt());
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("notR5");

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(3)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(1)).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).printMemoryBitmap(anyInt(), any(), anyInt(), anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenContentText() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("notR5");

        //act
        printerDevice.printContent(contents, 0);

        //assert
        verify(mockDynamicPrinter, times(3)).getDevice();
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).printNormal(anyInt(), any());
        verify(mockPrinter).clearOutput();
    }

    @Test
    public void printContent_WhenContentTextFails() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).printNormal(anyInt(), any());
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("notR5");

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(3)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(1)).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).printNormal(anyInt(), any());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenContentBarcodeImage() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        BarcodeContent barcodeContent = new BarcodeContent();
        barcodeContent.setType(ContentType.BARCODE);
        contents.add(barcodeContent);
        ImageContent imageContent = new ImageContent();
        imageContent.setType(ContentType.IMAGE);
        imageContent.setData("abc123");
        contents.add(imageContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("notR5");

        //act
        printerDevice.printContent(contents, 0);

        //assert
        verify(mockDynamicPrinter, times(3)).getDevice();
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).printBarCode(anyInt(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(mockPrinter).printMemoryBitmap(anyInt(), any(), anyInt(), anyInt(), anyInt());
        verify(mockPrinter).clearOutput();
    }

    @Test
    public void printContent_WhenContentBarcodeText() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        BarcodeContent barcodeContent = new BarcodeContent();
        barcodeContent.setType(ContentType.BARCODE);
        contents.add(barcodeContent);
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("notR5");

        //act
        printerDevice.printContent(contents, 0);

        //assert
        verify(mockDynamicPrinter, times(3)).getDevice();
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).printBarCode(anyInt(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(mockPrinter).printNormal(anyInt(), any());
        verify(mockPrinter).clearOutput();
    }

    @Test
    public void printContent_WhenContentImageText() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        ImageContent imageContent = new ImageContent();
        imageContent.setType(ContentType.IMAGE);
        imageContent.setData("abc123");
        contents.add(imageContent);
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("notR5");

        //act
        printerDevice.printContent(contents, 0);

        //assert
        verify(mockDynamicPrinter, times(3)).getDevice();
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).printMemoryBitmap(anyInt(), any(), anyInt(), anyInt(), anyInt());
        verify(mockPrinter).printNormal(anyInt(), any());
        verify(mockPrinter).clearOutput();
    }

    @Test
    public void printContent_WhenContentBarcodeImageText() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        BarcodeContent barcodeContent = new BarcodeContent();
        barcodeContent.setType(ContentType.BARCODE);
        contents.add(barcodeContent);
        ImageContent imageContent = new ImageContent();
        imageContent.setType(ContentType.IMAGE);
        imageContent.setData("abc123");
        contents.add(imageContent);
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("notR5");

        //act
        printerDevice.printContent(contents, 0);

        //assert
        verify(mockDynamicPrinter, times(3)).getDevice();
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).printBarCode(anyInt(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(mockPrinter).printMemoryBitmap(anyInt(), any(), anyInt(), anyInt(), anyInt());
        verify(mockPrinter).printNormal(anyInt(), any());
        verify(mockPrinter).clearOutput();
    }

    @Test
    public void printContent_WhenTransactionPrintNormal_ThrowsException() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).transactionPrint(0, POSPrinterConst.PTR_TP_NORMAL);

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(3)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenWaitForOutputToComplete_ThrowsException() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockDeviceListener).waitForOutputToComplete();

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(3)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_When111Exception_Reconnect() throws JposException, InterruptedException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDeviceLock.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        when(mockConnectLock.tryLock(printerDevice.getTryLockTimeout(), TimeUnit.SECONDS)).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_FAILURE)).when(mockDeviceListener).waitForOutputToComplete();

        //act
        try {
            printerDeviceLock.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(6)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockDynamicPrinter).disconnect();
            verify(mockDynamicPrinter).connect();
            verify(mockConnectLock).unlock();
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_When105Exception_Reconnect() throws JposException, InterruptedException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDeviceLock.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        when(mockConnectLock.tryLock(printerDeviceLock.getTryLockTimeout(), TimeUnit.SECONDS)).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_DISABLED)).when(mockDeviceListener).waitForOutputToComplete();

        //act
        try {
            printerDeviceLock.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(6)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockDynamicPrinter).disconnect();
            verify(mockDynamicPrinter).connect();
            verify(mockConnectLock).unlock();
            verify(mockPrinter).clearOutput();
            return;
        }  catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_When106Exception_Reconnect() throws JposException, InterruptedException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDeviceLock.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        when(mockConnectLock.tryLock(printerDeviceLock.getTryLockTimeout(), TimeUnit.SECONDS)).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_ILLEGAL)).when(mockDeviceListener).waitForOutputToComplete();

        //act
        try {
            printerDeviceLock.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            fail("Expected PrinterException, got JposException");
        } catch (PrinterException printerException) {
            verify(mockDynamicPrinter, times(6)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockDynamicPrinter).disconnect();
            verify(mockDynamicPrinter).connect();
            verify(mockConnectLock).unlock();
            verify(mockPrinter).clearOutput();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_When114_207Exception_Reconnect() throws JposException, InterruptedException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDeviceLock.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        when(mockConnectLock.tryLock(printerDevice.getTryLockTimeout(), TimeUnit.SECONDS)).thenReturn(true);
        doThrow(new JposException(114, 207)).when(mockDeviceListener).waitForOutputToComplete();

        //act
        try {
            printerDeviceLock.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            fail("Expected PrinterException, got JposException");
        }  catch (PrinterException printerException) {
            verify(mockDynamicPrinter, times(6)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockDynamicPrinter).disconnect();
            verify(mockDynamicPrinter).connect();
            verify(mockConnectLock).unlock();
            verify(mockPrinter).clearOutput();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenTryLockFalse_ThrowsPrinterBusyError() throws JposException, InterruptedException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDeviceLock.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        when(mockConnectLock.tryLock(printerDevice.getTryLockTimeout(), TimeUnit.SECONDS)).thenReturn(false);

        //act
        try {
            printerDeviceLock.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            fail("Expected PrinterException, got JposException");
        } catch (PrinterException printerException) {
            if (printerException.getDeviceError() != PrinterError.PRINTER_BUSY) {
                fail("Expected PRINTER_BUSY error, got " + printerException.getDeviceError());
            }
            verify(mockDynamicPrinter, never()).getDevice();
            verify(mockPrinter, never()).getPhysicalDeviceName();
            verify(mockPrinter, never()).transactionPrint(anyInt(), anyInt());
            verify(mockDynamicPrinter, never()).disconnect();
            verify(mockDynamicPrinter, never()).connect();
            verify(mockConnectLock, never()).unlock();
            verify(mockPrinter, never()).clearOutput();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenClearOutput_ThrowsException() throws JposException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).clearOutput();

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(3)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_WhenGetIsCheckInsertedFalse_DoesNotWithdrawCheck() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        printerDevice.setIsCheckInserted(false);

        //act
        printerDevice.printContent(contents, 0);

        //assert
        verify(mockDynamicPrinter, times(3)).getDevice();
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).clearOutput();
        verify(mockPrinter, never()).beginRemoval(anyInt());
        verify(mockPrinter, never()).endRemoval();
    }

    @Test
    public void printContent_WhenGetIsCheckInsertedTrue_WithdrawsCheck() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        printerDevice.setIsCheckInserted(true);

        //act
        printerDevice.printContent(contents, 0);

        //assert
        verify(mockDynamicPrinter, times(4)).getDevice();
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).clearOutput();
        verify(mockPrinter).beginRemoval(anyInt());
        verify(mockPrinter).endRemoval();
    }

    @Test
    public void printContent_WhenGetIsCheckInsertedTrue_WithdrawCheckThrowsError() throws JposException, PrinterException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        printerDevice.setIsCheckInserted(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).beginRemoval(anyInt());

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter, times(4)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockPrinter).clearOutput();
        }
        catch (PrinterException printerException) {
            fail("Expected jposException, got printerException");
        }
    }

    @Test
    public void withdrawCheck_CallsThrough() throws JposException{
        //arrange

        //act
        printerDevice.withdrawCheck();

        //assert
        verify(mockDynamicPrinter).getDevice();
        verify(mockPrinter).beginRemoval(0);
        verify(mockPrinter).endRemoval();
    }

    @Test
    public void withdrawCheck_beginRemovalThrowsError() throws JposException{
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).beginRemoval(0);

        //act
        try {
            printerDevice.withdrawCheck();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter).getDevice();
            verify(mockPrinter).beginRemoval(0);
            verify(mockPrinter, never()).endRemoval();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void withdrawCheck_endRemovalThrowsError() throws JposException{
        //arrange
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).endRemoval();

        //act
        try {
            printerDevice.withdrawCheck();
        }

        //assert
        catch (JposException jposException) {
            verify(mockDynamicPrinter).getDevice();
            verify(mockPrinter).beginRemoval(0);
            verify(mockPrinter).endRemoval();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void getDeviceName_ReturnsName() {
        //arrange
        String expectedDeviceName = "micr";
        when(mockDynamicPrinter.getDeviceName()).thenReturn(expectedDeviceName);

        //act
        String actualDeviceName = printerDevice.getDeviceName();

        //assert
        assertEquals(expectedDeviceName, actualDeviceName);
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOff_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(printerSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFF_OFFLINE);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(printerSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOffline_Disconnected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_OFFLINE);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(printerSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenDeviceOnline_Connected() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(JposConst.JPOS_SUE_POWER_ONLINE);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(printerSpy.isConnected());
    }

    @Test
    public void statusUpdateOccurred_WhenCoverOpen_CallSetters() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_COVER_OPEN);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(printerSpy.getWasDoorOpened());
        assertFalse(printerSpy.getIsReconnectNeeded());
    }

    @Test
    public void statusUpdateOccurred_WhenCoverOk_SingletonNotNull() {
        //arrange
        PrinterErrorHandlingSingleton printerErrorHandlingSingleton = PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton();
        printerErrorHandlingSingleton.setError(new PrinterException(PrinterError.UNEXPECTED_ERROR));
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_COVER_OK);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertNull(printerErrorHandlingSingleton.getError());
        assertFalse(printerSpy.getIsReconnectNeeded());
        assertFalse(printerSpy.getWasDoorOpened());
    }

    @Test
    public void statusUpdateOccurred_WhenCoverOk_WhenSingletonNull_WhenDoorOpenTrue_CallSetters() {
        //arrange
        PrinterErrorHandlingSingleton printerErrorHandlingSingleton = PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton();
        printerErrorHandlingSingleton.setError(null);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_COVER_OK);
        printerDevice.setWasDoorOpened(true);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(printerSpy.getIsReconnectNeeded());
        assertFalse(printerSpy.getWasDoorOpened());
    }

    @Test
    public void statusUpdateOccurred_WhenCoverOk_WhenDoorOpenFalse_DoesNotCallSetters() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_COVER_OK);
        printerDevice.setWasDoorOpened(false);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(printerSpy.getIsReconnectNeeded());
        assertFalse(printerSpy.getWasDoorOpened());
    }

    @Test
    public void statusUpdateOccurred_SingletonNull() {
        //arrange
        PrinterErrorHandlingSingleton printerErrorHandlingSingleton = PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton();
        printerErrorHandlingSingleton.setError(null);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_REC_EMPTY);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertEquals(printerErrorHandlingSingleton.getError().getDeviceError(), PrinterError.OUT_OF_PAPER);
        assertTrue(printerSpy.getWasPaperEmpty());
        assertFalse(printerSpy.getIsReconnectNeeded());
    }

    @Test
    public void statusUpdateOccurred_SingletonNotNull_WhenRecEmpty_CallSetters() {
        //arrange
        PrinterErrorHandlingSingleton printerErrorHandlingSingleton = PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton();
        printerErrorHandlingSingleton.setError(new PrinterException(PrinterError.UNEXPECTED_ERROR));
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_REC_EMPTY);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertEquals(printerErrorHandlingSingleton.getError().getDeviceError(), PrinterError.UNEXPECTED_ERROR);
        assertTrue(printerSpy.getWasPaperEmpty());
        assertFalse(printerSpy.getIsReconnectNeeded());
    }

    @Test
    public void statusUpdateOccurred_WhenRecNearEmpty_DoesNothing() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_REC_NEAREMPTY);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        //do nothing
    }

    @Test
    public void statusUpdateOccurred_SingletonNotNull() {
        //arrange
        PrinterErrorHandlingSingleton printerErrorHandlingSingleton = PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton();
        printerErrorHandlingSingleton.setError(new PrinterException(PrinterError.UNEXPECTED_ERROR));
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_REC_PAPEROK);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertNull(printerErrorHandlingSingleton.getError());
        assertFalse(printerSpy.getIsReconnectNeeded());
        assertFalse(printerSpy.getWasPaperEmpty());
    }

    @Test
    public void statusUpdateOccurred_SingletonNull_WhenRecPaperOk_WhenPaperEmptyTrue_CallSetters() {
        //arrange
        PrinterErrorHandlingSingleton printerErrorHandlingSingleton = PrinterErrorHandlingSingleton.getPrinterErrorHandlingSingleton();
        printerErrorHandlingSingleton.setError(null);
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_REC_PAPEROK);
        printerDevice.setWasPaperEmpty(true);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertNull(printerErrorHandlingSingleton.getError());
        assertTrue(printerSpy.getIsReconnectNeeded());
        assertFalse(printerSpy.getWasPaperEmpty());
    }

    @Test
    public void statusUpdateOccurred_WhenRecPaperOk_WhenPaperEmptyFalse_CallSetters() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_REC_PAPEROK);
        printerDevice.setWasPaperEmpty(false);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(printerSpy.getIsReconnectNeeded());
        assertFalse(printerSpy.getWasPaperEmpty());
    }

    @Test
    public void statusUpdateOccurred_WhenSlpEmpty_CallSetter() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_SLP_EMPTY);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertFalse(printerSpy.getIsCheckInserted());
    }

    @Test
    public void statusUpdateOccurred_WhenSlpPaperOk_CallSetter() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(POSPrinterConst.PTR_SUE_SLP_PAPEROK);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        assertTrue(printerSpy.getIsCheckInserted());
    }

    @Test
    public void statusUpdateOccurred_WhenOtherStatus_DoNothing() {
        //arrange
        when(mockStatusUpdateEvent.getStatus()).thenReturn(572);
        PrinterDevice printerSpy = spy(printerDevice);

        //act
        printerSpy.statusUpdateOccurred(mockStatusUpdateEvent);

        //assert
        //do nothing
    }

    @Test
    public void tryLock_WhenLock_ReturnsTrue() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(printerDeviceLock.getTryLockTimeout(), TimeUnit.SECONDS)).thenReturn(true);

        //act
        printerDeviceLock.tryLock();

        //assert
        assertTrue(printerDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenNotLock_ReturnsFalse() throws InterruptedException {
        //arrange
        when(mockConnectLock.tryLock(printerDeviceLock.getTryLockTimeout(), TimeUnit.SECONDS)).thenReturn(false);

        //act
        printerDeviceLock.tryLock();

        //assert
        assertFalse(printerDeviceLock.getIsLocked());
    }

    @Test
    public void tryLock_WhenLockThrowsException_ReturnsFalse() throws InterruptedException {
        //arrange
        doThrow(new InterruptedException()).when(mockConnectLock).tryLock(printerDeviceLock.getTryLockTimeout(), TimeUnit.SECONDS);

        //act
        printerDeviceLock.tryLock();

        //assert
        assertFalse(printerDeviceLock.getIsLocked());
    }

    @Test
    public void unlock_CallsThrough() {
        //arrange

        //act
        printerDeviceLock.unlock();

        //assert
        verify(mockConnectLock).unlock();
        assertFalse(printerDeviceLock.getIsLocked());
    }
}
