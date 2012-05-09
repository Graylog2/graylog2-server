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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.json.simple.JSONValue;

/**
 * LibratoMetricsFormatter.java: 08.05.2012 19:39:39
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class LibratoMetricsFormatter {

    private static final Logger LOG = Logger.getLogger(LibratoMetricsFormatter.class);

    private static final String SOURCE = "graylog2";

    private MessageCounter counter;
    List<String> streamFilter;
    String hostFilter;
    String prefix;

    public LibratoMetricsFormatter (MessageCounter counter, String prefix, List<String> streamFilter, String hostFilter) {
        this.counter = counter;
        this.streamFilter = streamFilter;
        this.hostFilter = hostFilter;
        this.prefix = prefix;
    }
    
    /*
     * Example:
     * 
     * {
     *   "gauges": [
     *     {
     *       "value": 200,
     *       "source": "graylog2",
     *       "name": "gl2-total"
     *     },
     *     {
     *       "value": 50,
     *       "source": "graylog2",
     *       "name": "gl2-stream-4f60e84e54f0ba1a8d000003"
     *     }
     *   ]
     * }
     */
    public String asJson() {
        Map<String, Object> m = Maps.newHashMap();
        List<Map<String, Object>> gauges = Lists.newArrayList();

        // Overall
        Map<String, Object> overall = Maps.newHashMap();
        overall.put("value", counter.getTotalCount());
        overall.put("source", SOURCE);
        overall.put("name", name("total"));
        gauges.add(overall);

        // Streams.
        for(Entry<String, Integer> stream : counter.getStreamCounts().entrySet()) {
            if (streamFilter.contains(stream.getKey())) {
                LOG.debug("Not sending stream <" + stream.getKey() + "> to Librato Metrics because it is listed in libratometrics_stream_filter");
                continue;
            }

            Map<String, Object> s = Maps.newHashMap();
            s.put("value", stream.getValue());
            s.put("source", SOURCE);
            s.put("name", name("stream-" + stream.getKey()));
            gauges.add(s);
        }

        // Hosts.
        for(Entry<String, Integer> host : counter.getHostCounts().entrySet()) {
            if (Tools.decodeBase64(host.getKey()).matches(hostFilter)) {
                LOG.debug("Not sending host <" + host.getKey() + "> to Librato Metrics because it was matched by libratometrics_host_filter");
                continue;
            }

            Map<String, Object> h = Maps.newHashMap();
            h.put("value", host.getValue());
            h.put("source", SOURCE);
            h.put("name", name("host-" + Tools.decodeBase64(host.getKey()).replaceAll("[^a-zA-Z0-9]", "")));
            gauges.add(h);
        }

        m.put("gauges", gauges);

        return JSONValue.toJSONString(m);
    }

    private String name(String name) {
        return prefix + "-" + name;
    }

}
