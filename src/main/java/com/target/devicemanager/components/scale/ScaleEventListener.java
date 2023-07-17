package com.target.devicemanager.components.scale;

import com.target.devicemanager.components.scale.entities.WeightErrorEvent;
import com.target.devicemanager.components.scale.entities.WeightEvent;

import java.util.EventListener;

public interface ScaleEventListener extends EventListener {

    void scaleLiveWeightEventOccurred(WeightEvent liveWeightEvent);
    void scaleStableWeightDataEventOccurred(WeightEvent stableWeightEvent);
    void scaleWeightErrorEventOccurred(WeightErrorEvent weightErrorEvent);
}
