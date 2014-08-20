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

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MetricsFormatter {

    public String buildStreamMetricName(String streamId, Map<String, String> streamNames) {
        if (!streamNames.containsKey(streamId) || streamNames.get(streamId) == null || streamNames.get(streamId).isEmpty()) {
            return "noname-" + streamId;
        }
        
        return streamNames.get(streamId).toLowerCase().replaceAll("[^a-zA-Z0-9\\-]", "");
    }

    public String buildHostMetricName(String hostname) {
        if (hostname == null || hostname.isEmpty()) {
            return "noname";
        }

        // First replace all dots with dashes.
        hostname = hostname.replaceAll("\\.", "-");

        return hostname.replaceAll("[^a-zA-Z0-9\\-]", "");
    }
    
}
