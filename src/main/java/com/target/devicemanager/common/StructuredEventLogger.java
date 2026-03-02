package com.target.devicemanager.common;

import com.target.devicemanager.common.entities.LogField;
import org.slf4j.Logger;
import java.util.Objects;

public class StructuredEventLogger {
    private final String serviceName;
    private final String component;
    private final Logger logger;

    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static final String CASH_DRAWER = "cash_drawer";
    private static final String CHECK = "check";
    private static final String LINE_DISPLAY = "line_display";
    private static final String PRINTER = "printer";
    private static final String SCALE = "scale";
    private static final String SCANNER = "scanner";
    private static final String DEVICE_MANAGER = "device_manager";
    private static final String COMMON = "common";
    private static final String CONFIGURATION = "configuration";

    public StructuredEventLogger(String serviceName, String component, Logger logger) {
        this.serviceName = serviceName;
        this.component = component;
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    public static StructuredEventLogger of(String serviceName, String component, Logger logger) {
        return new StructuredEventLogger(serviceName, component, logger);
    }

    public void success(String message, int severity) {
        LogPayloadBuilder b = base(inferCallerMethodName(), "success", severity)
                .add(LogField.MESSAGE, message);

        emitBySeverity(b, severity);
    }

    public void successAPI(String message, int severity, String path, String body, int code) {
        LogPayloadBuilder b = null;
        // Don't add null body or code  if the value is null and 0 respectively
        if(body != null || code != 0) {
            b = base(inferCallerMethodName(), "success", severity)
                    .add(LogField.MESSAGE, message)
                    .add(LogField.URL_PATH, path)
                    .add(LogField.HTTP_RESPONSE_BODY_CONTENT, body)
                    .add(LogField.HTTP_RESPONSE_STATUS_CODE, code);
        } else {
            b = base(inferCallerMethodName(), "success", severity)
                    .add(LogField.MESSAGE, message)
                    .add(LogField.URL_PATH, path);
        }


        emitBySeverity(b, severity);
    }

    public void failure(String message, int severity, Throwable t) {
        LogPayloadBuilder b = base(inferCallerMethodName(), "failure", severity)
                .add(LogField.MESSAGE, message);

        addThrowableFields(b, t);

        emitBySeverity(b, severity);
    }

    public void failureAPI(String message, int severity, String path, String body, int code, Throwable t) {
        LogPayloadBuilder b = null;
        // Don't add null body or code  if the value is null and 0 respectively
        if(body != null || code != 0) {
            b = base(inferCallerMethodName(), "failure", severity)
                    .add(LogField.MESSAGE, message)
                    .add(LogField.URL_PATH, path)
                    .add(LogField.HTTP_RESPONSE_BODY_CONTENT, body)
                    .add(LogField.HTTP_RESPONSE_STATUS_CODE, code);
        } else {
            b = base(inferCallerMethodName(), "failure", severity)
                    .add(LogField.MESSAGE, message)
                    .add(LogField.URL_PATH, path);
        }
        addThrowableFields(b, t);

        emitBySeverity(b, severity);
    }

    private void addThrowableFields(LogPayloadBuilder b, Throwable t) {
        if (t == null) return;

        b.add(LogField.ERROR_TYPE, t.getClass().getSimpleName())
                .add(LogField.ERROR_MESSAGE, t.getMessage());

        try {
            if (t instanceof jpos.JposException je) {
                b.add(LogField.ERROR_CODE, je.getErrorCode());
            }
        } catch (Throwable ignored) {}
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
        } else if (severity >= 13) {
            b.logWarn(logger);
        } else if (severity >= 9) {
            b.logInfo(logger);
        } else if (severity >= 5) {
            b.logDebug(logger);
        } else {
            b.logTrace(logger);
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

    public static String getCashDrawerServiceName(){
        return CASH_DRAWER;
    }

    public static String getCheckServiceName(){
        return CHECK;
    }

    public static String getLineDisplayServiceName(){
        return LINE_DISPLAY;
    }

    public static String getPrinterServiceName(){
        return PRINTER;
    }

    public static String getScaleServiceName(){
        return SCALE;
    }

    public static String getScannerServiceName(){
        return SCANNER;
    }

    public static String getDeviceManagerServiceName(){
        return DEVICE_MANAGER;
    }

    public static String getCommonServiceName(){
        return COMMON;
    }

    public static String getConfigurationServiceName(){
        return CONFIGURATION;
    }
}
