package org.graylog2.security;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class MongoDbSessionTest {

    private Map<String, Object> fields;

    @BeforeEach
    void setUp() {
        fields = new HashMap<>();
        fields.put("session_id", "session-id");
    }

    @Test
    public void noPrincipal() {
        assertThat(new MongoDbSession(fields).getUserIdAttribute()).isEmpty();
    }

    @Test
    public void singlePrincipal() {
        final MongoDbSession session = new MongoDbSession(fields);
        session.setAttributes(ImmutableMap.of(PRINCIPALS_SESSION_KEY, "a-user-id"));
        assertThat(session.getUserIdAttribute()).contains("a-user-id");
    }

    @Test
    public void emptyPrincipalsCollection() {
        final MongoDbSession session = new MongoDbSession(fields);
        session.setAttributes(ImmutableMap.of(PRINCIPALS_SESSION_KEY, Collections.emptyList()));
        assertThat(session.getUserIdAttribute()).isEmpty();
    }

    @Test
    public void principalsCollection() {
        final MongoDbSession session = new MongoDbSession(fields);
        session.setAttributes(ImmutableMap.of(PRINCIPALS_SESSION_KEY,
                ImmutableList.of("a-user-id", "secondary-principal")));
        assertThat(session.getUserIdAttribute()).contains("a-user-id");
    }
}
