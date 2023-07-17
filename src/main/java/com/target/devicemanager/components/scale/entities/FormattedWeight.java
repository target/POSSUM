package com.target.devicemanager.components.scale.entities;

import java.io.Serializable;

public class FormattedWeight implements Serializable{
    private static final long serialVersionUID = 1L;
    private static final String WEIGHT_ERROR_STRING = "-.--";
    private static final int THOUSANDTHS_CONVERSION_FACTOR = 1000;
    private static final int THOUSANDTHS_TO_HUNDREDTHS_ROUNDING_FACTOR = 10;

    public final String weight;

    public FormattedWeight() {
        this.weight = WEIGHT_ERROR_STRING;
    }

    public FormattedWeight(int weightFromScale) {
        //Just dropping the last digit for now. The scale always is returning 0 there.
        this.weight = (weightFromScale / THOUSANDTHS_CONVERSION_FACTOR) + "." + String.format("%02d", ((weightFromScale % THOUSANDTHS_CONVERSION_FACTOR) / THOUSANDTHS_TO_HUNDREDTHS_ROUNDING_FACTOR));
    }

}
