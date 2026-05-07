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

import com.google.common.eventbus.EventBus;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link SessionDAO}'s cache (via Shiro's {@link org.apache.shiro.session.mgt.eis.CachingSessionDAO})
 * actually prevents redundant MongoDB lookups on repeated {@code readSession()} calls.
 * <p>
 * This test reproduces the exact wiring from {@code DefaultSecurityManagerProvider}:
 * <ol>
 *   <li>Create a {@code SessionDAO} (extends {@code CachingSessionDAO})</li>
 *   <li>Set it on a {@code DefaultSessionManager}</li>
 *   <li>Set a {@code MemoryConstrainedCacheManager} on the session manager (which propagates to the DAO)</li>
 *   <li>Create a session (which should cache it)</li>
 *   <li>Read the session multiple times (should be cache hits, NOT MongoDB reads)</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class SessionDAOCacheTest {

    @Mock
    private SessionService sessionService;

    private SessionDAO sessionDAO;
    private DefaultSecurityManager securityManager;

    @BeforeEach
    void setUp() {
        final var eventBus = new EventBus("test");
        sessionDAO = new SessionDAO(sessionService, eventBus);

        // Wire exactly as DefaultSecurityManagerProvider does:
        securityManager = new DefaultSecurityManager();

        final var subjectDAO = new DefaultSubjectDAO();
        final var eval = new DefaultSessionStorageEvaluator();
        eval.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(eval);
        securityManager.setSubjectDAO(subjectDAO);

        final var sessionManager = (DefaultSessionManager) securityManager.getSessionManager();
        sessionManager.setSessionDAO(sessionDAO);       // line 87
        sessionManager.setDeleteInvalidSessions(true);  // line 88
        sessionManager.setCacheManager(new MemoryConstrainedCacheManager()); // line 90

        SecurityUtils.setSecurityManager(securityManager);
    }

    @Test
    void readSessionShouldHitCacheAfterCreate() {
        // Arrange: mock the MongoDB service
        when(sessionService.create(any(SessionDTO.class))).thenReturn("mongo-pk-1");

        final var testSession = buildTestSession();

        // Act: create the session (should populate the cache)
        final Serializable sessionId = sessionDAO.create(testSession);
        assertThat(sessionId).isNotNull();

        // Stub readSession fallback in case cache misses
        lenient().when(sessionService.getBySessionId(anyString()))
                .thenReturn(Optional.of(SessionDTO.builderFromSimpleSession(testSession).build()));

        // Act: read the session 10 times
        for (int i = 0; i < 10; i++) {
            final Session read = sessionDAO.readSession(sessionId);
            assertThat(read).isNotNull();
        }

        // Assert: getBySessionId should NOT have been called -- all reads should be cache hits
        verify(sessionService, times(0)).getBySessionId(anyString());
    }

    @Test
    void readSessionFallsBackToMongoOnCacheMiss() {
        // Arrange: create a session ID that was never cached via create()
        final String sessionId = "not-in-cache-session-id";
        final var dto = SessionDTO.builder()
                .sessionId(sessionId)
                .timeout(3600000)
                .startTimestamp(Instant.now())
                .lastAccessTime(Instant.now())
                .expired(false)
                .build();

        when(sessionService.getBySessionId(sessionId)).thenReturn(Optional.of(dto));

        // Act: read the session
        final Session read = sessionDAO.readSession(sessionId);

        // Assert: should have fallen back to MongoDB
        assertThat(read).isNotNull();
        verify(sessionService, times(1)).getBySessionId(sessionId);
    }

    @Test
    void readSessionShouldNotCacheAfterMiss() {
        // This test documents the current (broken?) behavior: readSession() does NOT
        // cache the result after a miss, so every call hits MongoDB.
        final String sessionId = "uncached-session-id";
        final var dto = SessionDTO.builder()
                .sessionId(sessionId)
                .timeout(3600000)
                .startTimestamp(Instant.now())
                .lastAccessTime(Instant.now())
                .expired(false)
                .build();

        when(sessionService.getBySessionId(sessionId)).thenReturn(Optional.of(dto));

        // Read 5 times
        for (int i = 0; i < 5; i++) {
            final Session read = sessionDAO.readSession(sessionId);
            assertThat(read).isNotNull();
        }

        // Each read hits MongoDB because readSession() doesn't cache the result
        verify(sessionService, times(5)).getBySessionId(sessionId);
    }

    @Test
    void sessionCreatedViaSecurityManagerShouldBeCached() {
        // This test reproduces the FULL runtime path:
        // 1. Create session via securityManager.start() (like SessionCreator.createForSubject)
        // 2. Read session via Subject.Builder.sessionId().buildSubject() (like SessionAuthenticator)

        // Arrange
        when(sessionService.create(any(SessionDTO.class))).thenReturn("mongo-pk-1");

        // Step 1: Create a session the way SessionCreator does
        final Subject loginSubject = new Subject.Builder(securityManager)
                .host("127.0.0.1")
                .sessionCreationEnabled(true)
                .buildSubject();

        // getSession(true) triggers securityManager.start() -> sessionDAO.create() -> cache()
        final Session createdSession = loginSubject.getSession(true);
        assertThat(createdSession).isNotNull();
        final Serializable sessionId = createdSession.getId();
        assertThat(sessionId).isNotNull();

        // Stub getBySessionId in case cache misses
        final var dto = SessionDTO.builder()
                .sessionId(sessionId.toString())
                .timeout(createdSession.getTimeout())
                .startTimestamp(Instant.now())
                .lastAccessTime(Instant.now())
                .expired(false)
                .build();
        lenient().when(sessionService.getBySessionId(anyString())).thenReturn(Optional.of(dto));

        // Clear invocation counts from session creation
        clearInvocations(sessionService);

        // Step 2: Resolve the session the way SessionAuthenticator does (10 times)
        for (int i = 0; i < 10; i++) {
            final Subject authSubject = new Subject.Builder(securityManager)
                    .sessionId(sessionId)
                    .buildSubject();
            final Session resolved = authSubject.getSession(false);
            assertThat(resolved).isNotNull();
            assertThat(resolved.getId()).isEqualTo(sessionId);
        }

        // Assert: if the cache works, getBySessionId should NOT have been called
        verify(sessionService, times(0)).getBySessionId(anyString());
    }

    private SimpleSession buildTestSession() {
        final var session = new SimpleSession();
        session.setTimeout(3600000);
        session.setHost("127.0.0.1");
        return session;
    }
}
