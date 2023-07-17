package com.target.devicemanager.common.events;

import java.util.EventListener;

public interface ConnectionEventListener extends EventListener {
    void connectionEventOccurred(ConnectionEvent connectionEvent);
}
