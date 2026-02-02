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
package org.graylog.security.authservice;

import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ProvisionerServiceTest {

    public static final String BACKEND_ID = "backend-id";
    public static final String BACKEND_TYPE = "backend-type";
    public static final String EMAIL = "email@graylog.com";
    public static final String FIRST_NAME = "First";
    public static final String FULL_NAME = "Full Name";
    public static final String LAST_NAME = "Last";
    public static final String USER_ID = "user-id";
    public static final String USERNAME = "username";

    private ProvisionerService provisionerService;

    @Mock
    private UserService userService;

    @Mock
    private AuthServiceBackend authServiceBackend;


    @BeforeEach
    public void setUp() throws Exception {
        provisionerService = new ProvisionerService(userService, new HashMap<>());
    }

    @Test
    public void testFirstLastNameOnlySuccess() throws ValidationException {
        when(authServiceBackend.backendId()).thenReturn(BACKEND_ID);
        when(authServiceBackend.backendType()).thenReturn(BACKEND_TYPE);
        final UserDetails.Builder detailsBuilder = provisionerService.newDetails(authServiceBackend);
        assertNotNull(detailsBuilder);
        detailsBuilder
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .base64AuthServiceUid("id")
                .username(USERNAME)
                .accountIsEnabled(true)
                .email(EMAIL)
                .defaultRoles(Collections.emptySet());
        final UserDetails userDetails = detailsBuilder.build();
        assertEquals(BACKEND_ID, userDetails.authServiceId());
        assertEquals(BACKEND_TYPE, userDetails.authServiceType());
        final User user = mock(User.class);
        when(userService.create()).thenReturn(user);
        when(userService.save(isA(User.class))).thenReturn(USER_ID);
        provisionerService.provision(userDetails);
        verify(userService, times(1)).save(isA(User.class));
        verify(user, times(1)).setFirstLastFullNames(eq(FIRST_NAME), eq(LAST_NAME));
        verify(user, times(1)).setTimeZone((DateTimeZone) isNull());
    }

    @Test
    public void testFullNameOnlySuccess() throws ValidationException {
        when(authServiceBackend.backendId()).thenReturn(BACKEND_ID);
        when(authServiceBackend.backendType()).thenReturn(BACKEND_TYPE);
        final UserDetails.Builder detailsBuilder = provisionerService.newDetails(authServiceBackend);
        assertNotNull(detailsBuilder);
        detailsBuilder
                .fullName(FULL_NAME)
                .base64AuthServiceUid("id")
                .username(USERNAME)
                .accountIsEnabled(true)
                .email(EMAIL)
                .defaultRoles(Collections.emptySet());
        final UserDetails userDetails = detailsBuilder.build();
        assertEquals(BACKEND_ID, userDetails.authServiceId());
        assertEquals(BACKEND_TYPE, userDetails.authServiceType());
        final User user = mock(User.class);
        when(userService.create()).thenReturn(user);
        when(userService.save(isA(User.class))).thenReturn(USER_ID);
        provisionerService.provision(userDetails);
        verify(userService, times(1)).save(isA(User.class));
        verify(user, times(1)).setFullName(FULL_NAME);
        verify(user, times(1)).setTimeZone((DateTimeZone) isNull());
    }
}
