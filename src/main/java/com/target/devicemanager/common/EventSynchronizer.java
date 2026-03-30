package com.target.devicemanager.common;

import jpos.JposConst;
import jpos.JposException;
import jpos.events.ErrorEvent;
import jpos.events.JposEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EventSynchronizer {

    private JposEvent lastEvent;
    private final Phaser phaser;
    private final AtomicInteger waitingPhase;
    private final AtomicBoolean areEventsActive;
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSynchronizer.class);
    private static final StructuredEventLogger log = StructuredEventLogger.of(StructuredEventLogger.getCommonServiceName(), "EventSynchronizer", LOGGER);


    public EventSynchronizer(Phaser phaser) {
        if (phaser == null) {
            throw new IllegalArgumentException("phaser cannot be null");
        }

        int registeredParties = phaser.getRegisteredParties();
        if (registeredParties != 1) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("phaser must have 1 and only 1 registered parties");
            log.failure("Phaser has " + registeredParties + "registered parties, must be 1", 17, illegalArgumentException);
            throw illegalArgumentException;
        }
        this.phaser = phaser;
        int currentPhase = phaser.getPhase();
        waitingPhase = new AtomicInteger(currentPhase);
        areEventsActive = new AtomicBoolean(false);
    }

    public synchronized void startEventSynchronizer() {
        lastEvent = null;
        waitingPhase.set(phaser.getPhase());
        areEventsActive.set(true);
    }

    public synchronized void triggerEvent(JposEvent event) {
        if (!areEventsActive.get()) {
            return;
        }
        this.lastEvent = event;
        phaser.arrive();
    }

    //Do not make this synchronized, the phaser guarantees this will only trigger when the other threads send us data
    public JposEvent waitForEvent() {
        phaser.awaitAdvance(waitingPhase.get());
        JposEvent tmpEvent;
        synchronized (this) {
            areEventsActive.set(false);
            tmpEvent = lastEvent;
        }
        return tmpEvent;
    }

    // Waits for an event up to the specified timeout.
    public JposEvent waitForEvent(long timeout, TimeUnit unit) throws JposException {
        try {
            phaser.awaitAdvanceInterruptibly(waitingPhase.get(), timeout, unit);
        } catch (TimeoutException timeoutException) {
            synchronized (this) {
                areEventsActive.set(false);
            }
            log.failure("waitForEvent timed out after " + unit.toMillis(timeout) + "ms", 18, null);
            throw new JposException(JposConst.JPOS_E_TIMEOUT);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            synchronized (this) {
                areEventsActive.set(false);
            }
            log.failure("waitForEvent interrupted", 17, interruptedException);
            throw new JposException(JposConst.JPOS_E_TIMEOUT);
        }
        JposEvent tmpEvent;
        synchronized (this) {
            areEventsActive.set(false);
            tmpEvent = lastEvent;
        }
        return tmpEvent;
    }

    public synchronized void stopWaitingForEvent() {
        this.lastEvent = new ErrorEvent(this, JposConst.JPOS_E_TIMEOUT, 0, 0, 0);
        phaser.arrive();
    }
}
