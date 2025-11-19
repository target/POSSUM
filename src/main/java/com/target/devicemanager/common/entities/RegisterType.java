package com.target.devicemanager.common.entities;

public enum RegisterType {
    SELF_CHECKOUT_ATM_R5("SELF_CHECKOUT_ATM_R5"),
    SELF_CHECKOUT_ATM("SELF_CHECKOUT_ATM"),
    SERVICE_DESK("SERVICE_DESK"),
    ELECTRONICS("ELECTRONICS"),
    PHARMACY("PHARMACY"),
    STARBUCKS("STARBUCKS"),
    TARGET_CAFE("TARGET_CAFE"),
    OPTICAL("OPTICAL"),
    SELF_CHECKOUT_CASH_DRAWER("SELF_CHECKOUT_CASH_DRAWER"),
    FRONT_LANE_SELF_CHECKOUT("FRONT_LANE_SELF_CHECKOUT"),
    LIQUOR_STORE("LIQUOR_STORE"),
    SELF_CHECKOUT_SLIMLINE("SELF_CHECKOUT_SLIMLINE"),
    ULTA("ULTA"),
    JEWELRY("JEWELRY"),
    CASH_REGISTER("CASH_REGISTER"),
    CUSTOM("CUSTOM");

    private final String displayName;

    RegisterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
