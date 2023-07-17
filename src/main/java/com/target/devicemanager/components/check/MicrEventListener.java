package com.target.devicemanager.components.check;

import com.target.devicemanager.components.check.entities.MicrDataEvent;
import com.target.devicemanager.components.check.entities.MicrErrorEvent;

public interface MicrEventListener {
    void micrDataEventOccurred(MicrDataEvent micrDataEvent);
    void micrErrorEventOccurred(MicrErrorEvent micrErrorEvent);
}
