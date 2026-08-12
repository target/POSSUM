package com.target.devicemanager.common.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeviceErrorStatusResponseTest {

    @TempDir
    Path tempDir;

    @Test
    public void loadDeviceErrorStatuses_WhenFileMissing_ReturnsEmptyList() {
        File missing = tempDir.resolve("confirmout.json").toFile();

        List<DeviceErrorStatus> result = DeviceErrorStatusResponse.loadDeviceErrorStatuses(missing);

        assertTrue(result.isEmpty());
    }

    @Test
    public void loadDeviceErrorStatuses_WhenNull_ReturnsEmptyList() {
        List<DeviceErrorStatus> result = DeviceErrorStatusResponse.loadDeviceErrorStatuses(null);

        assertTrue(result.isEmpty());
    }

    @Test
    public void loadDeviceErrorStatuses_WhenMalformedJson_ReturnsEmptyListWithoutThrowing() throws IOException {
        File bad = tempDir.resolve("confirmout.json").toFile();
        Files.writeString(bad.toPath(), "{ this is not valid json ");

        List<DeviceErrorStatus> result = DeviceErrorStatusResponse.loadDeviceErrorStatuses(bad);

        assertTrue(result.isEmpty());
    }

    @Test
    public void loadDeviceErrorStatuses_WhenValidJson_ReturnsADeviceStatusPerTopLevelField() throws IOException {
        File good = tempDir.resolve("confirmout.json").toFile();
        Files.writeString(good.toPath(), "{\"scanner\":{},\"printer\":{}}");

        List<DeviceErrorStatus> result = DeviceErrorStatusResponse.loadDeviceErrorStatuses(good);

        assertEquals(2, result.size());
        assertEquals("scanner", result.get(0).deviceName);
        assertEquals("printer", result.get(1).deviceName);
        assertTrue(result.stream().noneMatch(status -> status.faultPresent));
    }
}
