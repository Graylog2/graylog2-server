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
package org.graylog2.restclient.models.api.responses;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldHistogramResponse extends HistogramResponse {

    public Map<String, Map<String, Object>> results;

    /**
     * [{ x: -1893456000, y: 92228531 }, { x: -1577923200, y: 106021568 }]
     *
     * @return A JSON map representation of the result, suitable for Rickshaw data graphing.
     */
    public List<Map<String, Object>> getFormattedResults(String value) {
        List<Map<String, Object>> points = Lists.newArrayList();

        for (Map.Entry<String, Map<String, Object>> result : results.entrySet()) {
            Map<String, Object> point = Maps.newHashMap();
            point.put("x", Long.parseLong(result.getKey()));
            point.put("y", result.getValue().get(value));

            points.add(point);
        }

        return points;
    }

}
