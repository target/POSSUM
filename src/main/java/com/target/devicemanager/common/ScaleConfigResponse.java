package com.target.devicemanager.common;

public class ScaleConfigResponse extends DeviceConfigResponse {
    public Boolean calibrated;
    public Integer calibrated_count;
    public Boolean has_remote_display;

    public  ScaleConfigResponse(String devicename, String vidpid, String usbport, String manufacturer, String model, String config, String firmware, String serialnumber,boolean isFunctional, boolean attached, Boolean calibrated, Integer calibrated_count, Boolean has_remote_display ) {
        super(devicename, vidpid, usbport, manufacturer, model, config, firmware, serialnumber, isFunctional, attached);
        this.calibrated = calibrated;
        this.calibrated_count = calibrated_count;
        this.has_remote_display = has_remote_display;
    }

    public String toString() {
        return
                super.toString() +
                        ", calibrated=" + calibrated +
                        ", calibrated_count=" + calibrated_count +
                        ", has_remote_display=" + has_remote_display;
    }
}
