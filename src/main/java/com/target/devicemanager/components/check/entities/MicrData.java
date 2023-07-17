package com.target.devicemanager.components.check.entities;

public class MicrData {
    public String account_number;
    public String bank_number;
    public String transit_number;
    public String raw_data;
    public String sequence_number;

    public MicrData() {
        this("", "", "", "", "");
    }

    public MicrData(String account_number, String bank_number, String transit_number, String raw_data, String sequence_number) {
        this.account_number = account_number;
        this.bank_number = bank_number;
        this.transit_number = transit_number;
        this.raw_data = raw_data;
        this.sequence_number = sequence_number;
    }
}
