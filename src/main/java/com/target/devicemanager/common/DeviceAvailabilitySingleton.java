package com.target.devicemanager.common;

import com.target.devicemanager.components.cashdrawer.CashDrawerManager;
import com.target.devicemanager.components.check.MicrManager;
import com.target.devicemanager.components.linedisplay.LineDisplayManager;
import com.target.devicemanager.components.printer.PrinterManager;
import com.target.devicemanager.components.scale.ScaleManager;
import com.target.devicemanager.components.scanner.ScannerManager;

public class DeviceAvailabilitySingleton {

    /**
     * This class holds the single objects created by the device config files to be used when the devices need to be reconnected.
     */
    private static final DeviceAvailabilitySingleton deviceAvailabilitySingleton = new DeviceAvailabilitySingleton();
    private CashDrawerManager cashDrawerManager = null;
    private MicrManager micrManager = null;
    private LineDisplayManager lineDisplayManager = null;
    private PrinterManager printerManager = null;
    private ScaleManager scaleManager = null;
    private ScannerManager scannerManager = null;

    private DeviceAvailabilitySingleton() {
        // do nothing at the moment
    }

    public static DeviceAvailabilitySingleton getDeviceAvailabilitySingleton() {
        return deviceAvailabilitySingleton;
    }

    public CashDrawerManager getCashDrawerManager() {
        return cashDrawerManager;
    }

    public void setCashDrawerManager(CashDrawerManager cashDrawerManager) { this.cashDrawerManager = cashDrawerManager; }





    public MicrManager getMicrManager() {
        return micrManager;
    }

    public void setMicrManager(MicrManager micrManager) {
        this.micrManager = micrManager;
    }

    public LineDisplayManager getLineDisplayManager() {
        return lineDisplayManager;
    }

    public void setLineDisplayManager(LineDisplayManager lineDisplayManager) { this.lineDisplayManager = lineDisplayManager; }

    public PrinterManager getPrinterManager() {
        return printerManager;
    }

    public void setPrinterManager(PrinterManager printerManager) {
        this.printerManager = printerManager;
    }

    public ScaleManager getScaleManager() {
        return scaleManager;
    }

    public void setScaleManager(ScaleManager scaleManager) {
        this.scaleManager = scaleManager;
    }

    public ScannerManager getScannerManager() {
        return scannerManager;
    }

    public void setScannerManager(ScannerManager scannerManager) {
        this.scannerManager = scannerManager;
    }
}