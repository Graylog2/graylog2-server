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
package org.graylog.plugins.views.search.searchtypes.pivot.series;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class PercentileTest {

    @Test
    public void testLiteral() {
        final Percentile percentile1 = Percentile.builder()
                .percentile(25.0)
                .field("cloverfield")
                .id("dead-beef")
                .build();
        assertThat(percentile1.literal()).isEqualTo("percentile(cloverfield,25.0)");

        final Percentile percentile2 = Percentile.builder()
                .percentile(99.0)
                .field("nostromo")
                .id("dead-beef")
                .build();
        assertThat(percentile2.literal()).isEqualTo("percentile(nostromo,99.0)");
    }
}
