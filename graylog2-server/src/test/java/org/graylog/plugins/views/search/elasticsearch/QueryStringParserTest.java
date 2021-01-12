/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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

    private Set<String> parse(String query) {
        return queryStringParser.parse(query).usedParameterNames();
    }
}
