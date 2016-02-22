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
package org.graylog2.indexer.searches;

import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchesConfigTest {

    @Test
    public void defaultLimit() throws InvalidRangeParametersException {
        final SearchesConfig config = SearchesConfig.builder()
                .query("")
                .range(RelativeRange.create(5))
                .limit(0)
                .offset(0)
                .build();

        assertEquals("Limit should default", SearchesConfig.DEFAULT_LIMIT, config.limit());
    }

    @Test
    public void negativeLimit() throws InvalidRangeParametersException {
        final SearchesConfig config = SearchesConfig.builder()
                .query("")
                .range(RelativeRange.create(5))
                .limit(-100)
                .offset(0)
                .build();

        assertEquals("Limit should default", SearchesConfig.DEFAULT_LIMIT, config.limit());
    }

    @Test
    public void explicitLimit() throws InvalidRangeParametersException {
        final SearchesConfig config = SearchesConfig.builder()
                .query("")
                .range(RelativeRange.create(5))
                .limit(23)
                .offset(0)
                .build();

        assertEquals("Limit should not default", 23, config.limit());
    }

}