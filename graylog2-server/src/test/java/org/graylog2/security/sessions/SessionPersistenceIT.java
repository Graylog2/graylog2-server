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
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
        final var principals =
                new SimpleAccount("test-user-id", null, "TestRealm").getPrincipals();
        final var testAuthContext = new TestSessionAuthContext("some-value");

        final var session = new SimpleSession("localhost");
        session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, principals);
        session.setAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY, Boolean.TRUE);
        session.setAttribute(SessionDTO.USERNAME_SESSION_KEY, "test-user-name");
        session.setAttribute(SessionDTO.AUTH_CONTEXT_SESSION_KEY, testAuthContext);

        final var sessionId = sessionDAO().create(session);
        final var loadedSession = sessionDAO().readSession(sessionId);
        assertThat(loadedSession).isEqualTo(session);

        final var persistedDto = sessionService.getBySessionId((String) sessionId);
        assertThat(persistedDto).hasValueSatisfying(sessionDTO -> {
            assertThat(sessionDTO.userId()).contains("test-user-id");
            assertThat(sessionDTO.userName()).contains("test-user-name");
            assertThat(sessionDTO.authenticationRealm()).contains("TestRealm");
            assertThat(sessionDTO.authenticated()).contains(true);
            assertThat(sessionDTO.authContext()).contains(testAuthContext);
        });
    }

    @Test
    void updateSession() {
        final var session = new SimpleSession("localhost");
        final String sessionId = (String) sessionDAO().create(session);

        assertThat(sessionDAO().readSession(sessionId)).isEqualTo(session).satisfies(loadedSession -> {
                    assertThat(loadedSession.getTimeout()).isEqualTo(session.getTimeout());
                    assertThat(loadedSession.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY)).isNull();
                    assertThat(loadedSession.getAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY)).isNull();
            assertThat(loadedSession.getAttribute(SessionDTO.USERNAME_SESSION_KEY)).isNull();
            assertThat(loadedSession.getAttribute(SessionDTO.AUTH_CONTEXT_SESSION_KEY)).isNull();
                }
        );
        assertThat(sessionService.getBySessionId(sessionId)).hasValueSatisfying(sessionDTO -> {
            assertThat(sessionDTO.timeout()).isEqualTo(session.getTimeout());
            assertThat(sessionDTO.userId()).isEmpty();
            assertThat(sessionDTO.userName()).isEmpty();
            assertThat(sessionDTO.authenticationRealm()).isEmpty();
            assertThat(sessionDTO.authenticated()).isEmpty();
            assertThat(sessionDTO.authContext()).isEmpty();
        });

        final var principals =
                new SimpleAccount("test-user-id", null, "TestRealm").getPrincipals();
        final var testAuthContext = new TestSessionAuthContext("some-value");

        session.setTimeout(12345L);
        session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, principals);
        session.setAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY, Boolean.TRUE);
        session.setAttribute(SessionDTO.USERNAME_SESSION_KEY, "test-user-name");
        session.setAttribute(SessionDTO.AUTH_CONTEXT_SESSION_KEY, testAuthContext);

        sessionDAO().update(session);

        assertThat(sessionDAO().readSession(sessionId)).isEqualTo(session).satisfies(loadedSession -> {
                    assertThat(loadedSession.getTimeout()).isEqualTo(12345L);
                    assertThat(loadedSession.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY))
                            .isEqualTo(principals);
                    assertThat(loadedSession.getAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY))
                            .isEqualTo(Boolean.TRUE);
            assertThat(loadedSession.getAttribute(SessionDTO.USERNAME_SESSION_KEY))
                            .isEqualTo("test-user-name");
            assertThat(loadedSession.getAttribute(SessionDTO.AUTH_CONTEXT_SESSION_KEY))
                            .isEqualTo(testAuthContext);
                }
        );
        assertThat(sessionService.getBySessionId(sessionId)).hasValueSatisfying(sessionDTO -> {
            assertThat(sessionDTO.timeout()).isEqualTo(12345L);
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
    void ignoresUnknownSessionAttributes() {
        final var session = new SimpleSession("localhost");
        session.setAttribute(SessionDTO.USERNAME_SESSION_KEY, "test-user-name");
        session.setAttribute("some-unknown-key", "some-value");

        final var sessionId = (String) sessionDAO().create(session);
        assertThat(sessionDAO().readSession(sessionId)).satisfies(loadedSession ->
                assertThat(loadedSession.getAttributeKeys()).isEqualTo(Set.of(SessionDTO.USERNAME_SESSION_KEY)));
    }

    @Test
    void ignoresAdditionalPrincipals() {
        final var principals = new SimplePrincipalCollection();
        principals.add("test-user-1", "test-realm-1");
        principals.add("test-user-2", "test-realm-1");
        principals.add("test-user-3", "test-realm-2");

        final var session = new SimpleSession("localhost");
        session.setAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY, principals);

        final var sessionId = (String) sessionDAO().create(session);
        assertThat(sessionDAO().readSession(sessionId)).satisfies(loadedSession ->
                assertThat(loadedSession.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY))
                        .isEqualTo(new SimplePrincipalCollection("test-user-1", "test-realm-1")));
    }

    // The session DAO is caching sessions, but we want to make sure that every operation is hitting the database,
    // therefore we create fresh instances for each operation.
    SessionDAO sessionDAO() {
        return new SessionDAO(sessionService, eventBus);
    }


}
