/**
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
 */
package org.graylog2.metrics;

import org.graylog2.plugin.Tools;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GraphiteFormatter extends MetricsFormatter {

    //private final MessageCounter counter;
    private final String prefix;
    private final Map<String, String> streamNames;
    
    public GraphiteFormatter(String prefix, Map<String, String> streamNames) {
        //this.counter = counter;
        this.prefix = prefix;
        this.streamNames = streamNames;
    }

    public List<String> getAllMetrics() {
        List<String> r = Lists.newArrayList();

        // TODO

        /*int now = Tools.getUTCTimestamp();

        // Overall count.
        String overall = prefix() + "total " + counter.getTotalCount() + " " + now;
        r.add(overall);

        // Streams.
        for(Entry<String, Integer> stream : counter.getStreamCounts().entrySet()) {
            String sval = prefix() + "streams." + buildStreamMetricName(stream.getKey(), streamNames) + " " + stream.getValue() + " " + now;
            r.add(sval);
        }

        // Hosts.
        for(Entry<String, Integer> host : counter.getSourceCounts().entrySet()) {
            String hval = prefix() + "hosts." + buildHostMetricName(Tools.decodeBase64(host.getKey())) + " " + host.getValue() + " " + now;
            r.add(hval);
        }*/

        return r;
    }
    
    private String prefix() {
        return prefix + "." + "messagecounts" + ".";
    }

}
