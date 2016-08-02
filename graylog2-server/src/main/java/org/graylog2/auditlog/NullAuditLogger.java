package org.graylog2.auditlog;

import java.util.Map;

public class NullAuditLogger implements AuditLogger {
    @Override
    public void success(String subject, String action, String object) {
    }

    @Override
    public void success(String subject, String action, String object, Map<String, Object> context) {
    }

    @Override
    public void failure(String subject, String action, String object) {
    }

    @Override
    public void failure(String subject, String action, String object, Map<String, Object> context) {
    }
}
