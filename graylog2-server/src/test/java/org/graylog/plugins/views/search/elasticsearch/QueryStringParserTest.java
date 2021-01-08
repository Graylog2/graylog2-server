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
package org.graylog.plugins.views.search.elasticsearch;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryStringParserTest {
    private final static QueryStringParser queryStringParser = new QueryStringParser();

    @Test
    void testSimpleParsing() {
        assertThat(parse("foo:bar AND some:value")).isEmpty();
        assertThat(parse("foo:$bar$ AND some:value")).containsExactly("bar");
        assertThat(parse("foo:$bar$ AND some:$value$")).containsExactlyInAnyOrder("value", "bar");
        assertThat(parse("foo:bar$")).isEmpty();
        assertThat(parse("foo:bar$ OR foo:$baz")).isEmpty();
        assertThat(parse("foo:bar$ OR foo:$baz$")).containsExactly("baz");
        assertThat(parse("foo:bar$ AND baz$:$baz$")).containsExactly("baz");
    }

    private Set<String> parse(String query) {
        return queryStringParser.parse(query).usedParameterNames();
    }
}
