package com.target.devicemanager.components.printer.entities;

import jpos.POSPrinterConst;

public enum PrinterStationType {

    RECEIPT_PRINTER(POSPrinterConst.PTR_S_RECEIPT),
    CHECK_PRINTER(POSPrinterConst.PTR_S_SLIP);
    private final int type;

    PrinterStationType(int type) {
        this.type = type;
    }

    public int getValue() {
        return type;
    }

    @Override
    public String toString() {
        return name();
    }
}
