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
package org.graylog2.presence;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith({MongoDBExtension.class, MockitoExtension.class})
class PresenceServiceTest {
    private static final String NOTEBOOK = "grn::::notebook:5f1a00000000000000000010";
    private static final String OTHER_NOTEBOOK = "grn::::notebook:5f1a00000000000000000020";

    @Mock
    private UserService userService;

    private MutableClock clock;
    private PresenceService service;
    private User jane;
    private User bob;
    private MongoCollections mongoCollections;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.mongoCollections = mongoCollections;
        clock = new MutableClock(Instant.parse("2026-11-03T10:00:00Z"));
        jane = user("5f1a00000000000000000001", "jane", "Jane Doe");
        bob = user("5f1a00000000000000000002", "bob", null);
        final var users = Map.of(jane.getId(), jane, bob.getId(), bob);
        lenient().when(userService.loadByIds(anyCollection())).thenAnswer(invocation -> {
            final Collection<String> ids = invocation.getArgument(0);
            return ids.stream().filter(users::containsKey).map(users::get).toList();
        });
        service = new PresenceService(mongoCollections, userService, clock);
    }

    @Test
    void returnsNoViewersForUntouchedResource() {
        assertThat(service.viewers(NOTEBOOK)).isEmpty();
    }

    @Test
    void returnsViewersOfResourceSortedByName() {
        service.touch(NOTEBOOK, jane, "tab-1");
        service.touch(NOTEBOOK, bob, "tab-2");
        service.touch(OTHER_NOTEBOOK, jane, "tab-3");

        assertThat(service.viewers(NOTEBOOK)).containsExactly(
                new Viewer(bob.getId(), "bob", null),
                new Viewer(jane.getId(), "jane", "Jane Doe"));
        assertThat(service.viewers(OTHER_NOTEBOOK)).extracting(Viewer::username).containsExactly("jane");
    }

    @Test
    void countsUserWithSeveralClientsOnce() {
        service.touch(NOTEBOOK, jane, "tab-1");
        service.touch(NOTEBOOK, jane, "tab-2");

        assertThat(service.viewers(NOTEBOOK)).extracting(Viewer::username).containsExactly("jane");
    }

    @Test
    void dropsViewersAfterActiveWindow() {
        service.touch(NOTEBOOK, jane, "tab-1");
        service.touch(NOTEBOOK, bob, "tab-2");

        clock.advance(PresenceService.ACTIVE_WINDOW.minusSeconds(1));
        service.touch(NOTEBOOK, bob, "tab-2");
        clock.advance(Duration.ofSeconds(2));

        assertThat(service.viewers(NOTEBOOK)).extracting(Viewer::username).containsExactly("bob");
    }

    @Test
    void keepsUserWhileAnyClientIsActive() {
        service.touch(NOTEBOOK, jane, "tab-1");
        clock.advance(Duration.ofSeconds(8));
        service.touch(NOTEBOOK, jane, "tab-2");
        clock.advance(Duration.ofSeconds(8));

        assertThat(service.viewers(NOTEBOOK)).extracting(Viewer::username).containsExactly("jane");
    }

    @Test
    void throttlesWritesOfSameClient() {
        service.touch(NOTEBOOK, jane, "tab-1");
        clock.advance(Duration.ofSeconds(1));
        service.touch(NOTEBOOK, jane, "tab-1");
        clock.advance(PresenceService.ACTIVE_WINDOW.minusMillis(500));

        assertThat(service.viewers(NOTEBOOK)).isEmpty();
    }

    @Test
    void sharesEntriesBetweenNodes() {
        final var otherNode = new PresenceService(mongoCollections, userService, clock);
        service.touch(NOTEBOOK, jane, "tab-1");

        assertThat(otherNode.viewers(NOTEBOOK)).extracting(Viewer::username).containsExactly("jane");
    }

    @Test
    void skipsDeletedUsers() {
        service.touch(NOTEBOOK, jane, "tab-1");
        service.touch(NOTEBOOK, user("5f1a00000000000000000003", "deleted", null), "tab-2");

        assertThat(service.viewers(NOTEBOOK)).extracting(Viewer::username).containsExactly("jane");
    }

    @Test
    void removesAllEntriesOfResource() {
        service.touch(NOTEBOOK, jane, "tab-1");
        service.touch(OTHER_NOTEBOOK, jane, "tab-2");

        service.removeResource(NOTEBOOK);

        assertThat(service.viewers(NOTEBOOK)).isEmpty();
        assertThat(service.viewers(OTHER_NOTEBOOK)).hasSize(1);
    }

    @Test
    void doesNotLoadUsersWithoutViewers() {
        service.viewers(NOTEBOOK);

        verifyNoInteractions(userService);
    }

    private static User user(String id, String username, String fullName) {
        final var user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getName()).thenReturn(username);
        lenient().when(user.getFullName()).thenReturn(fullName);
        return user;
    }

    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
