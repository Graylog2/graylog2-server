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

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class SessionDTOTest {

    private Map<String, Object> fields;
    private SessionDTO session;

    @BeforeEach
    void setUp() {
        session = SessionDTO.builder()
                .id(new ObjectId().toHexString())
                .sessionId("session-id")
                .attributes(Collections.emptyMap())
                .expired(false)
                .host("localhost")
                .startTimestamp(Instant.now())
                .lastAccessTime(Instant.now())
                .timeout(10)
                .build();
    }

    @Test
    public void noPrincipal() {
        assertThat(session.userId()).isEmpty();
    }

    @Test
    public void singlePrincipal() {
        session = session.toBuilder().attributes(Map.of(PRINCIPALS_SESSION_KEY, "a-user-id")).build();
        assertThat(session.userId()).contains("a-user-id");
    }

    @Test
    public void emptyPrincipalsCollection() {
        session = session.toBuilder().attributes(Map.of(PRINCIPALS_SESSION_KEY, Collections.emptyList())).build();
        assertThat(session.userId()).isEmpty();
    }

    @Test
    public void principalsCollection() {
        session = session.toBuilder()
                .attributes(Map.of(PRINCIPALS_SESSION_KEY, List.of("a-user-id", "secondary-principal")))
                .build();
        assertThat(session.userId()).contains("a-user-id");
    }
}
