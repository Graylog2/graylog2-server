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

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlWhitelistTest {

    @Test
    public void isWhitelisted() {
        assertThat(UrlWhitelist.create(ImmutableList.of(new LiteralWhitelistEntry("foo")))
                .isWhitelisted(".foo")).isFalse();
        assertThat(UrlWhitelist.create(ImmutableList.of(new RegexWhitelistEntry("foo")))
                .isWhitelisted(".foo")).isTrue();
        assertThat(UrlWhitelist.create(ImmutableList.of(new RegexWhitelistEntry("^foo$")))
                .isWhitelisted(".foo")).isFalse();
        assertThat(UrlWhitelist.create(ImmutableList.of(new LiteralWhitelistEntry(".foo")))
                .isWhitelisted(".foo")).isTrue();
    }
}
