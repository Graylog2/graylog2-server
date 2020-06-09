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
package org.graylog.plugins.views.search;

import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public class TestData {
    public static Map<String, PluginMetadataSummary> requirementsMap(String... requirementNames) {
        final Map<String, PluginMetadataSummary> requirements = new HashMap<>();

        for (String req : requirementNames)
            requirements.put(req, PluginMetadataSummary.create("", req, "", URI.create("www.affenmann.info"), "6.6.6", ""));

        return requirements;
    }

    public static Query.Builder validQueryBuilder() {
        return Query.builder().id(UUID.randomUUID().toString()).timerange(mock(TimeRange.class)).query(new BackendQuery.Fallback());
    }
}
