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
import org.graylog2.rest.bulk.model.BulkDeleteRequest;
import org.graylog2.rest.bulk.model.BulkDeleteResponse;
import org.graylog2.rest.bulk.model.BulkOperationFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.bulk.SequentialBulkRemover.NO_ENTITY_IDS_FAILURE;
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
class SequentialBulkRemoverTest {

    private SequentialBulkRemover<Object, HasUser> toTest;
    @Mock
    private SingleEntityRemover<Object, HasUser> singleEntityRemover;
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
        toTest = new SequentialBulkRemover<>(singleEntityRemover, auditEventSender, successAuditLogContextCreator, failureAuditLogContextCreator);
    }

    @Test
    void returnsProperFailureMsgOnNullEntityIdsList() {
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(null), context, params);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(0);
        assertThat(bulkDeleteResponse.failures()).containsOnly(NO_ENTITY_IDS_FAILURE);
        verifyNoInteractions(singleEntityRemover);
        verifyNoInteractions(auditEventSender);
        verifyNoInteractions(successAuditLogContextCreator);
        verifyNoInteractions(failureAuditLogContextCreator);
    }

    @Test
    void returnsProperFailureMsgOnEmptyEntityIdsList() {
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(List.of()), context, params);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(0);
        assertThat(bulkDeleteResponse.failures()).containsOnly(NO_ENTITY_IDS_FAILURE);
        verifyNoInteractions(singleEntityRemover);
        verifyNoInteractions(auditEventSender);
        verifyNoInteractions(successAuditLogContextCreator);
        verifyNoInteractions(failureAuditLogContextCreator);
    }

    @Test
    void returnsProperResponseOnSuccessfulBulkRemoval() throws Exception {
        mockUserContext();
        Object entity1 = new Object();
        doReturn(entity1).when(singleEntityRemover).remove("1", context);
        Object entity2 = new Object();
        doReturn(entity2).when(singleEntityRemover).remove("2", context);
        Object entity3 = new Object();
        doReturn(entity3).when(singleEntityRemover).remove("3", context);
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(List.of("1", "2", "3")), context, params);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(3);
        assertThat(bulkDeleteResponse.failures()).isEmpty();
        verify(singleEntityRemover).remove("1", context);
        verify(singleEntityRemover).remove("2", context);
        verify(singleEntityRemover).remove("3", context);
        verifyNoMoreInteractions(singleEntityRemover);
        verify(auditEventSender, times(3)).success(any(), eq(eventType), any());
        verifyNoInteractions(failureAuditLogContextCreator);
        verify(successAuditLogContextCreator).create(entity1, Object.class);
        verify(successAuditLogContextCreator).create(entity2, Object.class);
        verify(successAuditLogContextCreator).create(entity3, Object.class);
        verifyNoMoreInteractions(successAuditLogContextCreator);
    }

    @Test
    void returnsProperResponseOnFailedBulkRemoval() throws Exception {
        mockUserContext();
        doThrow(new NotFoundException("!?!?")).when(singleEntityRemover).remove(any(), eq(context));
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(List.of("no", "good", "ids")), context, params);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(0);
        assertThat(bulkDeleteResponse.failures())
                .hasSize(3)
                .containsExactly(
                        new BulkOperationFailure("no", "!?!?"),
                        new BulkOperationFailure("good", "!?!?"),
                        new BulkOperationFailure("ids", "!?!?")
                );
        verify(singleEntityRemover).remove("no", context);
        verify(singleEntityRemover).remove("good", context);
        verify(singleEntityRemover).remove("ids", context);
        verifyNoMoreInteractions(singleEntityRemover);
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
        doThrow(new MongoException("MongoDB is striking against increasing retirement age")).when(singleEntityRemover).remove(eq("1"), eq(context));
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(List.of("1", "2", "3")), context, params);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(2);
        assertThat(bulkDeleteResponse.failures())
                .hasSize(1)
                .containsExactly(
                        new BulkOperationFailure("1", "MongoDB is striking against increasing retirement age")
                );
        verify(singleEntityRemover).remove("1", context);
        verify(singleEntityRemover).remove("2", context);
        verify(singleEntityRemover).remove("3", context);
        verifyNoMoreInteractions(singleEntityRemover);

        verify(auditEventSender, times(1)).failure(any(), eq(eventType), any());
        verify(auditEventSender, times(2)).success(any(), eq(eventType), any());

    }

    @Test
    void exceptionInAuditLogStoringDoesNotInfluenceResponse() throws Exception {
        mockUserContext();
        doThrow(new MongoException("MongoDB audit_log collection became anti-collection when bombed by Bozon particles")).when(auditEventSender).success(any(), anyString(), any());
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(List.of("1")), context, params);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(1);
        assertThat(bulkDeleteResponse.failures()).isEmpty();
        verify(singleEntityRemover).remove("1", context);
        verifyNoMoreInteractions(singleEntityRemover);
    }

    private void mockUserContext() {
        doReturn(user).when(context).getUser();
        doReturn("admin").when(user).getName();
    }
}
