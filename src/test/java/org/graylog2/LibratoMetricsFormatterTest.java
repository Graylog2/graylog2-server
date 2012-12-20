/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.Maps;
import java.util.Map;
import org.json.simple.JSONValue;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class LibratoMetricsFormatterTest {

    @Test
    public void testAsJson() {
        MessageCounterImpl counter = new MessageCounterImpl();

        // Total: 2
        counter.incrementTotal();
        counter.incrementTotal();

        // Host foo.example.org: 3
        counter.incrementHost("foo.example.org");
        counter.incrementHost("foo.example.org");
        counter.incrementHost("foo.example.org");

        // Host bar.example.org: 1
        counter.incrementHost("bar.example.org");

        // Stream id1: 2
        ObjectId id1 = new ObjectId();
        counter.incrementStream(id1);
        counter.incrementStream(id1);

        // Stream id2: 1
        ObjectId id2 = new ObjectId();
        counter.incrementStream(id2);

        LibratoMetricsFormatter f = new LibratoMetricsFormatter(counter, "gl2-", new ArrayList<String>(), "");

        Map<String, Map<String,Object>> gauges = parseGauges(f.asJson());

        assertEquals(5, gauges.size());
        
        assertEquals("gl2-graylog2-server", gauges.get("gl2-total").get("source"));
        assertEquals((long) 2, gauges.get("gl2-total").get("value"));
        assertEquals((long) 3, gauges.get("gl2-host-fooexampleorg").get("value"));
        assertEquals((long) 1, gauges.get("gl2-host-barexampleorg").get("value"));
        assertEquals((long) 2, gauges.get("gl2-stream-" + id1.toString()).get("value"));
        assertEquals((long) 1, gauges.get("gl2-stream-" + id2.toString()).get("value"));
    }

    @Test
    public void testAsJsonWithEmptyCounter() {
        MessageCounterImpl counter = new MessageCounterImpl();
        LibratoMetricsFormatter f = new LibratoMetricsFormatter(counter, "gl2-", new ArrayList<String>(), "");

        Map<String, Map<String,Object>> gauges = parseGauges(f.asJson());

        assertEquals(1, gauges.size());
        assertEquals((long) 0, gauges.get("gl2-total").get("value"));
    }

    @Test
    public void testAsJsonWithConfiguredStreamFilter() {
        MessageCounterImpl counter = new MessageCounterImpl();

        // Total: 2
        counter.incrementTotal();
        counter.incrementTotal();

        // Host foo.example.org: 3
        counter.incrementHost("foo.example.org");
        counter.incrementHost("foo.example.org");
        counter.incrementHost("foo.example.org");

        // Host bar.example.org: 1
        counter.incrementHost("bar.example.org");

        // Stream id1: 2
        ObjectId id1 = new ObjectId();
        counter.incrementStream(id1);
        counter.incrementStream(id1);

        // Stream id2: 1
        ObjectId id2 = new ObjectId();
        counter.incrementStream(id2);

        // Stream id3: 1
        ObjectId id3 = new ObjectId();
        counter.incrementStream(id3);

        List<String> streamFilter = new ArrayList<String>();
        streamFilter.add(id1.toString());
        streamFilter.add(id3.toString());
        streamFilter.add(new ObjectId().toString());

        LibratoMetricsFormatter f = new LibratoMetricsFormatter(counter, "gl2-", streamFilter, "");

        Map<String, Map<String,Object>> gauges = parseGauges(f.asJson());

        assertEquals(4, gauges.size());

        assertEquals("gl2-graylog2-server", gauges.get("gl2-total").get("source"));
        assertEquals((long) 2, gauges.get("gl2-total").get("value"));
        assertEquals((long) 3, gauges.get("gl2-host-fooexampleorg").get("value"));
        assertEquals((long) 1, gauges.get("gl2-host-barexampleorg").get("value"));
        assertEquals((long) 1, gauges.get("gl2-stream-" + id2.toString()).get("value"));
    }

    @Test
    public void testAsJsonWithConfiguredHostFilter() {
        MessageCounterImpl counter = new MessageCounterImpl();

        // Total: 2
        counter.incrementTotal();
        counter.incrementTotal();

        // Host foo.example.org: 3
        counter.incrementHost("foo.example.org");
        counter.incrementHost("foo.example.org");
        counter.incrementHost("foo.example.org");

        // Host bar.example.org: 1
        counter.incrementHost("bar.example.org");

        // Host bar.lolwut.example.org: 1
        counter.incrementHost("bar.lolwut.example.org");

        // Stream id1: 2
        ObjectId id1 = new ObjectId();
        counter.incrementStream(id1);
        counter.incrementStream(id1);

        // Stream id2: 1
        ObjectId id2 = new ObjectId();
        counter.incrementStream(id2);

        String hostFilter = "^bar.*\\.example.org$";

        LibratoMetricsFormatter f = new LibratoMetricsFormatter(counter, "gl2-", new ArrayList<String>(), hostFilter);

        Map<String, Map<String,Object>> gauges = parseGauges(f.asJson());

        assertEquals(4, gauges.size());

        assertEquals("gl2-graylog2-server", gauges.get("gl2-total").get("source"));
        assertEquals((long) 2, gauges.get("gl2-total").get("value"));
        assertEquals((long) 3, gauges.get("gl2-host-fooexampleorg").get("value"));
        assertEquals((long) 2, gauges.get("gl2-stream-" + id1.toString()).get("value"));
        assertEquals((long) 1, gauges.get("gl2-stream-" + id2.toString()).get("value"));
    }

    private Map<String, Map<String,Object>> parseGauges(String json) {
        Map<String, Map<String,Object>> result = Maps.newHashMap();

        JSONObject r = (JSONObject) JSONValue.parse(json);
        JSONArray gauges = (JSONArray) r.get("gauges");

        for (Object o : gauges) {
            JSONObject jsonObject = (JSONObject) o;
            Map<String, Object> gauge = Maps.newHashMap();
            gauge.put("source", jsonObject.get("source"));
            gauge.put("name", jsonObject.get("name"));
            gauge.put("value", jsonObject.get("value"));
            result.put((String) jsonObject.get("name"), gauge);
        }

        return result;
    }

}