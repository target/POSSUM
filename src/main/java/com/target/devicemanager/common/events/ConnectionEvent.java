package com.target.devicemanager.common.events;

import java.util.EventObject;

public class ConnectionEvent extends EventObject {

    private static final long serialVersionUID = 1L;
    private final boolean connectionStatus;

    public ConnectionEvent(Object source, boolean connectionStatus) {
        super(source);

        this.connectionStatus = connectionStatus;
    }

    public boolean isConnected() {
        return this.connectionStatus;
    }
}
