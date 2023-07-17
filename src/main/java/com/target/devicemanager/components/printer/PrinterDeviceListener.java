package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.DeviceListener;
import com.target.devicemanager.common.EventSynchronizer;
import jpos.JposConst;
import jpos.POSPrinterConst;

public class PrinterDeviceListener extends DeviceListener {

    public PrinterDeviceListener(EventSynchronizer eventSynchronizer) {
        super(eventSynchronizer);
    }

    @Override
    protected boolean isFailureStatus(int status) {
        switch (status) {
            case POSPrinterConst.PTR_SUE_COVER_OK:
            case POSPrinterConst.PTR_SUE_JRN_CARTRIDGE_OK:
            case POSPrinterConst.PTR_SUE_JRN_COVER_OK:
            case POSPrinterConst.PTR_SUE_JRN_PAPEROK:
            case POSPrinterConst.PTR_SUE_REC_CARTRIDGE_OK:
            case POSPrinterConst.PTR_SUE_REC_COVER_OK:
            case POSPrinterConst.PTR_SUE_REC_PAPEROK:
            case POSPrinterConst.PTR_SUE_SLP_CARTRIDGE_OK:
            case POSPrinterConst.PTR_SUE_SLP_COVER_OK:
            case POSPrinterConst.PTR_SUE_SLP_PAPEROK:
            case POSPrinterConst.PTR_SUE_SLP_EMPTY:
            case JposConst.JPOS_PS_ONLINE:
                return false;
            default:
                return true;
        }
    }
}
