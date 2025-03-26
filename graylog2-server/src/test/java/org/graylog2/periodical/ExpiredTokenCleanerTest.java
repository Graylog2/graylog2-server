/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side License for more details.
 *
 * You should have received a copy of the Server Side License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.periodical;

import com.google.common.collect.ImmutableMap;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.AccessTokenImpl;
import org.graylog2.security.AccessTokenService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.graylog2.audit.AuditEventTypes.USER_ACCESS_TOKEN_DELETE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiredTokenCleanerTest {

    @Mock
    private AccessTokenService tokenService;

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private NodeId nodeId;

    private Object[] mocks;

    private ExpiredTokenCleaner expiredTokenCleaner;

    @BeforeEach
    void setUp() {
        expiredTokenCleaner = new ExpiredTokenCleaner(tokenService, auditEventSender, nodeId);
        mocks = new Object[]{tokenService, auditEventSender, nodeId};
        lenient().when(nodeId.getNodeId()).thenReturn("nodeId1");
    }

    @Test
    void doNothingIfNoTokenHasExpired() {
        when(tokenService.findExpiredTokens(any())).thenReturn(Collections.emptyList());

        expiredTokenCleaner.doRun();

        verify(tokenService).findExpiredTokens(any());
        verifyNoMoreInteractions(mocks);
    }

    @Test
    void sendAuditEventIfExpiredTokenIsDeletedSuccessfully() {
        final int id = 1;
        when(tokenService.findExpiredTokens(any())).thenReturn(List.of(mkToken(id)));

        expiredTokenCleaner.doRun();

        verify(tokenService).findExpiredTokens(any());
        verify(tokenService).deleteById(String.valueOf(id));
        verify(nodeId).getNodeId();
        verify(auditEventSender).success(any(), eq(USER_ACCESS_TOKEN_DELETE), eq(expectedContext(id, null)));

        verifyNoMoreInteractions(mocks);
    }

    @Test
    void sendAuditEventIfExpiredTokenFailsToBeDeleted() {
        final int id = 2;
        final String errorMsg = "Boooom!";
        when(tokenService.findExpiredTokens(any())).thenReturn(List.of(mkToken(id)));
        when(tokenService.deleteById(String.valueOf(id))).thenThrow(new RuntimeException(errorMsg));

        expiredTokenCleaner.doRun();

        verify(tokenService).findExpiredTokens(any());
        verify(tokenService).deleteById(String.valueOf(id));
        verify(nodeId).getNodeId();
        verify(auditEventSender).failure(any(), eq(USER_ACCESS_TOKEN_DELETE), eq(expectedContext(id, errorMsg)));

        verifyNoMoreInteractions(mocks);
    }

    //-----------------
    // Helper methods

    private final DateTime baseDateTime = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);

    private AccessTokenService.ExpiredToken mkToken(int id) {
        return new AccessTokenService.ExpiredToken(String.valueOf(id), "token" + id, baseDateTime.plusDays(id), "user" + id, "username" + id);
    }


    private Map<String, Object> expectedContext(int id, String errorMsg) {
        ImmutableMap.Builder<String, Object> ctx = ImmutableMap.builder();
        ctx.put(AccessTokenImpl.NAME, "token" + id).put("userId", "user" + id).put("username", "username" + id);
        if (errorMsg != null) {
            ctx.put("Failure", errorMsg);
        }
        return ctx.build();
    }

}
