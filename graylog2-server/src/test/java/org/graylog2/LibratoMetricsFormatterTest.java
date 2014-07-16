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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.metrics.LibratoMetricsFormatter;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

public class LibratoMetricsFormatterTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testAsJson() throws IOException {

        // TODO

        /*MessageCounterImpl counter = new MessageCounterImpl();

        // Total: 2
        counter.incrementTotal();
        counter.incrementTotal();

        // Host foo.example.org: 3
        counter.incrementSource("foo.example.org");
        counter.incrementSource("foo.example.org");
        counter.incrementSource("foo.example.org");

        // Host bar.example.org: 1
        counter.incrementSource("bar.example.org");

        Map<String, String> fakeStreamNames = Maps.newHashMap();
        
        // Stream id1: 2
        ObjectId id1 = new ObjectId();
        fakeStreamNames.put(id1.toString(), "lol-stream1");
        counter.incrementStream(id1);
        counter.incrementStream(id1);

        // Stream id2: 1
        ObjectId id2 = new ObjectId();
        fakeStreamNames.put(id2.toString(), "lolano$therSTREAM");
        counter.incrementStream(id2);

        LibratoMetricsFormatter f = new LibratoMetricsFormatter(counter, "gl2-", new ArrayList<String>(), "", fakeStreamNames);

        Map<String, Map<String,Object>> gauges = parseGauges(f.asJson());

        assertEquals(5, gauges.size());

        assertEquals("gl2-graylog2-server", gauges.get("gl2-total").get("source"));
        assertEquals((long) 2, gauges.get("gl2-total").get("value"));
        assertEquals((long) 3, gauges.get("gl2-host-foo-example-org").get("value"));
        assertEquals((long) 1, gauges.get("gl2-host-bar-example-org").get("value"));
        assertEquals((long) 2, gauges.get("gl2-stream-lol-stream1").get("value"));
        assertEquals((long) 1, gauges.get("gl2-stream-lolanotherstream").get("value"));*/
    }

    @Test
    public void testAsJsonWithEmptyCounter() throws IOException {
        /*MessageCounterImpl counter = new MessageCounterImpl();
        LibratoMetricsFormatter f = new LibratoMetricsFormatter(counter, "gl2-", new ArrayList<String>(), "", new HashMap<String, String>());

        Map<String, Map<String,Object>> gauges = parseGauges(f.asJson());

        assertEquals(1, gauges.size());
        assertEquals((long) 0, gauges.get("gl2-total").get("value"));*/
    }

    @Test
    public void testAsJsonWithConfiguredStreamFilter() throws IOException {
        /*MessageCounterImpl counter = new MessageCounterImpl();

        // Total: 2
        counter.incrementTotal();
        counter.incrementTotal();

        // Host foo.example.org: 3
        counter.incrementSource("foo.example.org");
        counter.incrementSource("foo.example.org");
        counter.incrementSource("foo.example.org");

        // Host bar.example.org: 1
        counter.incrementSource("bar.example.org");

        Map<String, String> fakeStreamNames = Maps.newHashMap();
        
        // Stream id1: 2
        ObjectId id1 = new ObjectId();
        fakeStreamNames.put(id1.toString(), "some_stream");
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

        LibratoMetricsFormatter f = new LibratoMetricsFormatter(counter, "gl2-", streamFilter, "", fakeStreamNames);

        Map<String, Map<String,Object>> gauges = parseGauges(f.asJson());

        assertEquals(4, gauges.size());

        assertEquals("gl2-graylog2-server", gauges.get("gl2-total").get("source"));
        assertEquals((long) 2, gauges.get("gl2-total").get("value"));
        assertEquals((long) 3, gauges.get("gl2-host-foo-example-org").get("value"));
        assertEquals((long) 1, gauges.get("gl2-host-bar-example-org").get("value"));
        assertEquals((long) 1, gauges.get("gl2-stream-noname-" + id2.toString()).get("value"));*/
    }

    @Test
    public void testAsJsonWithConfiguredHostFilter() throws IOException {
        /*MessageCounterImpl counter = new MessageCounterImpl();

        // Total: 2
        counter.incrementTotal();
        counter.incrementTotal();

        // Host foo.example.org: 3
        counter.incrementSource("foo.example.org");
        counter.incrementSource("foo.example.org");
        counter.incrementSource("foo.example.org");

        // Host bar.example.org: 1
        counter.incrementSource("bar.example.org");

        // Host bar.lolwut.example.org: 1
        counter.incrementSource("bar.lolwut.example.org");

        Map<String, String> fakeStreamNames = Maps.newHashMap();
        
        // Stream id1: 2
        ObjectId id1 = new ObjectId();
        fakeStreamNames.put(id1.toString(), "some_stream");
        counter.incrementStream(id1);
        counter.incrementStream(id1);

        // Stream id2: 1
        ObjectId id2 = new ObjectId();
        fakeStreamNames.put(id2.toString(), " some_stream__ ___2 ");
        counter.incrementStream(id2);

        String hostFilter = "^bar.*\\.example.org$";

        LibratoMetricsFormatter f = new LibratoMetricsFormatter(counter, "gl2-", new ArrayList<String>(), hostFilter, fakeStreamNames);

        Map<String, Map<String,Object>> gauges = parseGauges(f.asJson());

        assertEquals(4, gauges.size());

        assertEquals("gl2-graylog2-server", gauges.get("gl2-total").get("source"));
        assertEquals(2L, gauges.get("gl2-total").get("value"));
        assertEquals(3L, gauges.get("gl2-host-foo-example-org").get("value"));
        assertEquals(2L, gauges.get("gl2-stream-somestream").get("value"));
        assertEquals(1L, gauges.get("gl2-stream-somestream2").get("value"));*/
    }

    private Map<String, Map<String,Object>> parseGauges(String json) throws IOException {
        Map<String, Map<String,Object>> result = Maps.newHashMap();

        JsonNode userData = objectMapper.readTree(json);
        JsonNode gauges = userData.get("gauges");

        for (JsonNode node : gauges) {
            Map<String, Object> gauge = Maps.newHashMap();
            gauge.put("source", node.get("source").asText());
            gauge.put("name", node.get("name").asText());
            gauge.put("value", node.get("value").asLong());
            result.put(node.get("name").asText(), gauge);
        }

        return result;
    }
}