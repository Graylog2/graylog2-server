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

import org.graylog2.plugin.Tools;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.MessageCounter;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class LibratoMetricsFormatter {

    private static final Logger LOG = LoggerFactory.getLogger(LibratoMetricsFormatter.class);
    
    private MessageCounter counter;
    private List<String> streamFilter;
    private String hostFilter;
    private String source;
    private Map<String, String> streamNames;

    public LibratoMetricsFormatter (MessageCounter counter, String prefix, List<String> streamFilter, String hostFilter, Map<String, String> streamNames) {
        this.counter = counter;
        this.streamFilter = streamFilter;
        this.hostFilter = hostFilter;
        this.source = prefix + "graylog2-server";
        this.streamNames = streamNames;
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
        overall.put("source", source);
        overall.put("name", "gl2-total");
        gauges.add(overall);
        
        // Streams.
        for(Entry<String, Integer> stream : counter.getStreamCounts().entrySet()) {
            if (streamFilter.contains(stream.getKey())) {
                LOG.debug("Not sending stream <{}> to Librato Metrics because it is listed in libratometrics_stream_filter", stream.getKey());
                continue;
            }

            Map<String, Object> s = Maps.newHashMap();
            s.put("value", stream.getValue());
            s.put("source", source);
            s.put("name", "gl2-stream-" + buildStreamMetricName(stream.getKey()));
            gauges.add(s);
        }

        // Hosts.
        for(Entry<String, Integer> host : counter.getHostCounts().entrySet()) {
            if (Tools.decodeBase64(host.getKey()).matches(hostFilter)) {
                LOG.debug("Not sending host <{}> to Librato Metrics because it was matched by libratometrics_host_filter", host.getKey());
                continue;
            }

            Map<String, Object> h = Maps.newHashMap();
            h.put("value", host.getValue());
            h.put("source", source);
            h.put("name", "gl2-host-" + Tools.decodeBase64(host.getKey()).replaceAll("[^a-zA-Z0-9]", ""));
            gauges.add(h);
        }

        m.put("gauges", gauges);

        return JSONValue.toJSONString(m);
    }

    private String buildStreamMetricName(String streamId) {
        if (!streamNames.containsKey(streamId) || streamNames.get(streamId) == null || streamNames.get(streamId).isEmpty()) {
            return "noname-" + streamId;
        }
        
        return streamNames.get(streamId).toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
    }
    
}
