package com.target.devicemanager.components.scanner.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.target.devicemanager.common.EnumDeserializer;
import jpos.ScannerConst;

import java.util.Arrays;

public enum BarcodeType {
    UPCA(ScannerConst.SCAN_SDT_UPCA),
    UPCA_S(ScannerConst.SCAN_SDT_UPCA_S),
    UPCE(ScannerConst.SCAN_SDT_UPCE),
    UPCE_S(ScannerConst.SCAN_SDT_UPCE_S),
    UPCD1(ScannerConst.SCAN_SDT_UPCD1),
    UPCD2(ScannerConst.SCAN_SDT_UPCD2),
    UPCD3(ScannerConst.SCAN_SDT_UPCD3),
    UPCD4(ScannerConst.SCAN_SDT_UPCD4),
    UPCD5(ScannerConst.SCAN_SDT_UPCD5),
    EAN8(ScannerConst.SCAN_SDT_EAN8),
    JAN8(ScannerConst.SCAN_SDT_JAN8),
    EAN8_S(ScannerConst.SCAN_SDT_EAN8_S),
    EAN13(ScannerConst.SCAN_SDT_EAN13),
    JAN13(ScannerConst.SCAN_SDT_JAN13),
    EAN13_S(ScannerConst.SCAN_SDT_EAN13_S),
    EAN128(ScannerConst.SCAN_SDT_EAN128),
    TWO_OF_FIVE(ScannerConst.SCAN_SDT_TF),
    INTERLEAVED_TWO_OF_FIVE(ScannerConst.SCAN_SDT_ITF),
    CODABAR(ScannerConst.SCAN_SDT_Codabar),
    CODE39(ScannerConst.SCAN_SDT_Code39),
    CODE93(ScannerConst.SCAN_SDT_Code93),
    CODE128(ScannerConst.SCAN_SDT_Code128),
    OCRA(ScannerConst.SCAN_SDT_OCRA),
    OCRB(ScannerConst.SCAN_SDT_OCRB),
    RSS14(ScannerConst.SCAN_SDT_RSS14),
    RSS_EXPANDED(ScannerConst.SCAN_SDT_RSS_EXPANDED),
    GS1DATABAR(ScannerConst.SCAN_SDT_GS1DATABAR),
    GS1DATABAR_E(ScannerConst.SCAN_SDT_GS1DATABAR_E),
    CCA(ScannerConst.SCAN_SDT_CCA),
    CCB(ScannerConst.SCAN_SDT_CCB),
    CCC(ScannerConst.SCAN_SDT_CCC),
    PDF417(ScannerConst.SCAN_SDT_PDF417),
    MAXICODE(ScannerConst.SCAN_SDT_MAXICODE),
    DATAMATRIX(ScannerConst.SCAN_SDT_DATAMATRIX),
    QRCODE(ScannerConst.SCAN_SDT_QRCODE),
    UQRCODE(ScannerConst.SCAN_SDT_UQRCODE),
    AZTEC(ScannerConst.SCAN_SDT_AZTEC),
    UPDF417(ScannerConst.SCAN_SDT_UPDF417),
    OTHER(ScannerConst.SCAN_SDT_OTHER),
    UNKNOWN(ScannerConst.SCAN_SDT_UNKNOWN);

    private final int scannerBarcodeType;

    BarcodeType(int barCodeType) {
        scannerBarcodeType = barCodeType;
    }

    @JsonCreator
    public static BarcodeType fromValue(String valueToDeserialize) {
        BarcodeType type = EnumDeserializer.deserialize(Arrays.asList(values()), valueToDeserialize);
        return type == null ? UNKNOWN : type;
    }

    public static BarcodeType fromInt(int id) {
        for (BarcodeType barcodeType : values()) {
            if (barcodeType.getValue() == id) {
                return barcodeType;
            }
        }
        return BarcodeType.UNKNOWN;
    }

    public int getValue() {
        return scannerBarcodeType;
    }

    @Override
    public String toString() {
        return name();
    }
}
