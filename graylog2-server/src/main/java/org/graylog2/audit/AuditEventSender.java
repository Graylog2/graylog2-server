/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.audit;

import java.util.Map;

public interface AuditEventSender {
    void success(AuditActor actor, AuditEventType type);

    void success(AuditActor actor, AuditEventType type, Map<String, Object> context);

    void failure(AuditActor actor, AuditEventType type);

    void failure(AuditActor actor, AuditEventType type, Map<String, Object> context);

    // Some convenience default methods which an audit event type of "String".

    default void success(AuditActor actor, String type) {
        success(actor, AuditEventType.create(type));
    }

    default void success(AuditActor actor, String type, Map<String, Object> context) {
        success(actor, AuditEventType.create(type), context);
    }

    default void failure(AuditActor actor, String type) {
        failure(actor, AuditEventType.create(type));
    }

    default void failure(AuditActor actor, String type, Map<String, Object> context) {
        failure(actor, AuditEventType.create(type), context);
    }
}
