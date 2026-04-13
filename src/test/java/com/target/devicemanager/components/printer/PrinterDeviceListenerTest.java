package com.target.devicemanager.components.printer;

import com.target.devicemanager.common.EventSynchronizer;
import jpos.JposConst;
import jpos.JposException;
import jpos.POSPrinterConst;
import jpos.events.OutputCompleteEvent;
import jpos.events.StatusUpdateEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class PrinterDeviceListenerTest {

    @Test
    public void waitForOutputToComplete_WhenFailureStatusArrives_ThrowsJposException() throws Exception {
        PrinterDeviceListener listener = new PrinterDeviceListener(new EventSynchronizer(new Phaser(1)));
        listener.startEventListeners();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Future<Void> waitFuture = executorService.submit(() -> {
                listener.waitForOutputToComplete();
                return null;
            });

            listener.statusUpdateOccurred(new StatusUpdateEvent(this, POSPrinterConst.PTR_SUE_COVER_OPEN));

            ExecutionException executionException = assertThrows(ExecutionException.class,
                    () -> waitFuture.get(1, TimeUnit.SECONDS));
            assertInstanceOf(JposException.class, executionException.getCause());
            JposException jposException = (JposException) executionException.getCause();
            assertEquals(JposConst.JPOS_E_EXTENDED, jposException.getErrorCode());
            assertEquals(POSPrinterConst.PTR_SUE_COVER_OPEN, jposException.getErrorCodeExtended());
        } finally {
            executorService.shutdownNow();
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    public void waitForOutputToComplete_WhenNonFailureStatusArrives_ContinuesWaitingForOutputComplete() throws Exception {
        PrinterDeviceListener listener = new PrinterDeviceListener(new EventSynchronizer(new Phaser(1)));
        listener.startEventListeners();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Future<Void> waitFuture = executorService.submit(() -> {
                listener.waitForOutputToComplete();
                return null;
            });

            listener.statusUpdateOccurred(new StatusUpdateEvent(this, POSPrinterConst.PTR_SUE_REC_PAPEROK));

            assertThrows(TimeoutException.class, () -> waitFuture.get(200, TimeUnit.MILLISECONDS));

            listener.outputCompleteOccurred(new OutputCompleteEvent(this, 1));
            assertNull(waitFuture.get(1, TimeUnit.SECONDS));
        } finally {
            executorService.shutdownNow();
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}

