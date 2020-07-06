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
package org.graylog.testing.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

public class BulkIndexRequest {
    private final Map<String, List<Map<String, Object>>> requests = new HashMap<>();

    public Map<String, List<Map<String, Object>>> requests() {
        return requests;
    }

    public void addRequest(String index, Map<String, Object> source) {
        this.requests.putIfAbsent(index, new ArrayList<>());
        this.requests.compute(index, (indexName, requests) -> {
            final List<Map<String, Object>> newRequests = firstNonNull(requests, new ArrayList<>());
            newRequests.add(source);
            return newRequests;
        });
    }
}
