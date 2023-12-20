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
package org.graylog2.rest.bulk;

import com.mongodb.MongoException;
import org.graylog.security.HasUser;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.jersey.FailureContextCreator;
import org.graylog2.audit.jersey.SuccessContextCreator;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.bulk.model.BulkOperationFailure;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.BadRequestException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.bulk.SequentialBulkExecutor.NO_ENTITY_IDS_ERROR;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SequentialBulkExecutorTest {

    private SequentialBulkExecutor<Object, HasUser> toTest;
    @Mock
    private SingleEntityOperationExecutor<Object, HasUser> singleEntityOperationExecutor;
    @Mock
    private HasUser context;
    @Mock
    private User user;
    @Mock
    private AuditEventSender auditEventSender;
    @Mock
    private SuccessContextCreator<Object> successAuditLogContextCreator;
    @Mock
    private FailureContextCreator failureAuditLogContextCreator;


    private final String eventType = "NVMD";
    private final String entityIdInPathParam = "id";
    private final AuditParams params = new AuditParams(eventType, entityIdInPathParam, Object.class);

    @BeforeEach
    void setUp() {
        toTest = new SequentialBulkExecutor<>(singleEntityOperationExecutor, auditEventSender, successAuditLogContextCreator, failureAuditLogContextCreator);
    }

    @Test
    void throwsBadRequestExceptionOnNullEntityIdsList() {
        assertThrows(BadRequestException.class,
                () -> toTest.executeBulkOperation(new BulkOperationRequest(null), context, params),
                NO_ENTITY_IDS_ERROR);
    }

    @Test
    void throwsBadRequestExceptionOnEmptyEntityIdsList() {
        assertThrows(BadRequestException.class,
                () -> toTest.executeBulkOperation(new BulkOperationRequest(List.of()), context, params),
                NO_ENTITY_IDS_ERROR);
    }

    @Test
    void returnsProperResponseOnSuccessfulBulkRemoval() throws Exception {
        mockUserContext();
        Object entity1 = new Object();
        doReturn(entity1).when(singleEntityOperationExecutor).execute("1", context);
        Object entity2 = new Object();
        doReturn(entity2).when(singleEntityOperationExecutor).execute("2", context);
        Object entity3 = new Object();
        doReturn(entity3).when(singleEntityOperationExecutor).execute("3", context);
        final BulkOperationResponse bulkOperationResponse = toTest.executeBulkOperation(new BulkOperationRequest(List.of("1", "2", "3")), context, params);
        assertThat(bulkOperationResponse.successfullyPerformed()).isEqualTo(3);
        assertThat(bulkOperationResponse.failures()).isEmpty();
        verify(singleEntityOperationExecutor).execute("1", context);
        verify(singleEntityOperationExecutor).execute("2", context);
        verify(singleEntityOperationExecutor).execute("3", context);
        verifyNoMoreInteractions(singleEntityOperationExecutor);
        verify(auditEventSender, times(3)).success(any(), eq(eventType), any());
        verifyNoInteractions(failureAuditLogContextCreator);
        verify(successAuditLogContextCreator).create(entity1, Object.class);
        verify(successAuditLogContextCreator).create(entity2, Object.class);
        verify(successAuditLogContextCreator).create(entity3, Object.class);
        verifyNoMoreInteractions(successAuditLogContextCreator);
    }

    @Test
    void doesNotCreateAuditLogIfAuditParamsAreNull() throws Exception {
        final BulkOperationResponse bulkOperationResponse = toTest.executeBulkOperation(new BulkOperationRequest(List.of("1", "2", "3")), context, null);
        assertThat(bulkOperationResponse.successfullyPerformed()).isEqualTo(3);
        assertThat(bulkOperationResponse.failures()).isEmpty();
        verify(singleEntityOperationExecutor).execute("1", context);
        verify(singleEntityOperationExecutor).execute("2", context);
        verify(singleEntityOperationExecutor).execute("3", context);
        verifyNoMoreInteractions(singleEntityOperationExecutor);
        verifyNoInteractions(auditEventSender);
        verifyNoInteractions(failureAuditLogContextCreator);
        verifyNoInteractions(successAuditLogContextCreator);
    }

    @Test
    void returnsProperResponseOnFailedBulkRemoval() throws Exception {
        mockUserContext();
        doThrow(new NotFoundException("!?!?")).when(singleEntityOperationExecutor).execute(any(), eq(context));
        final BulkOperationResponse bulkOperationResponse = toTest.executeBulkOperation(new BulkOperationRequest(List.of("no", "good", "ids")), context, params);
        assertThat(bulkOperationResponse.successfullyPerformed()).isEqualTo(0);
        assertThat(bulkOperationResponse.failures())
                .hasSize(3)
                .containsExactly(
                        new BulkOperationFailure("no", "!?!?"),
                        new BulkOperationFailure("good", "!?!?"),
                        new BulkOperationFailure("ids", "!?!?")
                );
        verify(singleEntityOperationExecutor).execute("no", context);
        verify(singleEntityOperationExecutor).execute("good", context);
        verify(singleEntityOperationExecutor).execute("ids", context);
        verifyNoMoreInteractions(singleEntityOperationExecutor);
        verifyNoInteractions(successAuditLogContextCreator);

        verify(failureAuditLogContextCreator).create(entityIdInPathParam, "no");
        verify(failureAuditLogContextCreator).create(entityIdInPathParam, "good");
        verify(failureAuditLogContextCreator).create(entityIdInPathParam, "ids");
        verifyNoMoreInteractions(failureAuditLogContextCreator);

        verify(auditEventSender, times(3)).failure(any(), eq(eventType), any());
    }

    @Test
    void returnsProperResponseOnPartiallySuccessfulBulkRemoval() throws Exception {
        mockUserContext();
        doThrow(new MongoException("MongoDB is striking against increasing retirement age")).when(singleEntityOperationExecutor).execute(eq("1"), eq(context));
        final BulkOperationResponse bulkOperationResponse = toTest.executeBulkOperation(new BulkOperationRequest(List.of("1", "2", "3")), context, params);
        assertThat(bulkOperationResponse.successfullyPerformed()).isEqualTo(2);
        assertThat(bulkOperationResponse.failures())
                .hasSize(1)
                .containsExactly(
                        new BulkOperationFailure("1", "MongoDB is striking against increasing retirement age")
                );
        verify(singleEntityOperationExecutor).execute("1", context);
        verify(singleEntityOperationExecutor).execute("2", context);
        verify(singleEntityOperationExecutor).execute("3", context);
        verifyNoMoreInteractions(singleEntityOperationExecutor);

        verify(auditEventSender, times(1)).failure(any(), eq(eventType), any());
        verify(auditEventSender, times(2)).success(any(), eq(eventType), any());

    }

    @Test
    void exceptionInAuditLogStoringDoesNotInfluenceResponse() throws Exception {
        mockUserContext();
        doThrow(new MongoException("MongoDB audit_log collection became anti-collection when bombed by Bozon particles")).when(auditEventSender).success(any(), anyString(), any());
        final BulkOperationResponse bulkOperationResponse = toTest.executeBulkOperation(new BulkOperationRequest(List.of("1")), context, params);
        assertThat(bulkOperationResponse.successfullyPerformed()).isEqualTo(1);
        assertThat(bulkOperationResponse.failures()).isEmpty();
        verify(singleEntityOperationExecutor).execute("1", context);
        verifyNoMoreInteractions(singleEntityOperationExecutor);
    }

    private void mockUserContext() {
        doReturn(user).when(context).getUser();
        doReturn("admin").when(user).getName();
    }
}
