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
        assertThat(UrlWhitelist.createEnabled(ImmutableList.of(new LiteralWhitelistEntry("a", "foo", null)))
                .isWhitelisted(".foo")).isFalse();
        assertThat(UrlWhitelist.createEnabled(ImmutableList.of(new RegexWhitelistEntry("b", "foo", null)))
                .isWhitelisted(".foo")).isTrue();
        assertThat(UrlWhitelist.createEnabled(ImmutableList.of(new RegexWhitelistEntry("c", "^foo$", null)))
                .isWhitelisted(".foo")).isFalse();
        assertThat(UrlWhitelist.createEnabled(ImmutableList.of(new LiteralWhitelistEntry("d", ".foo", null)))
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
                ImmutableList.of(new LiteralWhitelistEntry("a", "https://www.graylog.com", null),
                        new RegexWhitelistEntry("b", "https://www\\.graylog\\.com/.*", "regex test title"));
        UrlWhitelist orig = UrlWhitelist.createEnabled(entries);
        String json = objectMapper.writeValueAsString(orig);
        UrlWhitelist read = objectMapper.readValue(json, UrlWhitelist.class);
        assertThat(read).isEqualTo(orig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateIds() {
        UrlWhitelist.createEnabled(ImmutableList.of(new LiteralWhitelistEntry("a", "a", "a"),
                new RegexWhitelistEntry("a", "b", "b")));
    }
}
