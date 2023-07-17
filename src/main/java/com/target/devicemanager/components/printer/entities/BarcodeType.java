package com.target.devicemanager.components.printer.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.target.devicemanager.common.EnumDeserializer;
import jpos.POSPrinterConst;

import java.util.Arrays;

public enum BarcodeType {
    UPCA(POSPrinterConst.PTR_BCS_UPCA),
    UPCE(POSPrinterConst.PTR_BCS_UPCE),
    JAN8(POSPrinterConst.PTR_BCS_JAN8),
    EAN8(POSPrinterConst.PTR_BCS_EAN8),
    JAN13(POSPrinterConst.PTR_BCS_JAN13),
    EAN13(POSPrinterConst.PTR_BCS_EAN13),
    TF(POSPrinterConst.PTR_BCS_TF),
    ITF(POSPrinterConst.PTR_BCS_ITF),
    CODABAR(POSPrinterConst.PTR_BCS_Codabar),
    CODE_39(POSPrinterConst.PTR_BCS_Code39),
    CODE_93(POSPrinterConst.PTR_BCS_Code93),
    CODE_128(POSPrinterConst.PTR_BCS_Code128),
    UPCA_S(POSPrinterConst.PTR_BCS_UPCA_S),
    UPCE_S(POSPrinterConst.PTR_BCS_UPCE_S),
    UPCD1(POSPrinterConst.PTR_BCS_UPCD1),
    UPCD2(POSPrinterConst.PTR_BCS_UPCD2),
    UPCD3(POSPrinterConst.PTR_BCS_UPCD3),
    UPCD4(POSPrinterConst.PTR_BCS_UPCD4),
    UPCD5(POSPrinterConst.PTR_BCS_UPCD5),
    EAN8_S(POSPrinterConst.PTR_BCS_EAN8_S),
    EAN13_S(POSPrinterConst.PTR_BCS_EAN13_S),
    EAN128(POSPrinterConst.PTR_BCS_EAN128),
    OCRA(POSPrinterConst.PTR_BCS_OCRA),
    OCRB(POSPrinterConst.PTR_BCS_OCRB),
    CODE_128_PARSED(POSPrinterConst.PTR_BCS_Code128_Parsed),
    RSS14(POSPrinterConst.PTR_BCS_RSS14),
    RSS_EXPANDED(POSPrinterConst.PTR_BCS_RSS_EXPANDED),
    GS1DATABAR(POSPrinterConst.PTR_BCS_GS1DATABAR),
    GS1DATABAR_E(POSPrinterConst.PTR_BCS_GS1DATABAR_E),
    GS1DATABAR_S(POSPrinterConst.PTR_BCS_GS1DATABAR_S),
    GS1DATABAR_E_S(POSPrinterConst.PTR_BCS_GS1DATABAR_E),
    PDF417(POSPrinterConst.PTR_BCS_PDF417),
    MAXICODE(POSPrinterConst.PTR_BCS_MAXICODE),
    DATAMATRIX(POSPrinterConst.PTR_BCS_DATAMATRIX),
    QRCODE(POSPrinterConst.PTR_BCS_QRCODE),
    UQRCODE(POSPrinterConst.PTR_BCS_UQRCODE),
    AZTEC(POSPrinterConst.PTR_BCS_AZTEC),
    UPDF417(POSPrinterConst.PTR_BCS_UPDF417),
    OTHER(POSPrinterConst.PTR_BCS_OTHER);

    private final int uposBarcodeType;

    BarcodeType(int barCodeType) {
        uposBarcodeType = barCodeType;
    }

    @JsonCreator
    public static BarcodeType fromValue(String valueToDeserialize) {
        BarcodeType type = EnumDeserializer.deserialize(Arrays.asList(values()), valueToDeserialize);

        return type == null ? OTHER : type;
    }

    public int getValue() {
        return uposBarcodeType;
    }

    @Override
    public String toString(){
        return name();
    }
}
