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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AuditorTest {

    private static final String USER = "alice";
    private static final String EVENT = "server:tests:create";

    private static final AuditActor EXPECTED_ACTOR = AuditActor.user(USER);
    private static final AuditEventType EXPECTED_AUDIT_EVENT_TYPE = AuditEventType.create(EVENT);

    @Mock
    private AuditEventSender sender;
    private Auditor toTest;

    @BeforeEach
    void setUp() {
        toTest = new Auditor(sender, new ObjectMapper());
    }

    @Test
    void recordSuccessUsesAuditEventSenderCorrectlyWithNullContextData() {
        toTest.recordSuccess(USER, EVENT, null, null, null);

        verify(sender).success(EXPECTED_ACTOR, EXPECTED_AUDIT_EVENT_TYPE, Map.of());
        verifyNoMoreInteractions(sender);
    }

    @Test
    void recordSuccessUsesAuditEventSenderCorrectly() {
        toTest.recordSuccess(USER, EVENT, new RequestDto("hi"), new ResponseDto("42"), Map.of("k1", "v1", "k2", 42));

        verify(sender).success(EXPECTED_ACTOR, EXPECTED_AUDIT_EVENT_TYPE,
                Map.of(
                        "request_entity", Map.of("input_name", "hi"),
                        "response_entity", Map.of("output_id", "42"),
                        "k1", "v1",
                        "k2", 42
                )
        );
        verifyNoMoreInteractions(sender);
    }

    @Test
    void recordSuccessExtraOverridesRequestEntityWhenKeyCollides() {
        toTest.recordSuccess(USER,
                EVENT,
                new RequestDto("original"),
                null,
                Map.of("request_entity", "overwritten"));

        verify(sender).success(EXPECTED_ACTOR, EXPECTED_AUDIT_EVENT_TYPE,
                Map.of("request_entity", "overwritten"));
        verifyNoMoreInteractions(sender);
    }

    @Test
    void recordSuccessSwallowsExceptionThrownBySender() {
        doThrow(new RuntimeException("boom")).when(sender)
                .success(any(AuditActor.class), any(AuditEventType.class), anyMap());

        toTest.recordSuccess(USER, EVENT, null, null, null);
    }

    @Test
    void recordFailureUsesAuditEventSenderCorrectlyWithNullContextData() {
        toTest.recordFailure(USER, EVENT, null, null, null);
        toTest.recordFailure(USER, EVENT, null);

        verify(sender, times(2)).failure(EXPECTED_ACTOR, EXPECTED_AUDIT_EVENT_TYPE, Map.of());

        verifyNoMoreInteractions(sender);
    }

    @Test
    void recordFailureUsesAuditEventSenderCorrectly() {
        toTest.recordFailure(USER, EVENT, new RequestDto("hi"), new ResponseDto("42"), Map.of("k1", "v1", "k2", 42));

        verify(sender).failure(EXPECTED_ACTOR, EXPECTED_AUDIT_EVENT_TYPE,
                Map.of(
                        "request_entity", Map.of("input_name", "hi"),
                        "response_entity", Map.of("output_id", "42"),
                        "k1", "v1",
                        "k2", 42
                )
        );
        verifyNoMoreInteractions(sender);
    }

    @Test
    void recordFailureExtraOverridesRequestEntityWhenKeyCollides() {
        toTest.recordFailure(USER,
                EVENT,
                new RequestDto("original"),
                null,
                Map.of("request_entity", "overwritten"));

        verify(sender).failure(EXPECTED_ACTOR, EXPECTED_AUDIT_EVENT_TYPE,
                Map.of("request_entity", "overwritten"));
        verifyNoMoreInteractions(sender);
    }

    @Test
    void recordFailureSwallowsExceptionThrownBySender() {
        doThrow(new RuntimeException("boom")).when(sender)
                .failure(any(AuditActor.class), any(AuditEventType.class), anyMap());

        toTest.recordFailure(USER, EVENT, null, null, null);
        toTest.recordFailure(USER, EVENT, null);
    }

    @Test
    void auditedRecordsSuccessWithProperParametersOnResultIndicatingSuccess() {
        toTest.audited(USER,
                EVENT,
                new RequestDto("hi"),
                () -> new ResponseDto("42"),
                result -> true);

        verify(sender).success(
                EXPECTED_ACTOR,
                EXPECTED_AUDIT_EVENT_TYPE,
                Map.of(
                        "request_entity", Map.of("input_name", "hi"),
                        "response_entity", Map.of("output_id", "42")
                )
        );
        verifyNoMoreInteractions(sender);
    }

    @Test
    void auditedRecordsFailureWithProperParametersOnResultIndicatingFailure() {
        toTest.audited(USER,
                EVENT,
                new RequestDto("hi"),
                () -> new ResponseDto("42"),
                result -> false);

        verify(sender).failure(
                EXPECTED_ACTOR,
                EXPECTED_AUDIT_EVENT_TYPE,
                Map.of(
                        "request_entity", Map.of("input_name", "hi"),
                        "response_entity", Map.of("output_id", "42")
                )
        );
        verifyNoMoreInteractions(sender);
    }

    @Test
    void auditedRethrowsExceptionAndRecordsFailureWhenActionThrows() {
        final RuntimeException boom = new RuntimeException("kaboom");
        final Supplier<Object> throwing = () -> {
            throw boom;
        };

        assertThatThrownBy(() -> toTest.audited(USER, EVENT, new RequestDto("hi"), throwing))
                .isSameAs(boom);

        verify(sender).failure(
                EXPECTED_ACTOR,
                EXPECTED_AUDIT_EVENT_TYPE,
                Map.of(
                        "request_entity", Map.of("input_name", "hi"),
                        "error", "kaboom"
                )
        );
        verifyNoMoreInteractions(sender);
    }

    @Test
    void auditedDoesNotSwallowActionExceptionEvenIfSenderAlsoThrows() {
        final RuntimeException boom = new RuntimeException("kaboom");
        doThrow(new RuntimeException("sender exploded")).when(sender)
                .failure(any(AuditActor.class), any(AuditEventType.class), anyMap());

        assertThatThrownBy(() -> toTest.audited(USER, EVENT, null, () -> {
            throw boom;
        })).isSameAs(boom);
    }

    private record RequestDto(@JsonProperty("input_name") String name) {}
    private record ResponseDto(@JsonProperty("output_id") String id) {}
}
