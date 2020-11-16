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
package org.graylog2.system.urlwhitelist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlWhitelistTest {

    @Test
    public void isWhitelisted() {
        assertThat(UrlWhitelist.createEnabled(ImmutableList.of(LiteralWhitelistEntry.create("a", "title", "foo")))
                .isWhitelisted(".foo")).isFalse();
        assertThat(UrlWhitelist.createEnabled(ImmutableList.of(RegexWhitelistEntry.create("b", "title", "foo")))
                .isWhitelisted(".foo")).isTrue();
        assertThat(UrlWhitelist.createEnabled(ImmutableList.of(RegexWhitelistEntry.create("c", "title", "^foo$")))
                .isWhitelisted(".foo")).isFalse();
        assertThat(UrlWhitelist.createEnabled(ImmutableList.of(LiteralWhitelistEntry.create("d", "title", ".foo"
                )))
                .isWhitelisted(".foo")).isTrue();
    }

    @Test
    public void isDisabled() {
        assertThat(UrlWhitelist.create(Collections.emptyList(), false).isWhitelisted("test")).isFalse();
        assertThat(UrlWhitelist.create(Collections.emptyList(), true).isWhitelisted("test")).isTrue();
    }

    @Test
    public void serializationRoundtrip() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());

        List<WhitelistEntry> entries =
                ImmutableList.of(LiteralWhitelistEntry.create("a", "title", "https://www.graylog.com"),
                        RegexWhitelistEntry.create("b", "regex test title", "https://www\\.graylog\\.com/.*"));
        UrlWhitelist orig = UrlWhitelist.createEnabled(entries);
        String json = objectMapper.writeValueAsString(orig);
        UrlWhitelist read = objectMapper.readValue(json, UrlWhitelist.class);
        assertThat(read).isEqualTo(orig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateIds() {
        UrlWhitelist.createEnabled(ImmutableList.of(LiteralWhitelistEntry.create("a", "a", "a"),
                RegexWhitelistEntry.create("a", "b", "b")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRegex() {
        UrlWhitelist.createEnabled(ImmutableList.of(RegexWhitelistEntry.create("a", "b", "${")));
    }
}
