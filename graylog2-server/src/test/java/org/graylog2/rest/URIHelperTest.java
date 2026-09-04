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
package org.graylog2.rest;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class URIHelperTest {

    @Test
    void resolvesRelativePathAgainstBaseUri() {
        final var helper = new URIHelper(URI.create("https://example.com/graylog/"));

        assertThat(helper.resolve("api/openapi")).isEqualTo(URI.create("https://example.com/graylog/api/openapi"));
    }

    @Test
    void returnsAbsoluteUriUnchanged() {
        // URI#resolve returns the argument unchanged when it is itself absolute
        final var helper = new URIHelper(URI.create("https://example.com/"));

        assertThat(helper.resolve("https://other.example.com/foo"))
                .isEqualTo(URI.create("https://other.example.com/foo"));
    }

    @Test
    void exposesBaseUri() {
        final var base = URI.create("https://example.com/graylog/");
        final var helper = new URIHelper(base);

        assertThat(helper.baseUri()).isEqualTo(base);
    }

    @Test
    void requiresNonNullBaseUri() {
        assertThatNullPointerException().isThrownBy(() -> new URIHelper(null));
    }
}
