package com.target.devicemanager.common.entities;

import java.util.HashMap;
import java.util.Map;

public enum LogField {
    TIMESTAMP("@timestamp", "Date/time when the event originated."),
    APPLICATION("application", "The name of the application."),
    COMPONENT("component", "An identifier associated with the application."),
    ERROR_CODE("error.code", "Error code describing the error."),
    ERROR_ID("error.id", "Unique identifier for the error."),
    ERROR_MESSAGE("error.message", "Error message."),
    ERROR_STACK_TRACE("error.stack_trace", "The stack trace of this error in plain text."),
    ERROR_TYPE("error.type", "Error type."),
    EVENT_ACTION("event.action", "The action captured by the event."),
    EVENT_OUTCOME(
            "event.outcome",
            "The outcome of the event. The lowest level categorization field in the hierarchy."),
    EVENT_SEVERITY("event.severity", "Numeric severity of the event."),
    HOST_ID("host.id", "Unique host id."),
    HTTP_REQUEST_BODY_CONTENT("http.request.body.content", "The full HTTP request body."),
    HTTP_REQUEST_METHOD("http.request.method", "HTTP request method."),
    HTTP_RESPONSE_BODY_BYTES("http.response.body.bytes", "Size in bytes of the response body."),
    HTTP_RESPONSE_BODY_CONTENT("http.response.body.content", "The full HTTP response body."),
    HTTP_RESPONSE_STATUS_CODE("http.response.status_code", "HTTP response status code."),
    HTTP_RESPONSE_STATUS_MESSAGE(
            "http.response.status_message",
            "A full \"Reason-Phrase\" associated with this response, as defined in RFC2616. A common usage would be to differentiate 404 responses by type, which are decorated as a Reason-Phrase by Fastly."),
    LABELS_APPLICATION("labels.application", "The name of the application."),
    LABELS_BLOSSOM_ID(
            "labels.blossom_id",
            "The Blossom ID of the application. This is required for all non-TAP applications."),
    LABELS_DETAIL("labels.detail", "The deployment detail of the cluster."),
    LOG_LEVEL("log.level", "Log level of the log event."),
    MESSAGE("message", "Log message optimized for viewing in a log viewer."),
    SERVICE_NAME("service.name", "Name of the service."),
    TAGS("tags", "List of keywords used to tag each event."),
    TRACE_ID("trace.id", "Unique identifier of the trace."),
    TRANSACTION_ID(
            "transaction.id",
            "Unique identifier of the transaction within the scope of its trace."),
    URL_DOMAIN("url.domain", "Domain of the url."),
    URL_FULL("url.full", "Full url of the request"),
    URL_PATH("url.path", "Path of the request, such as \"/search\"."),
    URL_QUERY("url.query", "Query portion of the request url");

    private final String dotted;
    private final String[] parts;
    private final String description;

    LogField(String dotted, String description) {
        this.dotted = dotted;
        this.parts = dotted.split("\\.");
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unchecked")
    public void insertInto(Map<String, Object> root, Object value) {
        Map<String, Object> cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String p = parts[i];
            Object next = cur.get(p);
            if (next == null) {
                Map<String, Object> newMap = new HashMap<>();
                cur.put(p, newMap);
                cur = newMap;
            } else if (next instanceof Map) {
                cur = (Map<String, Object>) next;
            } else {
                // collision: existing primitive where we need a map
                // policy: move existing value to a sibling key and replace with map
                Object existing = next;
                Map<String, Object> newMap = new HashMap<>();
                newMap.put("_value_before_nesting", existing);
                cur.put(p, newMap);
                cur = newMap;
            }
        }
        // finally set the leaf
        cur.put(parts[parts.length - 1], value);
    }
}