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

import org.graylog.security.HasUser;
import org.graylog2.audit.AuditActor;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BulkExecutorTest {

    private final BulkExecutor<Object, HasUser> toTest = (request, userContext, params) -> null;

    @Mock
    HasUser context;

    @Test
    void returnsUnknownUsernameOnNullUser() {
        final String result = toTest.getUserName(context);
        assertEquals("<UNKNOWN>", result);
    }

    @Test
    void returnsUnknownUsernameOnUserWithNullName() {
        User user = mock(User.class);
        doReturn(user).when(context).getUser();
        final String result = toTest.getUserName(context);
        assertEquals("<UNKNOWN>", result);
    }

    @Test
    void returnsUnknownUsernameOnUserWithEmptyName() {
        User user = mock(User.class);
        doReturn(user).when(context).getUser();
        doReturn("").when(user).getName();
        final String result = toTest.getUserName(context);
        assertEquals("<UNKNOWN>", result);
    }

    @Test
    void returnsProperUsername() {
        User user = mock(User.class);
        doReturn(user).when(context).getUser();
        doReturn("Baldwin").when(user).getName();
        final String result = toTest.getUserName(context);
        assertEquals("Baldwin", result);
    }

    @Test
    void returnsProperAuditActor() {
        User user = mock(User.class);
        doReturn(user).when(context).getUser();
        doReturn("Baldwin").when(user).getName();
        final AuditActor result = toTest.getAuditActor(context);
        assertEquals("urn:graylog:user:Baldwin", result.urn());
    }
}
