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
package org.graylog2.security;

import com.google.common.eventbus.EventBus;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link MongoDbSessionDAO#readSession(Serializable)} caches sessions after a read miss,
 * preventing redundant MongoDB lookups on repeated calls.
 * <p>
 * Reproduces the wiring from {@link org.graylog2.bindings.providers.DefaultSecurityManagerProvider}:
 * SessionDAO set on DefaultSessionManager, then MemoryConstrainedCacheManager set (which propagates to the DAO).
 */
@ExtendWith(MockitoExtension.class)
class MongoDbSessionDAOCacheTest {

    @Mock
    private MongoDBSessionService sessionService;

    private MongoDbSessionDAO sessionDAO;

    @BeforeEach
    void setUp() {
        final var eventBus = new EventBus("test");
        sessionDAO = new MongoDbSessionDAO(sessionService, eventBus);

        // Wire exactly as DefaultSecurityManagerProvider does
        final var sessionManager = new DefaultSessionManager();
        sessionManager.setSessionDAO(sessionDAO);
        sessionManager.setCacheManager(new MemoryConstrainedCacheManager());
    }

    @Test
    void readSessionCachesAfterMiss() {
        // Arrange: a session exists in MongoDB but not in cache
        final String sessionId = "test-session-id";
        final var dbSession = buildMongoDbSession(sessionId);
        final var simpleSession = buildSimpleSession(sessionId);

        when(sessionService.load(sessionId)).thenReturn(dbSession);
        when(sessionService.daoToSimpleSession(dbSession)).thenReturn(simpleSession);

        // Act: read the session 10 times
        for (int i = 0; i < 10; i++) {
            final Session read = sessionDAO.readSession(sessionId);
            assertThat(read).isNotNull();
            assertThat(read.getId()).isEqualTo(sessionId);
        }

        // Assert: MongoDB should have been hit exactly once (first read), rest are cache hits
        verify(sessionService, times(1)).load(sessionId);
    }

    @Test
    void readSessionReturnsCachedSessionAfterCreate() {
        // Arrange: mock the persistence layer
        when(sessionService.saveWithoutValidation(any(MongoDbSession.class))).thenReturn("mongo-pk-1");

        final var testSession = new SimpleSession();
        testSession.setTimeout(3600000);
        testSession.setHost("127.0.0.1");

        // Act: create the session (populates cache via CachingSessionDAO.create)
        final Serializable createdId = sessionDAO.create(testSession);
        assertThat(createdId).isNotNull();

        // Stub the fallback in case cache misses
        lenient().when(sessionService.load(anyString())).thenReturn(buildMongoDbSession(createdId.toString()));
        lenient().when(sessionService.daoToSimpleSession(any())).thenReturn(testSession);

        // Act: read the session 10 times
        for (int i = 0; i < 10; i++) {
            final Session read = sessionDAO.readSession(createdId);
            assertThat(read).isNotNull();
        }

        // Assert: load() should never be called -- all reads served from cache
        verify(sessionService, times(0)).load(anyString());
    }

    @Test
    void readSessionThrowsForNonExistentSession() {
        final String sessionId = "nonexistent-session";
        when(sessionService.load(sessionId)).thenReturn(null);

        assertThatThrownBy(() -> sessionDAO.readSession(sessionId))
                .isInstanceOf(UnknownSessionException.class);
    }

    @Test
    void crossNodeReadCachesOnFirstAccess() {
        // Simulates a fan-out scenario: node2 receives a session ID it never created.
        // The first read should hit MongoDB and cache; subsequent reads should be cache hits.
        final String sessionId = "fanout-session-id";
        final var dbSession = buildMongoDbSession(sessionId);
        final var simpleSession = buildSimpleSession(sessionId);

        when(sessionService.load(sessionId)).thenReturn(dbSession);
        when(sessionService.daoToSimpleSession(dbSession)).thenReturn(simpleSession);

        // First read: cache miss, hits MongoDB
        final Session first = sessionDAO.readSession(sessionId);
        assertThat(first).isNotNull();
        verify(sessionService, times(1)).load(sessionId);

        // Next 50 reads: all cache hits
        for (int i = 0; i < 50; i++) {
            final Session read = sessionDAO.readSession(sessionId);
            assertThat(read).isNotNull();
        }

        // Still only 1 MongoDB call total
        verify(sessionService, times(1)).load(sessionId);
    }

    private MongoDbSession buildMongoDbSession(String sessionId) {
        final var fields = new java.util.HashMap<String, Object>();
        fields.put("session_id", sessionId);
        fields.put("host", "127.0.0.1");
        fields.put("start_timestamp", new Date());
        fields.put("last_access_time", new Date());
        fields.put("timeout", 3600000L);
        fields.put("expired", false);
        return new MongoDbSession(fields);
    }

    private SimpleSession buildSimpleSession(String sessionId) {
        final var session = new SimpleSession();
        session.setId(sessionId);
        session.setTimeout(3600000);
        session.setHost("127.0.0.1");
        return session;
    }
}
