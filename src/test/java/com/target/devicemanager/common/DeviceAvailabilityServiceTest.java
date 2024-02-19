package com.target.devicemanager.common;

import com.target.devicemanager.common.entities.DeviceException;
import com.target.devicemanager.common.entities.DeviceHealth;
import com.target.devicemanager.common.entities.DeviceHealthResponse;
import com.target.devicemanager.components.linedisplay.LineDisplayManager;
import com.target.devicemanager.components.printer.PrinterManager;
import com.target.devicemanager.components.scale.ScaleManager;
import com.target.devicemanager.components.scanner.ScannerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceAvailabilityServiceTest {

    DeviceAvailabilitySingleton deviceAvailabilitySingleton;

    @Mock
    private DeviceAvailabilityService deviceAvailabilityService;
    @Mock
    private PrinterManager mockPrinterManager;
    @Mock
    private ScaleManager mockScaleManager;
    @Mock
    private ScannerManager mockScannerManager;
    @Mock
    private LineDisplayManager mockLineDisplayManager;

    @BeforeEach
    void setUp() {
        deviceAvailabilityService = new DeviceAvailabilityService();
        deviceAvailabilitySingleton = DeviceAvailabilitySingleton.getDeviceAvailabilitySingleton();
    }

    @Test
    void Test_findDevStatus_Printer() {
        //arrange
        DeviceHealthResponse devReady = new DeviceHealthResponse("printer", DeviceHealth.READY);

        //act
        deviceAvailabilitySingleton.setPrinterManager(mockPrinterManager);

        //assert
        when(deviceAvailabilitySingleton.getPrinterManager().getStatus()).thenReturn(devReady);
        assertEquals(DeviceHealth.READY, deviceAvailabilityService.findDevStatus("printer"));
    }

    @Test
    void Test_findDevStatus_Scale() {
        //arrange
        DeviceHealthResponse devReady = new DeviceHealthResponse("scale", DeviceHealth.READY);

        //act
        deviceAvailabilitySingleton.setScaleManager(mockScaleManager);

        //assert
        when(deviceAvailabilitySingleton.getScaleManager().getStatus()).thenReturn(devReady);
        assertEquals(DeviceHealth.READY, deviceAvailabilityService.findDevStatus("scale"));
    }

    @Test
    void Test_findDevStatus_FlatbedScanner() {
        //arrange
        List<DeviceHealthResponse> devReady = new ArrayList<>();
        devReady.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        devReady.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        deviceAvailabilitySingleton.setScannerManager(mockScannerManager);
        when(deviceAvailabilitySingleton.getScannerManager().getStatus()).thenReturn(devReady);

        //act
        DeviceHealth actual = deviceAvailabilityService.findDevStatus("flatbedscanner");
        //assert
        assertEquals(DeviceHealth.READY, actual);
    }

    @Test
    void Test_findDevStatus_HandScanner() {
        //arrange
        List<DeviceHealthResponse> devReady = new ArrayList<>();
        devReady.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        devReady.add(new DeviceHealthResponse("HANDHELD", DeviceHealth.READY));
        deviceAvailabilitySingleton.setScannerManager(mockScannerManager);
        when(deviceAvailabilitySingleton.getScannerManager().getStatus()).thenReturn(devReady);


        //act
        DeviceHealth actual = deviceAvailabilityService.findDevStatus("handscanner");

        //assert
        assertEquals(DeviceHealth.READY, actual);
    }

    @Test
    void Test_findDevStatus_HandheldScanner_missing() {
        //arrange
        List<DeviceHealthResponse> devReady = new ArrayList<>();
        devReady.add(new DeviceHealthResponse("FLATBED", DeviceHealth.READY));
        deviceAvailabilitySingleton.setScannerManager(mockScannerManager);
        when(deviceAvailabilitySingleton.getScannerManager().getStatus()).thenReturn(devReady);

        //act
        DeviceHealth actual = deviceAvailabilityService.findDevStatus("handscanner");
        //assert
        assertEquals(DeviceHealth.NOTREADY, actual);
    }

    @Test
    void Test_findDevStatus_LineDisplay() {
        //arrange
        DeviceHealthResponse devReady = new DeviceHealthResponse("linedisplay", DeviceHealth.READY);

        //act
        deviceAvailabilitySingleton.setLineDisplayManager(mockLineDisplayManager);

        //assert
        when(deviceAvailabilitySingleton.getLineDisplayManager().getStatus()).thenReturn(devReady);
        assertEquals(DeviceHealth.READY, deviceAvailabilityService.findDevStatus("linedisplay"));
    }

    @Test
    void Test_findDevStatus_UnknownDevice() throws DeviceException {
        //arrange

        //act

        //assert
        assertEquals(DeviceHealth.NOTREADY, deviceAvailabilityService.findDevStatus("UNKNOWN"));
    }

    @Test
    void Test_getAvailableDevices_scale_null_null_null() {
        String confirmout_loc = "src/test/resources/scale_callibrated_null_callibration_null_has_remore_display_null.json";
        DeviceAvailabilityResponse response =   deviceAvailabilityService.getAvailableDevices(confirmout_loc);

        String respString = response.devicelist.toString();
        assertTrue(respString.contains("calibrated=null"));
        assertTrue(respString.contains("calibrated_count=null"));
        assertTrue(respString.contains("has_remote_display=null"));
    }

    @Test
    void Test_getAvailableDevices_scale_true_1_null() {
        String confirmout_loc = "src/test/resources/scale_callibrated_true_callibration_1_has_remore_display_null.json";
        DeviceAvailabilityResponse response =   deviceAvailabilityService.getAvailableDevices(confirmout_loc);

        String respString = response.devicelist.toString();
        assertTrue(respString.contains("calibrated=true"));
        assertTrue(respString.contains("calibrated_count=1"));
        assertTrue(respString.contains("has_remote_display=null"));
    }

    @Test
    void Test_getAvailableDevices_scale_false_2_null() {
        String confirmout_loc = "src/test/resources/scale_callibrated_false_callibration_2_has_remore_display_null.json";
        DeviceAvailabilityResponse response =   deviceAvailabilityService.getAvailableDevices(confirmout_loc);

        String respString = response.devicelist.toString();
        assertTrue(respString.contains("calibrated=false"));
        assertTrue(respString.contains("calibrated_count=2"));
        assertTrue(respString.contains("has_remote_display=null"));
    }

    @Test
    void Test_getAvailableDevices_scale_null_null_true() {
        String confirmout_loc = "src/test/resources/scale_calibrated_null_callibration_null_has_remote_display_true.json";
        DeviceAvailabilityResponse response =   deviceAvailabilityService.getAvailableDevices(confirmout_loc);

        String respString = response.devicelist.toString();
        assertTrue(respString.contains("calibrated=null"));
        assertTrue(respString.contains("calibrated_count=null"));
        assertTrue(respString.contains("has_remote_display=true"));
    }

    @Test
    void Test_getAvailableDevices_scale_null_null_false() {
        String confirmout_loc = "src/test/resources/scale_calibrated_null_callibration_null_has_remote_display_false.json";
        DeviceAvailabilityResponse response =   deviceAvailabilityService.getAvailableDevices(confirmout_loc);

        String respString = response.devicelist.toString();
        assertTrue(respString.contains("calibrated=null"));
        assertTrue(respString.contains("calibrated_count=null"));
        assertTrue(respString.contains("has_remote_display=false"));
    }
}
