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
package org.graylog2.system.urlallowlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlAllowlistTest {

    @Test
    public void isAllowlisted() {
        assertThat(UrlAllowlist.createEnabled(ImmutableList.of(LiteralAllowlistEntry.create("a", "title", "foo")))
                .isAllowlisted(".foo")).isFalse();
        assertThat(UrlAllowlist.createEnabled(ImmutableList.of(RegexAllowlistEntry.create("b", "title", "foo")))
                .isAllowlisted(".foo")).isTrue();
        assertThat(UrlAllowlist.createEnabled(ImmutableList.of(RegexAllowlistEntry.create("c", "title", "^foo$")))
                .isAllowlisted(".foo")).isFalse();
        assertThat(UrlAllowlist.createEnabled(ImmutableList.of(LiteralAllowlistEntry.create("d", "title", ".foo"
                )))
                .isAllowlisted(".foo")).isTrue();
    }

    @Test
    public void isDisabled() {
        assertThat(UrlAllowlist.create(Collections.emptyList(), false).isAllowlisted("test")).isFalse();
        assertThat(UrlAllowlist.create(Collections.emptyList(), true).isAllowlisted("test")).isTrue();
    }

    @Test
    public void serializationRoundtrip() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());

        List<AllowlistEntry> entries =
                ImmutableList.of(LiteralAllowlistEntry.create("a", "title", "https://www.graylog.com"),
                        RegexAllowlistEntry.create("b", "regex test title", "https://www\\.graylog\\.com/.*"));
        UrlAllowlist orig = UrlAllowlist.createEnabled(entries);
        String json = objectMapper.writeValueAsString(orig);
        UrlAllowlist read = objectMapper.readValue(json, UrlAllowlist.class);
        assertThat(read).isEqualTo(orig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateIds() {
        UrlAllowlist.createEnabled(ImmutableList.of(LiteralAllowlistEntry.create("a", "a", "a"),
                RegexAllowlistEntry.create("a", "b", "b")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRegex() {
        UrlAllowlist.createEnabled(ImmutableList.of(RegexAllowlistEntry.create("a", "b", "${")));
    }
}
