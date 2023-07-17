package com.target.devicemanager.common;

public class DeviceConfigResponse {
    public String devicename;
    public String vidpid;
    public String usbport;
    public String manufacturer;
    public String model;
    public String config;
    public String firmware;
    public String serialnumber;
    public boolean isFunctional;
    public boolean attached;

    public DeviceConfigResponse(String devicename, String vidpid, String usbport, String manufacturer, String model, String config, String firmware, String serialnumber, boolean isFunctional, boolean attached) {
        this.devicename = devicename;
        this.vidpid = vidpid;
        this.usbport = usbport;
        this.manufacturer = manufacturer;
        this.model = model;
        this.config = config;
        this.firmware = firmware;
        this.serialnumber = serialnumber;
        this.isFunctional = isFunctional;
        this.attached = attached;
    }

    public String toString() {
        return
                "deviceName=" + devicename +
                        ", vidpid=" + vidpid +
                        ", usbport=" + usbport +
                        ", manufacturer=" + manufacturer +
                        ", model=" + model +
                        ", config=" + config +
                        ", firmware=" + firmware +
                        ", serialnumber=" + serialnumber +
                        ", isFunctional=" + isFunctional +
                        ", attached=" + attached ;
    }
}
