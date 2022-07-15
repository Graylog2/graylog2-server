package org.graylog2.database.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScopedEntityTest {

    public static final String ARBITRARY_SCOPE = "a-scope";
    public static final String TITLE = "title";

    @Test
    void testDefaultScope() {
        final TestScopedEntity scopedEntity = TestScopedEntity.builder().title(TITLE).build();
        assertEquals(DefaultEntityScope.NAME, scopedEntity.scope());
        assertEquals(TITLE, scopedEntity.title());
    }

    @Test
    void testExplicitScope() {
        final TestScopedEntity scopedEntity = TestScopedEntity.builder().title(TITLE).scope(ARBITRARY_SCOPE).build();
        assertEquals(ARBITRARY_SCOPE, scopedEntity.scope());
        assertEquals(TITLE, scopedEntity.title());
    }

    @Test
    void testNullScope() {
        NullPointerException exception = Assertions.assertThrows(NullPointerException.class, () -> {
            TestScopedEntity.builder().title(TITLE).scope(null).build();
        });
        Assertions.assertEquals("Null scope", exception.getMessage());
    }
}
