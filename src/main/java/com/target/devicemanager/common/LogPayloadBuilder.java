package com.target.devicemanager.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.target.devicemanager.common.entities.LogField;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * LogPayloadBuilder:
 * - collect LogField + value pairs
 * - produce a nested Map or JSON string
 * - convenience methods to log via SLF4J logger at common levels
 */
public class LogPayloadBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private final Map<String, Object> root = new HashMap<>();

    private boolean hasMessage = false;
    private String errorMessageFallback = null;

    public LogPayloadBuilder() { }

    /** Add a LogField with a value (value may be String, Number, Map, List, etc). */
    public LogPayloadBuilder add(LogField field, Object value) {
        if (field == null || value == null) return this;

        if (field == LogField.MESSAGE) {
            hasMessage = true;
        } else if (field == LogField.ERROR_MESSAGE) {
            // Cache non-empty error message for potential fallback
            String v = String.valueOf(value);
            if (!v.isEmpty()) {
                errorMessageFallback = v;
            }
        }

        field.insertInto(root, value);
        return this;
    }

    // Ensure that a "message" field is populated, if not populates with error.message if it exists
    private void ensureMessage() {
        if (!hasMessage && errorMessageFallback != null) {
            this.add(LogField.MESSAGE, errorMessageFallback);
            hasMessage = true;
        }
    }

    /** Return JSON string representation (reused ObjectMapper). */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            // fallback minimal JSON to avoid breaking logging
            return "{\"serialization_error\":true}";
        }
    }

    /* ------------- SLF4J convenience logging helpers ------------- */

    /** Log at INFO: logs the JSON payload as the message. */
    public void logInfo(Logger logger) {
        if (logger == null) return;
        ensureMessage();
        this.add(LogField.LOG_LEVEL, "INFO");
        logger.info(toJson());
    }

    /** Log at TRACE */
    public void logTrace(Logger logger) {
        if (logger == null) return;
        ensureMessage();
        this.add(LogField.LOG_LEVEL, "TRACE");
        logger.trace(toJson());
    }

    /** Log at DEBUG */
    public void logDebug(Logger logger) {
        if (logger == null) return;
        ensureMessage();
        this.add(LogField.LOG_LEVEL, "DEBUG");
        logger.debug(toJson());
    }

    /** Log at WARN */
    public void logWarn(Logger logger) {
        if (logger == null) return;
        ensureMessage();
        this.add(LogField.LOG_LEVEL, "WARN");
        logger.warn(toJson());
    }

    /** Log at ERROR */
    public void logError(Logger logger) {
        if (logger == null) return;
        ensureMessage();
        this.add(LogField.LOG_LEVEL, "ERROR");
        logger.error(toJson());
    }
}
