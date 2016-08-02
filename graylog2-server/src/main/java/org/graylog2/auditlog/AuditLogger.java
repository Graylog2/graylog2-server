package org.graylog2.auditlog;

import java.util.Map;

public interface AuditLogger {
    void success(String subject, String action, String object);

    void success(String subject, String action, String object, Map<String, Object> context);

    void failure(String subject, String action, String object);

    void failure(String subject, String action, String object, Map<String, Object> context);
}
