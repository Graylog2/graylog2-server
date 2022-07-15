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
