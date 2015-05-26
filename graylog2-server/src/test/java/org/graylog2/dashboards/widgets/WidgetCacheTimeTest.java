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

package org.graylog2.dashboards.widgets;

import com.github.joschi.jadconfig.util.Duration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WidgetCacheTimeTest {

    @Test
    public void test() throws Exception {
        assertEquals(10, getCacheTime(Duration.seconds(10), 0));
        assertEquals(1, getCacheTime(Duration.seconds(10), 1));
        assertEquals(2, getCacheTime(Duration.seconds(10), 2));
    }

    private int getCacheTime(Duration defaultCacheTime, int cacheTime) {
        return new WidgetCacheTime(defaultCacheTime, cacheTime).getCacheTime();
    }
}