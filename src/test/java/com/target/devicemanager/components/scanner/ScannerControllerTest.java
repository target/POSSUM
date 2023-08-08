package com.target.devicemanager.components.scanner;

import com.target.devicemanager.common.entities.DeviceError;
import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.scanner.entities.Barcode;
import com.target.devicemanager.components.scanner.entities.ScannerException;
import com.target.devicemanager.components.scanner.entities.ScannerType;
import jpos.ScannerConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScannerControllerTest {

    private ScannerController scannerController;

    @Mock
    private ScannerManager mockScannerManager;

    @BeforeEach
    public void testInitialize() {
        scannerController = new ScannerController(mockScannerManager);
    }

    @Test
    public void ctor_WhenScannerManagerIsNull_ThrowsException() {
        try {
            new ScannerController(null);
        } catch (IllegalArgumentException iae) {
            assertEquals("scannerManager cannot be null", iae.getMessage());
            return;
        }

        fail("Expected Exception, but got none");
    }

    @Test
    public void ctor_WhenScannerManagerIsNew_DoesNotThrowException() {
        try {
            new ScannerController(mockScannerManager);
        } catch (Exception exception) {
            fail("Existing Manager Argument should not result in an Exception");
        }
    }

    @Test
    public void getScannerData_WhenDataIsNull_CallBothScanners() throws ScannerException {
        //arrange
        Barcode expected = new Barcode("data", ScannerConst.SCAN_SDT_UPCA, "HANDHELD");
        when(mockScannerManager.getData(any())).thenReturn(expected);

        //act
        Barcode actual = scannerController.getScannerData(null);

        //assert
        assertEquals(expected, actual);
        verify(mockScannerManager).getData(ScannerType.BOTH);
        verify(mockScannerManager, never()).getData(ScannerType.HANDHELD);
        verify(mockScannerManager, never()).getData(ScannerType.FLATBED);
    }

    @Test
    public void getScannerData_WhenDataIsHandheld_CallHandheldScanner() throws ScannerException {
        //arrange
        Barcode expected = new Barcode("data", ScannerConst.SCAN_SDT_UPCA, "HANDHELD");
        when(mockScannerManager.getData(any())).thenReturn(expected);

        //act
        Barcode actual = scannerController.getScannerData(ScannerType.HANDHELD);

        //assert
        assertEquals(expected, actual);
        verify(mockScannerManager, never()).getData(ScannerType.BOTH);
        verify(mockScannerManager).getData(ScannerType.HANDHELD);
        verify(mockScannerManager, never()).getData(ScannerType.FLATBED);
    }


    @Test
    public void getScannerData_WhenDataIsFlatbed_CallFlatbedScanner() throws ScannerException {
        //arrange
        Barcode expected = new Barcode("data", ScannerConst.SCAN_SDT_UPCA, "FLATBED");
        when(mockScannerManager.getData(any())).thenReturn(expected);

        //act
        Barcode actual = scannerController.getScannerData(ScannerType.FLATBED);

        //assert
        assertEquals(expected, actual);
        verify(mockScannerManager, never()).getData(ScannerType.BOTH);
        verify(mockScannerManager, never()).getData(ScannerType.HANDHELD);
        verify(mockScannerManager).getData(ScannerType.FLATBED);
    }

    @Test
    public void getScannerData_WhenBothThrowsError() throws DeviceException {
        //arrange
        doThrow(new ScannerException(DeviceError.DEVICE_BUSY)).when(mockScannerManager).getData(ScannerType.BOTH);

        //act
        try {
            scannerController.getScannerData(null);
        }

        //assert
        catch(ScannerException scannerException) {
            verify(mockScannerManager).getData(ScannerType.BOTH);
            assertEquals(DeviceError.DEVICE_BUSY, scannerException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getScannerData_WhenNotBothThrowsError() throws DeviceException {
        //arrange
        doThrow(new ScannerException(DeviceError.DEVICE_BUSY)).when(mockScannerManager).getData(ScannerType.HANDHELD);

        //act
        try {
            scannerController.getScannerData(ScannerType.HANDHELD);
        }

        //assert
        catch(ScannerException scannerException) {
            verify(mockScannerManager).getData(ScannerType.HANDHELD);
            assertEquals(DeviceError.DEVICE_BUSY, scannerException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void cancelScanRequest_CallsThroughToManager() throws ScannerException {
        //arrange

        //act
        scannerController.cancelScanRequest();

        //assert
        verify(mockScannerManager).cancelScanRequest();
    }

    @Test
    public void cancelScanRequest_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new ScannerException(DeviceError.DEVICE_BUSY)).when(mockScannerManager).cancelScanRequest();

        //act
        try {
            scannerController.cancelScanRequest();
        }

        //assert
        catch(ScannerException scannerException) {
            verify(mockScannerManager).cancelScanRequest();
            assertEquals(DeviceError.DEVICE_BUSY, scannerException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }

    @Test
    public void getHealth_WhenTypeIsNull_ReturnsBothScannersHealthFromManager() {
        //arrange
        DeviceHealthResponse expectedHandheld = new DeviceHealthResponse("handheld", DeviceHealth.READY);
        DeviceHealthResponse expectedFlatbed = new DeviceHealthResponse("flatbed", DeviceHealth.READY);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(expectedHandheld);
        expectedList.add(expectedFlatbed);
        ResponseEntity<List<DeviceHealthResponse>> expected = ResponseEntity.ok(expectedList);
        when(mockScannerManager.getHealth(any())).thenReturn(expectedList);

        //act
        ResponseEntity<List<DeviceHealthResponse>> actual = scannerController.getHealth(null);

        //assert
        assertEquals(expected, actual);
        verify(mockScannerManager).getHealth(ScannerType.BOTH);
        verify(mockScannerManager, never()).getHealth(ScannerType.HANDHELD);
        verify(mockScannerManager, never()).getHealth(ScannerType.FLATBED);
    }

    @Test
    public void getHealth_WhenTypeIsHandheld_ReturnsHandheldScannerHealthFromManager() {
        //arrange
        DeviceHealthResponse expectedHandheld = new DeviceHealthResponse("handheld", DeviceHealth.READY);
        DeviceHealthResponse expectedFlatbed = new DeviceHealthResponse("flatbed", DeviceHealth.READY);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(expectedHandheld);
        expectedList.add(expectedFlatbed);
        ResponseEntity<List<DeviceHealthResponse>> expected = ResponseEntity.ok(expectedList);
        when(mockScannerManager.getHealth(any())).thenReturn(expectedList);

        //act
        ResponseEntity<List<DeviceHealthResponse>> actual = scannerController.getHealth(ScannerType.HANDHELD);

        //assert
        assertEquals(expected, actual);
        verify(mockScannerManager, never()).getHealth(ScannerType.BOTH);
        verify(mockScannerManager).getHealth(ScannerType.HANDHELD);
        verify(mockScannerManager, never()).getHealth(ScannerType.FLATBED);
    }

    @Test
    public void getHealth_WhenTypeIsFlatbed_ReturnsFlatbedScannerHealthFromManager() {
        //arrange
        DeviceHealthResponse expectedHandheld = new DeviceHealthResponse("handheld", DeviceHealth.READY);
        DeviceHealthResponse expectedFlatbed = new DeviceHealthResponse("flatbed", DeviceHealth.READY);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(expectedHandheld);
        expectedList.add(expectedFlatbed);
        ResponseEntity<List<DeviceHealthResponse>> expected = ResponseEntity.ok(expectedList);
        when(mockScannerManager.getHealth(any())).thenReturn(expectedList);

        //act
        ResponseEntity<List<DeviceHealthResponse>> actual = scannerController.getHealth(ScannerType.FLATBED);

        //assert
        assertEquals(expected, actual);
        verify(mockScannerManager, never()).getHealth(ScannerType.BOTH);
        verify(mockScannerManager, never()).getHealth(ScannerType.HANDHELD);
        verify(mockScannerManager).getHealth(ScannerType.FLATBED);
    }

    @Test
    public void getStatus_ReturnsStatusFromManager() {
        //arrange
        DeviceHealthResponse expectedHandheld = new DeviceHealthResponse("handheld", DeviceHealth.READY);
        DeviceHealthResponse expectedFlatbed = new DeviceHealthResponse("flatbed", DeviceHealth.READY);
        List<DeviceHealthResponse> expectedList = new ArrayList<>();
        expectedList.add(expectedHandheld);
        expectedList.add(expectedFlatbed);
        ResponseEntity<List<DeviceHealthResponse>> expected = ResponseEntity.ok(expectedList);
        when(mockScannerManager.getStatus()).thenReturn(expectedList);

        //act
        ResponseEntity<List<DeviceHealthResponse>> actual = scannerController.getStatus();

        //assert
        assertEquals(expected, actual);
        verify(mockScannerManager).getStatus();
    }

    @Test
    public void reconnect_CallsThroughToScannerManager() throws DeviceException {
        //arrange

        //act
        try {
            scannerController.reconnect();
        } catch (Exception exception) {
            fail("lineDisplayController.reconnect() should not result in an Exception");
        }

        //assert
        verify(mockScannerManager).reconnectScanners();
    }

    @Test
    public void reconnect_WhenThrowsError() throws DeviceException {
        //arrange
        doThrow(new DeviceException(DeviceError.DEVICE_BUSY)).when(mockScannerManager).reconnectScanners();

        //act
        try {
            scannerController.reconnect();
        }

        //assert
        catch(DeviceException deviceException) {
            verify(mockScannerManager).reconnectScanners();
            assertEquals(DeviceError.DEVICE_BUSY, deviceException.getDeviceError());
            return;
        }
        fail("Expected Exception, but got none.");
    }
}
