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

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.shiro.subject.support.DefaultSubjectContext.AUTHENTICATED_SESSION_KEY;
import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.graylog2.security.sessions.SessionDTO.AUTH_CONTEXT_SESSION_KEY;
import static org.graylog2.security.sessions.SessionDTO.USERNAME_SESSION_KEY;

@ExtendWith(MongoDBExtension.class)
class SessionPersistenceIT {

    record TestSessionAuthContext(String someField) implements SessionAuthContext {
        static final String TYPE_NAME = "TestSessionAuthContext";

        @Override
        public String type() {
            return TYPE_NAME;
        }
    }

    EventBus eventBus;
    ClusterEventBus clusterEventBus;
    SessionService sessionService;

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService) {
        final var objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(TestSessionAuthContext.class, TestSessionAuthContext.TYPE_NAME));
        final var mongoCollections = new MongoCollections(new MongoJackObjectMapperProvider(objectMapper),
                mongoDBTestService.mongoConnection());

        eventBus = new EventBus();
        clusterEventBus = new ClusterEventBus();
        sessionService = new MongoDbSessionService(mongoCollections, clusterEventBus);
    }

    @Test
    void createSession() {
        final var referenceTime = Instant.now().truncatedTo(ChronoUnit.MILLIS).minus(1, ChronoUnit.HOURS);
        final var principals =
                new SimpleAccount("test-user-id", null, "TestRealm").getPrincipals();
        final var testAuthContext = new TestSessionAuthContext("some-value");

        final var session = new SimpleSession("localhost");
        session.setStartTimestamp(Date.from(referenceTime));
        session.setTimeout(10_000L);
        session.setExpired(false);
        session.setLastAccessTime(Date.from(referenceTime.plusSeconds(1)));
        session.setAttributes(Map.of(
                PRINCIPALS_SESSION_KEY, principals,
                AUTHENTICATED_SESSION_KEY, Boolean.TRUE,
                USERNAME_SESSION_KEY, "test-user-name",
                AUTH_CONTEXT_SESSION_KEY, testAuthContext));

        final var sessionId = (String) sessionDAO().create(session);

        assertThat(sessionDAO().readSession(sessionId)).isInstanceOfSatisfying(SimpleSession.class, simpleSession -> {
            assertThat(simpleSession.getStartTimestamp()).isEqualTo(Date.from(referenceTime));
            assertThat(simpleSession.getTimeout()).isEqualTo(10_000L);
            assertThat(simpleSession.isExpired()).isEqualTo(false);
            assertThat(simpleSession.getLastAccessTime()).isEqualTo(Date.from(referenceTime.plusSeconds(1)));
            assertThat(simpleSession.getAttributes()).isEqualTo(Map.of(
                    PRINCIPALS_SESSION_KEY, principals,
                    AUTHENTICATED_SESSION_KEY, Boolean.TRUE,
                    USERNAME_SESSION_KEY, "test-user-name",
                    AUTH_CONTEXT_SESSION_KEY, testAuthContext));
        });

        assertThat(sessionService.getBySessionId(sessionId)).hasValueSatisfying(sessionDTO -> {
            assertThat(sessionDTO.startTimestamp()).isEqualTo(referenceTime);
            assertThat(sessionDTO.timeout()).isEqualTo(10_000L);
            assertThat(sessionDTO.expired()).isEqualTo(false);
            assertThat(sessionDTO.lastAccessTime()).isEqualTo(referenceTime.plusSeconds(1));
            assertThat(sessionDTO.userId()).contains("test-user-id");
            assertThat(sessionDTO.userName()).contains("test-user-name");
            assertThat(sessionDTO.authenticationRealm()).contains("TestRealm");
            assertThat(sessionDTO.authenticated()).contains(true);
            assertThat(sessionDTO.authContext()).contains(testAuthContext);
        });
    }

    @Test
    void updateSession() {
        final var referenceTime = Instant.now().truncatedTo(ChronoUnit.MILLIS).minus(1, ChronoUnit.HOURS);
        final var principals =
                new SimpleAccount("test-user-id", null, "TestRealm").getPrincipals();
        final var testAuthContext = new TestSessionAuthContext("some-value");

        final String sessionId = (String) sessionDAO().create(new SimpleSession("example.com"));

        final var session = (SimpleSession) sessionDAO().readSession(sessionId);
        session.setStartTimestamp(Date.from(referenceTime));
        session.setTimeout(10_000L);
        session.setExpired(false);
        session.setLastAccessTime(Date.from(referenceTime.plusSeconds(1)));
        session.setAttributes(Map.of(
                PRINCIPALS_SESSION_KEY, principals,
                AUTHENTICATED_SESSION_KEY, Boolean.TRUE,
                USERNAME_SESSION_KEY, "test-user-name",
                AUTH_CONTEXT_SESSION_KEY, testAuthContext));

        sessionDAO().update(session);

        assertThat(sessionDAO().readSession(sessionId)).isInstanceOfSatisfying(SimpleSession.class, simpleSession -> {
            assertThat(simpleSession.getStartTimestamp()).isEqualTo(Date.from(referenceTime));
            assertThat(simpleSession.getTimeout()).isEqualTo(10_000L);
            assertThat(simpleSession.isExpired()).isEqualTo(false);
            assertThat(simpleSession.getLastAccessTime()).isEqualTo(Date.from(referenceTime.plusSeconds(1)));
            assertThat(simpleSession.getAttributes()).isEqualTo(Map.of(
                    PRINCIPALS_SESSION_KEY, principals,
                    AUTHENTICATED_SESSION_KEY, Boolean.TRUE,
                    USERNAME_SESSION_KEY, "test-user-name",
                    AUTH_CONTEXT_SESSION_KEY, testAuthContext));
        });

        assertThat(sessionService.getBySessionId(sessionId)).hasValueSatisfying(sessionDTO -> {
            assertThat(sessionDTO.startTimestamp()).isEqualTo(referenceTime);
            assertThat(sessionDTO.timeout()).isEqualTo(10_000L);
            assertThat(sessionDTO.expired()).isEqualTo(false);
            assertThat(sessionDTO.lastAccessTime()).isEqualTo(referenceTime.plusSeconds(1));
            assertThat(sessionDTO.userId()).contains("test-user-id");
            assertThat(sessionDTO.userName()).contains("test-user-name");
            assertThat(sessionDTO.authenticationRealm()).contains("TestRealm");
            assertThat(sessionDTO.authenticated()).contains(true);
            assertThat(sessionDTO.authContext()).contains(testAuthContext);
        });
    }

    @Test
    void deleteSession() {
        final var session = new SimpleSession("localhost");
        final var sessionId = (String) sessionDAO().create(session);
        assertThat(sessionDAO().readSession(sessionId)).isNotNull();
        assertThat(sessionService.getBySessionId(sessionId)).isNotEmpty();

        sessionDAO().delete(session);
        assertThatThrownBy(() -> sessionDAO().readSession(sessionId)).isInstanceOf(UnknownSessionException.class);
        assertThat(sessionService.getBySessionId(sessionId)).isEmpty();
    }

    @Test
    void getActiveSessions() {
        final var sessions = List.of(new SimpleSession("host1.example.com"), new SimpleSession("host2.example.com"));
        sessions.forEach(sessionDAO()::create);
        assertThat(sessionDAO().getActiveSessions()).isEqualTo(sessions);
    }

    @Test
    void expireSession() {
        final var referenceTime = Instant.now().truncatedTo(ChronoUnit.MILLIS).minus(1, ChronoUnit.HOURS);

        final var session = new SimpleSession("localhost");
        session.setStartTimestamp(Date.from(referenceTime));
        session.setTimeout(10_000L);
        session.setExpired(false);
        session.setLastAccessTime(Date.from(referenceTime.plusSeconds(1)));

        final var sessionId = (String) sessionDAO().create(session);

        assertThat(sessionService.getBySessionId(sessionId)).hasValueSatisfying(sessionDTO ->
                assertThat(sessionDTO.expired()).isFalse());

        // this will expire the session
        assertThatThrownBy(session::validate).isInstanceOf(InvalidSessionException.class);

        sessionDAO().update(session);

        assertThat(sessionService.getBySessionId(sessionId)).hasValueSatisfying(sessionDTO ->
                assertThat(sessionDTO.expired()).isTrue());
    }

    @Test
    void ignoresUnknownSessionAttributes() {
        final var session = new SimpleSession("localhost");
        session.setAttribute(USERNAME_SESSION_KEY, "test-user-name");
        session.setAttribute("some-unknown-key", "some-value");

        final var sessionId = (String) sessionDAO().create(session);
        assertThat(sessionDAO().readSession(sessionId)).satisfies(loadedSession ->
                assertThat(loadedSession.getAttributeKeys()).isEqualTo(Set.of(USERNAME_SESSION_KEY)));
    }

    @Test
    void ignoresAdditionalPrincipals() {
        final var principals = new SimplePrincipalCollection();
        principals.add("test-user-1", "test-realm-1");
        principals.add("test-user-2", "test-realm-1");
        principals.add("test-user-3", "test-realm-2");

        final var session = new SimpleSession("localhost");
        session.setAttribute(PRINCIPALS_SESSION_KEY, principals);

        final var sessionId = (String) sessionDAO().create(session);
        assertThat(sessionDAO().readSession(sessionId)).satisfies(loadedSession ->
                assertThat(loadedSession.getAttribute(PRINCIPALS_SESSION_KEY))
                        .isEqualTo(new SimplePrincipalCollection("test-user-1", "test-realm-1")));
    }

    // The session DAO is caching sessions, but we want to make sure that every operation is hitting the database,
    // therefore we create fresh instances for each operation.
    SessionDAO sessionDAO() {
        return new SessionDAO(sessionService, eventBus);
    }


}
