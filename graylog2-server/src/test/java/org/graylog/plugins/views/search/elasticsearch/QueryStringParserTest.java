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

import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.validation.SubstringMultilinePosition;
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
    }

    @Test
    void testRepeatedParamUsage() {
        assertThat(parse("foo:$bar$ AND lorem:$bar")).containsExactly("bar");
    }

    @Test
    void testStringsContainingDollars() {
        assertThat(parse("foo:bar$")).isEmpty();
        assertThat(parse("foo:bar$ OR foo:$baz")).isEmpty();
        assertThat(parse("foo:bar$ OR foo:$baz$")).containsExactly("baz");
        assertThat(parse("foo:$bar$ OR foo:$baz")).containsExactly("bar");
        assertThat(parse("foo:bar$ AND baz$:$baz$")).containsExactly("baz");
        assertThat(parse("foo:$$")).isEmpty();
        assertThat(parse("foo:$foo$ AND bar:$$")).containsExactly("foo");
    }

    @Test
    void testCharacterSpaceOfParameterNames() {
        assertThat(parse("foo:$some parameter$")).isEmpty();
        assertThat(parse("foo:$some-parameter$")).isEmpty();
        assertThat(parse("foo:$some/parameter$")).isEmpty();
        assertThat(parse("foo:$some42parameter$")).containsExactly("some42parameter");
        assertThat(parse("foo:$42parameter$")).isEmpty();
        assertThat(parse("foo:$parameter42$")).containsExactly("parameter42");
        assertThat(parse("foo:$someparameter$")).containsExactly("someparameter");
        assertThat(parse("foo:$some_parameter$")).containsExactly("some_parameter");
        assertThat(parse("foo:$_someparameter$")).containsExactly("_someparameter");
        assertThat(parse("foo:$_someparameter_$")).containsExactly("_someparameter_");
        assertThat(parse("foo:$_someparameter_$")).containsExactly("_someparameter_");
        assertThat(parse("foo:$_$")).containsExactly("_");
        assertThat(parse("foo:$s$")).containsExactly("s");
        assertThat(parse("foo:$9$")).isEmpty();
    }

    @Test
    void testParamPositions() {
        final QueryMetadata metadata = queryStringParser.parse("foo:$bar$ AND lorem:$bar$");
        assertThat(metadata.usedParameters().size()).isEqualTo(1);
        final QueryParam param = metadata.usedParameters().iterator().next();
        assertThat(param.name()).isEqualTo("bar");
        assertThat(param.positions()).containsExactly(
                SubstringMultilinePosition.create(1, 4, 9),
                SubstringMultilinePosition.create(1, 20, 25)
        );
    }

    @Test
    void testParamPositionsMultiline() {
        final QueryMetadata metadata = queryStringParser.parse("foo:$bar$ AND\nlorem:$bar$ OR ipsum:$bar$");
        assertThat(metadata.usedParameters().size()).isEqualTo(1);
        final QueryParam param = metadata.usedParameters().iterator().next();
        assertThat(param.name()).isEqualTo("bar");
        assertThat(param.positions()).containsExactly(
                SubstringMultilinePosition.create(1, 4, 9),
                SubstringMultilinePosition.create(2, 6, 11),
                SubstringMultilinePosition.create(2, 21, 26)
        );
    }

    private Set<String> parse(String query) {
        return queryStringParser.parse(query).usedParameterNames();
    }
}
