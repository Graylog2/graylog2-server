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
package org.graylog2.security.sessions;

import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.graylog2.rest.models.system.sessions.SessionUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConverterTest {
    record TestAuthContext(String type) implements SessionAuthContext {}

    @Test
    void sessionDTOToSimpleSession() {
        final var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        final var sessionDTO = SessionDTO.builder()
                .id("db-id")
                .sessionId("session-id")
                .authenticationRealm("realm")
                .host("localhost")
                .userId("user-id")
                .userName("user-name")
                .authenticated(true)
                .expired(false)
                .timeout(10_000)
                .startTimestamp(now.minusSeconds(10))
                .lastAccessTime(now)
                .authContext(new TestAuthContext("TEST"))
                .build();

        final var simpleSession = SessionConverter.sessionDTOToSimpleSession(sessionDTO);
        assertThat(simpleSession.getId()).isEqualTo("session-id");
        assertThat(simpleSession.getHost()).isEqualTo("localhost");
        assertThat(simpleSession.getTimeout()).isEqualTo(10_000);
        assertThat(simpleSession.getStartTimestamp().toInstant()).isEqualTo(now.minusSeconds(10));
        assertThat(simpleSession.getLastAccessTime().toInstant()).isEqualTo(now);
        assertThat(simpleSession.isExpired()).isFalse();

        // Verify session attributes
        assertThat(simpleSession.getAttribute(SessionUtils.USERNAME_SESSION_KEY)).isEqualTo("user-name");
        assertThat(simpleSession.getAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY)).isEqualTo(true);
        assertThat(simpleSession.getAttribute(SessionUtils.AUTH_CONTEXT_SESSION_KEY)).isEqualTo(new TestAuthContext("TEST"));

        // Verify principal collection
        final var principals = simpleSession.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
        assertThat(principals).isInstanceOf(SimplePrincipalCollection.class);
        final var principalCollection = (SimplePrincipalCollection) principals;
        assertThat(principalCollection.getPrimaryPrincipal()).isEqualTo("user-id");
        assertThat(principalCollection.getRealmNames()).containsExactly("realm");
    }

    @Test
    void simpleSessionToSessionDTO() {
        final var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        final var simpleSession = new SimpleSession();
        simpleSession.setId("session-id");
        simpleSession.setHost("localhost");
        simpleSession.setTimeout(10_000);
        simpleSession.setStartTimestamp(java.util.Date.from(now.minusSeconds(10)));
        simpleSession.setLastAccessTime(java.util.Date.from(now));
        simpleSession.setExpired(false);
        simpleSession.setAttribute(SessionUtils.USERNAME_SESSION_KEY, "user-name");
        simpleSession.setAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY, true);
        simpleSession.setAttribute(SessionUtils.AUTH_CONTEXT_SESSION_KEY, new TestAuthContext("TEST"));
        simpleSession.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY,
                new SimplePrincipalCollection("user-id", "realm"));

        final var sessionDTO = SessionConverter.simpleSessionToSessionDTO(simpleSession);

        assertThat(sessionDTO.sessionId()).isEqualTo("session-id");
        assertThat(sessionDTO.host()).contains("localhost");
        assertThat(sessionDTO.timeout()).isEqualTo(10_000);
        assertThat(sessionDTO.startTimestamp()).isEqualTo(now.minusSeconds(10));
        assertThat(sessionDTO.lastAccessTime()).isEqualTo(now);
        assertThat(sessionDTO.expired()).isFalse();
        assertThat(sessionDTO.userName()).contains("user-name");
        assertThat(sessionDTO.authenticated()).contains(Boolean.TRUE);
        assertThat(sessionDTO.authContext()).contains(new TestAuthContext("TEST"));
        assertThat(sessionDTO.userId()).contains("user-id");
        assertThat(sessionDTO.authenticationRealm()).contains("realm");
    }

    @Test
    void failsForUnknownAttributes() {
        final var simpleSession = new SimpleSession();
        simpleSession.setId("session-id");
        simpleSession.setHost("localhost");
        simpleSession.setAttribute("unknown-key", "some-value");

        assertThatThrownBy(() -> SessionConverter.simpleSessionToSessionDTO(simpleSession))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown attribute keys");
    }

    @Test
    void hasStrictPrincipalsHandlingg() {
        final var simpleSession = new SimpleSession();
        simpleSession.setId("session-id");
        simpleSession.setHost("localhost");

        assertThat(SessionConverter.simpleSessionToSessionDTO(simpleSession).userId()).isEmpty();

        simpleSession.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, new SimplePrincipalCollection());
        assertThat(SessionConverter.simpleSessionToSessionDTO(simpleSession).userId()).isEmpty();

        simpleSession.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, "not-a-collection");
        assertThatThrownBy(() -> SessionConverter.simpleSessionToSessionDTO(simpleSession))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected type");

        simpleSession.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY,
                new SimplePrincipalCollection(List.of("a", "b", "c"), "realm"));
        assertThatThrownBy(() -> SessionConverter.simpleSessionToSessionDTO(simpleSession))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a single principal");

        simpleSession.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY,
                new SimplePrincipalCollection(1, "realm"));
        assertThatThrownBy(() -> SessionConverter.simpleSessionToSessionDTO(simpleSession))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected type");
    }
}
