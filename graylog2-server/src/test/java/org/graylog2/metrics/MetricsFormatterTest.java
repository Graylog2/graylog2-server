/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.metrics;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MetricsFormatterTest {

    @Test
    public void testBuildStreamMetricName() throws Exception {
        MetricsFormatter f = new MetricsFormatter();
        Map<String, String> names = Maps.newHashMap();
        names.put("something", "foobar");

        names.put("123", "zomg 9001 $.& test");
        assertEquals("zomg9001test", f.buildStreamMetricName("123", names));

        names.put("789", "som$e-thing");
        assertEquals("some-thing", f.buildStreamMetricName("789", names));

        names.put("111", "");
        assertEquals("noname-111", f.buildStreamMetricName("111", names));

        names.put("222", null);
        assertEquals("noname-222", f.buildStreamMetricName("222", names));

        assertEquals("noname-999", f.buildStreamMetricName("999", names));

        assertEquals("noname-333", f.buildStreamMetricName("333", new HashMap<String, String>()));
    }

    @Test
    public void testBuildHostMetricName() throws Exception {
        MetricsFormatter f = new MetricsFormatter();

        assertEquals("noname", f.buildHostMetricName(null));
        assertEquals("noname", f.buildHostMetricName(""));
        assertEquals("foo-bar-example-org", f.buildHostMetricName("foo.bar.example.org"));
        assertEquals("foo-barexample-org", f.buildHostMetricName("foo.b$ar example.org"));
        assertEquals("foo-bar-host", f.buildHostMetricName("foo-bar-host"));

    }

}
