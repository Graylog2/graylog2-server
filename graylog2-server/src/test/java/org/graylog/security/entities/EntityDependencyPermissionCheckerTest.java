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
package org.graylog.security.entities;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.BuiltinCapabilities;
import org.graylog.security.GranteeAuthorizer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EntityDependencyPermissionCheckerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private GranteeAuthorizer.Factory userAuthorizerFactory;

    private EntityDependencyPermissionChecker resolver;
    private GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();

    @Before
    public void setUp() throws Exception {
        this.resolver = new EntityDependencyPermissionChecker(userAuthorizerFactory, new BuiltinCapabilities());
    }

    @Test
    public void checkWithPermitted() {
        // Read is permitted on the stream so we expect to receive an empty result from the check
        assertThat(runCheck(true, true)).satisfies(result -> {
            assertThat(result.values()).isEmpty();
        });
    }

    @Test
    public void checkWithDenied() {
        // Read is NOT permitted on the stream so we expect to receive the stream in the result
        assertThat(runCheck(true, false)).satisfies(result -> {
            assertThat(result.values()).isNotEmpty();
        });
    }

    @Test
    public void checkWithDeniedAndSharingUserDenied() {
        // Since the sharing user cannot view the dependency, we don't return it in the result even though the
        // grantee cannot access it (to avoid exposing the dependency)
        assertThat(runCheck(false, true)).satisfies(result -> {
            assertThat(result.values()).isEmpty();
        });
    }

    private ImmutableMultimap<GRN, EntityDescriptor> runCheck(boolean isSharingUserAuthorized, boolean isGranteeUserAuthorized) {
        final GRN granteeUser = grnRegistry.newGRN("user", "john");
        final GRN sharingUser = grnRegistry.newGRN("user", "jane");
        final GRN stream = grnRegistry.newGRN("stream", "54e3deadbeefdeadbeef0001");
        final GranteeAuthorizer sharingUserAuthorizer = mock(GranteeAuthorizer.class);
        final GranteeAuthorizer granteeUserAuthorizer = mock(GranteeAuthorizer.class);
        final ImmutableSet<GRN> selectedGrantees = ImmutableSet.of(granteeUser);
        final EntityDescriptor dependency = EntityDescriptor.create(stream, "Title", ImmutableSet.of());
        final ImmutableSet<EntityDescriptor> dependencies = ImmutableSet.of(dependency);

        when(userAuthorizerFactory.create(sharingUser)).thenReturn(sharingUserAuthorizer);
        when(userAuthorizerFactory.create(granteeUser)).thenReturn(granteeUserAuthorizer);

        when(sharingUserAuthorizer.isPermitted(anyString(), any(GRN.class))).thenReturn(isSharingUserAuthorized);
        when(granteeUserAuthorizer.isPermitted("streams:read", stream)).thenReturn(isGranteeUserAuthorized);

        final ImmutableMultimap<GRN, EntityDescriptor> checkResult = resolver.check(sharingUser, dependencies, selectedGrantees);

        verify(sharingUserAuthorizer, times(1)).isPermitted("streams:read", stream);
        verifyNoMoreInteractions(sharingUserAuthorizer);

        return checkResult;
    }
}
