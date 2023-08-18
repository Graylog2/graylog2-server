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
package org.graylog2.utilities.uri;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.graylog2.utilities.uri.TransportAddressSanitizer.INVALID_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportAddressSanitizerTest {

    private TransportAddressSanitizer toTest;

    @BeforeEach
    void setUp() {
        toTest = new TransportAddressSanitizer();
    }

    @Test
    void testRemovesCredentialsProperly() {
        String sanitized = toTest.withRemovedCredentials("https://admin:changeit@test-graylog:9200");
        assertEquals("https://test-graylog:9200", sanitized);

        sanitized = toTest.withRemovedCredentials("http://admin:changeit@test-graylog:9200");
        assertEquals("http://test-graylog:9200", sanitized);

        sanitized = toTest.withRemovedCredentials("http://admin:changeit@test-graylog:9200/bajobongo");
        assertEquals("http://test-graylog:9200/bajobongo", sanitized);
    }

    @Test
    void testReturnsInvalidUriStringOnWrongUri() {
        final String sanitized = toTest.withRemovedCredentials("https:::\\dfd!");
        assertEquals(INVALID_URI, sanitized);
    }

    @Test
    void testDoesNotChangeUriWithoutCredentials() {
        String sanitized = toTest.withRemovedCredentials("https://test-graylog:9200");
        assertEquals("https://test-graylog:9200", sanitized);

        sanitized = toTest.withRemovedCredentials("http://test-graylog:9200");
        assertEquals("http://test-graylog:9200", sanitized);

        sanitized = toTest.withRemovedCredentials("http://test-graylog:9200/bajobongo");
        assertEquals("http://test-graylog:9200/bajobongo", sanitized);
    }
}
