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
package org.graylog.security;

import com.google.common.collect.ImmutableMap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.subject.Subject;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserContextTest {


    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
    }

    @Test
    void UserContextWithoutContext() {
        assertThatExceptionOfType(UserContextMissingException.class).isThrownBy(() -> new UserContext.Factory(userService).create());
    }

    @Test
    void runAs() {
        // Simulate what we do in the DefaultSecurityManagerProvider
        DefaultSecurityManager sm = new DefaultSecurityManager();
        SecurityUtils.setSecurityManager(sm);
        final DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        final DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator() {
            @Override
            public boolean isSessionStorageEnabled(Subject subject) {
                // save to session if we already have a session. do not create on just for saving the subject
                return subject.getSession(false) != null;
            }
        };
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        sm.setSubjectDAO(subjectDAO);

        final User user = new UserImpl(mock(PasswordAlgorithmFactory.class), mock(Permissions.class), ImmutableMap.of());
        when(userService.load(anyString())).thenReturn(user);
        when(userService.loadById(anyString())).thenReturn(user);

        final String USERID = "123456";
        UserContext.<Void>runAs(USERID, () -> {

            final UserContext userContext = new UserContext.Factory(userService).create();
            assertThat(userContext.getUserId()).isEqualTo(USERID);
            assertThat(userContext.getUser()).isEqualTo(user);

            return null;
        });
    }
}
