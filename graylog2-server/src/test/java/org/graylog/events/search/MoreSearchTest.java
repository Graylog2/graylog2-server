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
package org.graylog.events.search;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MoreSearchTest {
    @Test
    public void buildStreamFilter() {
        final String filter1 = MoreSearch.buildStreamFilter(ImmutableSet.of("stream-1", "stream-2", "stream-3"));
        final String filter2 = MoreSearch.buildStreamFilter(ImmutableSet.of(" stream-1 ", "stream-2"));
        final String filter3 = MoreSearch.buildStreamFilter(ImmutableSet.of("stream-1"));

        assertThat(filter1).isEqualTo("streams:stream-1 OR streams:stream-2 OR streams:stream-3");
        assertThat(filter2).isEqualTo("streams:stream-1 OR streams:stream-2");
        assertThat(filter3).isEqualTo("streams:stream-1");

        assertThatThrownBy(() -> MoreSearch.buildStreamFilter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
        assertThatThrownBy(() -> MoreSearch.buildStreamFilter(ImmutableSet.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }
}