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
package org.graylog2.plugin.indexer.searches.timeranges;

import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbsoluteRangeTest {
    @Test
    public void testStringParse() throws Exception {
        final AbsoluteRange range1 = AbsoluteRange.create("2016-03-24T00:00:00.000Z", "2016-03-24T23:59:59.000Z");

        assertThat(range1.from().toString(ISODateTimeFormat.dateTime()))
                .isEqualTo("2016-03-24T00:00:00.000Z");
        assertThat(range1.to().toString(ISODateTimeFormat.dateTime()))
                .isEqualTo("2016-03-24T23:59:59.000Z");

        final AbsoluteRange range2 = AbsoluteRange.create("2016-03-24T00:00:00.000+09:00", "2016-03-24T23:59:59.000+09:00");

        // Check that time zone is kept while parsing.
        assertThat(range2.from().toString(ISODateTimeFormat.dateTime()))
                .isEqualTo("2016-03-24T00:00:00.000+09:00");
        assertThat(range2.to().toString(ISODateTimeFormat.dateTime()))
                .isEqualTo("2016-03-24T23:59:59.000+09:00");
    }
}