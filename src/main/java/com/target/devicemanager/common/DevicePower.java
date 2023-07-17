package com.target.devicemanager.common;

import jpos.BaseJposControl;
import jpos.JposConst;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DevicePower {

    public DevicePower(){
    }

    //Some implementations that need to use reflection because the BaseJposControl should
    //have included these for all devices but didn't
    public void enablePowerNotification(BaseJposControl device) {
        try {
            Method getPowerNotifyInstanceMethod = device.getClass().getMethod("getPowerNotify");
            Method getCapPowerNotifyInstanceMethod = device.getClass().getMethod("getCapPowerReporting");
            Method setPowerNotifyInstanceMethod = device.getClass().getMethod("setPowerNotify", int.class);

            int powerReportingCapability = (int) getCapPowerNotifyInstanceMethod.invoke(device);
            if (powerReportingCapability == JposConst.JPOS_PR_NONE) {
                return;
            }

            int powerNotify = (int) getPowerNotifyInstanceMethod.invoke(device);
            boolean isPowerNotifyEnabled = (powerNotify == JposConst.JPOS_PN_ENABLED);
            if (!isPowerNotifyEnabled) {
                setPowerNotifyInstanceMethod.invoke(device, JposConst.JPOS_PN_ENABLED);
            }

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            //Nothing to do. We can't enable power notification
        }
    }

    public int getPowerState(BaseJposControl device) {
        try {
            Method getPowerStateInstanceMethod = device.getClass().getMethod("getPowerState");
            Method getCapPowerNotifyInstanceMethod = device.getClass().getMethod("getCapPowerReporting");

            int powerReportingCapability = (int) getCapPowerNotifyInstanceMethod.invoke(device);
            if (powerReportingCapability == JposConst.JPOS_PR_NONE) {
                return JposConst.JPOS_PS_UNKNOWN;
            }

            return (int) getPowerStateInstanceMethod.invoke(device);

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return JposConst.JPOS_PS_UNKNOWN;
        }
    }
}
