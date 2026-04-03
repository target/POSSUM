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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
            // getDevice: 1st from printContent sync, 2nd from paperEmptyCheck sync
            verify(mockDynamicPrinter, times(2)).getDevice();
            // getPhysicalDeviceName: 1st from paperEmptyCheck
            verify(mockPrinter, times(1)).getPhysicalDeviceName();
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
            // getDevice: 1st from printContent sync, 2nd from paperEmptyCheck sync
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
        // reconnectR5Printer() is called inline in printContent() after paperEmptyCheck.
        // With isReconnectNeeded=true and R5 printer name, disconnect()+connect() are triggered.
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        printerDevice.setWasPaperEmpty(false);
        printerDevice.setIsReconnectNeeded(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NCR Kiosk POS Printer");

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert — paperEmptyCheck passes (ref[0] is 0 by default, not -2147482880),
        // reconnectR5Printer fires disconnect()+connect(), sets isReconnectNeeded=false,
        // then print proceeds normally with no exception
        catch (JposException jposException) {
            fail("Unexpected JposException: " + jposException.getMessage());
        } catch (PrinterException printerException) {
            fail("Unexpected PrinterException: " + printerException.getMessage());
        }

        // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer,
        //            4th disconnect sync, 5th connect attachListeners sync, 6th connect main sync
        verify(mockDynamicPrinter, times(6)).getDevice();
        // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter).directIO(anyInt(), any(), any());
        assertFalse(printerDevice.getIsReconnectNeeded());
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).clearOutput();
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
            // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
            verify(mockDynamicPrinter, times(3)).getDevice();
            // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, never()).directIO(anyInt(), any(), any());
            assertTrue(printerDevice.getIsReconnectNeeded());
            verify(mockPrinter).transactionPrint(0, POSPrinterConst.PTR_TP_TRANSACTION);
            verify(mockPrinter, never()).transactionPrint(0, POSPrinterConst.PTR_TP_NORMAL);
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
            // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
            //            (R5 + isReconnectNeeded=true → disconnect+connect fire)
            //            4th disconnect sync, 5th connect attachListeners sync, 6th connect main sync
            verify(mockDynamicPrinter, times(6)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockDynamicPrinter).disconnect();
            verify(mockDynamicPrinter).connect();
            verify(mockPrinter).directIO(anyInt(), any(), any());
            assertFalse(printerDevice.getIsReconnectNeeded());
            verify(mockPrinter).transactionPrint(0, POSPrinterConst.PTR_TP_TRANSACTION);
            verify(mockPrinter).transactionPrint(0, POSPrinterConst.PTR_TP_NORMAL);
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
            // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
            verify(mockDynamicPrinter, times(3)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            // transaction is started, then closed in finally
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
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
            // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
            verify(mockDynamicPrinter, times(3)).getDevice();
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            // transaction is started, then closed in finally
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
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
        // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
        verify(mockDynamicPrinter, times(3)).getDevice();
        // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
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
        // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
        verify(mockDynamicPrinter, times(3)).getDevice();
        // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
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
        // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
        verify(mockDynamicPrinter, times(3)).getDevice();
        // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
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
        // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
        verify(mockDynamicPrinter, times(3)).getDevice();
        // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
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
            // getDevice: 1st from printContent sync, 2nd from paperEmptyCheck sync, 3rd from reconnectR5Printer
            verify(mockDynamicPrinter, times(3)).getDevice();
            // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter).transactionPrint(0, POSPrinterConst.PTR_TP_TRANSACTION);
            verify(mockPrinter, times(2)).transactionPrint(0, POSPrinterConst.PTR_TP_NORMAL);
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
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockDeviceListener).waitForOutputToComplete(anyLong(), any(TimeUnit.class));

        //act
        try {
            printerDevice.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            // getDevice: 1st from printContent sync, 2nd from paperEmptyCheck sync, 3rd from reconnectR5Printer
            verify(mockDynamicPrinter, times(3)).getDevice();
            // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
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
    public void printContent_When111Exception_DoesNotReconnect() throws JposException, InterruptedException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDeviceLock.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        when(mockConnectLock.tryLock(printerDeviceLock.getTryLockTimeout(), TimeUnit.SECONDS)).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_FAILURE)).when(mockDeviceListener).waitForOutputToComplete(anyLong(), any(TimeUnit.class));

        //act
        try {
            printerDeviceLock.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            assertEquals(JposConst.JPOS_E_FAILURE, jposException.getErrorCode());
            // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
            verify(mockDynamicPrinter, times(3)).getDevice();
            // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockDynamicPrinter, never()).disconnect();
            verify(mockDynamicPrinter, never()).connect();
            // unlock called twice: once from reconnectR5Printer finally, once from printContent finally
            verify(mockConnectLock, times(2)).unlock();
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
            fail("Expected JposException, got PrinterException");
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void printContent_When105Exception_DoesNotReconnect() throws JposException, InterruptedException {
        //arrange
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDeviceLock.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        when(mockConnectLock.tryLock(printerDeviceLock.getTryLockTimeout(), TimeUnit.SECONDS)).thenReturn(true);
        doThrow(new JposException(JposConst.JPOS_E_DISABLED)).when(mockDeviceListener).waitForOutputToComplete(anyLong(), any(TimeUnit.class));

        //act
        try {
            printerDeviceLock.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            assertEquals(JposConst.JPOS_E_DISABLED, jposException.getErrorCode());
            // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
            verify(mockDynamicPrinter, times(3)).getDevice();
            // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            verify(mockDynamicPrinter, never()).disconnect();
            verify(mockDynamicPrinter, never()).connect();
            // unlock called twice: once from reconnectR5Printer finally, once from printContent finally
            verify(mockConnectLock, times(2)).unlock();
            verify(mockPrinter).clearOutput();
            return;
        } catch (PrinterException printerException) {
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
        doThrow(new JposException(JposConst.JPOS_E_ILLEGAL)).when(mockDeviceListener).waitForOutputToComplete(anyLong(), any(TimeUnit.class));

        //act
        try {
            printerDeviceLock.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            fail("Expected PrinterException, got JposException");
        } catch (PrinterException printerException) {
            // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
            verify(mockDynamicPrinter, times(3)).getDevice();
            // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            // invalid-format path does NOT reconnect
            verify(mockDynamicPrinter, never()).disconnect();
            verify(mockDynamicPrinter, never()).connect();
            // unlock called twice: once from reconnectR5Printer finally, once from printContent finally
            verify(mockConnectLock, times(2)).unlock();
            verify(mockPrinter).clearOutput();
            assertEquals(PrinterError.INVALID_FORMAT, printerException.getDeviceError());
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
        doThrow(new JposException(114, 207)).when(mockDeviceListener).waitForOutputToComplete(anyLong(), any(TimeUnit.class));

        //act
        try {
            printerDeviceLock.printContent(contents, 0);
        }

        //assert
        catch (JposException jposException) {
            fail("Expected PrinterException, got JposException");
        }  catch (PrinterException printerException) {
            // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
            verify(mockDynamicPrinter, times(3)).getDevice();
            // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
            verify(mockPrinter, times(2)).getPhysicalDeviceName();
            verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
            // invalid-format path does NOT reconnect
            verify(mockDynamicPrinter, never()).disconnect();
            verify(mockDynamicPrinter, never()).connect();
            // unlock called twice: once from reconnectR5Printer finally, once from printContent finally
            verify(mockConnectLock, times(2)).unlock();
            verify(mockPrinter).clearOutput();
            assertEquals(PrinterError.INVALID_FORMAT, printerException.getDeviceError());
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

        //act / assert
        assertDoesNotThrow(() -> printerDevice.printContent(contents, 0));

        // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
        verify(mockDynamicPrinter, times(3)).getDevice();
        // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter).transactionPrint(0, POSPrinterConst.PTR_TP_TRANSACTION);
        verify(mockPrinter).transactionPrint(0, POSPrinterConst.PTR_TP_NORMAL);
        verify(mockPrinter).clearOutput();
    }

    @Test
    public void printContent_WhenGetIsCheckInsertedFalse_DoesNotWithdrawCheck() throws JposException, PrinterException {
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        printerDevice.setIsCheckInserted(false);

        printerDevice.printContent(contents, 0);

        // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer
        verify(mockDynamicPrinter, times(3)).getDevice();
        // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).clearOutput();
        verify(mockPrinter, never()).beginRemoval(anyInt());
        verify(mockPrinter, never()).endRemoval();
    }

    @Test
    public void printContent_WhenGetIsCheckInsertedTrue_WithdrawsCheck() throws JposException, PrinterException {
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        printerDevice.setIsCheckInserted(true);

        printerDevice.printContent(contents, 0);

        // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer, 4th withdrawCheck sync
        verify(mockDynamicPrinter, times(4)).getDevice();
        // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).clearOutput();
        verify(mockPrinter).beginRemoval(anyInt());
        verify(mockPrinter).endRemoval();
    }

    @Test
    public void printContent_WhenGetIsCheckInsertedTrue_WithdrawCheckThrowsError() throws JposException, PrinterException {
        List<PrinterContent> contents = new ArrayList<>();
        TextContent textContent = new TextContent();
        textContent.setType(ContentType.TEXT);
        contents.add(textContent);
        printerDevice.setDeviceConnected(true);
        when(mockPrinter.getPhysicalDeviceName()).thenReturn("NotR5");
        printerDevice.setIsCheckInserted(true);
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).beginRemoval(anyInt());

        printerDevice.printContent(contents, 0);

        // getDevice: 1st printContent sync, 2nd paperEmptyCheck sync, 3rd reconnectR5Printer, 4th withdrawCheck sync
        verify(mockDynamicPrinter, times(4)).getDevice();
        // getPhysicalDeviceName: 1st paperEmptyCheck, 2nd reconnectR5Printer
        verify(mockPrinter, times(2)).getPhysicalDeviceName();
        verify(mockPrinter, times(2)).transactionPrint(anyInt(), anyInt());
        verify(mockPrinter).clearOutput();
        verify(mockPrinter).beginRemoval(anyInt());
        verify(mockPrinter, never()).endRemoval();
    }

    @Test
    public void withdrawCheck_CallsThrough() throws JposException {
        printerDevice.withdrawCheck();

        verify(mockDynamicPrinter).getDevice();
        verify(mockPrinter).beginRemoval(0);
        verify(mockPrinter).endRemoval();
    }

    @Test
    public void withdrawCheck_beginRemovalThrowsError() throws JposException {
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).beginRemoval(0);

        try {
            printerDevice.withdrawCheck();
        } catch (JposException jposException) {
            verify(mockDynamicPrinter).getDevice();
            verify(mockPrinter).beginRemoval(0);
            verify(mockPrinter, never()).endRemoval();
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void withdrawCheck_endRemovalThrowsError() throws JposException {
        doThrow(new JposException(JposConst.JPOS_E_EXTENDED)).when(mockPrinter).endRemoval();

        try {
            printerDevice.withdrawCheck();
        } catch (JposException jposException) {
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
        verify(mockDeviceListener).statusUpdateOccurred(mockStatusUpdateEvent);
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

    // -------------------------------------------------------------------------
    // forceUnlock() tests
    // -------------------------------------------------------------------------

    /**
     * When forceUnlock() is called and no thread holds the lock (lockOwnerThread == null),
     * it should do nothing — no disconnect, no reconnect — and leave deviceConnected false.
     */
    @Test
    public void forceUnlock_WhenOwnerIsNull_DoesNotDisconnectOrReconnect() throws InterruptedException {
        // arrange — printerDevice has a real ReentrantLock, nobody holds it
        printerDevice.setDeviceConnected(true);
        printerDevice.setAreListenersAttached(true);

        // act
        printerDevice.forceUnlock();

        // assert — immediate state changes
        assertFalse(printerDevice.isConnected());
        assertFalse(printerDevice.getAreListenersAttached());
        assertFalse(printerDevice.getIsLocked());
        // disconnect() should never have been called on the DynamicDevice
        verify(mockDynamicPrinter, never()).disconnect();
    }

    /**
     * When forceUnlock() is called while a worker thread holds the lock,
     * it interrupts that thread, the background thread disconnects the device,
     * then polls until it can acquire the lock to reconnect.
     *
     * The background thread checks lockOwnerThread.get() == owner on entry BEFORE
     * calling disconnect(). We use a latch in the doAnswer to confirm the background
     * thread reached disconnect(), then release the worker.
     */
    @Test
    public void forceUnlock_WhenOwnerIsAlive_InterruptsAndDisconnectsAndReconnects() throws Exception {
        // arrange
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);

        CountDownLatch workerHoldsLock   = new CountDownLatch(1);
        CountDownLatch backgroundStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker     = new CountDownLatch(1);
        AtomicBoolean  workerInterrupted = new AtomicBoolean(false);

        // Signal when background thread actually enters disconnect() (past owner-check)
        doAnswer(invocation -> {
            backgroundStarted.countDown();
            return null;
        }).when(mockDynamicPrinter).disconnect();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> worker = executor.submit(() -> {
            printerDevice.tryLock();
            workerHoldsLock.countDown();
            try {
                releaseWorker.await();      // hold even after interrupt so lockOwnerThread stays set
            } catch (InterruptedException ie) {
                workerInterrupted.set(true);
                // do NOT re-interrupt — wait for explicit release so owner is still valid
                try { releaseWorker.await(); } catch (InterruptedException ignored) {}
            }
            printerDevice.unlock();
        });

        assertTrue(workerHoldsLock.await(2, TimeUnit.SECONDS), "Worker should hold lock within 2s");

        // act
        printerDevice.forceUnlock();

        // Immediate state assertions (on calling thread)
        assertFalse(printerDevice.isConnected());
        assertFalse(printerDevice.getAreListenersAttached());
        assertFalse(printerDevice.getIsLocked());

        // Wait for background thread to enter disconnect() — confirms it passed the owner check
        assertTrue(backgroundStarted.await(3, TimeUnit.SECONDS), "Background thread should start disconnect within 3s");

        // Release the worker — it calls unlock(), freeing connectLock for the reconnect poll
        releaseWorker.countDown();
        worker.get(3, TimeUnit.SECONDS);
        executor.shutdown();

        // Give background thread time to finish reconnect poll
        Thread.sleep(3000);

        verify(mockDynamicPrinter, atLeastOnce()).disconnect();
        verify(mockDynamicPrinter, atLeastOnce()).connect();
        assertTrue(workerInterrupted.get(), "Worker thread should have been interrupted");
    }

    /**
     * When the background disconnect throws an exception, forceUnlock() absorbs it
     * and still proceeds to attempt reconnect.
     */
    @Test
    public void forceUnlock_WhenDisconnectThrows_StillAttemptsReconnect() throws Exception {
        // arrange
        CountDownLatch workerHoldsLock   = new CountDownLatch(1);
        CountDownLatch backgroundStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker     = new CountDownLatch(1);

        doAnswer(invocation -> {
            backgroundStarted.countDown();
            throw new RuntimeException("hardware error");
        }).when(mockDynamicPrinter).disconnect();
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.CONNECTED);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> worker = executor.submit(() -> {
            printerDevice.tryLock();
            workerHoldsLock.countDown();
            try {
                releaseWorker.await();
            } catch (InterruptedException ie) {
                try { releaseWorker.await(); } catch (InterruptedException ignored) {}
            }
            printerDevice.unlock();
        });

        assertTrue(workerHoldsLock.await(2, TimeUnit.SECONDS));
        printerDevice.forceUnlock();

        assertTrue(backgroundStarted.await(3, TimeUnit.SECONDS), "Background thread should start disconnect within 3s");

        releaseWorker.countDown();
        worker.get(3, TimeUnit.SECONDS);
        executor.shutdown();

        Thread.sleep(3000);

        verify(mockDynamicPrinter, atLeastOnce()).disconnect();
        // reconnect should still have been attempted even after the failed disconnect
        verify(mockDynamicPrinter, atLeastOnce()).connect();
    }

    /**
     * When the lock owner changes between forceUnlock() snapshotting it and the background
     * thread executing its owner check, the background thread must skip disconnect to avoid
     * disrupting the new legitimate owner (e.g. @Scheduled connect()).
     *
     * Achieved by releasing the lock BEFORE forceUnlock() is called so the background
     * thread sees a different (null) owner immediately on entry.
     */
    @Test
    public void forceUnlock_WhenOwnerChangesBeforeBackgroundRuns_SkipsDisconnect() throws Exception {
        // arrange — use a separate PrinterDevice with its own real lock
        PrinterDevice device = new PrinterDevice(mockDynamicPrinter, mockDeviceListener);

        CountDownLatch workerHoldsLock  = new CountDownLatch(1);
        CountDownLatch workerReleased   = new CountDownLatch(1);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> worker = executor.submit(() -> {
            device.tryLock();
            workerHoldsLock.countDown();
            // Release immediately — simulates worker finishing just before background thread runs
            device.unlock();
            workerReleased.countDown();
        });

        assertTrue(workerHoldsLock.await(2, TimeUnit.SECONDS));
        // Wait for worker to actually release so lockOwnerThread is null when forceUnlock snapshots it
        assertTrue(workerReleased.await(2, TimeUnit.SECONDS));

        // act — owner is now null, so forceUnlock should take the early-exit path
        device.forceUnlock();
        worker.get(2, TimeUnit.SECONDS);
        executor.shutdown();

        Thread.sleep(500);

        // Background thread should never have been spawned — disconnect must not be called
        verify(mockDynamicPrinter, never()).disconnect();
    }

    /**
     * When forceUnlock()'s reconnect attempt fails (connect returns NOT_CONNECTED),
     * it logs the failure and leaves reconnect to the @Scheduled connect() fallback.
     */
    @Test
    public void forceUnlock_WhenReconnectFails_LogsFailureAndReliesOnScheduledConnect() throws Exception {
        // arrange
        CountDownLatch workerHoldsLock   = new CountDownLatch(1);
        CountDownLatch backgroundStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker     = new CountDownLatch(1);

        doAnswer(invocation -> {
            backgroundStarted.countDown();
            return null;
        }).when(mockDynamicPrinter).disconnect();
        when(mockDynamicPrinter.connect()).thenReturn(DynamicDevice.ConnectionResult.NOT_CONNECTED);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> worker = executor.submit(() -> {
            printerDevice.tryLock();
            workerHoldsLock.countDown();
            try {
                releaseWorker.await();
            } catch (InterruptedException ie) {
                try { releaseWorker.await(); } catch (InterruptedException ignored) {}
            }
            printerDevice.unlock();
        });

        assertTrue(workerHoldsLock.await(2, TimeUnit.SECONDS));
        printerDevice.forceUnlock();

        assertTrue(backgroundStarted.await(3, TimeUnit.SECONDS), "Background thread should start disconnect within 3s");

        releaseWorker.countDown();
        worker.get(3, TimeUnit.SECONDS);
        executor.shutdown();

        Thread.sleep(3000);

        verify(mockDynamicPrinter, atLeastOnce()).disconnect();
        verify(mockDynamicPrinter, atLeastOnce()).connect();
        // printer remains not connected because connect() returned NOT_CONNECTED
        assertFalse(printerDevice.isConnected());
    }

    /**
     * forceUnlock() immediately resets isLocked, deviceConnected, and areListenersAttached
     * on the calling thread, regardless of what the background thread does later.
     */
    @Test
    public void forceUnlock_ResetsStateImmediatelyOnCallingThread() throws Exception {
        // arrange — lock is held by a worker so owner != null
        CountDownLatch workerHoldsLock = new CountDownLatch(1);
        CountDownLatch releaseWorker   = new CountDownLatch(1);

        printerDevice.setDeviceConnected(true);
        printerDevice.setAreListenersAttached(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> worker = executor.submit(() -> {
            printerDevice.tryLock();
            workerHoldsLock.countDown();
            try { releaseWorker.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            printerDevice.unlock();
        });

        assertTrue(workerHoldsLock.await(2, TimeUnit.SECONDS));

        // act
        printerDevice.forceUnlock();

        // assert — synchronous state changes happen before background thread completes
        assertFalse(printerDevice.isConnected(),             "deviceConnected must be false immediately");
        assertFalse(printerDevice.getAreListenersAttached(), "areListenersAttached must be false immediately");
        assertFalse(printerDevice.getIsLocked(),             "isLocked must be false immediately");

        releaseWorker.countDown();
        worker.get(2, TimeUnit.SECONDS);
        executor.shutdown();
    }
}
