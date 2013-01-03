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
import java.util.Map.Entry;

import org.graylog2.plugin.Counter;
import org.graylog2.plugin.MessageCounter;
import org.graylog2.plugin.Tools;

import com.google.common.collect.Lists;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GraphiteFormatter {

    private Integer timestamp = null;
    private MessageCounter counter;
    private String prefix;

    public GraphiteFormatter(MessageCounter counter, String prefix) {
        this(null, counter, prefix);
    }

    public GraphiteFormatter(Integer timestamp, MessageCounter counter, String prefix) {
        this.timestamp = timestamp;
        this.counter = counter;
        this.prefix = prefix;
    }

    public List<String> getAllMetrics() {
        List<String> r = Lists.newArrayList();

        int now = ((this.timestamp != null) ? this.timestamp.intValue() : Tools.getUTCTimestamp());

        // Overall count.
        String overall = prefix() + "total " + counter.getTotalCount() + " " + now;
        r.add(overall);

        // Streams.
        for(Entry<String, Counter> stream : counter.getStreamCounts().entrySet()) {
            String sval = prefix() + "streams." + stream.getKey() + " " + stream.getValue().get() + " " + now;
            r.add(sval);
        }

        // Hosts.
        for(Entry<String, Counter> host : counter.getHostCounts().entrySet()) {
            String hval = prefix() + "hosts." + Tools.decodeBase64(host.getKey()).replaceAll("[^a-zA-Z0-9\\.]", "") + " " + host.getValue().get() + " " + Tools.getUTCTimestamp();
            r.add(hval);
        }

        return r;
    }

    private String prefix() {
        return prefix + "." + "messagecounts" + ".";
    }

}
