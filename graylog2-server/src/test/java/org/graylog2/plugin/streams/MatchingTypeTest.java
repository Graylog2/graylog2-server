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
package org.graylog2.plugin.streams;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MatchingTypeTest {

    @Test
    public void testValueOfOrDefault() throws Exception {
        assertEquals(Stream.MatchingType.AND, Stream.MatchingType.valueOfOrDefault("AND"));
        assertEquals(Stream.MatchingType.OR, Stream.MatchingType.valueOfOrDefault("OR"));
        assertEquals(Stream.MatchingType.DEFAULT, Stream.MatchingType.valueOfOrDefault(null));
        assertEquals(Stream.MatchingType.DEFAULT, Stream.MatchingType.valueOfOrDefault(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfOrDefaultThrowsExceptionForUnknownEnumName() {
        Stream.MatchingType.valueOfOrDefault("FOO");
    }
}