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

import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
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
        provisionerService = new ProvisionerService(userService, new HashMap<>(), false);
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

    @Test
    public void testTimezoneSetWhenProvided() throws ValidationException {
        when(authServiceBackend.backendId()).thenReturn(BACKEND_ID);
        when(authServiceBackend.backendType()).thenReturn(BACKEND_TYPE);
        final UserDetails.Builder detailsBuilder = provisionerService.newDetails(authServiceBackend);
        detailsBuilder
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .base64AuthServiceUid("id")
                .username(USERNAME)
                .accountIsEnabled(true)
                .email(EMAIL)
                .timezone(ZoneId.of("America/New_York"))
                .defaultRoles(Collections.emptySet());
        final UserDetails userDetails = detailsBuilder.build();
        final User user = mock(User.class);
        when(userService.create()).thenReturn(user);
        when(userService.save(isA(User.class))).thenReturn(USER_ID);
        provisionerService.provision(userDetails);
        verify(userService, times(1)).save(isA(User.class));
        verify(user, times(1)).setTimeZone(eq(DateTimeZone.forID("America/New_York")));
    }

    private UserDetails externalUserDetails(String authServiceUid) throws ValidationException {
        when(authServiceBackend.backendId()).thenReturn(BACKEND_ID);
        when(authServiceBackend.backendType()).thenReturn(BACKEND_TYPE);
        // Provisioning a brand new user must stay possible in every one of these scenarios, so that a test only
        // fails because of the account it was pointed at, never because creating a user was impossible.
        when(userService.create()).thenReturn(mock(User.class));
        when(userService.save(isA(User.class))).thenReturn(USER_ID);
        return provisionerService.newDetails(authServiceBackend)
                .base64AuthServiceUid(authServiceUid)
                .username(USERNAME)
                .accountIsEnabled(true)
                .email(EMAIL)
                .fullName(FULL_NAME)
                .defaultRoles(Collections.emptySet())
                .build();
    }

    @Test
    public void refusesToTakeOverAnExistingInternalUserWithTheSameUsername() throws ValidationException {
        final UserDetails userDetails = externalUserDetails("external-uid");

        final User localUser = mock(User.class);
        when(localUser.isExternalUser()).thenReturn(false);
        when(userService.loadByAuthServiceUidOrUsername("external-uid", USERNAME))
                .thenReturn(Optional.of(localUser));

        assertThatThrownBy(() -> provisionerService.provision(userDetails))
                .isInstanceOf(ProvisionerServiceException.class);

        verify(userService, never()).save(isA(User.class));
    }

    @Test
    public void refusesToTakeOverAnExistingUserOfAnotherAuthService() throws ValidationException {
        final UserDetails userDetails = externalUserDetails("external-uid");

        final User foreignUser = mock(User.class);
        when(foreignUser.isExternalUser()).thenReturn(true);
        when(foreignUser.getAuthServiceId()).thenReturn("another-backend-id");
        when(userService.loadByAuthServiceUidOrUsername("external-uid", USERNAME))
                .thenReturn(Optional.of(foreignUser));

        assertThatThrownBy(() -> provisionerService.provision(userDetails))
                .isInstanceOf(ProvisionerServiceException.class);

        verify(userService, never()).save(isA(User.class));
    }

    @Test
    public void updatesAnExistingUserOfTheSameAuthServiceMatchedByUsername() throws ValidationException {
        final UserDetails userDetails = externalUserDetails("external-uid");

        final User ownUser = mock(User.class);
        when(ownUser.isExternalUser()).thenReturn(true);
        when(ownUser.getAuthServiceId()).thenReturn(BACKEND_ID);
        when(userService.loadByAuthServiceUidOrUsername("external-uid", USERNAME))
                .thenReturn(Optional.of(ownUser));

        provisionerService.provision(userDetails);

        verify(userService, times(1)).save(ownUser);
    }

    @Test
    public void updatesAnExistingUserMatchedByAuthServiceUid() throws ValidationException {
        final UserDetails userDetails = externalUserDetails("external-uid");

        // The username changed at the authentication service, so the user is found by its UID. The stored username
        // may well belong to an unrelated local account by now, but the UID proves it is the same identity.
        final User renamedUser = mock(User.class);
        when(renamedUser.isExternalUser()).thenReturn(false);
        when(renamedUser.getAuthServiceUid()).thenReturn("external-uid");
        when(userService.loadByAuthServiceUidOrUsername("external-uid", USERNAME))
                .thenReturn(Optional.of(renamedUser));

        provisionerService.provision(userDetails);

        verify(userService, times(1)).save(renamedUser);
    }
}
