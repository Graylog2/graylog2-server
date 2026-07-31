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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog2.audit.jersey.ResponseEntityConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Wraps an action with an audit-log emission around it. The input passed to the action and the
 * result returned by it are auto-serialized via {@link ResponseEntityConverter} and placed under
 * {@code request_entity} / {@code response_entity} keys in the audit context — matching the
 * convention used by the REST {@code AuditLogFilter} and bulk executors.
 *
 * <p>On exception, records a failure event (with the exception message added to the context) and
 * rethrows. On successful return, a caller-supplied predicate decides whether the result represents
 * a success or a failure (e.g. a {@code null} return value or a response object carrying errors).</p>
 *
 * <p>Failures inside {@link AuditEventSender} are logged and swallowed — an audit-store failure
 * must not mask or override the outcome of the wrapped action.</p>
 */
public class Auditor {
    private static final Logger LOG = LoggerFactory.getLogger(Auditor.class);

    private static final String REQUEST_ENTITY = "request_entity";
    private static final String RESPONSE_ENTITY = "response_entity";
    private static final String ERROR = "error";

    public static final Predicate<Object> SUCCESS_ON_TRUE = Boolean.TRUE::equals;
    public static final Predicate<Object> SUCCESS_ON_NON_NULL = Objects::nonNull;

    private final AuditEventSender sender;
    private final ResponseEntityConverter converter;

    @Inject
    public Auditor(final AuditEventSender sender, final ObjectMapper objectMapper) {
        this.sender = sender;
        this.converter = new ResponseEntityConverter(objectMapper);
    }

    public <T> T audited(final String username,
                         final String eventType,
                         final Object actionInput,
                         final Supplier<T> auditableAction) {
        return audited(username, eventType, actionInput, auditableAction, SUCCESS_ON_NON_NULL);
    }

    public <T> T audited(final String username,
                         final String eventType,
                         final Object actionInput,
                         final Supplier<T> auditableAction,
                         final Predicate<? super T> isSuccess) {
        final T actionResult;
        try {
            actionResult = auditableAction.get();
        } catch (RuntimeException e) {
            recordFailure(username,
                    eventType,
                    actionInput,
                    null,
                    Map.of(ERROR, String.valueOf(e.getMessage())));
            throw e;
        }
        if (isSuccess.test(actionResult)) {
            recordSuccess(username, eventType, actionInput, actionResult, null);
        } else {
            recordFailure(username, eventType, actionInput, actionResult, null);
        }
        return actionResult;
    }

    public void recordSuccess(final String username,
                              final String eventType,
                              final Object actionInput,
                              final Object actionResult,
                              final Map<String, Object> extra) {
        try {
            sender.success(
                    AuditActor.user(username),
                    AuditEventType.create(eventType),
                    buildContext(actionInput, actionResult, extra)
            );
        } catch (Exception auditLogStoreException) {
            LOG.error("Failed to store audit success event of type {}", eventType, auditLogStoreException);
        }
    }

    public void recordFailure(final String username,
                              final String eventType,
                              final Object actionInput) {
        recordFailure(username, eventType, actionInput, null, null);
    }


    public void recordFailure(final String username,
                              final String eventType,
                              final Object actionInput,
                              final Object actionResult,
                              final Map<String, Object> extra) {
        try {
            sender.failure(
                    AuditActor.user(username),
                    AuditEventType.create(eventType),
                    buildContext(actionInput, actionResult, extra)
            );
        } catch (Exception auditLogStoreException) {
            LOG.error("Failed to store audit failure event of type {}", eventType, auditLogStoreException);
        }
    }

    private Map<String, Object> buildContext(final Object actionInput,
                                             final Object actionResult,
                                             final Map<String, Object> extra) {
        final Map<String, Object> ctx = new LinkedHashMap<>();
        addEntity(ctx, REQUEST_ENTITY, actionInput);
        addEntity(ctx, RESPONSE_ENTITY, actionResult);
        if (extra != null) {
            ctx.putAll(extra);
        }
        return ctx;
    }

    private void addEntity(final Map<String, Object> ctx, final String key, final Object entity) {
        if (entity == null) {
            return;
        }
        final Map<String, Object> converted = converter.convertValue(entity, entity.getClass());
        if (converted != null) {
            ctx.put(key, converted);
        }
    }
}
