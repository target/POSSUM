package com.target.devicemanager.common;

import java.util.ArrayList;

public class DeviceAvailabilityResponse {
    public String possumversion;
    public String confirmversion;
    public ArrayList<DeviceConfigResponse> devicelist;

    public DeviceAvailabilityResponse() {
        devicelist = new ArrayList<>();
    }
}

