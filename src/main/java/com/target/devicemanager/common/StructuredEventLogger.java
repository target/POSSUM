package com.target.devicemanager.common;

import com.target.devicemanager.common.entities.LogField;
import org.slf4j.Logger;

public final class StructuredEventLogger {
    private final String serviceName;
    private final String component;
    private final Logger logger;

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public StructuredEventLogger(String serviceName, String component, Logger logger) {
        this.serviceName = serviceName;
        this.component = component;
        this.logger = logger;
    }

    public static StructuredEventLogger of(String serviceName, String component, Logger logger) {
        return new StructuredEventLogger(serviceName, component, logger);
    }


    public void success(String message, int severity) {
        LogPayloadBuilder b = base(inferCallerMethodName(), "success", severity)
                .add(LogField.MESSAGE, message);

        emitBySeverity(b, severity);
    }

    public void failure(String message, int severity, Throwable t) {
        LogPayloadBuilder b = base(inferCallerMethodName(), "failure", severity)
                .add(LogField.MESSAGE, message);

        if (t != null) {
            b.add(LogField.ERROR_TYPE, t.getClass().getSimpleName());
            b.add(LogField.ERROR_MESSAGE, t.getMessage());
            if (t instanceof jpos.JposException je) {
                try { b.add(LogField.ERROR_CODE, je.getErrorCode()); } catch (Throwable ignored) {}
            }
        }

        b.logError(logger);
    }

    public void failureWithTag(String message, int severity, Throwable t, String tag) {
        LogPayloadBuilder b = base(inferCallerMethodName(), "failure", severity)
                .add(LogField.MESSAGE, message)
                .add(LogField.TAGS, tag);

        if (t != null) {
            b.add(LogField.ERROR_TYPE, t.getClass().getSimpleName());
            b.add(LogField.ERROR_MESSAGE, t.getMessage());
        }

        b.logError(logger);
    }

    private LogPayloadBuilder base(String action, String outcome, int severity) {
        return new LogPayloadBuilder()
                .add(LogField.SERVICE_NAME, serviceName)
                .add(LogField.COMPONENT, component)
                .add(LogField.EVENT_ACTION, action)
                .add(LogField.EVENT_OUTCOME, outcome)
                .add(LogField.EVENT_SEVERITY, severity);
    }

    private void emitBySeverity(LogPayloadBuilder b, int severity) {
        if (severity >= 17) {
            b.logError(logger);
        } else if (severity >= 10) {
            b.logWarn(logger);
        } else if (severity < 9) {
            b.logTrace(logger);
        } else {
            b.logInfo(logger);
        }
    }

    private static String inferCallerMethodName() {
        return STACK_WALKER.walk(s ->
                s.filter(f -> f.getDeclaringClass() != StructuredEventLogger.class)
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse("unknown")
        );
    }
}
