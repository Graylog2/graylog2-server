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
package org.graylog2.shared.security;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.subject.ImmutablePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessTokenAudienceFilterTest {
    private ContainerRequestContext requestContext;
    private Subject subject;

    @BeforeEach
    void setUp() {
        requestContext = mock(ContainerRequestContext.class);
        subject = mock(Subject.class);
        final ShiroSecurityContext securityContext = mock(ShiroSecurityContext.class);
        when(securityContext.getSubject()).thenReturn(subject);
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(requestContext.getUriInfo()).thenReturn(mock(UriInfo.class));
    }

    @Test
    void rejectsRestrictedTokenOnOtherResources() {
        authenticatedWithToken("mcp");

        assertThatThrownBy(() -> new AccessTokenAudienceFilter(null).filter(requestContext))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> new AccessTokenAudienceFilter("other").filter(requestContext))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void allowsRestrictedTokenOnMatchingResource() {
        authenticatedWithToken("mcp");

        assertThatCode(() -> new AccessTokenAudienceFilter("mcp").filter(requestContext)).doesNotThrowAnyException();
    }

    @Test
    void allowsUnrestrictedTokensEverywhere() {
        authenticatedWithToken(null);

        assertThatCode(() -> new AccessTokenAudienceFilter(null).filter(requestContext)).doesNotThrowAnyException();
        assertThatCode(() -> new AccessTokenAudienceFilter("mcp").filter(requestContext)).doesNotThrowAnyException();
    }

    @Test
    void allowsSessionsWithoutTokenPrincipal() {
        when(subject.getPrincipals()).thenReturn(ImmutablePrincipalCollection.ofSinglePrincipal("user-id", "realm"));

        assertThatCode(() -> new AccessTokenAudienceFilter(null).filter(requestContext)).doesNotThrowAnyException();
    }

    private void authenticatedWithToken(String audience) {
        when(subject.getPrincipals()).thenReturn(ImmutablePrincipalCollection.ofSingleRealm(
                List.of("user-id", new AccessTokenPrincipal("token-id", audience)), "realm"));
    }
}
